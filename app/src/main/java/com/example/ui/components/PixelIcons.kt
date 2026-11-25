package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.EquipSlot
import com.example.model.GearRarity
import com.example.model.MaterialAge
import com.example.model.SkillType
import com.example.model.TroopType
import com.example.ui.theme.*

enum class ResourceIconType {
    WOOD, STONE, COAL, ORE, INGOT, RATION, CHICKEN, EGG, STEAK, MUTTON, LEATHER, WOOL, FISH, GRAIN, HERB, HIDE, DUST, DIAMOND, VIBRANIUM, COIN, DEVOTION, PICKAXE
}

@Composable
fun PixelEquipIcon(
    slot: EquipSlot,
    rarity: GearRarity,
    age: MaterialAge = MaterialAge.WOOD,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val frameColor = rarityColor(rarity)
    val metalColor = when (age) {
        MaterialAge.WOOD -> Color(0xFF8C735B)
        MaterialAge.STONE -> Color(0xFF828791)
        MaterialAge.IRON -> Color(0xFFA2A8B2)
        MaterialAge.GOLD -> Color(0xFFB3A997)
        MaterialAge.DIAMOND -> Color(0xFF7B95A6)
        MaterialAge.OBSIDIAN -> Color(0xFF827794)
        MaterialAge.VIBRANIUM -> Color(0xFF9C89A3)
    }
    val highlight = Color.White.copy(alpha = 0.30f)
    val handleColor = Color(0xFF5D4B3B)

    PixelBox(size = size, borderColor = frameColor.copy(alpha = 0.65f), modifier = modifier) { px ->
        when (slot) {
            EquipSlot.WEAPON -> {
                px(5, 1, metalColor, 2, 2)
                px(4, 2, metalColor, 2, 2)
                px(3, 3, metalColor, 2, 2)
                px(6, 1, highlight, 1, 1)
                px(2, 4, RoyalGold, 3, 1)
                px(1, 5, handleColor, 2, 2)
            }
            EquipSlot.HELM -> {
                px(2, 1, metalColor, 4, 2)
                px(1, 2, metalColor, 6, 4)
                px(2, 2, highlight, 2, 1)
                px(2, 4, CharcoalElevated, 4, 1)
                px(3, 5, CharcoalElevated, 2, 2)
            }
            EquipSlot.ARMOR -> {
                px(1, 1, metalColor, 2, 2)
                px(5, 1, metalColor, 2, 2)
                px(2, 2, metalColor, 4, 5)
                px(3, 3, frameColor, 2, 2)
                px(2, 2, highlight, 1, 2)
            }
            EquipSlot.SHIELD -> {
                px(1, 1, frameColor, 6, 5)
                px(2, 6, frameColor, 4, 1)
                px(3, 7, frameColor, 2, 1)
                px(2, 2, metalColor, 4, 3)
                px(3, 3, highlight, 2, 1)
            }
            EquipSlot.SIGNET_RING -> {
                px(3, 1, frameColor, 2, 2)
                px(3, 1, highlight, 1, 1)
                px(2, 3, RoyalGold, 4, 1)
                px(1, 4, RoyalGold, 1, 2)
                px(6, 4, RoyalGold, 1, 2)
                px(2, 6, RoyalGold, 4, 1)
            }
        }
    }
}

