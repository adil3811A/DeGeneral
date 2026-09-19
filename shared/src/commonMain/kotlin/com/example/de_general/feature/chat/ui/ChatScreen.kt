package com.example.de_general.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.de_general.core.ui.components.SectionCard
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.core.ui.theme.MindfulTheme

/**
 * A placeholder, and it says so.
 *
 * The tab exists because the bottom bar has three of them; the conversation behind it does not
 * exist yet. Rather than mock a transcript, the screen states plainly what is and is not there —
 * the same rule the rest of the app follows about numbers it has not measured.
 */
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val spacing = MindfulTheme.spacing

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = spacing.margin, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text("Chat", style = MaterialTheme.typography.displayMedium)

            SectionCard {
                Text(
                    "Not built yet.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "This is where a conversation with the on-device model will live. Nothing " +
                        "here talks to a server, and nothing here works yet either.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    MindfulScribeTheme { ChatScreen() }
}
