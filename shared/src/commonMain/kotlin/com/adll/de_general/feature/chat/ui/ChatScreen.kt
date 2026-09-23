package com.adll.de_general.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.adll.de_general.core.ai.EngineState
import com.adll.de_general.core.ui.components.Badge
import com.adll.de_general.core.ui.components.FloatingNavBarDefaults
import com.adll.de_general.core.ui.icons.Delete
import com.adll.de_general.core.ui.icons.Lock
import com.adll.de_general.core.ui.icons.Memory
import com.adll.de_general.core.ui.icons.MindfulIcons
import com.adll.de_general.core.ui.theme.MindfulScribeTheme
import com.adll.de_general.core.ui.theme.MindfulShapes
import com.adll.de_general.core.ui.theme.MindfulTheme
import com.adll.de_general.feature.chat.domain.ChatMessage
import com.adll.de_general.feature.chat.domain.ChatRole
import com.adll.de_general.feature.chat.ui.components.ChatBubble
import com.adll.de_general.feature.chat.ui.components.ChatComposer

/** Height the composer reserves at the bottom of the transcript, on top of the nav bar's inset. */
private val ComposerReserve: Dp = 64.dp

private val ChipIconSize: Dp = 14.dp

/**
 * Key for the in-progress reply.
 *
 * Negative so it can never collide with a Room row id, which autoincrements from 1.
 */
private const val STREAMING_KEY = -1L

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
    onClearChat: () -> Unit,
    onCancelClear: () -> Unit,
    onConfirmClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val listState = rememberLazyListState()

    // The composer has to clear the floating nav bar when the keyboard is down, and the keyboard
    // when it is up. These two are deliberately expressed as one continuous pair rather than an
    // "is the keyboard open" boolean: as the keyboard animates in, its inset grows by exactly as
    // much as the nav-bar reserve shrinks, so their sum never dips and the composer never jumps
    // or leaves a gap at any point in the animation. A boolean flips at one arbitrary frame and
    // is what produced the dead space below the composer.
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBarReserve = (FloatingNavBarDefaults.ContentInset - imeBottom).coerceAtLeast(0.dp)
    val transcriptReserve =
        maxOf(imeBottom, FloatingNavBarDefaults.ContentInset) + ComposerReserve

    // Follow both new turns and the reply being written, so the reader never chases it.
    val transcriptLength = state.messages.size to state.streamingReply?.length
    LaunchedEffect(transcriptLength) {
        val last = state.messages.size - if (state.streamingReply == null) 1 else 0
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(
            // safeDrawing, not safeContent: the latter unions in systemGestures and silently
            // doubles the horizontal inset on gesture-navigation phones.
            //
            // The keyboard is excluded here and handled once, below, on the composer. Left in,
            // it would inset this whole Box *and* the composer's own reserve — the screen would
            // lift by roughly two keyboards and leave dead space above the real one.
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Header()
                StatusRow(state, onClearChat)

                Box(Modifier.weight(1f)) {
                    Transcript(
                        state,
                        listState,
                        transcriptReserve,
                        onReflectDeeper,
                        onSaveInsight,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // The keyboard, as an inset — so Compose subtracts what the Box above already
                    // consumed and the composer lands exactly on top of it, not a system bar's
                    // height above.
                    .windowInsetsPadding(WindowInsets.ime)
                    // The floating nav bar, as plain padding — a fixed gap that belongs to this
                    // app's own chrome, not to the window, and must not be consumption-adjusted.
                    .padding(bottom = navBarReserve)
                    .padding(
                        horizontal = spacing.margin,
                        vertical = FloatingNavBarDefaults.ScreenOffset,
                    ),
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

    if (state.confirmingClear) {
        ClearChatDialog(onCancel = onCancelClear, onConfirm = onConfirmClear)
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
 * The model chip, and the button that clears the conversation.
 *
 * The model name and quantization come from the pinned `ModelSpec`. There is deliberately no
 * latency or tokens-per-second here — `docs/LOCAL_AI.md` lists those among the figures removed for
 * being unmeasurable before a reply exists.
 *
 * The clear button is disabled on an empty conversation rather than offering to delete nothing.
 * Its 48dp touch target comes from [IconButton] itself, not from padding.
 */
@Composable
private fun StatusRow(state: ChatUiState, onClearChat: () -> Unit) {
    val spacing = MindfulTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.margin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(
            icon = MindfulIcons.Memory,
            label = "${state.modelLabel} · ${state.engineLabel}",
            container = MaterialTheme.colorScheme.primaryFixed,
            content = MaterialTheme.colorScheme.onPrimaryFixed,
        )
        IconButton(
            onClick = onClearChat,
            enabled = state.canClear,
            // Disabled colour is derived from this by Material, so the dimmed state needs no alpha.
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(MindfulIcons.Delete, contentDescription = "Clear chat")
        }
    }
}

/**
 * Confirms a clear. Shaped like the composer's discard dialog — copied, not imported, because a
 * feature never imports another feature — and a dialog for the same reason: a destructive
 * confirmation must not be swipe-dismissible.
 */
@Composable
private fun ClearChatDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Clear the whole conversation?", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(
                "Every message in this chat is deleted from this device. Your journal entries " +
                    "are not touched. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
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
    bottomReserve: Dp,
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
                // Clear the composer and whatever the composer is sitting on.
                bottom = bottomReserve,
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

            // The answer being written. Not a row yet, and it carries no speed figure — nothing
            // has been measured until it finishes.
            state.streamingReply?.let { partial ->
                item(key = STREAMING_KEY) {
                    ChatBubble(
                        message = ChatMessage(
                            id = STREAMING_KEY,
                            role = ChatRole.Model.column,
                            text = partial,
                            timestamp = 0L,
                        ),
                        onReflectDeeper = {},
                        onSaveInsight = {},
                        showActions = false,
                    )
                }
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
 * A scripted conversation, so the design can be reviewed without loading 770 MB of weights.
 *
 * These two turns are a **fixture**, including the speed figure on the reply. In the running app
 * that number is measured across the real generation; here it is made up, which is fine in a
 * preview and would not be anywhere else.
 */
@Preview
@Composable
private fun ChatScreenPreview() {
    MindfulScribeTheme {
        ChatScreen(
            state = ChatUiState(
                loading = false,
                modelLabel = "Gemma 3 1B · Q4_K_M",
                engine = EngineState.Ready(loadMillis = 1840),
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
                        tokensPerSecond = 7.4,
                        generationMillis = 9_200,
                    ),
                ),
            ),
            onDraftChange = {},
            onSend = {},
            onAttach = {},
            onReflectDeeper = {},
            onSaveInsight = {},
            onDismissNotice = {},
            onClearChat = {},
            onCancelClear = {},
            onConfirmClear = {},
        )
    }
}
