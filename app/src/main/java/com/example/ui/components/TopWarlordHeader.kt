package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.engine.CalendarEventEngine
import com.example.engine.RealmSurgeStatus
import com.example.engine.SeasonalStatus
import com.example.engine.SoloEventStatus
import com.example.model.GameState
import com.example.model.SquadAssignment
import com.example.model.SquadTaskType
import com.example.ui.theme.*

@Composable
fun TopWarlordHeader(
    game: GameState,
    seasonalStatus: SeasonalStatus,
    soloEventStatus: SoloEventStatus,
    realmSurgeStatus: RealmSurgeStatus,
    isSquadStripExpanded: Boolean,
    onToggleSquadStrip: () -> Unit,
    onOpenTreasury: () -> Unit,
    onOpenEvents: () -> Unit,
    onAssignSquadClick: (Int) -> Unit,
    onRecallSquadClick: (Int) -> Unit
) {
    val accents = LocalSeasonalAccents.current
    val xpReq = GameCatalog.xpRequiredForLevel(game.warlordLevel).coerceAtLeast(1L)
    val xpProgress = (game.warlordXp.toFloat() / xpReq.toFloat()).coerceIn(0f, 1f)
    val currentAge = game.currentMaterialAge()

    Surface(
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Settlement + Age + Warlord Level/XP (Left) | Gold, Might, Treasury (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = game.settlementStageName(),
                            style = MaterialTheme.typography.titleMedium,
                            color = accents.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = EmeraldDim,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmeraldActive.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "LV ${game.warlordLevel}",
                                style = MonoStatStyle.copy(fontSize = 10.sp),
                                color = EmeraldActive,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentAge.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ProgressBarThin(
                            progress = xpProgress,
                            fillColor = EmeraldActive,
                            height = 4.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeaderStatChip(
                        icon = Icons.Filled.MonetizationOn,
                        value = formatCompact(game.gold),
                        tint = RoyalGold
                    )
                    HeaderStatChip(
                        icon = Icons.Filled.Shield,
                        value = formatCompact(game.effectiveArmyMight()),
                        tint = EmeraldActive
                    )
                    Surface(
                        onClick = onOpenTreasury,
                        shape = RoundedCornerShape(6.dp),
                        color = RoyalGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("treasury_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = "Open Royal Treasury",
                                tint = RoyalGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${game.royalCrowns}",
                                style = MonoStatStyle.copy(fontSize = 11.sp),
                                color = RoyalGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Compact Events Pill + Squad Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val activeHoliday = seasonalStatus.effectiveHoliday
                val pillColor = activeHoliday?.primaryAccent ?: accents.primary
                val pillLabel = if (activeHoliday != null) {
                    "${activeHoliday.name} • ${soloEventStatus.type.displayName}"
                } else {
                    "${soloEventStatus.type.displayName} • ${realmSurgeStatus.type.displayName}"
                }

                Surface(
                    onClick = onOpenEvents,
                    shape = RoundedCornerShape(6.dp),
                    color = CharcoalElevated,
                    border = BorderStroke(1.dp, pillColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("active_event_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pillLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CalendarEventEngine.formatDuration(realmSurgeStatus.remainingMs).substring(0, 5),
                            style = MonoStatStyle.copy(fontSize = 10.sp),
                            color = pillColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val unlocked = game.unlockedSquadSlots()
                val activeCount = game.squads.take(unlocked).count { it.taskType != SquadTaskType.IDLE }
                Surface(
                    onClick = onToggleSquadStrip,
                    shape = RoundedCornerShape(6.dp),
                    color = CharcoalElevated,
                    border = BorderStroke(1.dp, CharcoalBorder),
                    modifier = Modifier.testTag("toggle_squad_strip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Squads $activeCount/$unlocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Icon(
                            imageVector = if (isSquadStripExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Squad Bar",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSquadStripExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val unlocked = game.unlockedSquadSlots()
                val nextLockLevel = GameCatalog.squadUnlockCitadelLevels.getOrNull(unlocked)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until unlocked) {
                        SquadSlotTile(
                            assignment = game.squads[i],
                            compactMode = unlocked >= 4,
                            onAssign = { onAssignSquadClick(i) },
                            onRecall = { onRecallSquadClick(i) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (nextLockLevel != null && unlocked < 3) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianBg,
                            border = BorderStroke(1.dp, CharcoalDivider),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+Slot\nCit Lv.$nextLockLevel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderStatChip(
    icon: ImageVector,
    value: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = CharcoalElevated,
        border = BorderStroke(1.dp, CharcoalBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, style = MonoStatStyle.copy(fontSize = 11.sp), color = TextPrimary)
        }
    }
}

@Composable
private fun SquadSlotTile(
    assignment: SquadAssignment,
    compactMode: Boolean,
    onAssign: () -> Unit,
    onRecall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = assignment.taskType != SquadTaskType.IDLE
    val borderColor = if (isActive) EmeraldActive.copy(alpha = 0.65f) else CharcoalBorder
    val bgColor = if (isActive) CharcoalElevated else ObsidianBg.copy(alpha = 0.6f)

    val cycleMs = when (assignment.taskType) {
        SquadTaskType.ARTISAN_SKILL, SquadTaskType.SEASONAL_NODE -> 2500f
        SquadTaskType.RAID_REGION, SquadTaskType.DUNGEON_FLOOR, SquadTaskType.SEASONAL_BOSS,
        SquadTaskType.APEX_BOSS, SquadTaskType.WEEKLY_REALM_BOSS -> 1800f
        SquadTaskType.IDLE -> 1f
    }
    val progress = if (isActive) (assignment.progressMs.toFloat() / cycleMs).coerceIn(0f, 1f) else 0f

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .heightIn(min = 52.dp)
            .testTag("squad_slot_${assignment.slotIndex}")
            .clickable { if (isActive) onRecall() else onAssign() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (compactMode) "S${assignment.slotIndex + 1}" else if (isActive) assignment.targetLabel else "Squad ${assignment.slotIndex + 1} • Idle",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isActive) "Recall" else "Assign",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) CrimsonCombat else RoyalGold
                )
            }
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                ProgressBarThin(progress = progress, fillColor = EmeraldActive, height = 4.dp)
            }
        }
    }
}
