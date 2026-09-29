package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
                Pa4xMainScreen(viewModel = viewModel())
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
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = android.R.drawable.screen_background_dark),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KorgPa4xTheme.ChassisDarkGunmetal.copy(alpha = 0.88f))
            ) {
                Pa4xTopBar(viewModel, Modifier.fillMaxWidth())

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    when (currentTab) {
                        Pa4xDisplayTab.PERFORMANCE -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                KorgTouchviewMonitor(viewModel, Modifier.fillMaxWidth())
                                KorgPhysicalControlPanel(viewModel, Modifier.fillMaxWidth().padding(top = 2.dp))
                            }
                        }
                        Pa4xDisplayTab.STYLE_SELECT -> Pa4xStyleSelectPanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.SOUND_SELECT -> Pa4xSoundSelectPanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.QUARTER_TONE -> Pa4xQuarterTonePanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.MIXER_FX -> Pa4xMixerFxPanel(viewModel, Modifier.fillMaxSize())
                        Pa4xDisplayTab.SET_EXPLORER -> Pa4xSetExplorerDialog(viewModel, Modifier.fillMaxSize())
                    }
                }

                KorgHighDefKeyboardView(viewModel, Modifier.fillMaxWidth())
            }
        }
    }
}
