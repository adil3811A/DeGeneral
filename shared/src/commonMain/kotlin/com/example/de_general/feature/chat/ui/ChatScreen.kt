package com.example.de_general.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.components.Badge
import com.example.de_general.core.ui.components.FloatingNavBarDefaults
import com.example.de_general.core.ui.icons.Lock
import com.example.de_general.core.ui.icons.Memory
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.chat.domain.ChatMessage
import com.example.de_general.feature.chat.domain.ChatRole
import com.example.de_general.feature.chat.ui.components.ChatBubble
import com.example.de_general.feature.chat.ui.components.ChatComposer

/** Height the composer reserves at the bottom of the transcript, on top of the nav bar's inset. */
private val ComposerReserve: Dp = 64.dp

private val ChipIconSize: Dp = 14.dp

/**
 * The Companion Chat.
 *
 * Takes state and callbacks, never a `NavController` and never the repository — the same contract
 * as `JournalScreen`, and what keeps it previewable.
 *
 * **What this screen will not do:** show a reply the model did not produce, animate a typing
 * indicator for a model that is not running, or display a tokens-per-second figure. There is no
 * inference engine in this app yet (`docs/LOCAL_AI.md`), so when a message is sent the transcript
 * gains the person's turn and [ChatUiState.notice] says plainly that nothing can answer it. The
 * preview below is a fixture and is obviously one; the running app has no such shortcut.
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onReflectDeeper: () -> Unit,
    onSaveInsight: (ChatMessage) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val listState = rememberLazyListState()

    // A new turn should be on screen without the reader chasing it.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(
            Modifier
                // safeDrawing, not safeContent: the latter unions in systemGestures and silently
                // doubles the horizontal inset on gesture-navigation phones.
                .safeDrawingPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Header()
                StatusRow(state)

                Box(Modifier.weight(1f)) {
                    Transcript(state, listState, onReflectDeeper, onSaveInsight)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = spacing.margin,
                        vertical = FloatingNavBarDefaults.ScreenOffset,
                    )
                    // Sit above the floating nav bar rather than under it.
                    .padding(bottom = FloatingNavBarDefaults.ContentInset),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                state.notice?.let { Notice(it, onDismissNotice) }
                ChatComposer(
                    draft = state.draft,
                    canSend = state.canSend,
                    onDraftChange = onDraftChange,
                    onSend = onSend,
                    onAttach = onAttach,
                )
            }
        }
    }
}

@Composable
private fun Header() {
    val spacing = MindfulTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.margin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "Mindful Scribe",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text("Companion Chat", style = MaterialTheme.typography.headlineSmall)
        }
        // True, and the same claim the onboarding screens make. Nothing here goes to a server.
        Badge(icon = MindfulIcons.Lock, label = "100% on-device")
    }
}

/**
 * The model and the context, both read from things the app actually knows.
 *
 * The model name and quantization come from the pinned `ModelSpec`; the entry count is a count of
 * rows that exist. There is deliberately no latency or tokens-per-second here — `docs/LOCAL_AI.md`
 * lists those among the figures removed for being unmeasurable, and nothing has measured one yet.
 */
@Composable
private fun StatusRow(state: ChatUiState) {
    val spacing = MindfulTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.margin),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(
            icon = MindfulIcons.Memory,
            label = state.modelLabel,
            container = MaterialTheme.colorScheme.primaryFixed,
            content = MaterialTheme.colorScheme.onPrimaryFixed,
        )
        StatusChip(
            icon = MindfulIcons.Lock,
            label = state.contextLabel,
            container = MaterialTheme.colorScheme.surfaceContainer,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
) {
    val spacing = MindfulTheme.spacing

    Surface(shape = MindfulShapes.full, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Transcript(
    state: ChatUiState,
    listState: LazyListState,
    onReflectDeeper: () -> Unit,
    onSaveInsight: (ChatMessage) -> Unit,
) {
    val spacing = MindfulTheme.spacing

    when {
        state.loading -> Unit

        state.isEmpty -> Box(
            Modifier.fillMaxSize().padding(horizontal = spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Nothing said yet. Ask whatever is on your mind — it stays on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.margin,
                end = spacing.margin,
                top = spacing.sm,
                // Clear the composer and the floating nav bar below it.
                bottom = FloatingNavBarDefaults.ContentInset + ComposerReserve,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            items(state.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onReflectDeeper = onReflectDeeper,
                    onSaveInsight = { onSaveInsight(message) },
                )
            }
        }
    }
}

/**
 * The one-line answer to "what just happened".
 *
 * This is where the app admits there is no engine, and where the + button says attachments are not
 * built. Both are the same kind of statement, so they share the same piece of UI.
 */
@Composable
private fun Notice(text: String, onDismiss: () -> Unit) {
    val spacing = MindfulTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = spacing.md, end = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(vertical = spacing.sm),
            )
            TextButton(onClick = onDismiss) {
                Text("OK", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * A scripted conversation, so the design can be reviewed.
 *
 * These two turns are a **fixture**. The running app cannot produce the companion's reply — there
 * is no inference engine — and it does not pretend otherwise.
 */
@Preview
@Composable
private fun ChatScreenPreview() {
    MindfulScribeTheme {
        ChatScreen(
            state = ChatUiState(
                loading = false,
                modelLabel = "Gemma 3 1B · Q4_K_M",
                contextEntryCount = 3,
                messages = listOf(
                    ChatMessage(
                        id = 1,
                        role = ChatRole.User.column,
                        text = "I feel calmer after an hour away from screens, but I still don't " +
                            "know how to set clearer boundaries tomorrow.",
                        timestamp = 0L,
                    ),
                    ChatMessage(
                        id = 2,
                        role = ChatRole.Model.column,
                        text = "It takes some courage to step away at all. For tomorrow, what " +
                            "about one non-negotiable pause — five minutes before you open " +
                            "anything?",
                        timestamp = 1L,
                    ),
                ),
            ),
            onDraftChange = {},
            onSend = {},
            onAttach = {},
            onReflectDeeper = {},
            onSaveInsight = {},
            onDismissNotice = {},
        )
    }
}
