package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VariationType
import com.example.viewmodel.Pa4xDisplayTab
import com.example.viewmodel.Pa4xViewModel

@Composable
fun Pa4xTopBar(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    Surface(color = Color.Transparent, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarButton("PERF", Pa4xDisplayTab.PERFORMANCE) { viewModel.setTab(Pa4xDisplayTab.PERFORMANCE) }
            TopBarButton("STYLE", Pa4xDisplayTab.STYLE_SELECT) { viewModel.setTab(Pa4xDisplayTab.STYLE_SELECT) }
            TopBarButton("SOUND", Pa4xDisplayTab.SOUND_SELECT) { viewModel.setTab(Pa4xDisplayTab.SOUND_SELECT) }
            TopBarButton("TUNING", Pa4xDisplayTab.QUARTER_TONE) { viewModel.setTab(Pa4xDisplayTab.QUARTER_TONE) }
            TopBarButton("MIXER", Pa4xDisplayTab.MIXER_FX) { viewModel.setTab(Pa4xDisplayTab.MIXER_FX) }
            TopBarButton("SETS", Pa4xDisplayTab.SET_EXPLORER) { viewModel.setTab(Pa4xDisplayTab.SET_EXPLORER) }
        }
    }
}

@Composable
private fun TopBarButton(label: String, tab: Pa4xDisplayTab, onClick: () -> Unit) {
    val selected = false
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) KorgPa4xTheme.KorgAmberGold else Color(0xFF1E293B))
            .border(1.dp, KorgPa4xTheme.LcdBorderBlue, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KorgTouchviewMonitor(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    val currentSet by viewModel.currentSet.collectAsState()
    val currentStyle by viewModel.currentStyle.collectAsState()
    val currentChord by viewModel.sequencer.currentChord.collectAsState()
    val currentTempo by viewModel.sequencer.currentTempo.collectAsState()
    val currentVariation by viewModel.sequencer.currentVariation.collectAsState()
    val isPlaying by viewModel.sequencer.isPlaying.collectAsState()
    val upper1Sound by viewModel.upper1Sound.collectAsState()
    val upper2Sound by viewModel.upper2Sound.collectAsState()
    val lowerSound by viewModel.lowerSound.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3D4758), Color(0xFF1F2633), Color(0xFF0D1016))))
            .border(1.5.dp, Color(0xFF333D4D), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1324), Color(0xFF050B14), Color(0xFF02050A))))
                .border(1.dp, KorgPa4xTheme.LcdBorderBlue, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("STYLE PLAY", color = KorgPa4xTheme.KorgAmberGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text("SET: ${currentSet?.name ?: "Pa4X"}", color = Color(0xFF94A3B8), fontSize = 8.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B1424)).border(1.dp, KorgPa4xTheme.LcdBorderBlue, RoundedCornerShape(4.dp)).padding(8.dp)
                ) {
                    Column {
                        Text("STYLE / ریتم", color = KorgPa4xTheme.KorgCyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(currentStyle?.name ?: "Select Style", color = KorgPa4xTheme.KorgAmberGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("${currentStyle?.category ?: "Arranger"} • 4/4 Beat", color = Color(0xFF94A3B8), fontSize = 8.sp)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF08264D)).border(1.dp, Color(0xFF0284C7), RoundedCornerShape(4.dp)).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CHORD", color = KorgPa4xTheme.KorgCyanNeon, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text(currentChord.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier.weight(1.2f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B1424)).border(1.dp, KorgPa4xTheme.LcdBorderBlue, RoundedCornerShape(4.dp)).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TEMPO", color = Color(0xFF94A3B8), fontSize = 7.sp)
                        Text("${currentTempo}", color = KorgPa4xTheme.KorgAmberGold, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchTrackPill("UPPER 1", upper1Sound?.name ?: "Concert Grand", KorgPa4xTheme.KorgAmberGold, Modifier.weight(1.1f))
                TouchTrackPill("UPPER 2", upper2Sound?.name ?: "(Off)", KorgPa4xTheme.KorgCyanNeon, Modifier.weight(1f))
                TouchTrackPill("LOWER", lowerSound?.name ?: "(Off)", Color(0xFFA855F7), Modifier.weight(1f))
                Box(
                    modifier = Modifier.weight(1.2f).clip(RoundedCornerShape(3.dp)).background(Color(0xFF0B1424)).border(1.dp, KorgPa4xTheme.LcdBorderBlue, RoundedCornerShape(3.dp)).padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentVariation.label, color = if (isPlaying) KorgPa4xTheme.KorgLedGreen else Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun TouchTrackPill(label: String, soundName: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0B1424))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column {
            Text(label, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(soundName, color = Color.White, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
fun KorgPhysicalControlPanel(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    val isPlaying by viewModel.sequencer.isPlaying.collectAsState()
    val currentVariation by viewModel.sequencer.currentVariation.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()

    Surface(color = Color.Transparent, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPlaying) Brush.verticalGradient(listOf(Color(0xFF16A34A), Color(0xFF14532D))) else Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF7F1D1D))))
                        .border(1.2.dp, if (isPlaying) KorgPa4xTheme.KorgLedGreen else Color(0xFFFCA5A5), RoundedCornerShape(4.dp))
                        .clickable { viewModel.sequencer.toggleStartStop() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (isPlaying) KorgPa4xTheme.KorgLedGreen else Color(0xFFFCA5A5)))
                        Text(if (isPlaying) "STOP" else "START", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }

                HardwareLedTactileButton("SYNC START", false, KorgPa4xTheme.KorgLedRed) {}
                HardwareLedTactileButton("SYNC STOP", false, KorgPa4xTheme.KorgLedRed) {}
                HardwareLedTactileButton("MEMORY", true, KorgPa4xTheme.KorgAmberGold) {}

                HardwareVariationButton("VAR 1", currentVariation == VariationType.VAR_1, KorgPa4xTheme.KorgAmberGold) { viewModel.sequencer.selectVariation(VariationType.VAR_1) }
                HardwareVariationButton("VAR 2", currentVariation == VariationType.VAR_2, KorgPa4xTheme.KorgAmberGold) { viewModel.sequencer.selectVariation(VariationType.VAR_2) }
                HardwareVariationButton("VAR 3", currentVariation == VariationType.VAR_3, KorgPa4xTheme.KorgAmberGold) { viewModel.sequencer.selectVariation(VariationType.VAR_3) }
                HardwareVariationButton("VAR 4", currentVariation == VariationType.VAR_4, KorgPa4xTheme.KorgAmberGold) { viewModel.sequencer.selectVariation(VariationType.VAR_4) }
                HardwareVariationButton("FILL 1", currentVariation == VariationType.FILL_1, KorgPa4xTheme.KorgCyanNeon) { viewModel.sequencer.selectVariation(VariationType.FILL_1) }
                HardwareVariationButton("BREAK", currentVariation == VariationType.BREAK, Color(0xFFF43F5E)) { viewModel.sequencer.selectVariation(VariationType.BREAK) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (padId in 1..4) {
                        val padName = currentSet?.pads?.find { it.id == padId }?.name ?: "PAD"
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(KorgPa4xTheme.ButtonBodyTop)
                                .border(0.8.dp, KorgPa4xTheme.ButtonBorderLight, RoundedCornerShape(3.dp))
                                .clickable { viewModel.triggerPad(padId) }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PAD $padId: $padName", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareLedTactileButton(label: String, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) KorgPa4xTheme.ButtonPressedTop else KorgPa4xTheme.ButtonBodyTop)
            .border(0.9.dp, if (isActive) activeColor else KorgPa4xTheme.ButtonBorderLight, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isActive) activeColor else Color(0xFF331515)))
            Text(label, color = if (isActive) Color.White else Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HardwareVariationButton(label: String, isSelected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(if (isSelected) 2.dp else 3.dp, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) Brush.verticalGradient(listOf(Color(0xFF92400E), Color(0xFF451A03))) else Brush.verticalGradient(listOf(KorgPa4xTheme.ButtonBodyTop, KorgPa4xTheme.ButtonBodyBottom)))
            .border(if (isSelected) 1.2.dp else 0.8.dp, if (isSelected) activeColor else KorgPa4xTheme.ButtonBorderLight, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) activeColor else Color(0xFF404040)))
            Text(label, color = if (isSelected) Color.White else Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold)
        }
    }
}

