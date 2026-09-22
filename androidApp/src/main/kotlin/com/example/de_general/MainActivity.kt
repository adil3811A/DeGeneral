package com.example.de_general

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // A placeholder until App reports the resolved theme on its first composition, which
        // re-applies the bars to match. Edge-to-edge has to be on before super.onCreate either way.
        applySystemBars(darkTheme = false)
        super.onCreate(savedInstanceState)

        // Read, never built here. The container owns the inference engine, so one per process
        // and not one per activity — see DeGeneralApplication.
        val container = (application as DeGeneralApplication).container

        setContent {
            App(container, onDarkThemeChange = ::applySystemBars)
        }
    }

    /**
     * Bar icons that contrast with the canvas: dark icons on Mindful Scribe's warm paper, light
     * icons on Nocturnal Sanctuary's obsidian.
     *
     * Driven by the app's resolved theme, never by `SystemBarStyle.auto`: auto follows the
     * *system* setting, which is wrong the moment someone picks Light on a dark phone or Dark on a
     * light one.
     */
    private fun applySystemBars(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
