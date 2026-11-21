package com.example.system

import com.example.data.ListeningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LiveRuntimeState(
    val listeningState: ListeningState = ListeningState.IDLE,
    val isServiceRunning: Boolean = false,
    val rmsLevel: Float = 0f,
    val lastHeardText: String = "",
    val lastMatchedCommand: String = "",
    val lastDetectionTimestamp: Long = 0L,
    val isFlashlightOn: Boolean = false,
    val isFindMyPhoneActive: Boolean = false,
    val statusDetail: String = "",
    val errorMessage: String? = null
)

object ServiceStateBus {
    private val _state = MutableStateFlow(LiveRuntimeState())
    val state: StateFlow<LiveRuntimeState> = _state.asStateFlow()

    fun setServiceRunning(running: Boolean, listeningState: ListeningState) {
        _state.update {
            it.copy(
                isServiceRunning = running,
                listeningState = listeningState,
                rmsLevel = if (running) it.rmsLevel else 0f,
                errorMessage = if (listeningState != ListeningState.ERROR) null else it.errorMessage
            )
        }
    }

    fun updateRms(level: Float) {
        _state.update { it.copy(rmsLevel = level.coerceIn(0f, 1f)) }
    }

    fun updateHeardText(text: String) {
        _state.update { it.copy(lastHeardText = text) }
    }

    fun reportCommandDetected(commandName: String, heardText: String) {
        _state.update {
            it.copy(
                listeningState = ListeningState.DETECTED,
                lastHeardText = heardText,
                lastMatchedCommand = commandName,
                lastDetectionTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )
        }
    }

    fun updateStatusDetail(detail: String) {
        _state.update { it.copy(statusDetail = detail) }
    }

    fun setError(message: String?) {
        _state.update {
            it.copy(
                listeningState = if (message != null) ListeningState.ERROR else it.listeningState,
                errorMessage = message
            )
        }
    }

    fun setFlashlightOn(on: Boolean) {
        _state.update { it.copy(isFlashlightOn = on) }
    }

    fun setFindMyPhoneActive(active: Boolean) {
        _state.update { it.copy(isFindMyPhoneActive = active) }
    }
}
