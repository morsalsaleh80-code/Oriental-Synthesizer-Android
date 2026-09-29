package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.viewmodel.Pa4xDisplayTab
import com.example.viewmodel.Pa4xViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pa4xAppTheme {
                Pa4xMainScreen()
            }
        }
    }
}

@Composable
fun Pa4xMainScreen(viewModel: Pa4xViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()

    BackHandler(enabled = currentTab != Pa4xDisplayTab.PERFORMANCE) {
        viewModel.setTab(Pa4xDisplayTab.PERFORMANCE)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = KorgPa4xTheme.ChassisDarkGunmetal
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(KorgPa4xTheme.ChassisDarkGunmetal)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Pa4xTopBar(viewModel, Modifier.fillMaxWidth().height(40.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    when (currentTab) {
                        Pa4xDisplayTab.PERFORMANCE -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                KorgTouchviewMonitor(viewModel, Modifier.fillMaxWidth().weight(0.4f))
                                Spacer(modifier = Modifier.height(2.dp))
                                KorgPhysicalControlPanel(viewModel, Modifier.fillMaxWidth().weight(0.3f))
                            }
                        }
                        Pa4xDisplayTab.STYLE_SELECT -> Pa4xStyleSelectPanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.SOUND_SELECT -> Pa4xSoundSelectPanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.QUARTER_TONE -> Pa4xQuarterTonePanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.MIXER_FX -> Pa4xMixerFxPanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.SET_EXPLORER -> Pa4xSetExplorerDialog(viewModel, Modifier.fillMaxSize())
                    }
                }

                KorgHighDefKeyboardView(viewModel, Modifier.fillMaxWidth().height(140.dp))
            }
        }
    }
}
