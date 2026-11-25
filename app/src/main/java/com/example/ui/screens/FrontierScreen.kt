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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.engine.*
import com.example.model.*
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PixelMonsterIcon
import com.example.ui.components.ProgressBarThin
import com.example.ui.components.formatCompact
import com.example.ui.theme.*

@Composable
fun FrontierScreen(
    game: GameState,
    seasonalStatus: SeasonalStatus,
    soloEventStatus: SoloEventStatus,
    combatLogs: List<CombatLogEntry>,
    onDeployRegion: (String) -> Unit,
    onDeployDungeonFloor: (Int) -> Unit,
    onAttackDungeonFloor: (Int) -> Unit,
    onEatBestFood: () -> Unit,
    onPunchTree: () -> Unit,
    onSelectPhalanx: (PhalanxStance) -> Unit,
    onOpenEventsModal: () -> Unit,
    onDeployApexBoss: (String, String) -> Unit,
    onBuyBonusApex: () -> Unit,
    onClaimContract: (String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Dungeon, 1 = Regions
    val activeRegion = GameCatalog.regionById(game.activeRegionId)
    val activeRegionTitle = if (activeRegion.id == "village_outskirts") "${game.villageName} Outskirts" else activeRegion.name
    val activeHoliday = seasonalStatus.effectiveHoliday
    val currentAge = game.currentMaterialAge()
    val nextAge = currentAge.next()
    val citadelLvl = game.buildingLevel(BuildingType.CITADEL)
    val lodgeLvl = game.buildingLevel(BuildingType.HUNTERS_LODGE)

    var showAllRegions by remember { mutableStateOf(false) }
    val visibleRegions = remember(game.warlordLevel, citadelLvl, showAllRegions) {
        if (showAllRegions) {
            GameCatalog.regions
        } else {
            val unlocked = GameCatalog.regions.filter {
                game.warlordLevel >= it.reqWarlordLevel && citadelLvl >= it.reqCitadelLevel
            }
            val nextLocked = GameCatalog.regions.firstOrNull {
                game.warlordLevel < it.reqWarlordLevel || citadelLvl < it.reqCitadelLevel
            }
            if (nextLocked != null) unlocked + nextLocked else unlocked
        }
    }

    val maxApex = 3 + game.apexBonusAttempts
    val apexLeft = (maxApex - game.apexAttemptsUsed).coerceAtLeast(0)
    val readyContracts = game.dailyContracts.filter { !it.claimed && it.progress >= it.target }
    val isDungeonAssigned = game.squads.take(game.unlockedSquadSlots())
        .any { it.taskType == SquadTaskType.DUNGEON_FLOOR }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Zone Header
        item {
            ObsidianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "${currentAge.displayName} • ${game.settlementStageName()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = if (mode == 0) "Dungeon Floor ${game.currentDungeonFloor}" else activeRegionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenEventsModal,
                        border = BorderStroke(1.dp, CharcoalBorder),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("open_events_button")
                    ) {
                        Text(
                            text = if (activeHoliday != null) activeHoliday.name else "Events & Bosses",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // True Minecraft Survival Path Card for Early Game
        if (citadelLvl < 3 || game.pickaxeTier == 0 || game.axeTier == 0) {
            item {
                val floor1Kills = game.dungeonFloorKills["1"] ?: 0
                val stepHint = when {
                    game.wood < 4 && game.axeTier == 0 ->
                        "Step 1: Start from nothing — punch trees to collect Wood for your first tools."
                    game.axeTier == 0 || game.pickaxeTier == 0 ->
                        "Step 2: Open Artisan -> Smith & Forge to craft your Wood Axe & Wood Pickaxe."
                    floor1Kills < 3 ->
                        "Step 3: Hunt Dungeon Floor 1 (Chicken, Cow, Sheep) for meat, eggs, leather & wool."
                    game.ironBars == 0L ->
                        "Step 4: Cook food in Artisan, mine Ore veins with your Pickaxe & smelt Ingots in Furnace."
                    else ->
                        "Step 5: Upgrade Citadel to Lv.${nextAge?.reqCitadelLevel ?: 3} in Stronghold to enter ${nextAge?.displayName ?: "Stone Age"}!"
                }
                ObsidianCard(borderColor = EmeraldActive.copy(alpha = 0.45f), contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Survival Progression", style = MaterialTheme.typography.titleSmall, color = EmeraldActive)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(stepHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (game.wood < 4 && game.axeTier == 0) {
                            Button(
                                onClick = onPunchTree,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("punch_tree_button")
                            ) {
                                Text("Punch Tree", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Claimable Contract Quick Bar
        if (readyContracts.isNotEmpty()) {
            item {
                val c = readyContracts.first()
                ObsidianCard(borderColor = EmeraldActive.copy(alpha = 0.6f), contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Contract Complete: ${c.title}", style = MaterialTheme.typography.titleSmall, color = EmeraldActive)
                            Text("+${c.rewardGold}g • +${c.rewardCrowns} Crowns", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Button(
                            onClick = { onClaimContract(c.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Claim", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Combat Card + Quick Eat
        item {
            val showingDungeon = mode == 0 || isDungeonAssigned
            val displayedEnemyName = if (showingDungeon) game.currentDungeonEnemyName else game.currentEnemyName
            val displayedEnemyHp = if (showingDungeon) game.currentDungeonEnemyHp else game.currentEnemyHp
            val displayedEnemyMaxHp = if (showingDungeon) game.currentDungeonEnemyMaxHp else game.currentEnemyMaxHp

            ObsidianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HP ${game.currentHp}/${game.maxHp()}", style = MonoStatStyle, color = EmeraldActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        ProgressBarThin(
                            progress = game.currentHp.toFloat() / game.maxHp().coerceAtLeast(1),
                            fillColor = EmeraldActive,
                            height = 6.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelMonsterIcon(
                                troopType = activeRegion.enemyTroopType,
                                isBoss = displayedEnemyMaxHp > 350,
                                size = 22.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$displayedEnemyName: $displayedEnemyHp", style = MonoStatStyle, color = CrimsonCombat, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ProgressBarThin(
                            progress = displayedEnemyHp.toFloat() / displayedEnemyMaxHp.coerceAtLeast(1),
                            fillColor = CrimsonCombat,
                            height = 6.dp
                        )
                    }
                    OutlinedButton(
                        onClick = onEatBestFood,
                        enabled = game.totalCookedFoodCount() > 0 && game.currentHp < game.maxHp(),
                        border = BorderStroke(1.dp, CharcoalBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("frontier_eat_food_button")
                    ) {
                        Text("Eat (${game.totalCookedFoodCount()})", fontSize = 11.sp, color = EmeraldActive)
                    }
                }

                if (game.troops.totalTroops() >= 4 || citadelLvl >= 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val mult = GameSimulationEngine.phalanxMultiplier(game.activePhalanx, activeRegion.enemyTroopType)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Phalanx Formation", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            text = "Matchup ${(mult * 100).toInt()}%",
                            style = MonoStatStyle,
                            color = if (mult > 1f) EmeraldActive else if (mult < 1f) CrimsonCombat else TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PhalanxStance.entries.forEach { stance ->
                            val selected = game.activePhalanx == stance
                            Surface(
                                onClick = { onSelectPhalanx(stance) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) CharcoalElevated else ObsidianBg,
                                border = BorderStroke(1.dp, if (selected) TextPrimary else CharcoalBorder),
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                    Text(
                                        text = stance.troopType.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (combatLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CharcoalDivider)
                    Spacer(modifier = Modifier.height(6.dp))
                    combatLogs.take(2).forEach { log ->
                        Text(
                            text = "• ${log.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                log.isAlert -> CrimsonCombat
                                log.isLoot -> VioletForge
                                else -> TextSecondary
                            }
                        )
                    }
                }
            }
        }

        // Toggle: Dungeon Floors vs Frontier Regions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Dungeon Floors", "Frontier Regions").forEachIndexed { idx, label ->
                    val selected = mode == idx
                    Surface(
                        onClick = { mode = idx },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected) CharcoalElevated else CharcoalSurface,
                        border = BorderStroke(1.dp, if (selected) EmeraldActive else CharcoalBorder),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("frontier_mode_$idx")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (mode == 0) {
            items(GameCatalog.dungeonFloors) { floor ->
                val unlocked = floor.floor <= game.highestDungeonFloor
                val kills = game.dungeonFloorKills[floor.floor.toString()] ?: 0
                val isCurrent = game.currentDungeonFloor == floor.floor
                val monstersText = floor.monsters.joinToString(" • ") { "${it.name} (${it.dropsSummary})" }

                ObsidianCard(
                    borderColor = when {
                        !unlocked -> CharcoalDivider
                        isCurrent -> EmeraldActive.copy(alpha = 0.65f)
                        else -> CharcoalBorder
                    }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(floor.name, style = MaterialTheme.typography.titleSmall, color = if (unlocked) TextPrimary else TextMuted)
                                Text(floor.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            if (floor.floor < GameCatalog.dungeonFloors.size) {
                                val nextPct = if (kills >= floor.clearsToUnlockNext) "Next Open" else "$kills/${floor.clearsToUnlockNext}"
                                Text(nextPct, style = MonoStatStyle, color = if (kills >= floor.clearsToUnlockNext) EmeraldActive else TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(monstersText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onAttackDungeonFloor(floor.floor) },
                                enabled = unlocked,
                                border = BorderStroke(1.dp, if (unlocked) EmeraldActive else CharcoalBorder),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("attack_dungeon_floor_${floor.floor}")
                            ) {
                                Text(if (unlocked) "Hunt Now" else "Locked", fontSize = 12.sp, color = if (unlocked) EmeraldActive else TextMuted)
                            }
                            Button(
                                onClick = { onDeployDungeonFloor(floor.floor) },
                                enabled = unlocked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent && isDungeonAssigned) EmeraldActive else CharcoalElevated,
                                    contentColor = if (isCurrent && isDungeonAssigned) ObsidianBg else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (unlocked) CharcoalBorder else CharcoalDivider),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("deploy_dungeon_floor_${floor.floor}")
                            ) {
                                Text(if (unlocked) "Auto-Delve" else "Floor ${floor.floor - 1} First", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hunting & Regions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    TextButton(onClick = { showAllRegions = !showAllRegions }) {
                        Text(if (showAllRegions) "Compact List" else "Show All Realms", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            items(visibleRegions) { region ->
                val unlocked = game.warlordLevel >= region.reqWarlordLevel && citadelLvl >= region.reqCitadelLevel
                val kills = game.regionKills[region.id] ?: 0
                val masteryPct = GameSimulationEngine.conquestMasteryBonusPct(kills, lodgeLvl)
                val isActive = game.activeRegionId == region.id
                val regionTitle = if (region.id == "village_outskirts") "${game.villageName} Outskirts" else region.name

                ObsidianCard(
                    borderColor = when {
                        !unlocked -> CharcoalDivider
                        isActive -> EmeraldActive.copy(alpha = 0.65f)
                        else -> CharcoalBorder
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            PixelMonsterIcon(
                                troopType = region.enemyTroopType,
                                isBoss = false,
                                size = 30.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(regionTitle, style = MaterialTheme.typography.titleSmall, color = if (unlocked) TextPrimary else TextMuted)
                                Text(region.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${region.enemyName} • +${region.goldReward}g • Might ${formatCompact(region.recommendedMight)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (unlocked) TextSecondary else CrimsonCombat
                                )
                                if (kills > 0) {
                                    Text(
                                        text = "Mastery: $kills slain (+$masteryPct% Loot & Dmg)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldActive
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { onDeployRegion(region.id) },
                            enabled = unlocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) EmeraldActive else CharcoalElevated,
                                contentColor = if (isActive) ObsidianBg else TextPrimary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("deploy_region_${region.id}")
                        ) {
                            Text(
                                text = if (!unlocked) "Lv.${region.reqWarlordLevel}/C${region.reqCitadelLevel}" else "Deploy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Daily Apex Siege Bosses
        if (citadelLvl >= 3) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Apex Siege", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Attempts: $apexLeft / $maxApex", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = onBuyBonusApex,
                        border = BorderStroke(1.dp, CharcoalBorder),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("+1 Try (20 Cr)", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
            items(GameCatalog.apexBosses.filter { citadelLvl >= it.reqCitadelLevel }) { boss ->
                ObsidianCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(boss.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${boss.title} • +${boss.goldReward}g & +${boss.crownBonus} Crowns", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Button(
                            onClick = { onDeployApexBoss(boss.id, boss.name) },
                            enabled = apexLeft > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletForge, contentColor = TextPrimary),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Siege", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
