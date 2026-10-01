package pe.edu.upeu.pharmamobil.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Micro-interacción física para botones y tarjetas interactivas (Emil Kowalski / Apple Design).
 * Reduce sutilmente la escala (scale) y opacidad (alpha) cuando el usuario mantiene presionado el componente.
 */
@Composable
fun Modifier.pressFeedback(
    scaleDown: Float = 0.96f,
    alphaDown: Float = 0.92f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScaleSpec"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed) alphaDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressAlphaSpec"
    )

    val modifier = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }

    return if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        modifier
    }
}

/**
 * Animación de pulso suave (Shimmer Pulse) para esqueletos de carga elegantes.
 */
@Composable
fun Modifier.pulseLoading(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseLoadingTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    return this.graphicsLayer {
        this.alpha = alpha
    }
}
