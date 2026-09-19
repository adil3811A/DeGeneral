package com.example.de_general.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.de_general.ui.theme.MindfulScribeTheme
import com.example.de_general.ui.theme.MindfulTheme

/** Stands in until the writing screen is built. */
@Composable
fun JournalScreen(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(MindfulTheme.spacing.margin),
            verticalArrangement = Arrangement.spacedBy(
                MindfulTheme.spacing.sm,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Your journal", style = MaterialTheme.typography.displayMedium)
            Text(
                "The writing screen is next. Onboarding is done.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun JournalScreenPreview() {
    MindfulScribeTheme { JournalScreen() }
}
