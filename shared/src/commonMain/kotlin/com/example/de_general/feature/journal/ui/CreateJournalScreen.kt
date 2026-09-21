package com.example.de_general.feature.journal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.components.Badge
import com.example.de_general.core.ui.icons.ErrorCircle
import com.example.de_general.core.ui.icons.Lock
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.journal.domain.DateAnchor
import com.example.de_general.feature.journal.domain.JournalMood
import com.example.de_general.feature.journal.domain.anchorDate
import com.example.de_general.feature.journal.domain.canPickMillis
import com.example.de_general.feature.journal.domain.dateToPickerMillis
import com.example.de_general.feature.journal.ui.components.DateAnchorCard
import com.example.de_general.feature.journal.ui.components.MoodChip
import com.example.de_general.feature.journal.ui.components.PolishCard
import com.example.de_general.feature.journal.ui.components.TagRow
import kotlinx.datetime.TimeZone

private val ErrorIconSize: Dp = 20.dp

/** Roughly a screenful of writing room before the field starts to scroll with the page. */
private const val BODY_MIN_LINES = 8

/**
 * The full-screen composer behind the pencil FAB.
 *
 * Takes state and callbacks, never a `NavController` and never the repository — the same contract
 * as `JournalScreen` and `ChatScreen`, and what keeps the previews below possible.
 *
 * **Save lives in the fixed top bar, on purpose.** It sits above the scroll area and outside the
 * keyboard's inset, so the keyboard physically cannot cover it. It also matches the Stitch design,
 * which puts Save top-right.
 *
 * **Text buttons, not icons, in that bar.** `MindfulIcons` has no close, back, save or calendar
 * glyph, and each new one is Material Symbols path data that has to be split at fixed columns —
 * `CLAUDE.md` names that as the standard way to silently corrupt a shape. `EntryCard`'s Delete
 * button set this precedent in the neighbouring file. The design's centred title is dropped
 * because every other screen in this app puts its title on its own line.
 */
// The opt-in is belt-and-braces for FlowRow. The overload without `overflow` is stable in
// foundation-layout 1.12.0; the deprecated one beside it is not, and both have every parameter
// defaulted, so the opt-in costs a warning if resolution picks the stable one and saves a compile
// error if it does not.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateJournalScreen(
    state: CreateJournalUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onClearBody: () -> Unit,
    onMoodChange: (JournalMood) -> Unit,
    onAnchorChange: (DateAnchor) -> Unit,
    onOpenDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    onDatePicked: (Long?) -> Unit,
    onTagDraftChange: (String) -> Unit,
    onCommitTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onRefine: () -> Unit,
    onAcceptPolish: () -> Unit,
    onDiscardPolish: () -> Unit,
    onUndoPolish: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onCancelDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing

    // Without this, system back pops straight past the confirm dialog and takes the draft with it.
    SystemBack(onClose)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                // safeDrawing, not safeContent: the latter unions in systemGestures and silently
                // adds ~40dp per side on gesture-navigation phones.
                //
                // The keyboard is excluded here and applied once, below, to the scrolling half —
                // so the top bar never rides up with it.
                //
                // Note what is *not* here: FloatingNavBarDefaults.ContentInset. The floating bar
                // is not drawn on this route (`selectedTab` returns null for it), so there is no
                // bar to clear. Both neighbouring screens reserve that space and copying them here
                // would leave 96dp of nothing at the bottom of the page.
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
        ) {
            TopBar(canSave = state.canSave, saving = state.saving, onSave = onSave, onClose = onClose)

            Column(
                modifier = Modifier
                    .weight(1f)
                    // Outside verticalScroll on purpose: as an inset here it shrinks the scrolling
                    // viewport, which is what keeps the focused line above the keyboard. Inside,
                    // it would only pad content that then scrolls back under it.
                    .windowInsetsPadding(WindowInsets.ime)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.margin, vertical = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        "Mindful Scribe",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text("New entry", style = MaterialTheme.typography.displayMedium)
                    // Not "encrypted", and not "auto-synced". Neither is true of this app.
                    Badge(icon = MindfulIcons.Lock, label = state.privacyLabel)
                }

                DateAnchorCard(
                    anchor = state.anchor,
                    dateLabel = state.dateLabel,
                    caption = state.dateCaption,
                    onToday = { onAnchorChange(DateAnchor.Today) },
                    onYesterday = { onAnchorChange(DateAnchor.Yesterday) },
                    onPickDate = onOpenDatePicker,
                )

                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Text("How did it feel?", style = MaterialTheme.typography.headlineSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        JournalMood.entries.forEach { mood ->
                            MoodChip(
                                mood = mood,
                                selected = state.mood == mood,
                                onClick = { onMoodChange(mood) },
                            )
                        }
                    }
                }

                // BasicTextField rather than OutlinedTextField, the ChatComposer precedent: the
                // page is the page, not a box drawn on it.
                PlainField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = "Give it a title",
                    style = MaterialTheme.typography.headlineMedium,
                    singleLine = true,
                    // Next, so the keyboard walks from the title into the body.
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                PlainField(
                    value = state.body,
                    onValueChange = onBodyChange,
                    placeholder = "What happened today?",
                    // headlineSmall is Newsreader 20/28. Stitch asks for line-height 1.8 on this
                    // field; 28 is kept. A hand-set lineHeight in a screen is the typographic
                    // equivalent of a raw dp — if looser leading is right for long prose it
                    // belongs in Type.kt, and in Stitch before that. A deliberate deviation.
                    style = MaterialTheme.typography.headlineSmall,
                    singleLine = false,
                    // Default, not Done: Enter has to insert a newline in a journal entry.
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    minLines = BODY_MIN_LINES,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Words, counted. No "1 min read" — that is arithmetic about a reader nobody
                    // timed.
                    Text(
                        state.wordCountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onClearBody, enabled = state.body.isNotEmpty()) {
                        Text("Clear", style = MaterialTheme.typography.labelLarge)
                    }
                }

                PolishCard(
                    state = state.polish,
                    refineLabel = state.refineLabel,
                    refineCaption = state.refineCaption,
                    refinedInLabel = state.refinedInLabel,
                    modelLabel = state.modelLabel,
                    canRefine = state.canRefine,
                    canUndo = state.canUndo,
                    undoLabel = state.undoLabel,
                    onRefine = onRefine,
                    onAccept = onAcceptPolish,
                    onDiscard = onDiscardPolish,
                    onUndo = onUndoPolish,
                )

                TagRow(
                    tags = state.tags,
                    draft = state.tagDraft,
                    onDraftChange = onTagDraftChange,
                    onCommit = onCommitTag,
                    onRemove = onRemoveTag,
                )

                if (state.errorMessage != null) {
                    ErrorRow(state.errorMessage, onDismissError)
                }
            }
        }
    }

    if (state.confirmingDiscard) {
        DiscardDialog(onCancel = onCancelDiscard, onConfirm = onConfirmDiscard)
    }

    if (state.pickingDate) {
        BackdateDialog(state = state, onDismiss = onDismissDatePicker, onPicked = onDatePicked)
    }
}