@Composable
fun PixelResourceIcon(
    type: ResourceIconType,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val wood = Color(0xFF7D624C)
    val bark = Color(0xFF594534)
    val stone = Color(0xFF7D828C)
    val ore = Color(0xFF917560)
    val ingot = Color(0xFFA2A8B2)
    val sage = EmeraldActive
    val slate = RarityRare
    val plum = VioletForge
    val clay = CrimsonCombat

    PixelBox(size = size, borderColor = CharcoalBorder, modifier = modifier) { px ->
        when (type) {
            ResourceIconType.WOOD -> {
                px(1, 2, bark, 6, 4)
                px(2, 3, wood, 4, 2)
                px(5, 2, Color(0xFF9C8168), 2, 4)
            }
            ResourceIconType.STONE -> {
                px(2, 1, stone, 4, 2)
                px(1, 3, stone, 6, 4)
                px(2, 2, Color.White.copy(alpha = 0.25f), 2, 1)
            }
            ResourceIconType.ORE -> {
                px(1, 2, stone, 6, 5)
                px(2, 3, ore, 2, 2)
                px(5, 4, ore, 1, 2)
            }
            ResourceIconType.INGOT -> {
                px(2, 2, ingot, 4, 1)
                px(1, 3, ingot, 6, 3)
                px(2, 3, Color.White.copy(alpha = 0.3f), 3, 1)
            }
            ResourceIconType.RATION -> {
                px(2, 2, wood, 4, 4)
                px(3, 1, clay, 2, 2)
                px(2, 5, Color(0xFFA69888), 4, 2)
            }
            ResourceIconType.FISH -> {
                px(2, 3, slate, 4, 2)
                px(1, 2, slate, 1, 4)
                px(6, 3, Color(0xFF92A8BF), 1, 2)
            }
            ResourceIconType.GRAIN -> {
                px(3, 1, Color(0xFFA69B85), 2, 5)
                px(2, 2, Color(0xFFA69B85), 1, 2)
                px(5, 3, Color(0xFFA69B85), 1, 2)
            }
            ResourceIconType.HERB -> {
                px(3, 2, sage, 2, 5)
                px(1, 3, sage, 2, 2)
                px(5, 2, sage, 2, 2)
            }
            ResourceIconType.HIDE -> {
                px(2, 1, wood, 4, 6)
                px(1, 2, bark, 1, 4)
                px(6, 2, bark, 1, 4)
            }
            ResourceIconType.DUST -> {
                px(2, 4, plum, 4, 3)
                px(3, 2, plum, 2, 2)
                px(1, 6, plum, 6, 1)
            }
            ResourceIconType.DIAMOND -> {
                px(3, 1, RarityDivine, 2, 1)
                px(2, 2, RarityDivine, 4, 2)
                px(1, 3, RarityCelestial, 6, 2)
                px(3, 5, RarityDivine, 2, 2)
            }
            ResourceIconType.VIBRANIUM -> {
                px(2, 1, RarityTranscendent, 4, 2)
                px(1, 3, plum, 6, 3)
                px(3, 6, RarityTranscendent, 2, 1)
            }
            ResourceIconType.COIN -> {
                px(2, 1, RoyalGold, 4, 6)
                px(1, 2, RoyalGold, 6, 4)
                px(3, 3, CharcoalSurface, 2, 2)
            }
            ResourceIconType.DEVOTION -> {
                px(3, 1, TextPrimary, 2, 2)
                px(2, 3, plum, 4, 2)
                px(3, 5, slate, 2, 1)
                px(2, 6, slate, 4, 1)
            }
            ResourceIconType.COAL -> {
                px(2, 2, CharcoalElevated, 4, 4)
                px(1, 3, Color(0xFF2B303A), 6, 3)
                px(2, 2, TextMuted, 2, 1)
            }
            ResourceIconType.CHICKEN -> {
                px(2, 2, Color(0xFFB58C6B), 4, 4)
                px(5, 4, Color(0xFFD6CFC7), 2, 2)
                px(1, 3, Color(0xFF9C7254), 2, 2)
            }
            ResourceIconType.EGG -> {
                px(3, 1, Color(0xFFD6CFC7), 2, 1)
                px(2, 2, Color(0xFFC9BFB2), 4, 4)
                px(3, 6, Color(0xFFB3A99C), 2, 1)
            }
            ResourceIconType.STEAK -> {
                px(1, 2, clay, 6, 4)
                px(2, 3, Color(0xFFC28E8E), 3, 2)
                px(5, 3, Color(0xFFD6CFC7), 1, 2)
            }
            ResourceIconType.MUTTON -> {
                px(2, 2, clay, 4, 3)
                px(2, 5, Color(0xFFC9BFB2), 2, 2)
            }
            ResourceIconType.LEATHER -> {
                px(2, 1, Color(0xFF8C5D43), 4, 6)
                px(1, 2, Color(0xFF6B442E), 1, 4)
                px(6, 2, Color(0xFF6B442E), 1, 4)
            }
            ResourceIconType.WOOL -> {
                px(2, 2, Color(0xFFC8CDD4), 4, 4)
                px(1, 3, Color(0xFFAFB6BF), 6, 2)
            }
            ResourceIconType.PICKAXE -> {
                px(1, 1, slate, 6, 2)
                px(3, 3, wood, 2, 4)
            }
        }
    }
}

