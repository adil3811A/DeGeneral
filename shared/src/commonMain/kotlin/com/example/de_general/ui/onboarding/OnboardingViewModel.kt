package com.example.de_general.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.de_general.ai.CompatibilityReport
import com.example.de_general.ai.DeviceProbe
import com.example.de_general.ai.DeviceSnapshot
import com.example.de_general.ai.GemmaThreeOneB
import com.example.de_general.ai.InstallState
import com.example.de_general.ai.ModelInstaller
import com.example.de_general.ai.ModelSpec
import com.example.de_general.ai.checkCompatibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    /** Null until the device probe has run. */
    val snapshot: DeviceSnapshot? = null,
    val report: CompatibilityReport? = null,
    val install: InstallState = InstallState.NotInstalled,
    val keepScreenAwake: Boolean = true,
    val spec: ModelSpec = GemmaThreeOneB,
) {
    val probing: Boolean get() = report == null
}

/**
 * Drives both onboarding screens.
 *
 * Knows nothing about which screen is showing — that is the navigation back stack's job. Scoped to
 * the onboarding graph, so it and the running download survive moving between Welcome and Install.
 *
 * Two jobs, kept apart:
 *  - reading the device once, so the welcome screen can say something true about it
 *  - running the installer, which the user can pause and resume
 *
 * Pausing is literally cancelling [downloadJob]. [ModelInstaller] treats cancellation as a pause
 * and keeps its partial file, so there is no separate "pause flag" that could disagree with what
 * is actually happening on the wire.
 */
class OnboardingViewModel(
    private val deviceProbe: DeviceProbe,
    private val installer: ModelInstaller,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            installer.state.collect { state ->
                _uiState.update { it.copy(install = state) }
            }
        }
        probe()
    }

    /** Reads the device and decides whether it can run the model. */
    fun probe() {
        viewModelScope.launch {
            val snapshot = deviceProbe.snapshot()
            _uiState.update {
                it.copy(snapshot = snapshot, report = checkCompatibility(snapshot, it.spec))
            }
        }
    }

    /**
     * Re-reads what is on disk before the install screen appears.
     *
     * The download is deliberately *not* started here. A 769 MB transfer should begin because
     * someone asked for it, not because a screen appeared.
     */
    fun prepareInstall() {
        installer.refresh()
    }

    fun startOrResumeDownload() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            try {
                installer.install()
            } catch (cancellation: CancellationException) {
                // Pause. ModelInstaller has already parked the state; nothing to add.
                throw cancellation
            }
        }
    }

    fun pauseDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    fun retry() {
        installer.refresh()
        startOrResumeDownload()
    }

    /** Throws the weights away and returns to a clean slate. */
    fun discardDownload() {
        pauseDownload()
        installer.deleteInstall()
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenAwake = enabled) }
    }

    override fun onCleared() {
        pauseDownload()
        super.onCleared()
    }
}
