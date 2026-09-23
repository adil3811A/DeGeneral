package com.adll.de_general.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

actual class DatabaseFactory(private val context: Context) {
    actual fun newBuilder(): RoomDatabase.Builder<DeGeneralDatabase> {
        val dbFile = context.getDatabasePath(DATABASE_FILE_NAME)
        return Room.databaseBuilder(
            context = context.applicationContext,
            name = dbFile.absolutePath,
        )
    }
}

internal actual val databaseQueryContext: CoroutineContext = Dispatchers.IO
