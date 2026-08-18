package com.aryariap.forfh.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Token Shapes Material 3 & Linear Design System.
 * Corner radii diekstrak dari spec Linear & Notion Calendar (6dp, 8dp, 12dp, 16dp, Capsule).
 */
val ForfhShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)
