package com.example.de_general.ai

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val installerDispatcher: CoroutineDispatcher = Dispatchers.IO
