package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.theme.AppColor

@Composable
fun CircleButton(
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    background: Color = AppColor.current.cardCircle,
    tint: Color = AppColor.current.ink,
    shadow: Dp = 0.dp,
    enabled: Boolean = true,
    squircleOnPress: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Exponent 2 is a circle, 4 a squircle: morph to the squircle while held.
    val exponent by animateFloatAsState(
        targetValue = if (squircleOnPress && pressed) 4f else 2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "squircleMorph"
    )
    val shape = if (squircleOnPress) SuperellipseShape(exponent) else CircleShape

    Box(
        modifier = modifier
            .size(size)
            .then(if (shadow > 0.dp) Modifier.shadow(shadow, shape, clip = false) else Modifier)
            .clip(shape)
            .background(background)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(iconSize))
    }
}
