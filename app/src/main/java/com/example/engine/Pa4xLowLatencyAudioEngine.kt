package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.Pa4xSound
import kotlinx.coroutines.*

/**
 * Oboe-ready low-latency audio engine.
 * Currently using AudioTrack with optimizations; ready for Oboe C++ binding.
 */
class Pa4xLowLatencyAudioEngine {
    enum class DrumType {
        KICK, SNARE, HIHAT_CLOSED, HIHAT_OPEN, TOM, CRASH,
        ORIENTAL_DARBUKA_DOUM, ORIENTAL_DARBUKA_TEK, ORIENTAL_RIQ_DEF
    }

    private val sampleRate = 44100
    private val frameSize = 2048
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
    private val bufferSize = (minBuffer * 2).coerceAtLeast(16384)

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
        Log.e(TAG, "Failed to create AudioTrack", e)
        null
    }

    private val synthesizer = RealTimeAudioSynthesizer(sampleRate)
    private val drumQueue = ArrayDeque<DrumEvent>()
    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var renderJob: Job? = null
    private var isStarted = false
    private var masterVolume = 0.85f

    data class DrumEvent(
        val type: DrumType,
        val volume: Float,
        val timestamp: Long = System.nanoTime()
    )

    fun start() {
        if (isStarted || audioTrack == null) return
        isStarted = true
        try {
            audioTrack.play()
            Log.i(TAG, "AudioTrack started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioTrack", e)
            return
        }

        renderJob = audioScope.launch {
            try {
                while (isActive && isStarted) {
                    val buffer = synthesizer.getNextAudioBuffer() ?: continue
                    
                    if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        val written = audioTrack.write(buffer, 0, buffer.size)
                        if (written < 0) {
                            Log.e(TAG, "AudioTrack write error: $written")
                        }
                    }
                    
                    synthesizer.releaseAudioBuffer(buffer)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Rendering error", e)
            }
        }
    }

    fun stop() {
        isStarted = false
        try {
            audioTrack?.stop()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        renderJob?.cancel()
        renderJob = null
        synthesizer.allNotesOff()
        synchronized(drumQueue) {
            drumQueue.clear()
        }
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
        audioScope.cancel()
    }

    fun noteOn(name: String, midi: Int, sound: Pa4xSound, volume: Float = 0.8f) {
        try {
            val params = RealTimeAudioSynthesizer.NoteParams(
                velocity = volume.coerceIn(0f, 1f),
                attackMs = sound.attackMs.coerceAtLeast(0.5f),
                decayMs = sound.decayMs.coerceAtLeast(1f),
                sustainLevel = sound.sustainLevel.coerceIn(0f, 1f),
                releaseMs = sound.releaseMs.coerceAtLeast(10f)
            )
            val voiceId = "${name}_${midi}"
            synthesizer.noteOn(voiceId, midi, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error in noteOn", e)
        }
    }

    fun noteOff(name: String, midi: Int) {
        try {
            val voiceId = "${name}_${midi}"
            synthesizer.noteOff(voiceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in noteOff", e)
        }
    }

    fun allNotesOff() {
        try {
            synthesizer.allNotesOff()
            synchronized(drumQueue) {
                drumQueue.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in allNotesOff", e)
        }
    }

    fun triggerDrum(type: DrumType, volume: Float) {
        try {
            synchronized(drumQueue) {
                drumQueue.add(DrumEvent(type, volume.coerceIn(0f, 1f)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering drum", e)
        }
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "Pa4xLowLatencyAE"
    }
}