@Composable
fun KorgHighDefKeyboardView(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    val activeNotes by viewModel.activeMidiNotes.collectAsState()
    val octaveShift by viewModel.octaveShift.collectAsState()
    val pitchTranspose by viewModel.pitchTranspose.collectAsState()
    val joystickX by viewModel.joystickX.collectAsState()
    val joystickY by viewModel.joystickY.collectAsState()

    val baseMidi = 48 + (octaveShift * 12) + pitchTranspose
    val numberOfKeysWhite = 17

    Surface(color = Color(0xFF090D14), modifier = modifier.fillMaxWidth().height(140.dp).border(1.dp, KorgPa4xTheme.ChassisBorder)) {
        Row(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            Box(
                modifier = Modifier.width(62.dp).fillMaxHeight().padding(end = 3.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF131A24)).border(0.8.dp, Color(0xFF2E3846), RoundedCornerShape(4.dp)).padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                    Text("JOYSTICK", color = KorgPa4xTheme.KorgAmberGold, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF0A0F16)).border(0.8.dp, Color(0xFF334155), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.offset(x = (joystickX * 12).dp, y = (-joystickY * 12).dp).size(20.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF64748B), Color(0xFF1E293B))).also { }).border(1.dp, Color(0xFFF59E0B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        }
                    }
                    Text("PITCH / MOD", color = Color(0xFF94A3B8), fontSize = 6.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Color(0xFF0F172A)).border(1.dp, Color(0xFF334155), RoundedCornerShape(3.dp))
            ) {
                val whiteKeyWidth = maxWidth / numberOfKeysWhite
                val blackKeyWidth = whiteKeyWidth * 0.68f
                val blackKeyHeight = maxHeight * 0.60f
                val whiteOffsets = listOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28)

                Row(modifier = Modifier.fillMaxSize()) {
                    whiteOffsets.forEach { semi ->
                        val midi = baseMidi + semi
                        val isPressed = activeNotes.contains(midi)
                        val isLowerSplit = midi < viewModel.splitPointMidi
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 0.5.dp)
                                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(if (isPressed) Color(0xFFBAE6FD) else if (isLowerSplit) Color(0xFFF1F5F9) else Color.White)
                                .border(0.8.dp, if (isPressed) Color(0xFF0284C7) else KorgPa4xTheme.IvoryWhiteKeyBorder, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                .clickable { viewModel.onKeyPressed(midi) }
                                .padding(bottom = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (midi % 12 == 0) {
                                    Text("C${(midi / 12) - 1}", color = if (isPressed) Color(0xFF0284C7) else Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                if (isLowerSplit) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                            }
                        }
                    }
                }

                val blackKeyPositions = listOf(0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10, 7 to 13, 8 to 15, 10 to 18, 11 to 20, 12 to 22, 14 to 25, 15 to 27)
                blackKeyPositions.forEach { (whiteIdx, semitoneOffset) ->
                    val midi = baseMidi + semitoneOffset
                    val isPressed = activeNotes.contains(midi)
                    val leftOffset = (whiteKeyWidth * (whiteIdx + 1)) - (blackKeyWidth / 2)
                    Box(
                        modifier = Modifier
                            .offset(x = leftOffset, y = 0.dp)
                            .width(blackKeyWidth)
                            .height(blackKeyHeight)
                            .shadow(4.dp, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (isPressed) Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))) else Brush.verticalGradient(listOf(KorgPa4xTheme.EbonyBlackHighlight, KorgPa4xTheme.EbonyBlackTop, KorgPa4xTheme.EbonyBlackBottom)))
                            .border(0.8.dp, if (isPressed) KorgPa4xTheme.KorgAmberGold else KorgPa4xTheme.EbonyBlackBorder, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .clickable { viewModel.onKeyPressed(midi) },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (isPressed) Box(modifier = Modifier.padding(bottom = 3.dp).size(5.dp).clip(CircleShape).background(KorgPa4xTheme.KorgAmberGold))
                    }
                }
            }
        }
    }
}