@Composable
fun PixelMonsterIcon(
    troopType: TroopType,
    isBoss: Boolean = false,
    size: Dp = 30.dp,
    modifier: Modifier = Modifier
) {
    val body = if (isBoss) CrimsonCombat else TextSecondary
    val eye = if (isBoss) TextPrimary else CrimsonCombat
    val trim = if (isBoss) VioletForge else CharcoalBorder

    PixelBox(size = size, borderColor = if (isBoss) CrimsonCombat.copy(alpha = 0.6f) else CharcoalBorder, modifier = modifier) { px ->
        when (troopType) {
            TroopType.CAVALRY -> {
                // Beast / Wolf / Mounted silhouette
                px(1, 2, body, 2, 2)
                px(5, 2, body, 2, 2)
                px(2, 3, body, 4, 3)
                px(2, 4, eye, 1, 1)
                px(5, 4, eye, 1, 1)
                px(3, 6, trim, 2, 1)
            }
            TroopType.RANGED -> {
                // Hooded Ranger / Marksman
                px(2, 1, EmeraldActive, 4, 2)
                px(2, 3, body, 4, 3)
                px(3, 4, eye, 2, 1)
                px(6, 1, Color(0xFF7D624C), 1, 6)
            }
            TroopType.INFANTRY -> {
                // Visored Sentinel / Goblin
                px(2, 1, body, 4, 5)
                px(1, 2, body, 6, 2)
                px(2, 3, eye, 1, 1)
                px(5, 3, eye, 1, 1)
                px(2, 6, trim, 4, 1)
            }
            TroopType.SIEGE -> {
                // Horned Construct / Titan
                px(1, 1, trim, 1, 2)
                px(6, 1, trim, 1, 2)
                px(2, 2, body, 4, 5)
                px(3, 3, eye, 2, 1)
                px(1, 4, body, 6, 2)
            }
        }
    }
}

@Composable
fun PixelSkillIcon(
    skill: SkillType,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    when (skill) {
        SkillType.WOODCUTTING -> PixelResourceIcon(ResourceIconType.WOOD, size, modifier)
        SkillType.MINING -> PixelResourceIcon(ResourceIconType.ORE, size, modifier)
        SkillType.FISHING -> PixelResourceIcon(ResourceIconType.FISH, size, modifier)
        SkillType.FARMING -> PixelResourceIcon(ResourceIconType.GRAIN, size, modifier)
        SkillType.HUNTING -> PixelResourceIcon(ResourceIconType.HIDE, size, modifier)
        SkillType.FORAGING -> PixelResourceIcon(ResourceIconType.HERB, size, modifier)
        SkillType.COOKING -> PixelResourceIcon(ResourceIconType.RATION, size, modifier)
        SkillType.SMITHING -> PixelResourceIcon(ResourceIconType.INGOT, size, modifier)
        SkillType.CRAFTING -> PixelEquipIcon(EquipSlot.WEAPON, GearRarity.COMMON, MaterialAge.IRON, size, modifier)
        SkillType.ALCHEMY -> PixelBox(size, EmeraldActive.copy(alpha = 0.5f), modifier) { px ->
            px(3, 1, TextSecondary, 2, 2)
            px(2, 3, EmeraldActive, 4, 4)
        }
        SkillType.TAILORING -> PixelEquipIcon(EquipSlot.ARMOR, GearRarity.COMMON, MaterialAge.WOOD, size, modifier)
        SkillType.CONSTRUCTION -> PixelResourceIcon(ResourceIconType.STONE, size, modifier)
        SkillType.DIVINITY -> PixelResourceIcon(ResourceIconType.DEVOTION, size, modifier)
        SkillType.THIEVING -> PixelResourceIcon(ResourceIconType.COIN, size, modifier)
        SkillType.ENCHANTING -> PixelResourceIcon(ResourceIconType.DUST, size, modifier)
        else -> PixelEquipIcon(EquipSlot.SHIELD, GearRarity.COMMON, MaterialAge.IRON, size, modifier)
    }
}

@Composable
private fun PixelBox(
    size: Dp,
    borderColor: Color,
    modifier: Modifier = Modifier,
    drawPixels: (px: (Int, Int, Color, Int, Int) -> Unit) -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .background(ObsidianBg, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 6.dp)) {
            val cell = this.size.width / 8f
            drawPixels { x, y, c, w, h ->
                drawRect(
                    color = c,
                    topLeft = Offset(x * cell, y * cell),
                    size = Size(w * cell, h * cell)
                )
            }
        }
    }
}
