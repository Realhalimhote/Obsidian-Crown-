package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.model.*
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PixelEquipIcon
import com.example.ui.components.RarityPill
import com.example.ui.components.rarityColor
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun WarlordScreen(
    game: GameState,
    onEquipItem: (EquipmentItem) -> Unit,
    onInstantUpgradeSlot: (EquipSlot) -> Unit,
    onEnchantSlot: (EquipSlot) -> Unit,
    onUpdateAutoEat: (Int) -> Unit,
    onEatRationNow: () -> Unit,
    onEatFood: (FoodType) -> Unit,
    onSellJunk: () -> Unit,
    onToggleAutoSalvage: (Boolean) -> Unit,
    onSelectTitle: (String) -> Unit,
    onOpenBackupModal: () -> Unit,
    onStartNewVillage: () -> Unit
) {
    var selectedSlotFilter by remember { mutableStateOf<EquipSlot?>(null) }
    var confirmRestart by remember { mutableStateOf(false) }

    val availableTitles = remember(game.warlordLevel, game.buildings, game.royalObsidianTitleUnlocked) {
        GameCatalog.availableTitles(game)
    }
    val filteredVault = remember(game.vaultItems, selectedSlotFilter) {
        val base = if (selectedSlotFilter == null) game.vaultItems
        else game.vaultItems.filter { it.slot == selectedSlotFilter }
        base.sortedByDescending { it.rarity.rank }
    }
    val combatSkills = remember {
        SkillType.entries.filter { it.category == SkillCategory.COMBAT }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Warlord Profile & Combat Skills Summary (Clean text & numbers, no decorative banner clutter)
        item {
            ObsidianCard {
                Text(game.warlordTitle, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = "${game.settlementStageName()} • ${game.currentMaterialAge().displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("ATK ${game.attackPower()}", CrimsonCombat)
                    StatPill("DEF ${game.defensePower()}", EmeraldActive)
                    StatPill("HP ${game.maxHp()}", TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CharcoalDivider)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Combat Proficiencies", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    combatSkills.forEach { sk ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${game.skillProgress(sk).level}", style = MonoStatStyle, color = TextPrimary)
                            Text(sk.displayName.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val unitMastery = game.unitMasteryBonuses()
                Text(
                    text = "Skill Mastery -> Units: +${unitMastery.unitAttackPct}% ATK (Smith) • +${unitMastery.unitHpPct}% HP (Cook) • +${unitMastery.unitDefensePct}% DEF (Divinity)",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldActive
                )
                if (availableTitles.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableTitles) { title ->
                            FilterChip(
                                selected = game.warlordTitle == title,
                                onClick = { onSelectTitle(title) },
                                label = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Collection Hall Discovery Summary
        item {
            ObsidianCard(contentPadding = PaddingValues(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Royal Collection Hall (Lv.${game.buildingLevel(BuildingType.COLLECTION_HALL)})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${game.discoveredGearIds.size} unique relics cataloged", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text("+${game.collectionHallBonusPct()}% Hall Bonus", style = MonoStatStyle, color = EmeraldActive)
                }
            }
        }

        // Auto-Eat Rations Control
        item {
            ObsidianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pantry Food & Auto-Eat", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            text = "Auto-eat at ${game.autoEatThresholdPct}% HP • Total Food: ${game.totalCookedFoodCount()} (Rations: ${game.warRations})",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldActive
                        )
                    }
                    Button(
                        onClick = onEatRationNow,
                        enabled = game.totalCookedFoodCount() > 0 && game.currentHp < game.maxHp(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Eat Best", fontWeight = FontWeight.Bold)
                    }
                }
                val ownedFoods = FoodType.entries.filter { (game.cookedFoods[it.name] ?: 0) > 0 }
                if (ownedFoods.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ownedFoods) { food ->
                            val count = game.cookedFoods[food.name] ?: 0
                            OutlinedButton(
                                onClick = { onEatFood(food) },
                                enabled = game.currentHp < game.maxHp(),
                                border = BorderStroke(1.dp, CharcoalBorder),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("${food.displayName} x$count (+${food.healHp})", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
                Slider(
                    value = game.autoEatThresholdPct.toFloat(),
                    onValueChange = { onUpdateAutoEat(it.roundToInt()) },
                    valueRange = 10f..85f,
                    colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = EmeraldActive),
                    modifier = Modifier.testTag("auto_eat_slider")
                )
            }
        }

        // 5 Equipment Slots
        item {
            Text("Equipped Gear", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        items(EquipSlot.entries) { slot ->
            val equipped = game.equipped[slot.name]
            val border = if (equipped != null) rarityColor(equipped.rarity).copy(alpha = 0.6f) else CharcoalBorder
            ObsidianCard(borderColor = border) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        if (equipped != null) {
                            PixelEquipIcon(equipped.slot, equipped.rarity, equipped.age, size = 34.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(slot.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            if (equipped != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RarityPill(equipped.rarity)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (equipped.enchantLevel > 0) "${equipped.name} +${equipped.enchantLevel}" else equipped.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${equipped.age.displayName} • +${equipped.attackBonus} ATK • +${equipped.defenseBonus} DEF • +${equipped.hpBonus} HP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldActive
                                )
                            } else {
                                Text("Empty Slot", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (equipped != null && equipped.rarity.next() != null) {
                            val (uGold, uBars) = GameCatalog.rarityUpgradeCost(equipped)
                            val canUpg = game.gold >= uGold && game.ironBars >= uBars
                            OutlinedButton(
                                onClick = { onInstantUpgradeSlot(slot) },
                                enabled = canUpg,
                                border = BorderStroke(1.dp, if (canUpg) EmeraldActive else CharcoalBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("+Upg", fontSize = 11.sp, color = if (canUpg) EmeraldActive else TextMuted)
                            }
                        }
                        if (equipped != null && game.runeDust >= 4L + equipped.enchantLevel * 4L) {
                            OutlinedButton(
                                onClick = { onEnchantSlot(slot) },
                                border = BorderStroke(1.dp, VioletForge),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("+Ench", fontSize = 11.sp, color = VioletForge)
                            }
                        }
                        OutlinedButton(
                            onClick = { selectedSlotFilter = if (selectedSlotFilter == slot) null else slot },
                            border = BorderStroke(1.dp, CharcoalBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (selectedSlotFilter == slot) "All" else "Filter", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Vault Management Header
        item {
            ObsidianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inventory Vault", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${game.vaultItems.size} / ${game.maxVaultCapacity()} Slots", style = MonoStatStyle, color = TextSecondary)
                    }
                    Button(
                        onClick = onSellJunk,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonCombat, contentColor = TextPrimary),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("sell_junk_button")
                    ) {
                        Text("Sell Junk", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Salvage Common Junk", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            text = if (game.vaultExpansionOwned) "Automatically sells Common non-seasonal drops"
                            else "Requires Vault Expansion Pack in Treasury",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = game.autoSalvageJunkEnabled,
                        onCheckedChange = onToggleAutoSalvage,
                        enabled = game.vaultExpansionOwned
                    )
                }
            }
        }

        if (filteredVault.isEmpty()) {
            item {
                ObsidianCard {
                    Text(
                        text = "Vault empty. Craft Wood Age gear in Artisan or hunt in ${game.villageName} Outskirts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(filteredVault, key = { it.id }) { item ->
                ObsidianCard(borderColor = rarityColor(item.rarity).copy(alpha = 0.55f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            PixelEquipIcon(item.slot, item.rarity, item.age, size = 32.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RarityPill(item.rarity)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                }
                                Text(
                                    text = "${item.age.displayName} • +${item.attackBonus} ATK • +${item.defenseBonus} DEF • +${item.hpBonus} HP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Button(
                            onClick = { onEquipItem(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                            border = BorderStroke(1.dp, CharcoalBorder),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Equip", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Local Save Export / Import Backup & Fresh Village
        item {
            OutlinedButton(
                onClick = onOpenBackupModal,
                border = BorderStroke(1.dp, CharcoalBorder),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_backup_modal_button")
            ) {
                Text("Local Save Backup (Export / Import JSON)", color = TextPrimary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { confirmRestart = true },
                border = BorderStroke(1.dp, CharcoalBorder),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text("Start Fresh in New Random Village", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            containerColor = CharcoalSurface,
            title = { Text("Start Fresh Village?", color = TextPrimary) },
            text = { Text("You will start from scratch in a new random Wood Age hamlet. Permanent Treasury perks are kept.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRestart = false
                        onStartNewVillage()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonCombat, contentColor = TextPrimary)
                ) {
                    Text("Start Fresh")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MonoStatStyle.copy(fontSize = 11.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
