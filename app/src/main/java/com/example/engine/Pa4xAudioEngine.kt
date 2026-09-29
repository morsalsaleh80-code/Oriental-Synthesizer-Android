package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.ChordInfo
import com.example.model.Pa4xSound
import com.example.model.SoundCategory
import com.example.model.VariationType
import com.example.model.WaveformType
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class Pa4xAudioEngine {
    enum class DrumType {
        KICK, SNARE, HIHAT_CLOSED, HIHAT_OPEN, TOM, CRASH,
        ORIENTAL_DARBUKA_DOUM, ORIENTAL_DARBUKA_TEK, ORIENTAL_RIQ_DEF
    }

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
    private val audioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(encoding)
            .setChannelMask(channelConfig)
            .build(),
        minBuffer.coerceAtLeast(2048),
        AudioTrack.MODE_STREAM,
        0
    )

    private val noteStates = linkedMapOf<String, MutableMap<Int, NoteVoice>>()
    private val drumEvents = ArrayDeque<DrumEvent>()
    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var renderJob: Job? = null
    private var isStarted = false

    data class NoteVoice(
        var frequency: Double,
        var time: Double,
        var amplitude: Double,
        var phase: Double,
        var attackMs: Float,
        var decayMs: Float,
        var sustainLevel: Float,
        var releaseMs: Float,
        var waveform: WaveformType,
        var noteName: String
    )

    data class DrumEvent(
        val type: DrumType,
        val volume: Float,
        val timestamp: Long = System.nanoTime()
    )

    fun start() {
        if (isStarted) return
        isStarted = true
        audioTrack.play()
        renderJob = audioScope.launch {
            val frameSize = 2 * 2
            val buffer = ShortArray(minBuffer / frameSize)
            while (isActive && isStarted) {
                renderFrame(buffer)
                audioTrack.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        isStarted = false
        audioTrack.stop()
        audioTrack.flush()
        renderJob?.cancel()
        noteStates.clear()
        drumEvents.clear()
    }

    fun release() {
        stop()
        audioTrack.release()
        audioScope.cancel()
    }

    fun noteOn(name: String, midi: Int, sound: Pa4xSound, volume: Float = 0.8f) {
        val freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
        val voice = NoteVoice(
            frequency = freq,
            time = 0.0,
            amplitude = volume.toDouble(),
            phase = 0.0,
            attackMs = sound.attackMs,
            decayMs = sound.decayMs,
            sustainLevel = sound.sustainLevel,
            releaseMs = sound.releaseMs,
            waveform = sound.waveformType,
            noteName = name
        )
        noteStates.getOrPut(name) { linkedMapOf() }[midi] = voice
    }

    fun noteOff(name: String, midi: Int) {
        noteStates[name]?.remove(midi)
    }

    fun allNotesOff() {
        noteStates.clear()
        drumEvents.clear()
    }

    fun triggerDrum(type: DrumType, volume: Float) {
        drumEvents.add(DrumEvent(type, volume))
    }

    fun triggerPatternDrum(type: VariationType, step: Int, beat: Int) {
        when (type) {
            VariationType.VAR_1 -> {
                if (step == 0 || step == 8) triggerDrum(DrumType.KICK, 0.9f)
                if (step == 4 || step == 12) triggerDrum(DrumType.SNARE, 0.75f)
                if (step % 2 == 0) triggerDrum(DrumType.HIHAT_CLOSED, 0.5f)
            }
            else -> {}
        }
    }

    private fun renderFrame(buffer: ShortArray) {
        val secondsPerSample = 1.0 / sampleRate
        val maxAmplitude = 32767

        for (i in buffer.indices) {
            var sampleLeft = 0.0
            var sampleRight = 0.0

            for ((noteName, voices) in noteStates) {
                val iterator = voices.values.iterator()
                while (iterator.hasNext()) {
                    val voice = iterator.next()
                    val envelope = computeEnvelope(voice)
                    val tone = when (voice.waveform) {
                        WaveformType.SINE -> sin(2.0 * PI * voice.frequency * (i.toDouble() / sampleRate))
                        WaveformType.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(2.0 * PI * voice.frequency * (i.toDouble() / sampleRate)))
                        WaveformType.SAW -> 2.0 * ((voice.frequency * (i.toDouble() / sampleRate)) % 1.0) - 1.0
                        WaveformType.SQUARE -> if (sin(2.0 * PI * voice.frequency * (i.toDouble() / sampleRate)) >= 0) 1.0 else -1.0
                        else -> sin(2.0 * PI * voice.frequency * (i.toDouble() / sampleRate))
                    }
                    val smoothed = tone * envelope
                    sampleLeft += smoothed * 0.25
                    sampleRight += smoothed * 0.25
                    voice.time += secondsPerSample
                    voice.phase += 2.0 * PI * voice.frequency / sampleRate
                }
            }

            while (drumEvents.isNotEmpty()) {
                val event = drumEvents.removeFirst()
                val trig = drumTone(event.type, event.volume)
                sampleLeft += trig * 0.7
                sampleRight += trig * 0.7
            }

            val clampedLeft = (sampleLeft * maxAmplitude).coerceIn(-32768.0, 32767.0).toInt().toShort()
            val clampedRight = (sampleRight * maxAmplitude).coerceIn(-32768.0, 32767.0).toInt().toShort()
            val idx = i * 2
            buffer[idx] = clampedLeft
            buffer[idx + 1] = clampedRight
        }
    }

    private fun computeEnvelope(voice: NoteVoice): Double {
        val nowMs = voice.time * 1000.0
        val attackT = voice.attackMs.coerceAtLeast(1f)
        val decayT = voice.decayMs.coerceAtLeast(1f)
        val releaseT = voice.releaseMs.coerceAtLeast(1f)

        return when {
            nowMs < attackT -> (nowMs / attackT).toDouble()
            nowMs < attackT + decayT -> (1.0 - ((nowMs - attackT) / decayT) * (1.0 - voice.sustainLevel))
            else -> voice.sustainLevel.toDouble() * (1.0 - ((nowMs - (attackT + decayT)) / releaseT).coerceIn(0.0, 1.0))
        }.coerceIn(0.0, 1.0)
    }

    private fun drumTone(type: DrumType, volume: Float): Double {
        val t = System.nanoTime() / 1_000_000_000.0
        return when (type) {
            DrumType.KICK -> sin(2.0 * PI * 45.0 * t) * (1.0 / (1.0 + t * 0.08)) * volume
            DrumType.SNARE -> (sin(2.0 * PI * 180.0 * t) + kotlin.math.sin(2.0 * PI * 320.0 * t) * 0.5) * volume * 0.35
            DrumType.HIHAT_CLOSED -> (sin(2.0 * PI * 7000.0 * t) + sin(2.0 * PI * 9000.0 * t) * 0.5) * volume * 0.2
            DrumType.HIHAT_OPEN -> (sin(2.0 * PI * 4000.0 * t) + sin(2.0 * PI * 5200.0 * t) * 0.5) * volume * 0.25
            DrumType.TOM -> sin(2.0 * PI * 120.0 * t) * volume * 0.5
            DrumType.CRASH -> sin(2.0 * PI * 1500.0 * t) * volume * 0.35
            DrumType.ORIENTAL_DARBUKA_DOUM -> sin(2.0 * PI * 80.0 * t) * volume * 0.85
            DrumType.ORIENTAL_DARBUKA_TEK -> sin(2.0 * PI * 140.0 * t) * volume * 0.75
            DrumType.ORIENTAL_RIQ_DEF -> sin(2.0 * PI * 220.0 * t) * volume * 0.6
        }
    }
}
