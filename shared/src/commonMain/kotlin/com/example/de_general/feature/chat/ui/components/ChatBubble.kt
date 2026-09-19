package com.example.de_general.feature.chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.icons.AutoStories
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.icons.Psychology
import com.example.de_general.core.ui.icons.Spa
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme
import com.example.de_general.feature.chat.domain.ChatMessage
import com.example.de_general.feature.chat.domain.ChatRole
import com.example.de_general.feature.chat.domain.chatRole

/** How wide a bubble is allowed to get, as a fraction of the row. */
private const val BUBBLE_WIDTH_FRACTION = 0.88f

/** Component dimensions, not spacing — the same reason `ProgressRing` carries its own diameter. */
private val AvatarSize: Dp = 24.dp
private val ChipIconSize: Dp = 16.dp

/** The 4pt half-step between `sm` and `md`, which the bubble padding wants. */
private val BubbleVerticalPadding: Dp = 12.dp

/**
 * One turn of the conversation.
 *
 * Two shapes of the same idea: the companion speaks from the left on a raised surface, the person
 * from the right in `primary`. The corner nearest the speaker is squared off — the design's way of
 * pointing a bubble at whoever said it — using the theme's own `extraSmall` radius rather than a
 * number invented here.
 *
 * [onReflectDeeper] and [onSaveInsight] hang under companion turns only. They are real: one sends
 * a follow-up, the other writes the text into the journal. [showActions] turns them off for a
 * reply still being written — there is nothing to act on until it is finished.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    onReflectDeeper: () -> Unit,
    onSaveInsight: () -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
) {
    when (message.chatRole) {
        ChatRole.User -> UserTurn(message, modifier)
        ChatRole.Model -> ModelTurn(message, onReflectDeeper, onSaveInsight, showActions, modifier)
    }
}

@Composable
private fun UserTurn(message: ChatMessage, modifier: Modifier = Modifier) {
    val spacing = MindfulTheme.spacing
    val shapes = MaterialTheme.shapes

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            modifier = Modifier.fillMaxWidth(BUBBLE_WIDTH_FRACTION),
            shape = shapes.medium.copy(topEnd = shapes.extraSmall.topEnd),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(
                    horizontal = spacing.md,
                    vertical = BubbleVerticalPadding,
                ),
            )
        }
    }
}

@Composable
private fun ModelTurn(
    message: ChatMessage,
    onReflectDeeper: () -> Unit,
    onSaveInsight: () -> Unit,
    showActions: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = MindfulTheme.spacing
    val shapes = MaterialTheme.shapes
    val tier = MindfulTheme.elevation.floating

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        SpeakerLine()

        Surface(
            modifier = Modifier.fillMaxWidth(BUBBLE_WIDTH_FRACTION),
            shape = shapes.medium.copy(topStart = shapes.extraSmall.topStart),
            color = tier.container,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = tier.shadow,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(spacing.cardPadding),
            )
        }

        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                ActionChip(MindfulIcons.Psychology, "Reflect deeper", onReflectDeeper)
                ActionChip(MindfulIcons.AutoStories, "Save insight", onSaveInsight)
            }
        }

        message.speedLabel()?.let { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * The measured generation speed, or null when there is nothing measured to show.
 *
 * `docs/LOCAL_AI.md` struck an estimated "24 tok/s" from the design and said to bring a speed back
 * only once one had actually been timed. This is that figure: counted across the real generation,
 * stored on the row, and absent — not guessed — on every reply written before the app could time
 * one.
 */
private fun ChatMessage.speedLabel(): String? {
    val rate = tokensPerSecond ?: return null
    val whole = rate.toInt()
    val tenths = ((rate - whole) * 10).toInt()
    return "$whole.$tenths tokens/sec"
}

/** The avatar, the name, and the one-word reminder of where the answer came from. */
@Composable
private fun SpeakerLine() {
    val spacing = MindfulTheme.spacing

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(AvatarSize),
            shape = MindfulShapes.full,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    MindfulIcons.Spa,
                    contentDescription = null,
                    modifier = Modifier.size(ChipIconSize),
                )
            }
        }
        Text(
            "Mindful Scribe",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        // Not decoration: it is the whole promise of the app, restated where the answer appears.
        Text(
            "Local",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    val spacing = MindfulTheme.spacing

    Surface(
        onClick = onClick,
        shape = MindfulShapes.full,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BubbleVerticalPadding, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
