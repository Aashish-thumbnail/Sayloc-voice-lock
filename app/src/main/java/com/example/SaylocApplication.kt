package com.example

import android.app.Application
import com.example.audio.HapticManager
import com.example.audio.SoundSynthesizer
import com.example.data.SaylocDatabase
import com.example.data.SaylocRepository
import com.example.data.SettingsDataStore
import com.example.service.VoiceListeningService
import com.example.system.ActionExecutor
import com.example.system.FindMyPhoneController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Local-only application dependency container shared across MainActivity,
 * VoiceListeningService, Quick Settings tile, and BootReceiver.
 */
class SaylocApplication : Application() {

    lateinit var database: SaylocDatabase
        private set
    lateinit var repository: SaylocRepository
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var soundSynthesizer: SoundSynthesizer
        private set
    lateinit var hapticManager: HapticManager
        private set
    lateinit var findMyPhoneController: FindMyPhoneController
        private set
    lateinit var actionExecutor: ActionExecutor
        private set

    override fun onCreate() {
        super.onCreate()
        VoiceListeningService.ensureNotificationChannels(this)

        database = SaylocDatabase.getInstance(this)
        repository = SaylocRepository(
            commandDao = database.voiceCommandDao(),
            logDao = database.commandLogDao()
        )
        settingsDataStore = SettingsDataStore(this)
        soundSynthesizer = SoundSynthesizer()
        hapticManager = HapticManager(this)
        findMyPhoneController = FindMyPhoneController(this, soundSynthesizer)
        actionExecutor = ActionExecutor(
            context = this,
            repository = repository,
            settingsDataStore = settingsDataStore,
            soundSynthesizer = soundSynthesizer,
            hapticManager = hapticManager,
            findMyPhoneController = findMyPhoneController
        )

        // Ensure 3 starter commands exist on very first launch
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.ensureStarterCommands()
        }
    }
}
