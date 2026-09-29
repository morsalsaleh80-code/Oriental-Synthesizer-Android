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
    private val frameSize = 2048
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
    private val bufferSize = (minBuffer * 1.5).toInt().coerceAtLeast(8192)

    private val audioTrack: AudioTrack? = try {
        AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .setChannelMask(channelConfig)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            0
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private val noteStates = linkedMapOf<String, MutableMap<Int, NoteVoice>>()
    private val drumEvents = ArrayDeque<DrumEvent>()
    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var renderJob: Job? = null
    private var isStarted = false
    private var masterVolume = 0.85f

    data class NoteVoice(
        var frequency: Double,
        var time: Double = 0.0,
        var amplitude: Double,
        var phase: Double = 0.0,
        val attackMs: Float,
        val decayMs: Float,
        val sustainLevel: Float,
        val releaseMs: Float,
        val waveform: WaveformType,
        val noteName: String,
        var isReleasing: Boolean = false,
        var releaseStartTime: Double = 0.0
    )

    data class DrumEvent(
        val type: DrumType,
        val volume: Float,
        val timestamp: Long = System.nanoTime(),
        var played: Boolean = false
    )

    fun start() {
        if (isStarted || audioTrack == null) return
        isStarted = true
        try {
            audioTrack.play()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        renderJob = audioScope.launch {
            val buffer = ShortArray(frameSize * 2)
            try {
                while (isActive && isStarted) {
                    renderAudioFrame(buffer)
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.write(buffer, 0, buffer.size)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        isStarted = false
        try {
            audioTrack?.stop()
            audioTrack?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        renderJob?.cancel()
        renderJob = null
        noteStates.clear()
        drumEvents.clear()
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioScope.cancel()
    }

    fun noteOn(name: String, midi: Int, sound: Pa4xSound, volume: Float = 0.8f) {
        val freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
        val voice = NoteVoice(
            frequency = freq,
            time = 0.0,
            amplitude = volume.toDouble().coerceIn(0.0, 1.0),
            phase = 0.0,
            attackMs = sound.attackMs.coerceAtLeast(1f),
            decayMs = sound.decayMs.coerceAtLeast(1f),
            sustainLevel = sound.sustainLevel.coerceIn(0f, 1f),
            releaseMs = sound.releaseMs.coerceAtLeast(1f),
            waveform = sound.waveformType,
            noteName = name
        )
        synchronized(noteStates) {
            noteStates.getOrPut(name) { linkedMapOf() }[midi] = voice
        }
    }

    fun noteOff(name: String, midi: Int) {
        synchronized(noteStates) {
            noteStates[name]?.let { voices ->
                voices[midi]?.let { voice ->
                    voice.isReleasing = true
                    voice.releaseStartTime = voice.time
                }
            }
        }
    }

    fun allNotesOff() {
        synchronized(noteStates) {
            noteStates.clear()
        }
        synchronized(drumEvents) {
            drumEvents.clear()
        }
    }

    fun triggerDrum(type: DrumType, volume: Float) {
        synchronized(drumEvents) {
            drumEvents.add(DrumEvent(type, volume.coerceIn(0f, 1f)))
        }
    }

    private fun renderAudioFrame(buffer: ShortArray) {
        val secondsPerSample = 1.0 / sampleRate
        val maxAmplitude = 32767.0

        for (i in 0 until (frameSize * 2) step 2) {
            var sampleLeft = 0.0
            var sampleRight = 0.0

            synchronized(noteStates) {
                for ((noteName, voices) in noteStates) {
                    val iterator = voices.iterator()
                    while (iterator.hasNext()) {
                        val (_, voice) = iterator.next()
                        val envelope = computeEnvelope(voice)
                        if (envelope <= 0.0 && voice.isReleasing) {
                            iterator.remove()
                            continue
                        }
                        val tone = generateWaveform(voice, i.toDouble() / sampleRate)
                        val smoothed = tone * envelope * voice.amplitude
                        sampleLeft += smoothed * 0.25
                        sampleRight += smoothed * 0.25
                        voice.time += secondsPerSample
                        voice.phase += 2.0 * PI * voice.frequency / sampleRate
                    }
                }
            }

            synchronized(drumEvents) {
                val iterator = drumEvents.iterator()
                while (iterator.hasNext()) {
                    val event = iterator.next()
                    if (!event.played) {
                        val trig = drumTone(event.type, event.volume)
                        sampleLeft += trig * 0.5
                        sampleRight += trig * 0.5
                        event.played = true
                    }
                    if (event.timestamp + 100_000_000 < System.nanoTime()) {
                        iterator.remove()
                    }
                }
            }

            val clampedLeft = (sampleLeft * maxAmplitude * masterVolume).coerceIn(-32768.0, 32767.0).toInt().toShort()
            val clampedRight = (sampleRight * maxAmplitude * masterVolume).coerceIn(-32768.0, 32767.0).toInt().toShort()
            buffer[i] = clampedLeft
            buffer[i + 1] = clampedRight
        }
    }

    private fun generateWaveform(voice: NoteVoice, sampleTime: Double): Double {
        val phase = voice.phase % (2.0 * PI)
        return when (voice.waveform) {
            WaveformType.SINE -> sin(phase)
            WaveformType.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase))
            WaveformType.SAW -> 2.0 * ((voice.frequency * sampleTime) % 1.0) - 1.0
            WaveformType.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
            else -> sin(phase)
        }.coerceIn(-1.0, 1.0)
    }

    private fun computeEnvelope(voice: NoteVoice): Double {
        val nowMs = voice.time * 1000.0
        val attackT = voice.attackMs.coerceAtLeast(0.5f)
        val decayT = voice.decayMs.coerceAtLeast(1f)
        val releaseT = voice.releaseMs.coerceAtLeast(1f)

        return if (voice.isReleasing) {
            val releaseMs = (voice.time - voice.releaseStartTime) * 1000.0
            (voice.sustainLevel.toDouble() * (1.0 - (releaseMs / releaseT).coerceIn(0.0, 1.0))).coerceIn(0.0, 1.0)
        } else {
            when {
                nowMs < attackT -> (nowMs / attackT).coerceIn(0.0, 1.0)
                nowMs < attackT + decayT -> (1.0 - ((nowMs - attackT) / decayT) * (1.0 - voice.sustainLevel)).coerceIn(voice.sustainLevel.toDouble(), 1.0)
                else -> voice.sustainLevel.toDouble()
            }
        }
    }

    private fun drumTone(type: DrumType, volume: Float): Double {
        val t = (System.nanoTime() % 100_000_000) / 1_000_000_000.0
        val decay = 1.0 / (1.0 + t * 8.0)
        return when (type) {
            DrumType.KICK -> sin(2.0 * PI * 50.0 * t) * decay * volume * 0.8
            DrumType.SNARE -> (sin(2.0 * PI * 200.0 * t) + sin(2.0 * PI * 400.0 * t) * 0.6) * decay * volume * 0.5
            DrumType.HIHAT_CLOSED -> (sin(2.0 * PI * 8000.0 * t) + sin(2.0 * PI * 10000.0 * t) * 0.7) * decay * volume * 0.3
            DrumType.HIHAT_OPEN -> (sin(2.0 * PI * 5000.0 * t) + sin(2.0 * PI * 7000.0 * t) * 0.6) * decay * volume * 0.35
            DrumType.TOM -> sin(2.0 * PI * 150.0 * t) * decay * volume * 0.6
            DrumType.CRASH -> sin(2.0 * PI * 2000.0 * t) * decay * volume * 0.4
            DrumType.ORIENTAL_DARBUKA_DOUM -> sin(2.0 * PI * 90.0 * t) * decay * volume * 0.85
            DrumType.ORIENTAL_DARBUKA_TEK -> sin(2.0 * PI * 160.0 * t) * decay * volume * 0.75
            DrumType.ORIENTAL_RIQ_DEF -> (sin(2.0 * PI * 250.0 * t) + sin(2.0 * PI * 380.0 * t) * 0.5) * decay * volume * 0.65
        }
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }
}
