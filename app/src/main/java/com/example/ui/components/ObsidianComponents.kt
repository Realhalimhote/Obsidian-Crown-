package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GearRarity
import com.example.ui.theme.*

@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CharcoalBorder,
    backgroundColor: Color = CharcoalSurface,
    cornerRadius: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun ProgressBarThin(
    progress: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = ObsidianBg,
    height: Dp = 5.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "progress_bar_fill"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(2.dp))
                .background(fillColor)
        )
    }
}

@Composable
fun RarityPill(rarity: GearRarity, modifier: Modifier = Modifier) {
    val color = rarityColor(rarity)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.40f))
    ) {
        Text(
            text = rarity.displayName.uppercase(),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

fun rarityColor(rarity: GearRarity): Color = when (rarity) {
    GearRarity.COMMON -> RarityCommon
    GearRarity.UNCOMMON -> RarityUncommon
    GearRarity.RARE -> RarityRare
    GearRarity.EPIC -> RarityEpic
    GearRarity.LEGENDARY -> RarityLegendary
    GearRarity.MYTHIC -> RarityMythic
    GearRarity.ANCIENT -> RarityAncient
    GearRarity.DIVINE -> RarityDivine
    GearRarity.CELESTIAL -> RarityCelestial
    GearRarity.ETERNAL -> RarityEternal
    GearRarity.SOVEREIGN -> RaritySovereign
    GearRarity.TRANSCENDENT -> RarityTranscendent
}

fun formatCompact(value: Long): String {
    return when {
        value >= 1_000_000L -> String.format("%.2fM", value / 1_000_000.0)
        value >= 10_000L -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
