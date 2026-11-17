package com.conreo.couchytv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@Composable
fun tvFocusColors(background: Color) = ClickableSurfaceDefaults.colors(
    containerColor = background,
    focusedContainerColor = lerp(Color(0xFF151B25), MaterialTheme.colorScheme.primary, .12f),
    contentColor = Color.White, focusedContentColor = Color.White,
)

@Composable
fun tvFocusBorder(shape: Shape = RoundedCornerShape(12.dp)) = ClickableSurfaceDefaults.border(
    focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = shape),
)
