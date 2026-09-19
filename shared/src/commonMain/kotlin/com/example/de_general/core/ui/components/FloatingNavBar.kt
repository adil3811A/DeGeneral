package com.example.de_general.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.de_general.core.ui.icons.AutoStories
import com.example.de_general.core.ui.icons.ChatBubble
import com.example.de_general.core.ui.icons.Edit
import com.example.de_general.core.ui.icons.MindfulIcons
import com.example.de_general.core.ui.icons.Settings
import com.example.de_general.core.ui.theme.MindfulScribeTheme
import com.example.de_general.core.ui.theme.MindfulShapes
import com.example.de_general.core.ui.theme.MindfulTheme

/**
 * One destination in a [FloatingNavBar], or the bar's single trailing action.
 *
 * Deliberately route-agnostic: `core` never imports `feature`, and a bar that named `Journal` or
 * `Settings` would have to. Whoever knows the routes builds these — see
 * `navigation/MainNavBar.kt`.
 */
@Immutable
data class NavBarItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * The app's bottom navigation: a floating pill of [items] with [action] riding beside it.
 *
 * Material 3's own `HorizontalFloatingToolbar`, dressed in the Mindful Scribe tier. The FAB
 * overload is the one that matches the design: it lays the action button out *beside* the capsule
 * with a gap, rather than inside it, which is how the Stitch showcase draws the pencil button.
 *
 * One implementation for both platforms. `HorizontalFloatingToolbar` is declared in material3's
 * own `commonMain`, so there is nothing for an `expect`/`actual` to buy here — the bar is the same
 * object on Android and iOS, and previewing it on Android previews what iOS will draw.
 *
 * Nothing here needs an experimental opt-in: `HorizontalFloatingToolbar` is plain public API in
 * material3 1.12.0-alpha03.
 *
 * Two limits worth knowing before changing it:
 *
 * - **[modifier] carries position and insets only, never a width.** The toolbar measures itself
 *   from its content and ignores an imposed width; hand it `fillMaxWidth()` and it sits in the
 *   left-hand corner of a full-width node.
 * - **It is not frosted.** The design puts a blurred backdrop behind the capsule, and Compose
 *   Multiplatform has no portable backdrop blur — the gap `MindfulTier.bar` already documents in
 *   `Elevation.kt`. The container is a flat tonal fill instead.
 *
 * The bar floats over content rather than reserving space, so a scrolling screen has to leave
 * [FloatingNavBarDefaults.ContentInset] free at its bottom or the bar will sit on its last row.
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    action: NavBarItem,
    modifier: Modifier = Modifier,
) {
    val barShadow = MindfulTheme.elevation.bar.shadow

    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = action.onClick,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(action.icon, contentDescription = action.label)
            }
        },
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MindfulTheme.elevation.bar.container,
            toolbarContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fabContainerColor = MaterialTheme.colorScheme.primaryFixed,
            fabContentColor = MaterialTheme.colorScheme.onPrimaryFixed,
        ),
        shape = MindfulShapes.full,
        expandedShadowElevation = barShadow,
        collapsedShadowElevation = barShadow,
    ) {
        items.forEach { ToolbarTab(it) }
    }
}

/**
 * One tab: a tinted pill behind the icon when selected, label underneath.
 *
 * Fixed width, never `Modifier.weight` — see [FloatingNavBarDefaults.TabWidth] for why that does
 * not work here. `selectable` rather than `clickable` so TalkBack announces "selected" instead of
 * leaving the user to infer it from a colour.
 */
@Composable
private fun ToolbarTab(item: NavBarItem) {
    val spacing = MindfulTheme.spacing

    Column(
        modifier = Modifier
            .width(FloatingNavBarDefaults.TabWidth)
            .clip(MindfulShapes.full)
            .selectable(
                selected = item.selected,
                role = Role.Tab,
                onClick = item.onClick,
            )
            .padding(vertical = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Surface(
            modifier = Modifier.size(
                width = FloatingNavBarDefaults.IndicatorWidth,
                height = FloatingNavBarDefaults.IndicatorHeight,
            ),
            shape = MindfulShapes.full,
            color = if (item.selected) {
                MaterialTheme.colorScheme.primaryFixed
            } else {
                Color.Transparent
            },
            contentColor = if (item.selected) {
                MaterialTheme.colorScheme.onPrimaryFixed
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(FloatingNavBarDefaults.IconSize),
                )
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (item.selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

object FloatingNavBarDefaults {
    /**
     * How much bottom padding a screen owes the bar.
     *
     * Material sizes the toolbar-plus-FAB row to 80dp, and it sits [ScreenOffset] off the bottom
     * edge; the remainder is the gap that keeps a card from looking pinned to it. A component
     * dimension, not a spacing decision, which is why it lives here rather than in
     * `MindfulSpacing`.
     */
    val ContentInset: Dp = 96.dp

    /** The bar's distance from the bottom edge, inside the window's safe area. */
    val ScreenOffset: Dp = 16.dp

    /**
     * One tab's width.
     *
     * Fixed rather than a `Modifier.weight`, and that is not a style preference. The toolbar puts
     * its content inside a `horizontalScroll`, which measures children with an infinite max width;
     * `RowColumnMeasurePolicy` then falls back to the *minimum* width when dividing weights, which
     * is zero. Weighted tabs come out zero-wide and the capsule vanishes, leaving only the FAB.
     *
     * 72dp across three tabs puts the bar just about at the width of a phone screen between the
     * margins, and leaves it a centred island on anything wider.
     */
    internal val TabWidth: Dp = 72.dp

    /** The tinted pill behind a selected item's icon. */
    internal val IndicatorWidth: Dp = 48.dp
    internal val IndicatorHeight: Dp = 28.dp

    /** Icon size inside that pill. Material Symbols are drawn on a 24dp box. */
    internal val IconSize: Dp = 20.dp
}

/**
 * The bar on its own.
 *
 * Worth having: there is no emulator on the machine this was written on, so the preview pane is
 * the first place the capsule can be seen to have width at all.
 */
@Preview
@Composable
private fun FloatingNavBarPreview() {
    MindfulScribeTheme {
        FloatingNavBar(
            items = listOf(
                NavBarItem(MindfulIcons.ChatBubble, "Chat") {},
                NavBarItem(MindfulIcons.AutoStories, "Journal", selected = true) {},
                NavBarItem(MindfulIcons.Settings, "Settings") {},
            ),
            action = NavBarItem(MindfulIcons.Edit, "New entry") {},
        )
    }
}
