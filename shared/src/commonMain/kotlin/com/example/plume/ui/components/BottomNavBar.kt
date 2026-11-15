package com.example.plume.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class NavTab(val route: String, val titleKey: String, val icon: ImageVector) {
    data object Home : NavTab("home", "home", Icons.Default.Home)
    data object Search : NavTab("search", "search", Icons.Default.Search)
    data object Library : NavTab("library", "library", Icons.Default.Menu)

    companion object {
        val tabs: List<NavTab>
            get() = listOf(Home, Search, Library)
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(32.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp)
            .shadow(
                elevation = 20.dp,
                shape = pillShape,
                ambientColor = Color.Black,
                spotColor = Color.White.copy(alpha = 0.25f)
            ),
        color = Color(0xFF0C0C10).copy(alpha = 0.88f),
        shape = pillShape,
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.88f else if (selected) 1.05f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "tabScale"
                )

                Box(
                    modifier = Modifier
                        .size(52.dp, 44.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .then(
                            if (selected) {
                                Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(20.dp)
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.route,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp)
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
