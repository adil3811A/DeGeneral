package com.example.de_general.data

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal actual val databaseQueryContext: CoroutineContext = Dispatchers.Default

actual class DatabaseFactory {
    actual fun newBuilder(): RoomDatabase.Builder<DeGeneralDatabase> =
        Room.databaseBuilder<DeGeneralDatabase>(
            name = "${documentsDirectory()}/$DATABASE_FILE_NAME",
        )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val documentsUrl: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentsUrl?.path) { "Could not resolve the iOS documents directory" }
}
