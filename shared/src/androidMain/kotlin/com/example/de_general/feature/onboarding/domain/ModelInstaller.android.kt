package com.example.de_general.feature.onboarding.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val installerDispatcher: CoroutineDispatcher = Dispatchers.IO
