package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticStyle
import com.example.audio.SoundEffect
import com.example.data.AccentTheme
import com.example.data.AppSettings
import com.example.data.BackupManager
import com.example.data.BatterySaverMode
import com.example.data.CommandLog
import com.example.data.LogSource
import com.example.data.PermissionHealthStatus
import com.example.data.ThemeMode
import com.example.data.UiLanguage
import com.example.data.VoiceCommand
import com.example.data.VoiceLanguage
import com.example.service.VoiceListeningService
import com.example.system.LiveRuntimeState
import com.example.system.PermissionHelper
import com.example.system.ServiceStateBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowMessage(val text: String) : UiEvent
    data object RequestMicPermission : UiEvent
    data object RequestDeviceAdmin : UiEvent
    data object RequestDndAccess : UiEvent
}

class SaylocViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SaylocApplication
    private val repo = app.repository
    private val store = app.settingsDataStore

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    val settings: StateFlow<AppSettings> = store.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val commands: StateFlow<List<VoiceCommand>> = repo.commands.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<CommandLog>> = repo.logs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val runtime: StateFlow<LiveRuntimeState> = ServiceStateBus.state

    private val _health = MutableStateFlow(PermissionHelper.checkHealth(application))
    val health: StateFlow<PermissionHealthStatus> = _health.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            store.settingsFlow.collect { s ->
                app.soundSynthesizer.enabled = s.soundEnabled
                app.hapticManager.enabled = s.hapticsEnabled
                _settingsLoaded.value = true
            }
        }
    }

    fun refreshHealth() {
        _health.value = PermissionHelper.checkHealth(app)
    }

    fun playTapFeedback() {
        app.soundSynthesizer.play(SoundEffect.TAP)
        app.hapticManager.trigger(HapticStyle.TAP)
    }

    fun toggleListening() {
        refreshHealth()
        if (runtime.value.isServiceRunning) {
            VoiceListeningService.stop(app)
            return
        }
        if (!PermissionHelper.hasMicrophonePermission(app)) {
            _events.tryEmit(UiEvent.RequestMicPermission)
            return
        }
        runCatching {
            VoiceListeningService.start(app)
        }.onFailure { err ->
            app.soundSynthesizer.play(SoundEffect.ERROR)
            _events.tryEmit(UiEvent.ShowMessage("Could not start listening: ${err.message}"))
        }
    }

    fun startListeningIfPermitted() {
        refreshHealth()
        if (!runtime.value.isServiceRunning && PermissionHelper.hasMicrophonePermission(app)) {
            runCatching { VoiceListeningService.start(app) }
        }
    }

    fun lockNow(source: LogSource = LogSource.MANUAL) {
        viewModelScope.launch {
            val outcome = app.actionExecutor.lockNow(source)
            if (outcome.needsDeviceAdmin) {
                _events.emit(UiEvent.ShowMessage("Enable Device Admin first to allow instant lock."))
                _events.emit(UiEvent.RequestDeviceAdmin)
            } else if (!outcome.succeeded) {
                _events.emit(UiEvent.ShowMessage(outcome.message))
            }
        }
    }

    fun toggleFlashlight() {
        val outcome = app.actionExecutor.toggleFlashlight(null)
        _events.tryEmit(UiEvent.ShowMessage(outcome.message))
    }

    fun testCommand(command: VoiceCommand) {
        viewModelScope.launch {
            val outcome = app.actionExecutor.executeCommand(
                command = command,
                heardText = "Test: ${command.name}",
                source = LogSource.TEST
            )
            _events.emit(UiEvent.ShowMessage("\"${command.name}\" → ${outcome.message}"))
            if (outcome.needsDeviceAdmin) {
                _events.emit(UiEvent.RequestDeviceAdmin)
            } else if (outcome.needsDndAccess) {
                _events.emit(UiEvent.RequestDndAccess)
            }
        }
    }

    fun stopFindMyPhone() {
        app.findMyPhoneController.stop()
        app.soundSynthesizer.play(SoundEffect.TAP)
        app.hapticManager.trigger(HapticStyle.TAP)
    }

    fun saveCommand(command: VoiceCommand) {
        viewModelScope.launch {
            repo.upsertCommand(command)
            _events.emit(UiEvent.ShowMessage("Saved \"${command.name}\""))
        }
    }

    fun deleteCommand(command: VoiceCommand) {
        viewModelScope.launch {
            repo.deleteCommand(command.id)
            _events.emit(UiEvent.ShowMessage("Deleted \"${command.name}\""))
        }
    }

    fun setCommandEnabled(command: VoiceCommand, enabled: Boolean) {
        viewModelScope.launch {
            repo.setCommandEnabled(command.id, enabled)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repo.clearLogs()
            _events.emit(UiEvent.ShowMessage("Command history cleared"))
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val list = repo.getAllCommands()
            val result = BackupManager.exportToUri(app, uri, list)
            result.onSuccess { count ->
                _events.emit(UiEvent.ShowMessage("Backed up $count commands to JSON"))
            }.onFailure { err ->
                app.soundSynthesizer.play(SoundEffect.ERROR)
                _events.emit(UiEvent.ShowMessage("Backup failed: ${err.message}"))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = BackupManager.importFromUri(app, uri)
            result.onSuccess { imported ->
                val added = repo.importCommands(imported)
                _events.emit(UiEvent.ShowMessage("Restored $added new command(s) from JSON"))
            }.onFailure { err ->
                app.soundSynthesizer.play(SoundEffect.ERROR)
                _events.emit(UiEvent.ShowMessage("Invalid backup JSON: ${err.message}"))
            }
        }
    }

    // Settings updates
    fun completeOnboarding() = viewModelScope.launch { store.setOnboardingCompleted(true) }
    fun reopenOnboarding() = viewModelScope.launch { store.setOnboardingCompleted(false) }
    fun setUiLanguage(lang: UiLanguage) = viewModelScope.launch { store.setUiLanguage(lang) }
    fun setVoiceLanguage(lang: VoiceLanguage) = viewModelScope.launch { store.setVoiceLanguage(lang) }
    fun setSensitivity(value: Float) = viewModelScope.launch { store.setSensitivity(value) }
    fun setBatteryMode(mode: BatterySaverMode) = viewModelScope.launch { store.setBatterySaverMode(mode) }
    fun setTimeWindow(start: Int, end: Int) = viewModelScope.launch { store.setTimeWindow(start, end) }
    fun setAutoLockMinutes(mins: Int) = viewModelScope.launch { store.setAutoLockMinutes(mins) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { store.setSoundEnabled(enabled) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { store.setHapticsEnabled(enabled) }
    fun setAccentTheme(accent: AccentTheme) = viewModelScope.launch { store.setAccentTheme(accent) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }
    fun setReduceEffects(reduce: Boolean) = viewModelScope.launch { store.setReduceEffects(reduce) }
    fun setStartOnBoot(enabled: Boolean) = viewModelScope.launch { store.setStartOnBoot(enabled) }
    fun setShakeToLock(enabled: Boolean) = viewModelScope.launch { store.setShakeToLockEnabled(enabled) }
    fun setShakeSensitivity(value: Float) = viewModelScope.launch { store.setShakeSensitivity(value) }
    fun setExperimentalUnlock(enabled: Boolean) = viewModelScope.launch { store.setExperimentalUnlockEnabled(enabled) }
}
