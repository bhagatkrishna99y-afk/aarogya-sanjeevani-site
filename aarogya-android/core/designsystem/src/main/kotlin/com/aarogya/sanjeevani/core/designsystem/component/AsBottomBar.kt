// AsBottomBarItem lives here, next to the AsBottomBar this file is really about.
@file:Suppress("MatchingDeclarationName")

package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** One destination in [AsBottomBar]. [selectedIcon] defaults to [icon]
 *  when a destination has no distinct filled/outlined pair. [badgeText],
 *  when non-null, draws a small count badge on this item's icon (an
 *  unread-notifications count, e.g.) -- the caller is responsible for
 *  already-formatted/capped text (a patient-app shared helper does
 *  that, not this generic design-system component). */
data class AsBottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeText: String? = null,
)

/** The one bottom navigation bar either app's root scaffold should use.
 *  Each [NavigationBarItem] already gives Material's own 48dp+ touch
 *  target and selected/unselected semantics for TalkBack -- nothing
 *  extra to add here beyond wiring [items] consistently. */
@Composable
fun AsBottomBar(
    items: List<AsBottomBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(index) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                icon = {
                    BadgedBox(
                        badge = {
                            item.badgeText?.let { text ->
                                Badge(modifier = Modifier.semantics { contentDescription = "$text unread" }) {
                                    Text(text)
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = null,
                        )
                    }
                },
                label = { Text(item.label) },
            )
        }
    }
}

@Preview(name = "Bottom bar - light")
@Preview(name = "Bottom bar - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsBottomBarPreview() {
    AarogyaTheme {
        Surface {
            AsBottomBar(
                items =
                    listOf(
                        AsBottomBarItem(label = "Home", icon = Icons.Filled.Home),
                        AsBottomBarItem(label = "Bookings", icon = Icons.Filled.CalendarToday),
                        AsBottomBarItem(label = "Profile", icon = Icons.Filled.Person),
                    ),
                selectedIndex = 0,
                onItemSelected = {},
            )
        }
    }
}
