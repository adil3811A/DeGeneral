package com.example.de_general.feature.onboarding.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** `Dispatchers.IO` is JVM-only; on Darwin the default pool is the right home for this work. */
internal actual val installerDispatcher: CoroutineDispatcher = Dispatchers.Default
