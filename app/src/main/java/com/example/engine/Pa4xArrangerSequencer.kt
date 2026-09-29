package com.example.engine

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Pa4xArrangerSequencer(
    private val audioEngine: Pa4xAudioEngine,
    private val scope: CoroutineScope
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentVariation = MutableStateFlow(VariationType.VAR_1)
    val currentVariation: StateFlow<VariationType> = _currentVariation.asStateFlow()

    private val _currentTempo = MutableStateFlow(128)
    val currentTempo: StateFlow<Int> = _currentTempo.asStateFlow()

    private val _currentBeat = MutableStateFlow(1)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private val _currentChord = MutableStateFlow(ChordInfo(0, ChordType.MAJOR, "C Maj"))
    val currentChord: StateFlow<ChordInfo> = _currentChord.asStateFlow()

    var synchroStart = false
    var synchroStop = false
    var memoryHold = true
    var autoFillOnVarChange = true

    private var queuedVariation: VariationType? = null
    private var activeStyle: Pa4xStyle? = null
    private var sequencerJob: Job? = null
    private val tapTimestamps = mutableListOf<Long>()

    private val bassSound = Pa4xSound(
        id = "acc_bass",
        name = "Acoustic Finger Bass",
        category = SoundCategory.BASS_GUITAR,
        waveformType = WaveformType.TRIANGLE,
        attackMs = 5f, decayMs = 400f, sustainLevel = 0.6f, releaseMs = 250f
    )

    private val chordSound = Pa4xSound(
        id = "acc_chord",
        name = "Stereo Strings & Piano",
        category = SoundCategory.STRINGS_ORCH,
        waveformType = WaveformType.PCM_STRINGS,
        attackMs = 30f, decayMs = 500f, sustainLevel = 0.5f, releaseMs = 300f
    )

    fun setStyle(style: Pa4xStyle) {
        activeStyle = style
        _currentTempo.value = style.defaultTempo
    }

    fun setTempo(tempo: Int) {
        _currentTempo.value = tempo.coerceIn(40, 280)
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        tapTimestamps.add(now)
        if (tapTimestamps.size > 4) tapTimestamps.removeAt(0)
        if (tapTimestamps.size >= 2) {
            var sum = 0L
            for (i in 1 until tapTimestamps.size) sum += (tapTimestamps[i] - tapTimestamps[i - 1])
            val avg = sum / (tapTimestamps.size - 1)
            if (avg > 150) _currentTempo.value = (60000 / avg).toInt().coerceIn(40, 260)
        }
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        startSequencerLoop()
    }

    fun stop() {
        _isPlaying.value = false
        sequencerJob?.cancel()
        sequencerJob = null
        _currentBeat.value = 1
        audioEngine.noteOff("ACC_BASS", 36)
        audioEngine.noteOff("ACC_CHORD", 60)
    }

    fun toggleStartStop() {
        if (_isPlaying.value) stop() else start()
    }

    fun selectVariation(type: VariationType) {
        if (!_isPlaying.value) {
            _currentVariation.value = type
            return
        }
        if (autoFillOnVarChange && type.name.startsWith("VAR_")) {
            val fill = when (type) {
                VariationType.VAR_1 -> VariationType.FILL_1
                VariationType.VAR_2 -> VariationType.FILL_2
                VariationType.VAR_3 -> VariationType.FILL_3
                VariationType.VAR_4 -> VariationType.FILL_4
                else -> VariationType.FILL_1
            }
            _currentVariation.value = fill
            queuedVariation = type
        } else {
            queuedVariation = type
        }
    }

    fun onKeysPressed(pressedMidiNotes: Set<Int>) {
        val rootMidi = pressedMidiNotes.minOrNull() ?: 60
        val rootNoteIndex = rootMidi % 12
        val rootName = getNoteName(rootNoteIndex)
        val pitchClasses = pressedMidiNotes.map { (it % 12 - rootNoteIndex + 12) % 12 }.toSet()
        val chordType = when {
            pitchClasses.contains(3) && pitchClasses.contains(10) -> ChordType.MINOR_SEVENTH
            pitchClasses.contains(4) && pitchClasses.contains(10) -> ChordType.SEVENTH
            pitchClasses.contains(4) && pitchClasses.contains(11) -> ChordType.MAJOR_SEVENTH
            pitchClasses.contains(3) && pitchClasses.contains(6) -> ChordType.DIMINISHED
            pitchClasses.contains(5) -> ChordType.SUSPENDED_4
            pitchClasses.contains(3) -> ChordType.MINOR
            else -> ChordType.MAJOR
        }
        _currentChord.value = ChordInfo(rootNoteIndex, chordType, "$rootName ${chordType.symbol}")
    }

    private fun getNoteName(semitone: Int): String {
        return arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")[semitone % 12]
    }

    private fun startSequencerLoop() {
        sequencerJob?.cancel()
        sequencerJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && _isPlaying.value) {
                val bpm = _currentTempo.value
                val stepDurationMs = (60000L / (bpm * 4))
                _currentBeat.value = (step / 4) + 1
                playSequencerStep(step, _currentVariation.value)
                step++
                if (step >= 16) {
                    step = 0
                    if (_currentVariation.value.name.startsWith("FILL_")) {
                        _currentVariation.value = queuedVariation ?: VariationType.VAR_1
                        queuedVariation = null
                    } else if (queuedVariation != null) {
                        _currentVariation.value = queuedVariation!!
                        queuedVariation = null
                    } else if (_currentVariation.value.name.startsWith("INTRO_")) {
                        _currentVariation.value = VariationType.VAR_1
                    } else if (_currentVariation.value.name.startsWith("ENDING_")) {
                        stop()
                        break
                    }
                }
                delay(stepDurationMs)
            }
        }
    }

    private fun playSequencerStep(step: Int, variation: VariationType) {
        val chord = _currentChord.value
        val root = chord.rootNote
        val intervals = chord.chordType.intervals

        when (variation) {
            VariationType.VAR_1 -> {
                if (step == 0 || step == 8) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.KICK, 0.9f)
                if (step == 4 || step == 12) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.SNARE, 0.75f)
                if (step % 2 == 0) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.HIHAT_CLOSED, 0.5f)
                if (step == 6 || step == 14) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_TEK, 0.6f)
                if (step == 0) audioEngine.noteOn("ACC_BASS", 36 + root, bassSound, 0.85f)
                if (step == 8) audioEngine.noteOn("ACC_BASS", 36 + root + 7, bassSound, 0.75f)
                if (step == 0) intervals.forEach { audioEngine.noteOn("ACC_CHORD", 60 + root + it, chordSound, 0.6f) }
            }
            VariationType.VAR_2 -> {
                if (step == 0 || step == 6 || step == 10) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.KICK, 0.95f)
                if (step == 4 || step == 12) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.SNARE, 0.85f)
                if (step % 2 == 0) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_RIQ_DEF, 0.55f)
                if (step == 2 || step == 8 || step == 14) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_DOUM, 0.85f)
                if (step == 5 || step == 11 || step == 15) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_TEK, 0.75f)
                if (step == 0) audioEngine.noteOn("ACC_BASS", 36 + root, bassSound, 0.9f)
                if (step == 6) audioEngine.noteOn("ACC_BASS", 36 + root + 3, bassSound, 0.8f)
                if (step == 10) audioEngine.noteOn("ACC_BASS", 36 + root + 7, bassSound, 0.85f)
                if (step == 4 || step == 12) intervals.forEach { audioEngine.noteOn("ACC_CHORD", 60 + root + it, chordSound, 0.7f) }
            }
            VariationType.VAR_3 -> {
                if (step == 0 || step == 3 || step == 8 || step == 11) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_DOUM, 1.0f)
                if (step == 2 || step == 5 || step == 7 || step == 10 || step == 14) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_TEK, 0.85f)
                if (step == 4 || step == 12) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.SNARE, 0.9f)
                if (step % 2 == 0) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_RIQ_DEF, 0.65f)
                if (step == 0) audioEngine.noteOn("ACC_BASS", 36 + root, bassSound, 0.95f)
                if (step == 8) audioEngine.noteOn("ACC_BASS", 36 + root + 12, bassSound, 0.9f)
                if (step % 4 == 0) intervals.forEach { audioEngine.noteOn("ACC_CHORD", 60 + root + it, chordSound, 0.75f) }
            }
            VariationType.VAR_4 -> {
                if (step % 4 == 0) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.KICK, 1.0f)
                if (step == 4 || step == 12) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.SNARE, 1.0f)
                if (step % 2 == 1) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.HIHAT_OPEN, 0.65f)
                if (step % 4 == 2) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_DOUM, 0.95f)
                audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.ORIENTAL_DARBUKA_TEK, 0.75f)
                if (step % 4 == 0) audioEngine.noteOn("ACC_BASS", 36 + root, bassSound, 1.0f)
                if (step == 0 || step == 6) intervals.forEach { audioEngine.noteOn("ACC_CHORD", 60 + root + it, chordSound, 0.8f) }
            }
            VariationType.FILL_1, VariationType.FILL_2, VariationType.FILL_3, VariationType.FILL_4 -> {
                if (step < 8) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.SNARE, 0.8f)
                else if (step < 12) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.TOM, 0.9f)
                else audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.CRASH, 1.0f)
            }
            VariationType.BREAK -> {
                if (step == 0) {
                    audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.CRASH, 1.0f)
                    audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.KICK, 1.0f)
                }
            }
            VariationType.INTRO_1, VariationType.INTRO_2, VariationType.INTRO_3 -> {
                if (step == 0) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.CRASH, 0.9f)
                if (step % 4 == 0) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.KICK, 0.9f)
            }
            VariationType.ENDING_1, VariationType.ENDING_2, VariationType.ENDING_3 -> {
                if (step == 12) audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.CRASH, 1.0f)
            }
        }
    }
}
