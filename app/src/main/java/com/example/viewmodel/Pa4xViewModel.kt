package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.engine.Pa4xArrangerSequencer
import com.example.engine.Pa4xAudioEngine
import com.example.engine.Pa4xSetParser
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Pa4xDisplayTab {
    PERFORMANCE,
    STYLE_SELECT,
    SOUND_SELECT,
    QUARTER_TONE,
    MIXER_FX,
    SET_EXPLORER
}

class Pa4xViewModel : ViewModel() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val audioEngine = Pa4xAudioEngine()
    val sequencer = Pa4xArrangerSequencer(audioEngine, appScope)

    private val _currentTab = MutableStateFlow(Pa4xDisplayTab.PERFORMANCE)
    val currentTab: StateFlow<Pa4xDisplayTab> = _currentTab.asStateFlow()

    private val _currentSet = MutableStateFlow<Pa4xSet?>(null)
    val currentSet: StateFlow<Pa4xSet?> = _currentSet.asStateFlow()

    private val _currentStyle = MutableStateFlow<Pa4xStyle?>(null)
    val currentStyle: StateFlow<Pa4xStyle?> = _currentStyle.asStateFlow()

    private val _activeSts = MutableStateFlow(1)
    val activeSts: StateFlow<Int> = _activeSts.asStateFlow()

    private val _upper1Sound = MutableStateFlow<Pa4xSound?>(null)
    val upper1Sound: StateFlow<Pa4xSound?> = _upper1Sound.asStateFlow()

    private val _upper2Sound = MutableStateFlow<Pa4xSound?>(null)
    val upper2Sound: StateFlow<Pa4xSound?> = _upper2Sound.asStateFlow()

    private val _lowerSound = MutableStateFlow<Pa4xSound?>(null)
    val lowerSound: StateFlow<Pa4xSound?> = _lowerSound.asStateFlow()

    private val _activeMidiNotes = MutableStateFlow(setOf<Int>())
    val activeMidiNotes: StateFlow<Set<Int>> = _activeMidiNotes.asStateFlow()

    private val _octaveShift = MutableStateFlow(0)
    val octaveShift: StateFlow<Int> = _octaveShift.asStateFlow()

    private val _pitchTranspose = MutableStateFlow(0)
    val pitchTranspose: StateFlow<Int> = _pitchTranspose.asStateFlow()

    private val _joystickX = MutableStateFlow(0f)
    val joystickX: StateFlow<Float> = _joystickX.asStateFlow()

    private val _joystickY = MutableStateFlow(0f)
    val joystickY: StateFlow<Float> = _joystickY.asStateFlow()

    val splitPointMidi = 48

    init {
        val factorySet = Pa4xSetParser.getFactorySets().first()
        _currentSet.value = factorySet
        _currentStyle.value = factorySet.styles.firstOrNull()
        val firstSound = factorySet.sounds.firstOrNull()
        _upper1Sound.value = firstSound
        _upper2Sound.value = factorySet.sounds.getOrNull(1)
        _lowerSound.value = factorySet.sounds.getOrNull(2)
        sequencer.start()
        audioEngine.start()
    }

    fun setTab(tab: Pa4xDisplayTab) {
        _currentTab.value = tab
    }

    fun selectSts(stsId: Int) {
        _activeSts.value = stsId
    }

    fun triggerPad(padId: Int) {
        val set = _currentSet.value
        val sound = set?.pads?.firstOrNull { it.id == padId }
        if (sound != null) {
            audioEngine.triggerDrum(Pa4xAudioEngine.DrumType.KICK, 1.0f)
        }
    }

    fun onKeyPressed(midi: Int) {
        val updated = _activeMidiNotes.value.toMutableSet().apply { add(midi) }
        _activeMidiNotes.value = updated
        val sound = _upper1Sound.value ?: Pa4xSetParser.getStandardPa4xSounds().first()
        audioEngine.noteOn("KEYBOARD", midi, sound, 0.9f)
        sequencer.onKeysPressed(updated)
    }

    fun onKeyReleased(midi: Int) {
        val updated = _activeMidiNotes.value.toMutableSet().apply { remove(midi) }
        _activeMidiNotes.value = updated
        audioEngine.noteOff("KEYBOARD", midi)
        sequencer.onKeysPressed(updated)
    }

    fun setJoystick(x: Float, y: Float) {
        _joystickX.value = x.coerceIn(-1f, 1f)
        _joystickY.value = y.coerceIn(0f, 1f)
    }

    override fun onCleared() {
        super.onCleared()
        sequencer.stop()
        audioEngine.release()
    }
}
