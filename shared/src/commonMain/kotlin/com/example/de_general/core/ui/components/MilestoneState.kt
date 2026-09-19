package com.example.de_general.core.ui.components

/**
 * How far along one of the four setup milestones is.
 *
 * UI vocabulary rather than a composable, so the pure copy mappers in
 * `feature/onboarding/ui/InstallCopy.kt` can return it and `commonTest` can assert on it without
 * pulling in the Compose runtime.
 */
enum class MilestoneState {
    Done,
    Active,
    Pending,
    Failed,
}
