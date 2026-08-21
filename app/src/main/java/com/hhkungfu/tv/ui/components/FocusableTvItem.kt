package com.hhkungfu.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hhkungfu.tv.ui.theme.FocusBorder
import com.hhkungfu.tv.ui.theme.NetflixRed

@Composable
fun FocusableTvItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    focusedScale: Float = 1.06f,
    focusedBorderColor: Color = FocusBorder,
    focusedBorderWidth: Dp = 2.5.dp,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "FocusScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 12.dp else 0.dp,
                shape = shape,
                ambientColor = NetflixRed,
                spotColor = NetflixRed
            )
            .clip(shape)
            .border(
                border = if (isFocused) BorderStroke(focusedBorderWidth, focusedBorderColor) else BorderStroke(0.dp, Color.Transparent),
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        content(isFocused)
    }
}
