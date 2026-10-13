package com.volteenabler.app

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.volteenabler.app.data.DeviceInfo
import com.volteenabler.app.shizuku.ShizukuManager
import com.volteenabler.app.ui.screens.HomeScreen
import com.volteenabler.app.ui.screens.NetworkToolsScreen
import com.volteenabler.app.ui.screens.RadioInfoScreen
import com.volteenabler.app.ui.screens.TerminalScreen
import com.volteenabler.app.ui.screens.VolteMethodsScreen
import com.volteenabler.app.ui.theme.VoLTEEnablerTheme
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppTab(val titleRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Default.Home),
    RADIO_INFO(R.string.nav_radio_info, Icons.Default.SignalCellularAlt),
    METHODS(R.string.nav_volte_methods, Icons.Default.Lock),
    TOOLS(R.string.nav_network_tools, Icons.Default.CellTower),
    TERMINAL(R.string.nav_terminal, Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoLTEEnablerTheme {
                MainContent(
                    onToggleLanguage = { toggleLanguage() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.checkStatus()
    }

    private fun toggleLanguage() {
        val currentLocale = resources.configuration.locales.get(0)
        val newLang = if (currentLocale.language == "ar") "en" else "ar"
        val locale = Locale(newLang)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    onToggleLanguage: () -> Unit
) {
    val isInstalled by ShizukuManager.isInstalled.collectAsStateWithLifecycle()
    val isRunning by ShizukuManager.isRunning.collectAsStateWithLifecycle()
    val isAuthorized by ShizukuManager.isAuthorized.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var deviceInfo by remember { mutableStateOf(DeviceInfo()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit, isAuthorized) {
        deviceInfo = DeviceInfo.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onToggleLanguage) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.switch_language)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.titleRes)) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.HOME -> HomeScreen(
                isInstalled = isInstalled,
                isRunning = isRunning,
                isAuthorized = isAuthorized,
                deviceInfo = deviceInfo,
                onRequestPermission = { ShizukuManager.requestPermission() },
                onRefreshDevice = { scope.launch { deviceInfo = DeviceInfo.load() } },
                onNavigateToMethods = { selectedTab = AppTab.METHODS },
                onNavigateToTools = { selectedTab = AppTab.TOOLS },
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.RADIO_INFO -> RadioInfoScreen(
                isAuthorized = isAuthorized,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.METHODS -> VolteMethodsScreen(
                isAuthorized = isAuthorized,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.TOOLS -> NetworkToolsScreen(
                isAuthorized = isAuthorized,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.TERMINAL -> TerminalScreen(
                isAuthorized = isAuthorized,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
