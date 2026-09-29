package com.example.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time audio synthesizer using Android AudioTrack with optimizations for low latency.
 * This is the foundation for Oboe integration when available.
 */
class RealTimeAudioSynthesizer(private val sampleRate: Int = 44100) {
    private val frameSize = 2048
    private val bufferPoolSize = 4
    private val bufferPool = ArrayDeque<ShortArray>(bufferPoolSize)
    private val activeSynthVoices = LinkedHashMap<String, SynthVoice>()
    private val voiceLock = Any()
    private val drainQueue = ArrayDeque<DrainNote>()

    init {
        repeat(bufferPoolSize) {
            bufferPool.add(ShortArray(frameSize * 2))
        }
    }

    data class SynthVoice(
        val id: String,
        var frequency: Double,
        var amplitude: Double,
        var phase: Double = 0.0,
        val attackMs: Float,
        val decayMs: Float,
        val sustainLevel: Float,
        val releaseMs: Float,
        var timeMs: Double = 0.0,
        var isReleasing: Boolean = false,
        var releaseStartMs: Double = 0.0
    )

    data class DrainNote(
        val voiceId: String,
        val timestamp: Long = System.nanoTime()
    )

    fun noteOn(id: String, midi: Int, params: NoteParams) {
        val frequency = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
        val voice = SynthVoice(
            id = id,
            frequency = frequency,
            amplitude = params.velocity.toDouble().coerceIn(0.0, 1.0),
            attackMs = params.attackMs.coerceAtLeast(0.5f),
            decayMs = params.decayMs.coerceAtLeast(1f),
            sustainLevel = params.sustainLevel.coerceIn(0f, 1f),
            releaseMs = params.releaseMs.coerceAtLeast(10f)
        )
        synchronized(voiceLock) {
            activeSynthVoices[id] = voice
        }
    }

    fun noteOff(id: String) {
        synchronized(voiceLock) {
            activeSynthVoices[id]?.let { voice ->
                voice.isReleasing = true
                voice.releaseStartMs = voice.timeMs
            }
        }
    }

    fun allNotesOff() {
        synchronized(voiceLock) {
            activeSynthVoices.values.forEach { it.isReleasing = true }
        }
    }

    fun getNextAudioBuffer(): ShortArray? {
        val buffer = bufferPool.removeFirstOrNull() ?: ShortArray(frameSize * 2)
        renderFrame(buffer)
        return buffer
    }

    fun releaseAudioBuffer(buffer: ShortArray) {
        if (bufferPool.size < bufferPoolSize) {
            buffer.fill(0)
            bufferPool.add(buffer)
        }
    }

    private fun renderFrame(buffer: ShortArray) {
        val secondsPerSample = 1.0 / sampleRate
        val maxAmplitude = 32767.0

        synchronized(voiceLock) {
            val iterator = activeSynthVoices.iterator()
            while (iterator.hasNext()) {
                val (_, voice) = iterator.next()

                for (i in 0 until (frameSize * 2) step 2) {
                    val envelope = computeEnvelope(voice)
                    if (envelope <= 0.0 && voice.isReleasing) {
                        iterator.remove()
                        break
                    }

                    val sampleValue = generateWaveform(voice) * envelope * voice.amplitude
                    val sample = (sampleValue * maxAmplitude).toInt().toShort()

                    buffer[i] = (buffer[i].toInt() + sample.toInt()).coerceIn(-32768, 32767).toShort()
                    buffer[i + 1] = buffer[i]

                    voice.timeMs += secondsPerSample * 1000.0
                    voice.phase += 2.0 * PI * voice.frequency / sampleRate
                }
            }
        }
    }

    private fun generateWaveform(voice: SynthVoice): Double {
        val phase = voice.phase % (2.0 * PI)
        return sin(phase).coerceIn(-1.0, 1.0)
    }

    private fun computeEnvelope(voice: SynthVoice): Double {
        val nowMs = voice.timeMs
        val attackT = voice.attackMs.coerceAtLeast(0.5f)
        val decayT = voice.decayMs.coerceAtLeast(1f)
        val releaseT = voice.releaseMs.coerceAtLeast(1f)

        return if (voice.isReleasing) {
            val releaseMs = (nowMs - voice.releaseStartMs).coerceAtLeast(0.0)
            (voice.sustainLevel.toDouble() * (1.0 - (releaseMs / releaseT).coerceIn(0.0, 1.0))).coerceIn(0.0, 1.0)
        } else {
            when {
                nowMs < attackT -> (nowMs / attackT).coerceIn(0.0, 1.0)
                nowMs < attackT + decayT -> (1.0 - ((nowMs - attackT) / decayT) * (1.0 - voice.sustainLevel)).coerceIn(voice.sustainLevel.toDouble(), 1.0)
                else -> voice.sustainLevel.toDouble()
            }
        }
    }

    data class NoteParams(
        val velocity: Float = 0.8f,
        val attackMs: Float = 10f,
        val decayMs: Float = 300f,
        val sustainLevel: Float = 0.7f,
        val releaseMs: Float = 350f
    )
}
