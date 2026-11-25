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
import com.example.model.*
import com.example.ui.components.ObsidianCard
import com.example.ui.components.ProgressBarThin
import com.example.ui.components.formatCompact
import com.example.ui.theme.*
import kotlin.math.ceil

@Composable
fun StrongholdScreen(
    game: GameState,
    onUpgradeBuilding: (BuildingType) -> Unit,
    onTrainTroops: (TroopType, Int) -> Unit,
    onHealWounded: () -> Unit,
    onUnlockOrEquipCompanion: (String) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Buildings, 1 = Garrison, 2 = Sanctuary
    var selectedCategory by remember { mutableStateOf(BuildingCategory.CORE) }
    var selectedTier by remember { mutableIntStateOf(1) }

    val barracksLvl = game.buildingLevel(BuildingType.BARRACKS)
    val citadelLvl = game.buildingLevel(BuildingType.CITADEL)
    val infirmaryLvl = game.buildingLevel(BuildingType.INFIRMARY)
    val sanctuaryLvl = game.buildingLevel(BuildingType.BEAST_SANCTUARY)
    val hasteActive = game.isBlessingActive(RoyalBlessingType.BUILD_TRAINING_HASTE)
    val baseBatch = 3 + (citadelLvl - 1)
    val batchSize = if (hasteActive) ceil(baseBatch * 1.25).toInt() else baseBatch
    val costMult = if (hasteActive) 0.75 else 1.0
    val infCapacity = 10 + infirmaryLvl * 12

    val currentAge = game.currentMaterialAge()
    val nextAge = currentAge.next()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Settlement & Material Age Summary
        item {
            ObsidianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(game.settlementStageName(), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = "Citadel Lv.$citadelLvl • ${currentAge.displayName} (Age ${currentAge.tier}/7)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${game.troops.totalTroops()} Troops", style = MonoStatStyle, color = EmeraldActive)
                        Text("${formatCompact(game.effectiveArmyMight())} Might • Instant Upgrades", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                ProgressBarThin(
                    progress = currentAge.tier / 7f,
                    fillColor = EmeraldActive,
                    height = 5.dp
                )
                if (nextAge != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Next: ${nextAge.displayName} at Citadel Lv.${nextAge.reqCitadelLevel} — ${nextAge.summary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Sub-navigation inside Stronghold
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Buildings (15)", "Garrison", "Companions").forEachIndexed { idx, label ->
                    val selected = selectedSection == idx
                    Surface(
                        onClick = { selectedSection = idx },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected) CharcoalElevated else CharcoalSurface,
                        border = BorderStroke(1.dp, if (selected) TextPrimary else CharcoalBorder),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("stronghold_tab_$idx")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) TextPrimary else TextSecondary)
                        }
                    }
                }
            }
        }

        when (selectedSection) {
            0 -> {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BuildingCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                items(BuildingType.entries.filter { it.category == selectedCategory }) { building ->
                    val curLvl = game.buildingLevel(building)
                    val goldCost = ceil(GameCatalog.buildingGoldCost(building, curLvl) * costMult).toLong()
                    val woodCost = ceil(GameCatalog.buildingWoodCost(building, curLvl) * costMult).toLong()
                    val stoneCost = ceil(GameCatalog.buildingStoneCost(building, curLvl) * costMult).toLong()
                    val barCost = ceil(GameCatalog.buildingBarCost(building, curLvl) * costMult).toLong()
                    val canAfford = curLvl < building.maxLevel &&
                        game.gold >= goldCost && game.wood >= woodCost &&
                        game.stone >= stoneCost && game.ironBars >= barCost

                    val costString = buildString {
                        append("${goldCost}g • $woodCost Wood")
                        if (stoneCost > 0) append(" • $stoneCost Stone")
                        if (barCost > 0) append(" • $barCost Ingots")
                    }

                    ObsidianCard(borderColor = if (canAfford) EmeraldActive.copy(alpha = 0.55f) else CharcoalBorder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(building.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lv.$curLvl/${building.maxLevel}", style = MonoStatStyle, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(building.perkSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (curLvl >= building.maxLevel) "Max Level" else costString,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canAfford) EmeraldActive else CrimsonCombat
                                )
                            }
                            Button(
                                onClick = { onUpgradeBuilding(building) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                                border = BorderStroke(1.dp, if (canAfford) EmeraldActive else CharcoalBorder),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("upgrade_${building.name.lowercase()}")
                            ) {
                                Text(if (curLvl >= building.maxLevel) "Max" else "Upgrade", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            1 -> {
                if (game.woundedTroops > 0) {
                    item {
                        ObsidianCard(borderColor = CrimsonCombat.copy(alpha = 0.6f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Infirmary Triage", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("Wounded: ${game.woundedTroops} / $infCapacity (${game.woundedTroops * 10}g)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Button(
                                    onClick = onHealWounded,
                                    enabled = game.gold >= game.woundedTroops * 10L,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("heal_wounded_button")
                                ) {
                                    Text("Heal", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    val mastery = game.unitMasteryBonuses()
                    ObsidianCard(borderColor = VioletForge.copy(alpha = 0.45f)) {
                        Text("Unit Skill Mastery (Permanent)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unit ATK +${mastery.unitAttackPct}%", style = MonoStatStyle, color = CrimsonCombat)
                            Text("Unit HP +${mastery.unitHpPct}%", style = MonoStatStyle, color = EmeraldActive)
                            Text("Unit DEF +${mastery.unitDefensePct}%", style = MonoStatStyle, color = RarityAncient)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Trained via Smithing (ATK), Cooking (HP) & Divinity (DEF) • +${mastery.unitMightPct}% Army Might",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                item {
                    Column {
                        Text("Recruit Troops (Batch +$batchSize)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Rations available: ${game.warRations}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (barracksLvl >= 4) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1 to 1, 2 to 4, 3 to 9).forEach { (tier, reqBarracks) ->
                                val unlocked = barracksLvl >= reqBarracks
                                val selected = selectedTier == tier
                                Surface(
                                    onClick = { if (unlocked) selectedTier = tier },
                                    enabled = unlocked,
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selected) CharcoalElevated else CharcoalSurface,
                                    border = BorderStroke(1.dp, if (selected) TextPrimary else CharcoalBorder),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                        Text(
                                            text = if (unlocked) "Tier $tier" else "T$tier (Bar $reqBarracks)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (!unlocked) TextMuted else if (selected) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(TroopType.entries) { troop ->
                    val r = game.troops
                    val totalOfType = when (troop) {
                        TroopType.INFANTRY -> r.t1Infantry + r.t2Infantry + r.t3Infantry
                        TroopType.RANGED -> r.t1Ranged + r.t2Ranged + r.t3Ranged
                        TroopType.CAVALRY -> r.t1Cavalry + r.t2Cavalry + r.t3Cavalry
                        TroopType.SIEGE -> r.t1Siege + r.t2Siege + r.t3Siege
                    }
                    val goldCost = ceil(baseBatch * (6L * selectedTier * selectedTier) * costMult).toLong()
                    val rationCost = ceil(baseBatch * selectedTier * costMult).toInt().coerceAtLeast(1)
                    val canTrain = game.gold >= goldCost && game.warRations >= rationCost

                    ObsidianCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("${troop.displayName} ($totalOfType)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    text = "Cost: ${goldCost}g • $rationCost Rations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (canTrain) TextSecondary else CrimsonCombat
                                )
                            }
                            Button(
                                onClick = { onTrainTroops(troop, selectedTier) },
                                enabled = canTrain,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("train_${troop.name.lowercase()}")
                            ) {
                                Text("+$batchSize", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Text("Beast Sanctuary (Lv.$sanctuaryLvl) Companions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Each Sanctuary level amplifies active companion bonuses by +6%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                items(GameCatalog.companions) { pet ->
                    val unlocked = pet.id in game.unlockedCompanions
                    val active = game.activeCompanionId == pet.id
                    val reqMet = sanctuaryLvl >= pet.reqSanctuaryLevel
                    val canAfford = game.gold >= pet.goldCost && reqMet

                    ObsidianCard(borderColor = if (active) EmeraldActive.copy(alpha = 0.65f) else CharcoalBorder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("${pet.name} • ${pet.role}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(pet.bonusText, style = MaterialTheme.typography.bodySmall, color = EmeraldActive)
                                if (!unlocked) {
                                    Text(
                                        text = if (reqMet) "Bond: ${pet.goldCost}g" else "Requires Beast Sanctuary Lv.${pet.reqSanctuaryLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (canAfford) TextSecondary else CrimsonCombat
                                    )
                                }
                            }
                            Button(
                                onClick = { onUnlockOrEquipCompanion(pet.id) },
                                enabled = unlocked || canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) EmeraldDim else CharcoalElevated,
                                    contentColor = if (active) EmeraldActive else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (active) EmeraldActive else CharcoalBorder),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(if (active) "Active" else if (unlocked) "Equip" else "Bond", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
