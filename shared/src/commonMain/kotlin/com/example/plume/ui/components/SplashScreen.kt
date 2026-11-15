package com.example.plume.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plume.ui.theme.NowPlayingBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Cinematic startup splash screen with floating neon particles,
 * glowing Plume brand emblem reveal, and dynamic audio wave aura.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Infinite breathing glow transition
    val transition = rememberInfiniteTransition(label = "splashPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Particle seed
    val particles = remember {
        List(24) {
            ParticleData(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = Random.nextFloat() * 4.5f + 1.5f,
                speedY = Random.nextFloat() * 0.4f + 0.1f,
                alpha = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            scale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        delay(400)
        subtitleAlpha.animateTo(1f, animationSpec = tween(600))
        delay(1400)
        alpha.animateTo(0f, animationSpec = tween(400))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NowPlayingBase),
        contentAlignment = Alignment.Center
    ) {
        // Floating neon particles background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                val x = p.xRatio * width
                val y = (p.yRatio * height) % height
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha * alpha.value * pulse),
                    radius = p.radius.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Radial central ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f * pulse * alpha.value),
                        Color.White.copy(alpha = 0.05f * pulse * alpha.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.65f
                ),
                radius = size.minDimension * 0.65f,
                center = center
            )
        }

        // Center Emblem & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .padding(24.dp)
        ) {
            // Glowing Glass Logo Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        ambientColor = Color.White,
                        spotColor = Color.White.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title "PLUME"
            Text(
                text = "PLUME",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    fontSize = 36.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Tagline
            Text(
                text = "PREMIUM SOUND ENGINE · 2026",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.55f * subtitleAlpha.value)
            )
        }
    }
}

private data class ParticleData(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val speedY: Float,
    val alpha: Float
)
