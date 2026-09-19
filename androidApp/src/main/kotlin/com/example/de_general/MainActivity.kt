package com.example.de_general

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

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

        // Read, never built here. The container owns the inference engine, so one per process
        // and not one per activity — see DeGeneralApplication.
        val container = (application as DeGeneralApplication).container

        setContent {
            App(container)
        }
    }
}
