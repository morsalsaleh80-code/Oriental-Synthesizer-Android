package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Pa4xDisplayTab {
    PERFORMANCE,
    STYLE_SELECT,
    SOUND_SELECT,
    QUARTER_TONE,
    MIXER_FX,
    SET_EXPLORER
}

class Pa4xViewModel : ViewModel() {
    val audioEngine = Pa4xLowLatencyAudioEngine()
    val sequencer = Pa4xArrangerSequencer(audioEngine as Any, viewModelScope)

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

    private val _masterVolume = MutableStateFlow(0.85f)
    val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    val splitPointMidi = 48

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val factorySet = Pa4xSetParser.getFactorySets().firstOrNull()
                if (factorySet != null) {
                    _currentSet.value = factorySet
                    _currentStyle.value = factorySet.styles.firstOrNull()
                    val sounds = factorySet.sounds.ifEmpty { Pa4xSetParser.getStandardPa4xSounds() }
                    _upper1Sound.value = sounds.getOrNull(0)
                    _upper2Sound.value = sounds.getOrNull(1)
                    _lowerSound.value = sounds.getOrNull(2)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        audioEngine.start()
    }

    fun setTab(tab: Pa4xDisplayTab) {
        _currentTab.value = tab
    }

    fun selectSts(stsId: Int) {
        _activeSts.value = stsId
    }

    fun triggerPad(padId: Int) {
        try {
            audioEngine.triggerDrum(Pa4xLowLatencyAudioEngine.DrumType.KICK, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onKeyPressed(midi: Int) {
        try {
            val updated = _activeMidiNotes.value.toMutableSet().apply { add(midi) }
            _activeMidiNotes.value = updated
            val sound = _upper1Sound.value ?: Pa4xSetParser.getStandardPa4xSounds().firstOrNull() ?: return
            audioEngine.noteOn("KEYBOARD", midi, sound, 0.85f)
            sequencer.onKeysPressed(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onKeyReleased(midi: Int) {
        try {
            val updated = _activeMidiNotes.value.toMutableSet().apply { remove(midi) }
            _activeMidiNotes.value = updated
            audioEngine.noteOff("KEYBOARD", midi)
            sequencer.onKeysPressed(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importSet(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _importStatus.value = "Importing..."
                val importedSet = Pa4xBinaryParser.parseSetFromUri(context, uri)
                if (importedSet != null) {
                    _currentSet.value = importedSet
                    _currentStyle.value = importedSet.styles.firstOrNull()
                    val sounds = importedSet.sounds.ifEmpty { Pa4xSetParser.getStandardPa4xSounds() }
                    _upper1Sound.value = sounds.getOrNull(0)
                    _upper2Sound.value = sounds.getOrNull(1)
                    _lowerSound.value = sounds.getOrNull(2)
                    _importStatus.value = "Imported: ${importedSet.name}"
                } else {
                    _importStatus.value = "Import failed"
                }
            } catch (e: Exception) {
                _importStatus.value = "Error: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun setJoystick(x: Float, y: Float) {
        _joystickX.value = x.coerceIn(-1f, 1f)
        _joystickY.value = y.coerceIn(0f, 1f)
    }

    fun setMasterVolume(volume: Float) {
        _masterVolume.value = volume.coerceIn(0f, 1f)
        audioEngine.setMasterVolume(volume)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            sequencer.stop()
            audioEngine.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
