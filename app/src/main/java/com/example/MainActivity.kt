package com.example

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LogSource
import com.example.data.ThemeMode
import com.example.data.VoiceCommand
import com.example.system.PermissionHelper
import com.example.system.ShakeToLockDetector
import com.example.ui.components.FloatingPillNav
import com.example.ui.components.PillNavItem
import com.example.ui.components.SaylocBackground
import com.example.ui.components.rememberParallaxTilt
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.CommandEditorScreen
import com.example.ui.screens.CommandHistoryScreen
import com.example.ui.screens.CommandsScreen
import com.example.ui.screens.FindMyPhoneOverlay
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionHealthScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.SaylocTheme

private enum class ModalOverlay {
    NONE,
    PERMISSION_HEALTH,
    COMMAND_EDITOR,
    COMMAND_HISTORY
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AUTO_START_LISTENING = "extra_auto_start_listening"
        const val EXTRA_OPEN_PERMISSION_HEALTH = "extra_open_permission_health"
        const val EXTRA_DISMISS_NON_SECURE_KEYGUARD = "extra_dismiss_non_secure_keyguard"
    }

    private val viewModel: SaylocViewModel by viewModels()
    private var openHealthOnStart = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
            val isDark = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }

            SaylocTheme(
                themeMode = settings.themeMode,
                accentTheme = settings.accentTheme,
                reduceEffects = settings.reduceEffects
            ) {
                CompositionLocalProvider(
                    LocalSaylocStrings provides stringsFor(settings.uiLanguage)
                ) {
                    if (settingsLoaded) {
                        SaylocRoot(
                            viewModel = viewModel,
                            openHealthFromIntent = openHealthOnStart.value,
                            onConsumedHealthIntent = { openHealthOnStart.value = false }
                        )
                    } else {
                        SaylocBackground { }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_AUTO_START_LISTENING, false)) {
            viewModel.startListeningIfPermitted()
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_PERMISSION_HEALTH, false)) {
            openHealthOnStart.value = true
        }
        if (intent.getBooleanExtra(EXTRA_DISMISS_NON_SECURE_KEYGUARD, false)) {
            val km = getSystemService(KeyguardManager::class.java)
            if (km != null && !km.isKeyguardSecure) {
                km.requestDismissKeyguard(this, null)
            }
        }
    }
}

