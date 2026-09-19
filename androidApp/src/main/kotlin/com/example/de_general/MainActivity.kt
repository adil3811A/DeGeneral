package com.example.de_general

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.de_general.core.data.DatabaseFactory
import com.example.de_general.di.AppContainer
import com.example.de_general.feature.onboarding.domain.DeviceProbe
import com.example.de_general.feature.onboarding.domain.ModelStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The Mindful Scribe canvas is warm unbleached paper (#FDF9F5), so the system bars need
        // dark icons. Bare enableEdgeToEdge() would follow the system theme and leave light icons
        // invisible against it.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Application context, not the activity: the container outlives configuration changes.
        val container = AppContainer(
            deviceProbe = DeviceProbe(applicationContext),
            modelStorage = ModelStorage(applicationContext),
            databaseFactory = DatabaseFactory(applicationContext),
            now = System::currentTimeMillis,
        )

        setContent {
            App(container)
        }
    }
}
