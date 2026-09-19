package com.example.de_general.feature.chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.icons.Add
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.icons.Send
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme

/** Component dimensions. Both buttons are comfortably past the 48dp touch minimum once padded. */
private val ButtonSize: Dp = 40.dp
private val ButtonIconSize: Dp = 20.dp

/**
 * The pill at the bottom of the Chat screen: attach, type, send.
 *
 * A [BasicTextField] rather than `OutlinedTextField` because the pill already *is* the container —
 * a Material text field would draw its own box inside this one.
 *
 * [onAttach] is wired to a real button that reports honestly that attachments do not exist yet.
 * A disabled or missing button would leave the design short of what it draws; a working-looking
 * one that silently did nothing would be worse than either.
 */
@Composable
fun ChatComposer(
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val tier = MindfulTheme.elevation.floating

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MindfulShapes.full,
        color = tier.container,
        shadowElevation = tier.shadow,
    ) {
        Row(
            modifier = Modifier.padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(
                onClick = onAttach,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = MindfulIcons.Add,
                label = "Add an attachment",
            )

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) {
                    Text(
                        "Ask your local companion…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4,
                )
            }

            RoundButton(
                onClick = onSend,
                enabled = canSend,
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                icon = MindfulIcons.Send,
                label = "Send",
            )
        }
    }
}

@Composable
private fun RoundButton(
    onClick: () -> Unit,
    container: Color,
    content: Color,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(ButtonSize),
        shape = MindfulShapes.full,
        color = if (enabled) container else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) content else MaterialTheme.colorScheme.outline,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(ButtonIconSize))
        }
    }
}
