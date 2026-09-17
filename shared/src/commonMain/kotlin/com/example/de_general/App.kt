package com.example.de_general

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.de_general.ui.onboarding.InstallScreen
import com.example.de_general.ui.onboarding.OnboardingStep
import com.example.de_general.ui.onboarding.OnboardingViewModel
import com.example.de_general.ui.onboarding.WelcomeScreen
import com.example.de_general.ui.theme.MindfulScribeTheme
import com.example.de_general.ui.theme.MindfulTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


/**
 * The app.
 *
 * Onboarding is a three-step sequence held in a view model rather than a navigation graph. Two
 * screens and a placeholder do not need back stacks or deep links; when the journal screens land,
 * a navigation library earns its place and this becomes its start destination.
 */
@Composable
fun App(container: AppContainer) {
    MindfulScribeTheme {
        val viewModel = viewModel {
            OnboardingViewModel(container.deviceProbe, container.modelInstaller)
        }
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val width = if (forward) 1 else -1
                (
                    slideInHorizontally { it * width / 6 } + fadeIn() togetherWith
                        slideOutHorizontally { -it * width / 6 } + fadeOut()
                    )
            },
            label = "onboarding",
        ) { step ->
            when (step) {
                OnboardingStep.Welcome -> WelcomeScreen(
                    state = state,
                    onContinue = viewModel::continueToInstall,
                    onSkip = viewModel::finishOnboarding,
                )

                OnboardingStep.Install -> InstallScreen(
                    state = state,
                    onStartOrResume = viewModel::startOrResumeDownload,
                    onPause = viewModel::pauseDownload,
                    onRetry = viewModel::retry,
                    onKeepScreenAwakeChange = viewModel::setKeepScreenAwake,
                    onFinish = viewModel::finishOnboarding,
                )

                OnboardingStep.Done -> JournalPlaceholder()
            }
        }
    }
}

/** Stands in until the journal screen is built. */
@Composable
private fun JournalPlaceholder() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .padding(MindfulTheme.spacing.margin),
            verticalArrangement = Arrangement.spacedBy(MindfulTheme.spacing.sm, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Your journal", style = MaterialTheme.typography.displayMedium)
            Text(
                "The writing screen is next. Onboarding is done.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun JournalPlaceholderPreview() {
    MindfulScribeTheme { JournalPlaceholder() }
}