@Composable
fun Pa4xStyleSelectPanel(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF111827)).padding(16.dp)) {
        Column {
            Text("Style Selector", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a Pa4x arrangement style for live play.", color = Color(0xFFCBD5E1))
        }
    }
}

@Composable
fun Pa4xSoundSelectPanel(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF111827)).padding(16.dp)) {
        Column {
            Text("Sound Browser", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Load KORG Pa4x sounds and samples.", color = Color(0xFFCBD5E1))
        }
    }
}

@Composable
fun Pa4xQuarterTonePanel(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF111827)).padding(16.dp)) {
        Column {
            Text("Quarter-Tone Presets", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Bayati, Rast, Segah, Shur, Homayoun", color = Color(0xFFCBD5E1))
        }
    }
}

@Composable
fun Pa4xMixerFxPanel(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF111827)).padding(16.dp)) {
        Column {
            Text("Mixer / FX", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Reverb, Chorus, EQ and per-track mix.", color = Color(0xFFCBD5E1))
        }
    }
}

@Composable
fun Pa4xSetExplorerDialog(viewModel: Pa4xViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF111827)).padding(16.dp)) {
        Column {
            Text("Set Explorer", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Browse factory and imported Pa4x sets.", color = Color(0xFFCBD5E1))
        }
    }
}
