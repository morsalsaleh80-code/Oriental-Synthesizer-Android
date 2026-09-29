package com.example.model

enum class SoundCategory(val displayName: String, val iconName: String) {
    ORIENTAL("Oriental / شرقی", "Kanun"),
    PERSIAN_TRAD("Persian / سنتی", "Tar"),
    TURKISH_ARABIC("Turkish / Arabic", "Oud"),
    PIANO_KEYS("Piano & Keys", "Piano"),
    STRINGS_ORCH("Strings & Orch", "Violin"),
    BRASS_WINDS("Brass & Winds", "Sax"),
    SYNTH_LEAD("Synth Leads", "Synth"),
    ACCORDION("Accordion / Musette", "Accordion"),
    BASS_GUITAR("Bass & Guitar", "Guitar"),
    DRUMS_PERC("Drums & Percussion", "Drums")
}

data class Pa4xSound(
    val id: String,
    val name: String,
    val category: SoundCategory,
    val waveformType: WaveformType = WaveformType.SAW,
    val attackMs: Float = 10f,
    val decayMs: Float = 300f,
    val sustainLevel: Float = 0.7f,
    val releaseMs: Float = 350f,
    val filterCutoff: Float = 4500f,
    val resonance: Float = 1.2f,
    val vibratoRate: Float = 5.5f,
    val vibratoDepth: Float = 0.05f,
    val octaveOffset: Int = 0,
    val isOriental: Boolean = false,
    val pcmSampleName: String? = null
)

enum class WaveformType {
    SINE, TRIANGLE, SAW, SQUARE,
    PCM_PIANO, PCM_STRINGS, PCM_KANUN, PCM_OUD,
    PCM_NAY, PCM_TAR, PCM_ACCORDION, PCM_SAX, PCM_LEAD
}

enum class VariationType(val label: String, val shortLabel: String) {
    INTRO_1("Intro 1", "IN1"),
    INTRO_2("Intro 2", "IN2"),
    INTRO_3("Intro 3", "IN3"),
    VAR_1("Variation 1", "V1"),
    VAR_2("Variation 2", "V2"),
    VAR_3("Variation 3", "V3"),
    VAR_4("Variation 4", "V4"),
    FILL_1("Fill 1", "F1"),
    FILL_2("Fill 2", "F2"),
    FILL_3("Fill 3", "F3"),
    FILL_4("Fill 4", "F4"),
    BREAK("Break", "BRK"),
    ENDING_1("Ending 1", "ED1"),
    ENDING_2("Ending 2", "ED2"),
    ENDING_3("Ending 3", "ED3")
}

data class SingleTouchSetting(
    val id: Int,
    val name: String,
    val upper1Sound: Pa4xSound,
    val upper2Sound: Pa4xSound? = null,
    val upper3Sound: Pa4xSound? = null,
    val lowerSound: Pa4xSound? = null
)

data class StylePattern(
    val variation: VariationType,
    val bars: Int = 2,
    val drumSteps: List<DrumStep> = emptyList(),
    val bassSteps: List<BassStep> = emptyList(),
    val chordSteps: List<ChordStep> = emptyList()
)

data class DrumStep(
    val step: Int,
    val kick: Boolean = false,
    val snare: Boolean = false,
    val hihat: Boolean = false,
    val perc: Boolean = false,
    val crash: Boolean = false,
    val tom: Boolean = false,
    val accent: Float = 1.0f
)

data class BassStep(
    val step: Int,
    val degree: Int = 0,
    val durationSteps: Int = 2,
    val velocity: Float = 0.9f
)

data class ChordStep(
    val step: Int,
    val strumStyle: Int = 0,
    val durationSteps: Int = 4,
    val velocity: Float = 0.8f
)

data class Pa4xStyle(
    val id: String,
    val name: String,
    val category: String,
    val defaultTempo: Int = 120,
    val timeSignatureNumerator: Int = 4,
    val timeSignatureDenominator: Int = 4,
    val patterns: Map<VariationType, StylePattern> = emptyMap(),
    val stsList: List<SingleTouchSetting> = emptyList(),
    val sourceFile: String? = null
)

data class Pa4xPad(
    val id: Int,
    val name: String,
    val soundType: String,
    val isLoop: Boolean = false,
    val durationSeconds: Float = 1.5f
)

data class QuarterToneTuning(
    val name: String,
    val persianName: String,
    val semitoneOffsets: List<Int>
)

data class ChordInfo(
    val rootNote: Int,
    val chordType: ChordType,
    val name: String
)

enum class ChordType(val symbol: String, val intervals: List<Int>) {
    MAJOR("", listOf(0, 4, 7)),
    MINOR("m", listOf(0, 3, 7)),
    SEVENTH("7", listOf(0, 4, 7, 10)),
    MINOR_SEVENTH("m7", listOf(0, 3, 7, 10)),
    MAJOR_SEVENTH("Maj7", listOf(0, 4, 7, 11)),
    SUSPENDED_4("sus4", listOf(0, 5, 7)),
    DIMINISHED("dim", listOf(0, 3, 6)),
    ORIENTAL_BAYATI("Bayati", listOf(0, 3, 7))
}

data class Pa4xSet(
    val id: String,
    val name: String,
    val author: String = "KORG User",
    val description: String = "",
    val isFactory: Boolean = false,
    val dateCreated: String = "2026",
    val styles: List<Pa4xStyle> = emptyList(),
    val sounds: List<Pa4xSound> = emptyList(),
    val pads: List<Pa4xPad> = emptyList(),
    val tuningPresets: List<QuarterToneTuning> = emptyList(),
    val pcmSampleCount: Int = 0,
    val rawFilesFound: List<String> = emptyList()
)
