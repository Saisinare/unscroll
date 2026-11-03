package com.unscroll.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.data.ConfigStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UnscrollTheme { MainShell() } }
    }
}

private val TABS = listOf(
    NavItem("Today", Icons.Filled.Home),
    NavItem("Apps", Icons.AutoMirrored.Filled.List),
    NavItem("Content", Icons.Filled.PlayArrow),
    NavItem("Sites", Icons.Filled.Search),
    NavItem("Strict", Icons.Filled.Lock),
)

@Composable
private fun MainShell() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ui = remember { UiActions(snackbar, scope) }
    val config by ConfigStore.state.collectAsStateWithLifecycle()

    // Content scrolls underneath the floating nav, so every screen pads its bottom by ~120dp.
    Box(Modifier.fillMaxSize().background(palette.bg)) {
        // Instant switch: a crossfade would draw two full screens per frame. The state holder keeps
        // each tab's scroll position when you come back to it.
        val tabState = rememberSaveableStateHolder()
        Box(Modifier.statusBarsPadding()) {
            tabState.SaveableStateProvider(tab) {
                when (tab) {
                    0 -> TodayScreen(config, onOpenStrict = { tab = 4 })
                    1 -> AppsScreen(config, ui)
                    2 -> ContentScreen(config, ui)
                    3 -> SitesScreen(config, ui)
                    else -> StrictScreen(config, ui)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            SnackbarHost(snackbar)
            FloatingNav(TABS, tab, onSelect = { tab = it })
        }
    }
}