@Composable
private fun SaylocRoot(
    viewModel: SaylocViewModel,
    openHealthFromIntent: Boolean,
    onConsumedHealthIntent: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val commands by viewModel.commands.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val runtime by viewModel.runtime.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()

    val tilt by rememberParallaxTilt()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activeOverlay by remember { mutableStateOf(ModalOverlay.NONE) }
    var editingCommand by remember { mutableStateOf<VoiceCommand?>(null) }

    LaunchedEffect(openHealthFromIntent) {
        if (openHealthFromIntent) {
            activeOverlay = ModalOverlay.PERMISSION_HEALTH
            onConsumedHealthIntent()
        }
    }

    // Refresh permissions whenever returning from Android System Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Foreground in-app Shake-to-Lock when background service is idle
    DisposableEffect(settings.shakeToLockEnabled, settings.shakeSensitivity, runtime.isServiceRunning) {
        if (settings.shakeToLockEnabled && !runtime.isServiceRunning) {
            val detector = ShakeToLockDetector(context) {
                viewModel.lockNow(LogSource.SHAKE)
            }.apply {
                sensitivity = settings.shakeSensitivity
                start()
            }
            onDispose { detector.stop() }
        } else {
            onDispose { }
        }
    }

    // Permission & system launchers
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshHealth()
        if (granted && settings.onboardingCompleted) {
            viewModel.startListeningIfPermitted()
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshHealth()
    }

    val systemSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshHealth()
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    fun requestMic() {
        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.refreshHealth()
        }
    }

    fun requestDeviceAdmin() {
        runCatching {
            systemSettingsLauncher.launch(PermissionHelper.createDeviceAdminIntent(context))
        }
    }

    fun requestBatteryExemption() {
        runCatching {
            systemSettingsLauncher.launch(PermissionHelper.createBatteryOptimizationIntent(context))
        }.onFailure {
            runCatching {
                systemSettingsLauncher.launch(PermissionHelper.createAppDetailsIntent(context))
            }
        }
    }

    fun requestDndAccess() {
        runCatching {
            systemSettingsLauncher.launch(PermissionHelper.createDndSettingsIntent())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                UiEvent.RequestMicPermission -> requestMic()
                UiEvent.RequestDeviceAdmin -> requestDeviceAdmin()
                UiEvent.RequestDndAccess -> requestDndAccess()
            }
        }
    }

    SaylocBackground {
        if (!settings.onboardingCompleted) {
            OnboardingScreen(
                health = health,
                uiLanguage = settings.uiLanguage,
                tilt = tilt,
                onSelectLanguage = viewModel::setUiLanguage,
                onRequestMic = ::requestMic,
                onRequestNotifications = ::requestNotifications,
                onRequestDeviceAdmin = ::requestDeviceAdmin,
                onRequestBatteryExemption = ::requestBatteryExemption,
                onTapFeedback = viewModel::playTapFeedback,
                onFinish = viewModel::completeOnboarding
            )
        } else {
            when (activeOverlay) {
                ModalOverlay.PERMISSION_HEALTH -> {
                    PermissionHealthScreen(
                        health = health,
                        tilt = tilt,
                        onRequestMic = ::requestMic,
                        onRequestNotifications = ::requestNotifications,
                        onRequestDeviceAdmin = ::requestDeviceAdmin,
                        onRequestBattery = ::requestBatteryExemption,
                        onRequestDnd = ::requestDndAccess,
                        onBack = { activeOverlay = ModalOverlay.NONE }
                    )
                }

                ModalOverlay.COMMAND_EDITOR -> {
                    CommandEditorScreen(
                        initialCommand = editingCommand,
                        uiLanguage = settings.uiLanguage,
                        experimentalUnlockEnabled = settings.experimentalUnlockEnabled,
                        onSave = { cmd ->
                            viewModel.saveCommand(cmd)
                            editingCommand = null
                            activeOverlay = ModalOverlay.NONE
                        },
                        onCancel = {
                            editingCommand = null
                            activeOverlay = ModalOverlay.NONE
                        },
                        onTapFeedback = viewModel::playTapFeedback
                    )
                }

                ModalOverlay.COMMAND_HISTORY -> {
                    CommandHistoryScreen(
                        logs = logs,
                        onClearLogs = viewModel::clearLogs,
                        onBack = { activeOverlay = ModalOverlay.NONE }
                    )
                }

                ModalOverlay.NONE -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Depth-style animated screen transitions (slide + scale + fade)
                        AnimatedContent(
                            targetState = selectedTab,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                (slideInHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) { it / 4 * direction } +
                                    scaleIn(tween(260), initialScale = 0.94f) +
                                    fadeIn(tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(tween(220)) { -it / 4 * direction } +
                                            scaleOut(tween(220), targetScale = 0.94f) +
                                            fadeOut(tween(180))
                                    )
                            },
                            label = "mainTabTransition"
                        ) { tab ->
                            when (tab) {
                                0 -> HomeScreen(
                                    runtime = runtime,
                                    settings = settings,
                                    commands = commands,
                                    health = health,
                                    tilt = tilt,
                                    onToggleListening = viewModel::toggleListening,
                                    onLockNow = { viewModel.lockNow(LogSource.MANUAL) },
                                    onToggleFlashlight = viewModel::toggleFlashlight,
                                    onToggleShakeLock = { viewModel.setShakeToLock(!settings.shakeToLockEnabled) },
                                    onOpenHealthCheck = { activeOverlay = ModalOverlay.PERMISSION_HEALTH },
                                    onTestCommand = viewModel::testCommand,
                                    onTapFeedback = viewModel::playTapFeedback
                                )

                                1 -> CommandsScreen(
                                    commands = commands,
                                    uiLanguage = settings.uiLanguage,
                                    tilt = tilt,
                                    onAddNew = {
                                        editingCommand = null
                                        activeOverlay = ModalOverlay.COMMAND_EDITOR
                                    },
                                    onEdit = { cmd ->
                                        editingCommand = cmd
                                        activeOverlay = ModalOverlay.COMMAND_EDITOR
                                    },
                                    onDelete = viewModel::deleteCommand,
                                    onToggleEnabled = viewModel::setCommandEnabled,
                                    onTestCommand = viewModel::testCommand,
                                    onTapFeedback = viewModel::playTapFeedback
                                )

                                else -> SettingsScreen(
                                    settings = settings,
                                    tilt = tilt,
                                    onUiLanguageChange = viewModel::setUiLanguage,
                                    onVoiceLanguageChange = viewModel::setVoiceLanguage,
                                    onSensitivityChange = viewModel::setSensitivity,
                                    onBatteryModeChange = viewModel::setBatteryMode,
                                    onTimeWindowChange = viewModel::setTimeWindow,
                                    onAutoLockChange = viewModel::setAutoLockMinutes,
                                    onShakeEnabledChange = viewModel::setShakeToLock,
                                    onShakeSensitivityChange = viewModel::setShakeSensitivity,
                                    onStartOnBootChange = viewModel::setStartOnBoot,
                                    onAccentChange = viewModel::setAccentTheme,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onReduceEffectsChange = viewModel::setReduceEffects,
                                    onSoundEnabledChange = viewModel::setSoundEnabled,
                                    onHapticsEnabledChange = viewModel::setHapticsEnabled,
                                    onExperimentalUnlockChange = viewModel::setExperimentalUnlock,
                                    onOpenHealthCheck = { activeOverlay = ModalOverlay.PERMISSION_HEALTH },
                                    onOpenHistory = { activeOverlay = ModalOverlay.COMMAND_HISTORY },
                                    onBackupJson = { backupLauncher.launch("sayloc_commands_backup.json") },
                                    onRestoreJson = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                    onRerunOnboarding = viewModel::reopenOnboarding,
                                    onTapFeedback = viewModel::playTapFeedback
                                )
                            }
                        }

                        // Floating 3D Pill Navigation Bar
                        FloatingPillNav(
                            items = listOf(
                                PillNavItem("home", strings.navHome, Icons.Rounded.Home),
                                PillNavItem("commands", strings.navCommands, Icons.Rounded.GraphicEq),
                                PillNavItem("settings", strings.navSettings, Icons.Rounded.Settings)
                            ),
                            selectedIndex = selectedTab,
                            onSelect = { index ->
                                viewModel.playTapFeedback()
                                selectedTab = index
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // Full-screen Find My Phone dismiss overlay when active
        AnimatedVisibility(
            visible = runtime.isFindMyPhoneActive,
            enter = fadeIn(tween(200)) + scaleIn(tween(250), initialScale = 0.92f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f)
        ) {
            FindMyPhoneOverlay(onDismiss = viewModel::stopFindMyPhone)
        }

        // Frosted floating Snackbar feedback at top safe area
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = palette.pillBg,
                contentColor = if (palette.isDark) Color.White else palette.textPrimary
            )
        }
    }
}
