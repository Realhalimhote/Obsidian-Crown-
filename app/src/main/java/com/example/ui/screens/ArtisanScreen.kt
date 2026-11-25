package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.engine.SeasonalStatus
import com.example.model.*
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PixelEquipIcon
import com.example.ui.components.PixelResourceIcon
import com.example.ui.components.PixelSkillIcon
import com.example.ui.components.ProgressBarThin
import com.example.ui.components.RarityPill
import com.example.ui.components.ResourceIconType
import com.example.ui.components.formatCompact
import com.example.ui.theme.*

@Composable
fun ArtisanScreen(
    game: GameState,
    seasonalStatus: SeasonalStatus,
    onAssignSkill: (SkillType) -> Unit,
    onInstantSkill: (SkillType) -> Unit,
    onTogglePrayer: (DivinePrayer) -> Unit,
    onCraftTool: (ToolType) -> Unit,
    onSmeltOre: (FurnaceRecipe) -> Unit,
    onCookFood: (FoodType) -> Unit,
    onEatFood: (FoodType) -> Unit,
    onAssignSeasonalNode: (String, String) -> Unit,
    onFuseGear: (String, GearRarity) -> Unit,
    onFuseAllGear: () -> Unit,
    onCraftAgeGear: (MaterialAge) -> Unit,
    onInstantUpgradeSlot: (EquipSlot) -> Unit,
    onEnchantSlot: (EquipSlot) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Professions, 1 = Smith & Forge, 2 = Divinity
    var selectedCategory by remember { mutableStateOf<SkillCategory?>(null) }
    val activeHoliday = seasonalStatus.effectiveHoliday
    val citadelLvl = game.buildingLevel(BuildingType.CITADEL)
    val currentAge = game.currentMaterialAge()

    val fusionGroups = remember(game.vaultItems) {
        game.vaultItems
            .filter { it.rarity != GearRarity.TRANSCENDENT }
            .groupBy { it.templateId to it.rarity }
            .values
            .sortedWith(compareByDescending<List<EquipmentItem>> { it.size >= 3 }.thenByDescending { it.first().rarity.rank })
    }

    val visibleAges = remember(citadelLvl) {
        val unlocked = MaterialAge.entries.filter { citadelLvl >= it.reqCitadelLevel }
        val nextLocked = MaterialAge.entries.firstOrNull { citadelLvl < it.reqCitadelLevel }
        if (nextLocked != null) unlocked + nextLocked else unlocked
    }

    val filteredSkillNodes = remember(selectedCategory) {
        if (selectedCategory == null) GameCatalog.skillNodes
        else GameCatalog.skillNodes.filter { it.skill.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Compact Material Ledger
        ObsidianCard(contentPadding = PaddingValues(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResourceBadge("Wood", formatCompact(game.wood), EmeraldActive, ResourceIconType.WOOD)
                ResourceBadge("Stone", formatCompact(game.stone), TextSecondary, ResourceIconType.STONE)
                ResourceBadge("Coal", formatCompact(game.coal), TextSecondary, ResourceIconType.COAL)
                ResourceBadge("Ore", formatCompact(game.ore), TextPrimary, ResourceIconType.ORE)
                ResourceBadge("Ingots", "${game.ironBars}", VioletForge, ResourceIconType.INGOT)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResourceBadge("Fish", "${game.rawFish}", TextSecondary, ResourceIconType.FISH)
                ResourceBadge("Chicken", "${game.rawChicken}", TextPrimary, ResourceIconType.CHICKEN)
                ResourceBadge("Eggs", "${game.eggs}", TextSecondary, ResourceIconType.EGG)
                ResourceBadge("Beef", "${game.rawBeef}", CrimsonCombat, ResourceIconType.STEAK)
                ResourceBadge("Mutton", "${game.rawMutton}", TextPrimary, ResourceIconType.MUTTON)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResourceBadge("Leather", "${game.leather}", TextSecondary, ResourceIconType.LEATHER)
                ResourceBadge("Wool", "${game.wool}", TextPrimary, ResourceIconType.WOOL)
                ResourceBadge("Herbs", "${game.herbs}", EmeraldActive, ResourceIconType.HERB)
                ResourceBadge("Food", "${game.totalCookedFoodCount()}", EmeraldActive, ResourceIconType.RATION)
                ResourceBadge("Devotion", "${game.devotion}", RarityAncient, ResourceIconType.DEVOTION)
            }
            if (currentAge.tier >= MaterialAge.GOLD.tier || game.goldOre > 0 || game.goldBars > 0 ||
                game.diamonds > 0 || game.obsidianShards > 0 || game.vibraniumOre > 0 || game.vibranium > 0
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (game.goldOre > 0 || game.goldBars > 0 || currentAge.tier >= MaterialAge.GOLD.tier) {
                        ResourceBadge("Gold Ing", "${game.goldBars}", RoyalGold, ResourceIconType.INGOT)
                    }
                    if (game.diamonds > 0 || currentAge.tier >= MaterialAge.DIAMOND.tier) {
                        ResourceBadge("Diamonds", "${game.diamonds}", RarityDivine, ResourceIconType.DIAMOND)
                    }
                    if (game.obsidianShards > 0 || currentAge.tier >= MaterialAge.OBSIDIAN.tier) {
                        ResourceBadge("Obsidian", "${game.obsidianShards}", VioletForge, ResourceIconType.STONE)
                    }
                    if (game.vibraniumOre > 0 || game.vibranium > 0 || currentAge.tier >= MaterialAge.VIBRANIUM.tier) {
                        ResourceBadge("Vibran", "${game.vibranium}", RarityTranscendent, ResourceIconType.VIBRANIUM)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SubModeButton(
                label = "Skills (15)",
                selected = selectedSection == 0,
                accent = EmeraldActive,
                onClick = { selectedSection = 0 },
                modifier = Modifier.weight(1f).testTag("artisan_skilling_tab")
            )
            val readyFusions = fusionGroups.count { it.size >= 3 }
            SubModeButton(
                label = if (readyFusions > 0) "Tools & Forge ($readyFusions)" else "Tools & Forge",
                selected = selectedSection == 1,
                accent = VioletForge,
                onClick = { selectedSection = 1 },
                modifier = Modifier.weight(1.1f).testTag("artisan_forge_tab")
            )
            SubModeButton(
                label = "Divinity",
                selected = selectedSection == 2,
                accent = RarityAncient,
                onClick = { selectedSection = 2 },
                modifier = Modifier.weight(0.9f).testTag("artisan_divinity_tab")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedSection) {
            0 -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All", fontSize = 11.sp) }
                    )
                    listOf(SkillCategory.GATHERING, SkillCategory.PRODUCTION, SkillCategory.SPECIALIZATION).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val mastery = game.unitMasteryBonuses()
                        ObsidianCard(contentPadding = PaddingValues(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Unit Mastery", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                Text(
                                    text = "ATK +${mastery.unitAttackPct}% • HP +${mastery.unitHpPct}% • DEF +${mastery.unitDefensePct}%",
                                    style = MonoStatStyle.copy(fontSize = 11.sp),
                                    color = EmeraldActive
                                )
                            }
                        }
                    }

                    items(filteredSkillNodes) { node ->
                        val progress = game.skillProgress(node.skill)
                        val masteryDetail = game.skillMasteryDetail(node.skill)
                        val xpReq = GameCatalog.xpRequiredForLevel(progress.level).coerceAtLeast(1L)
                        val isAssigned = game.squads.take(game.unlockedSquadSlots())
                            .any { it.taskType == SquadTaskType.ARTISAN_SKILL && it.targetId == node.skill.name }

                        ObsidianCard(borderColor = if (isAssigned) EmeraldActive.copy(alpha = 0.65f) else CharcoalBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    PixelSkillIcon(node.skill, size = 28.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(node.skill.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Lv.${progress.level}/${node.skill.maxLevel}", style = MonoStatStyle, color = EmeraldActive)
                                        }
                                        Text("${node.actionTitle} • ${node.yieldDescription}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        if (masteryDetail != null) {
                                            Text(
                                                text = "Mastery (${masteryDetail.rank.label}): ${masteryDetail.bonusLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RarityAncient
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        ProgressBarThin(
                                            progress = progress.xp.toFloat() / xpReq.toFloat(),
                                            fillColor = EmeraldActive,
                                            height = 4.dp
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onInstantSkill(node.skill) },
                                        border = BorderStroke(1.dp, CharcoalBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("instant_skill_${node.skill.name.lowercase()}")
                                    ) {
                                        Text(instantActionLabel(node.skill), fontSize = 11.sp, color = TextPrimary)
                                    }
                                    Button(
                                        onClick = { onAssignSkill(node.skill) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAssigned) EmeraldDim else CharcoalElevated,
                                            contentColor = if (isAssigned) EmeraldActive else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isAssigned) EmeraldActive else CharcoalBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("assign_skill_${node.skill.name.lowercase()}")
                                    ) {
                                        Text(if (isAssigned) "Auto" else "Squad", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (activeHoliday != null) {
                        item {
                            Text(
                                text = "${activeHoliday.name} Nodes",
                                style = MaterialTheme.typography.titleSmall,
                                color = activeHoliday.primaryAccent
                            )
                        }
                        items(activeHoliday.nodes) { sNode ->
                            ObsidianCard(borderColor = activeHoliday.primaryAccent.copy(alpha = 0.55f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                        Text(sNode.name, style = MaterialTheme.typography.titleSmall, color = activeHoliday.primaryAccent)
                                        Text(sNode.yieldSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = { onAssignSeasonalNode(sNode.id, sNode.name) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                                        border = BorderStroke(1.dp, activeHoliday.primaryAccent),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Harvest", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gathering Tools (Bare Hands -> Wood -> Stone -> Iron -> ... -> Vibranium)
                    item {
                        Text("Gathering Tools Progression", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Craft Wood tools first to mine Stone & Ore veins, then upgrade up to Vibranium", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    items(ToolType.entries) { tool ->
                        val curTier = game.toolTier(tool)
                        val nextTier = curTier + 1
                        val cost = if (nextTier <= 7) GameCatalog.toolCraftCost(nextTier) else null
                        val canCraft = cost != null && game.wood >= cost.wood && game.stone >= cost.stone &&
                            game.ironBars >= cost.ironBars && game.goldBars >= cost.goldBars &&
                            game.diamonds >= cost.diamonds && game.obsidianShards >= cost.obsidian &&
                            game.vibranium >= cost.vibraniumBars
                        val costText = if (cost == null) "Max Tier Reached" else buildList {
                            if (cost.wood > 0) add("${cost.wood} Wood")
                            if (cost.stone > 0) add("${cost.stone} Stone")
                            if (cost.ironBars > 0) add("${cost.ironBars} Iron Ingot")
                            if (cost.goldBars > 0) add("${cost.goldBars} Gold Ingot")
                            if (cost.diamonds > 0) add("${cost.diamonds} Diamond")
                            if (cost.obsidian > 0) add("${cost.obsidian} Obsidian")
                            if (cost.vibraniumBars > 0) add("${cost.vibraniumBars} Vibranium")
                        }.joinToString(" • ")

                        ObsidianCard(borderColor = if (canCraft) EmeraldActive.copy(alpha = 0.55f) else CharcoalBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    PixelResourceIcon(
                                        type = when (tool) {
                                            ToolType.AXE -> ResourceIconType.WOOD
                                            ToolType.PICKAXE -> ResourceIconType.PICKAXE
                                            ToolType.FISHING_ROD -> ResourceIconType.FISH
                                        },
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${tool.displayName} (${ToolType.tierLabel(curTier)})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = if (nextTier <= 7) "Next: ${ToolType.tierLabel(nextTier)} • $costText" else "Vibranium Max Tier",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (canCraft) EmeraldActive else TextSecondary
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onCraftTool(tool) },
                                    enabled = canCraft,
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, if (canCraft) EmeraldActive else CharcoalBorder),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("craft_tool_${tool.name.lowercase()}")
                                ) {
                                    Text(if (nextTier > 7) "Max" else "Craft", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Furnace Smelting (Raw Ore + Coal/Wood Fuel -> Usable Ingots)
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Furnace Smelting", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Fuel: ${game.coal} Coal or ${game.wood} Wood • Smelt raw ore veins into usable ingots", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    items(FurnaceRecipe.entries) { recipe ->
                        val hasFuel = game.coal >= 1L || game.wood >= 1L
                        val rawCount = when (recipe) {
                            FurnaceRecipe.SMELT_IRON -> game.ore
                            FurnaceRecipe.SMELT_GOLD -> game.goldOre
                            FurnaceRecipe.SMELT_VIBRANIUM -> game.vibraniumOre
                        }
                        val canSmelt = rawCount >= 2L && hasFuel
                        ObsidianCard(borderColor = if (canSmelt) VioletForge.copy(alpha = 0.55f) else CharcoalBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    PixelResourceIcon(ResourceIconType.INGOT, size = 26.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${recipe.displayName} -> ${recipe.outputName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = "2 Raw Ore ($rawCount owned) + 1 Coal/Wood Fuel • +${recipe.xpReward} Smith XP",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (canSmelt) EmeraldActive else TextSecondary
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onSmeltOre(recipe) },
                                    enabled = canSmelt,
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, if (canSmelt) VioletForge else CharcoalBorder),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("smelt_${recipe.name.lowercase()}")
                                ) {
                                    Text("Smelt", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Campfire & Hearth Kitchen (7 Food Options)
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Campfire & Hearth Kitchen", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Cook Chicken, Eggs, Beef, Mutton, Fish & Grain into hearty meals", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    items(FoodType.entries) { food ->
                        val owned = game.cookedFoods[food.name] ?: 0
                        val canCook = when (food) {
                            FoodType.ROAST_CHICKEN -> game.rawChicken >= 1L && game.wood >= 1L
                            FoodType.CAMPFIRE_EGGS -> game.eggs >= 2L
                            FoodType.SEARED_STEAK -> game.rawBeef >= 1L && game.wood >= 1L
                            FoodType.ROAST_MUTTON -> game.rawMutton >= 1L && game.herbs >= 1L
                            FoodType.GRILLED_FISH -> game.rawFish >= 1L && game.wood >= 1L
                            FoodType.HEARTH_BREAD -> game.grain >= 2L
                            FoodType.WARLORD_STEW -> game.rawChicken >= 1L && game.rawFish >= 1L && game.herbs >= 1L
                        }
                        val iconType = when (food) {
                            FoodType.ROAST_CHICKEN -> ResourceIconType.CHICKEN
                            FoodType.CAMPFIRE_EGGS -> ResourceIconType.EGG
                            FoodType.SEARED_STEAK -> ResourceIconType.STEAK
                            FoodType.ROAST_MUTTON -> ResourceIconType.MUTTON
                            FoodType.GRILLED_FISH -> ResourceIconType.FISH
                            FoodType.HEARTH_BREAD -> ResourceIconType.GRAIN
                            FoodType.WARLORD_STEW -> ResourceIconType.RATION
                        }
                        ObsidianCard(borderColor = if (canCook) EmeraldActive.copy(alpha = 0.45f) else CharcoalBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    PixelResourceIcon(iconType, size = 26.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${food.displayName} (x$owned)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = "+${food.healHp} HP • +${food.rationsYield} Rations • ${food.ingredientSummary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (canCook) EmeraldActive else TextSecondary
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { onCookFood(food) },
                                        enabled = canCook,
                                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                                        border = BorderStroke(1.dp, if (canCook) EmeraldActive else CharcoalBorder),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("cook_${food.name.lowercase()}")
                                    ) {
                                        Text("Cook", fontSize = 11.sp)
                                    }
                                    if (owned > 0) {
                                        OutlinedButton(
                                            onClick = { onEatFood(food) },
                                            enabled = game.currentHp < game.maxHp(),
                                            border = BorderStroke(1.dp, CharcoalBorder),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("eat_${food.name.lowercase()}")
                                        ) {
                                            Text("Eat", fontSize = 11.sp, color = EmeraldActive)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Instant Craft Material Age Gear", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Progress from Wood Age to Vibranium Age via Citadel upgrades", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    items(visibleAges.reversed()) { age ->
                        val unlocked = citadelLvl >= age.reqCitadelLevel
                        val cost = GameCatalog.ageCraftCost(age)
                        val canCraft = unlocked && game.gold >= cost.gold && game.wood >= cost.wood &&
                            game.stone >= cost.stone && game.ironBars >= cost.bars &&
                            game.diamonds >= cost.diamonds && game.vibranium >= cost.vibranium

                        val costLabel = buildString {
                            append("${cost.gold}g")
                            if (cost.wood > 0) append(" • ${cost.wood} Wood")
                            if (cost.stone > 0) append(" • ${cost.stone} Stone")
                            if (cost.bars > 0) append(" • ${cost.bars} Ingots")
                            if (cost.diamonds > 0) append(" • ${cost.diamonds} Diamonds")
                            if (cost.vibranium > 0) append(" • ${cost.vibranium} Vibranium")
                        }

                        ObsidianCard(borderColor = if (canCraft) EmeraldActive.copy(alpha = 0.5f) else CharcoalBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(age.displayName, style = MaterialTheme.typography.titleSmall, color = if (unlocked) TextPrimary else TextMuted)
                                    Text(
                                        text = if (unlocked) costLabel else "Unlocks at Citadel Lv.${age.reqCitadelLevel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (canCraft) EmeraldActive else TextSecondary
                                    )
                                }
                                Button(
                                    onClick = { onCraftAgeGear(age) },
                                    enabled = canCraft,
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, if (canCraft) EmeraldActive else CharcoalBorder),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("craft_age_${age.name.lowercase()}")
                                ) {
                                    Text(if (unlocked) "Craft" else "Locked", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (game.equipped.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Instant Gear Upgrade & Enchant", style = MaterialTheme.typography.titleMedium, color = VioletForge)
                            Text("Upgrade rarity immediately with Ingots (+25%) or Enchant with Rune Dust (+12%)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        items(EquipSlot.entries.filter { game.equipped[it.name] != null }) { slot ->
                            val item = game.equipped[slot.name]!!
                            val (upgGold, upgIngots) = GameCatalog.rarityUpgradeCost(item)
                            val canUpgradeRarity = item.rarity.next() != null && game.gold >= upgGold && game.ironBars >= upgIngots
                            val dustCost = 4L + (item.enchantLevel * 4L)
                            val canEnchant = game.runeDust >= dustCost

                            ObsidianCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        PixelEquipIcon(item.slot, item.rarity, item.age, size = 30.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${item.name} +${item.enchantLevel}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(
                                                text = if (item.rarity.next() != null) "Upg: ${upgGold}g, $upgIngots Ingots • Ench: $dustCost Dust"
                                                else "Max Rarity • Ench: $dustCost Dust",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (item.rarity.next() != null) {
                                            OutlinedButton(
                                                onClick = { onInstantUpgradeSlot(slot) },
                                                enabled = canUpgradeRarity,
                                                border = BorderStroke(1.dp, if (canUpgradeRarity) EmeraldActive else CharcoalBorder),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                modifier = Modifier.heightIn(min = 48.dp).testTag("instant_upgrade_${slot.name.lowercase()}")
                                            ) {
                                                Text("Upgrade", fontSize = 11.sp, color = if (canUpgradeRarity) EmeraldActive else TextMuted)
                                            }
                                        }
                                        Button(
                                            onClick = { onEnchantSlot(slot) },
                                            enabled = canEnchant,
                                            colors = ButtonDefaults.buttonColors(containerColor = VioletForge, contentColor = TextPrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Ench", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        val readyCount = fusionGroups.count { it.size >= 3 }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Royal Fusion Forge (Common -> Transcendent)", style = MaterialTheme.typography.titleMedium, color = VioletForge)
                                Text("Merge 3 identical pieces instantaneously for +25% stats", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            if (readyCount > 0) {
                                Button(
                                    onClick = onFuseAllGear,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                                    modifier = Modifier.heightIn(min = 44.dp).testTag("fuse_all_button")
                                ) {
                                    Text("Fuse All ($readyCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (fusionGroups.isEmpty()) {
                        item {
                            ObsidianCard {
                                Text("Craft gear above or loot enemies to gather 3 duplicates for Fusion.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    } else {
                        items(fusionGroups) { group ->
                            val sample = group.first()
                            val ready = group.size >= 3
                            val nextRarity = sample.rarity.next()
                            ObsidianCard(borderColor = if (ready) VioletForge else CharcoalBorder) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        PixelEquipIcon(sample.slot, sample.rarity, sample.age, size = 32.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RarityPill(sample.rarity)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(sample.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${sample.age.displayName} • ${group.size}/3 -> ${nextRarity?.displayName ?: "Max"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (ready) VioletForge else TextSecondary
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onFuseGear(sample.templateId, sample.rarity) },
                                        enabled = ready,
                                        colors = ButtonDefaults.buttonColors(containerColor = VioletForge, contentColor = TextPrimary),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("fuse_${sample.templateId}_${sample.rarity.name.lowercase()}")
                                    ) {
                                        Text(if (ready) "Fuse 3->1" else "${group.size}/3", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val divProg = game.skillProgress(SkillType.DIVINITY)
                val divReq = GameCatalog.xpRequiredForLevel(divProg.level).coerceAtLeast(1L)
                val shrineLvl = game.buildingLevel(BuildingType.DIVINITY_SHRINE)
                val isDivAssigned = game.squads.take(game.unlockedSquadSlots())
                    .any { it.taskType == SquadTaskType.ARTISAN_SKILL && it.targetId == SkillType.DIVINITY.name }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ObsidianCard(borderColor = RarityAncient.copy(alpha = 0.6f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PixelSkillIcon(SkillType.DIVINITY, size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Divinity & Sanctum Shrine (Lv.$shrineLvl)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Divinity Lv.${divProg.level}/${SkillType.DIVINITY.maxLevel} • Devotion: ${game.devotion}", style = MonoStatStyle, color = RarityAncient)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            ProgressBarThin(
                                progress = divProg.xp.toFloat() / divReq.toFloat(),
                                fillColor = RarityAncient,
                                height = 5.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onInstantSkill(SkillType.DIVINITY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RarityAncient, contentColor = ObsidianBg),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("instant_pray_button")
                                ) {
                                    Text("Pray Now (+Devotion)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onAssignSkill(SkillType.DIVINITY) },
                                    border = BorderStroke(1.dp, if (isDivAssigned) EmeraldActive else CharcoalBorder),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text(if (isDivAssigned) "Squad Auto" else "Assign Vigil", fontSize = 12.sp, color = if (isDivAssigned) EmeraldActive else TextPrimary)
                                }
                            }
                        }
                    }

                    item {
                        Text("Divine Prayers & Smite Boons", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Active prayer consumes Devotion on actions; Sanctum Shrine amplifies potency", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    items(DivinePrayer.entries) { prayer ->
                        val unlocked = divProg.level >= prayer.reqDivinityLevel
                        val active = game.activePrayerId == prayer.name
                        ObsidianCard(borderColor = if (active) EmeraldActive.copy(alpha = 0.7f) else CharcoalBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(prayer.displayName, style = MaterialTheme.typography.titleSmall, color = if (active) EmeraldActive else TextPrimary)
                                    Text(
                                        text = if (unlocked) "${prayer.bonusSummary} (${prayer.devotionCostPerAction} Dev/act)"
                                        else "Unlocks at Divinity Lv.${prayer.reqDivinityLevel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (unlocked) TextSecondary else CrimsonCombat
                                    )
                                }
                                Button(
                                    onClick = { onTogglePrayer(prayer) },
                                    enabled = unlocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) EmeraldDim else CharcoalElevated,
                                        contentColor = if (active) EmeraldActive else TextPrimary
                                    ),
                                    border = BorderStroke(1.dp, if (active) EmeraldActive else CharcoalBorder),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("prayer_${prayer.name.lowercase()}")
                                ) {
                                    Text(if (!unlocked) "Locked" else if (active) "Active" else "Invoke", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun instantActionLabel(skill: SkillType): String = when (skill) {
    SkillType.COOKING -> "Cook"
    SkillType.SMITHING -> "Smelt"
    SkillType.DIVINITY -> "Pray"
    SkillType.ALCHEMY -> "Brew"
    SkillType.ENCHANTING -> "Refine"
    SkillType.CRAFTING -> "Craft"
    SkillType.TAILORING -> "Weave"
    SkillType.CONSTRUCTION -> "Build"
    SkillType.THIEVING -> "Pilfer"
    SkillType.WOODCUTTING -> "Chop"
    SkillType.MINING -> "Mine"
    SkillType.FISHING -> "Fish"
    SkillType.FARMING -> "Farm"
    SkillType.HUNTING -> "Hunt"
    SkillType.FORAGING -> "Forage"
    else -> "Act"
}

@Composable
private fun ResourceBadge(label: String, value: String, color: Color, iconType: ResourceIconType) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PixelResourceIcon(iconType, size = 18.dp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MonoStatStyle.copy(fontSize = 11.sp), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun SubModeButton(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) CharcoalElevated else CharcoalSurface,
        border = BorderStroke(1.dp, if (selected) accent else CharcoalBorder),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) TextPrimary else TextSecondary
            )
        }
    }
}