/**
 * System back, as this app's first use of it.
 *
 * Wrapped in a composable of its own so the two annotations below cover exactly one call and not a
 * whole screen's worth of code.
 *
 * `BackHandler` is `@ExperimentalComposeUiApi` **and**, as of Compose Multiplatform 1.12.0,
 * deprecated in favour of `androidx.navigationevent.compose.NavigationEventHandler`. It still
 * works — the deprecation is warning-level, and `BackHandler` is implemented on top of that very
 * class. Migrating means adopting the `navigationevent-compose` artifact and hoisting a
 * `NavigationEventState`, which is a piece of work for the whole app rather than something to do
 * inside one screen. Recorded here so the next person does not have to rediscover it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
private fun SystemBack(onBack: () -> Unit) {
    BackHandler(enabled = true, onBack = onBack)
}

/**
 * Cancel and Save, fixed above the scroll area.
 *
 * A plain `Row(SpaceBetween)` rather than a `TopAppBar`: Material's bar brings its own typography
 * and inset opinions, and this screen's title is a line of the page, not a bar label.
 */
@Composable
private fun TopBar(
    canSave: Boolean,
    saving: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val spacing = MindfulTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onClose) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge)
        }
        Button(onClick = onSave, enabled = canSave, shape = MindfulShapes.full) {
            Text(
                if (saving) "Saving…" else "Save",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** A text field that is just text: the hand-rolled placeholder is the `ChatComposer` pattern. */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    minLines: Int = 1,
) {
    Box(Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(placeholder, style = style, color = MaterialTheme.colorScheme.outline)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.merge(
                style.copy(color = MaterialTheme.colorScheme.onSurface),
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
        )
    }
}

/** The same error shape `JournalScreen` uses, so a failed write reads the same in both places. */
@Composable
private fun ErrorRow(message: String, onDismiss: () -> Unit) {
    val spacing = MindfulTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            MindfulIcons.ErrorCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(ErrorIconSize),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("Dismiss", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The app's first `AlertDialog`.
 *
 * A dialog rather than a bottom sheet because a destructive confirmation must not be
 * swipe-dismissible: the gesture that gets rid of the sheet would also get rid of the entry.
 */
@Composable
private fun DiscardDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Discard this entry?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                "Nothing has been saved yet, so this cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Discard",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Keep writing", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/**
 * Material's date picker, bounded to days that have happened.
 *
 * Two caveats worth knowing before touching this. It brings its own typography opinions, which will
 * not match Newsreader — checked against material3 1.12.0-alpha03, where the picker is no longer
 * `@ExperimentalMaterial3Api`, so no opt-in is needed. And `selectedDateMillis` is **UTC midnight**
 * of the chosen day, not a local instant — the conversion lives in `JournalDates.kt` and must not
 * be done by reading that value in the device's zone.
 */
@Composable
private fun BackdateDialog(
    state: CreateJournalUiState,
    onDismiss: () -> Unit,
    onPicked: (Long?) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateToPickerMillis(
            anchorDate(state.anchor, state.nowMillis, state.zone),
        ),
        selectableDates = remember(state.nowMillis, state.zone) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    canPickMillis(utcTimeMillis, state.nowMillis, state.zone)
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(pickerState.selectedDateMillis) }) {
                Text("Use this date", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/**
 * Preview fixtures.
 *
 * [PREVIEW_NOW] is 08:45 PM on Thursday 22 October 2026 in `America/New_York` — a real instant,
 * checked, so the date card renders a weekday that actually matches the date. The Stitch mock's own
 * sample ("Thursday, Oct 24, 2026") is a Saturday, which is why nothing here copies a date string.
 */
private const val PREVIEW_NOW = 1_792_716_300_000L

private val PreviewZone = TimeZone.of("America/New_York")

private fun previewState(): CreateJournalUiState = CreateJournalUiState(
    sessionKey = "preview",
    nowMillis = PREVIEW_NOW,
    zone = PreviewZone,
    modelLabel = "Gemma 3 1B · Q4_K_M",
)

@Composable
private fun PreviewHost(state: CreateJournalUiState) {
    MindfulScribeTheme {
        CreateJournalScreen(
            state = state,
            onTitleChange = {},
            onBodyChange = {},
            onClearBody = {},
            onMoodChange = {},
            onAnchorChange = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDatePicked = {},
            onTagDraftChange = {},
            onCommitTag = {},
            onRemoveTag = {},
            onRefine = {},
            onAcceptPolish = {},
            onDiscardPolish = {},
            onUndoPolish = {},
            onSave = {},
            onClose = {},
            onCancelDiscard = {},
            onConfirmDiscard = {},
            onDismissError = {},
        )
    }
}

/** A blank composer, as it opens. */
@Preview
@Composable
private fun CreateJournalScreenEmptyPreview() {
    PreviewHost(previewState())
}

/** Mid-write: a mood picked, two tags, a backdated anchor. */
@Preview
@Composable
private fun CreateJournalScreenWritingPreview() {
    PreviewHost(
        previewState().copy(
            title = "The long way home",
            body = "Walked back along the canal instead of taking the bus. Nothing happened, " +
                "which was the point.",
            mood = JournalMood.Reflective,
            anchor = DateAnchor.Yesterday,
            tags = listOf("walking", "quiet"),
        ),
    )
}

/**
 * A finished suggestion.
 *
 * The 1,240 ms is a **fixture**. In the running app that figure is timed across the real call —
 * this state is otherwise only reachable by loading 770 MB of weights, which a preview cannot do.
 * Made up here, and nowhere else, exactly as `ChatScreenPreview` says of its speed figure.
 */
@Preview
@Composable
private fun CreateJournalScreenPolishedPreview() {
    PreviewHost(
        previewState().copy(
            body = "walked back along the canal insted of takeing the bus. nothing happend, " +
                "which was the point",
            mood = JournalMood.Calm,
            polish = PolishState.Ready(
                title = "The long way home",
                text = "Walked back along the canal instead of taking the bus. Nothing " +
                    "happened, which was the point.",
                millis = 1_240,
            ),
        ),
    )
}

/**
 * Straight after "Keep this version": both fields now hold the model's version, and one undo puts
 * them back. The undo goes away as soon as either field is edited.
 */
@Preview
@Composable
private fun CreateJournalScreenKeptPreview() {
    val title = "The long way home"
    val body = "Walked back along the canal instead of taking the bus. Nothing happened, " +
        "which was the point."

    PreviewHost(
        previewState().copy(
            title = title,
            body = body,
            mood = JournalMood.Calm,
            undo = PolishUndo(
                previousTitle = "",
                previousBody = "walked back along the canal insted of takeing the bus. nothing " +
                    "happend, which was the point",
                appliedTitle = title,
                appliedBody = body,
            ),
        ),
    )
}

/** The model is not on disk, so the engine refused to load. The button offers another go. */
@Preview
@Composable
private fun CreateJournalScreenNoModelPreview() {
    PreviewHost(
        previewState().copy(
            body = "Tried to refine this before the model finished installing.",
            polish = PolishState.Failed("The model file could not be opened."),
        ),
    )
}
