package com.adll.de_general

import android.app.Application
import com.adll.de_general.core.data.DatabaseFactory
import com.adll.de_general.core.data.PreferencesStorage
import com.adll.de_general.di.AppContainer
import com.adll.de_general.feature.onboarding.domain.DeviceProbe
import com.adll.de_general.feature.onboarding.domain.ModelStorage

/**
 * Holds the one [AppContainer] for the life of the process.
 *
 * This exists because of the model. `MainActivity.onCreate` used to build the container, which
 * meant a **new one on every activity recreation** — rotate the phone and you got a second
 * database handle and a second installer. Survivable while those were cheap. It stops being
 * survivable the moment the container owns an inference engine holding ~770 MB of weights: the
 * second copy would start loading them while the first still held them.
 *
 * Registered as `android:name` in the manifest. Nothing else belongs here.
 */
class DeGeneralApplication : Application() {

    val container: AppContainer by lazy {
        AppContainer(
            deviceProbe = DeviceProbe(this),
            modelStorage = ModelStorage(this),
            databaseFactory = DatabaseFactory(this),
            preferencesStorage = PreferencesStorage(this),
            now = System::currentTimeMillis,
        )
    }
}
