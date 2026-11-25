package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ads.AdAvailability
import com.example.ads.AdManagerUiState
import com.example.billing.IapProduct
import com.example.data.GameCatalog
import com.example.engine.*
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun OfflineReportModal(
    report: OfflineReport,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ObsidianCard(
            borderColor = RoyalGold.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Warlord's Return Report", style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                    Text("Simulated ${report.elapsedMinutes}m offline (${report.cappedHours}h cap)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CharcoalDivider)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportStatBox("Gold", "+${formatCompact(report.goldEarned)}", RoyalGold, Modifier.weight(1f))
                ReportStatBox("Enemies", "${report.enemiesSlain}", CrimsonCombat, Modifier.weight(1f))
                ReportStatBox("Rations", "${report.rationsConsumed}", EmeraldActive, Modifier.weight(1f))
            }

            if (report.warlordXpGained > 0 || report.skillXpGained > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "XP Gained: +${formatCompact(report.warlordXpGained)} Warlord • +${formatCompact(report.skillXpGained)} Artisan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldActive
                )
            }

            if (report.resourcesSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(report.resourcesSummary.joinToString(" • "), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }

            if (report.itemsFound.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Spoils Found (${report.itemsFound.size})", style = MaterialTheme.typography.titleSmall, color = VioletForge)
                Spacer(modifier = Modifier.height(4.dp))
                report.itemsFound.take(4).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        PixelEquipIcon(item.slot, item.rarity, item.age, size = 24.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        RarityPill(item.rarity)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianBg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("claim_offline_report_button")
            ) {
                Text("Claim Spoils & Resume", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReportStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, CharcoalBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MonoStatStyle.copy(fontSize = 15.sp), color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun EventsAndAlmanacModal(
    game: GameState,
    seasonalStatus: SeasonalStatus,
    soloEventStatus: SoloEventStatus,
    realmSurgeStatus: RealmSurgeStatus,
    festivalStatus: KingdomFestivalStatus,
    onClaimSoloTier: (Int) -> Unit,
    onClaimContract: (String) -> Unit,
    onDeployWeeklyBoss: () -> Unit,
    onFightTournament: (Int) -> Unit,
    onSummonBoss: (HolidayId) -> Unit,
    onDeploySeasonalBoss: (HolidayId, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    val activeHoliday = seasonalStatus.effectiveHoliday

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ObsidianCard(
            borderColor = RoyalGold.copy(alpha = 0.65f),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .widthIn(max = 580.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Realm Events & Almanac", style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                    Text("Tokens: ${game.chronicleTokens} • Festival: ${festivalStatus.name}", style = MaterialTheme.typography.labelSmall, color = VioletForge)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Events", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Daily", "Weekly", "Seasonal").forEachIndexed { idx, label ->
                    val selected = selectedSection == idx
                    Surface(
                        onClick = { selectedSection = idx },
                        shape = RoundedCornerShape(4.dp),
                        color = if (selected) RoyalGold.copy(alpha = 0.2f) else CharcoalElevated,
                        border = BorderStroke(1.dp, if (selected) RoyalGold else CharcoalBorder),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) RoyalGold else TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedSection) {
                    0 -> {
                        // 2-Hour Realm Surge
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldDim,
                                border = BorderStroke(1.dp, EmeraldActive.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("2H REALM SURGE • ${realmSurgeStatus.type.displayName}", style = MaterialTheme.typography.titleSmall, color = EmeraldActive)
                                        Text(realmSurgeStatus.type.bonusSummary, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    }
                                    Text(CalendarEventEngine.formatDuration(realmSurgeStatus.remainingMs), style = MonoStatStyle, color = EmeraldActive)
                                }
                            }
                        }

                        // 6-Hour Rotating Solo Event
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CharcoalElevated,
                                border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("6H SOLO EVENT • ${soloEventStatus.type.displayName}", style = MaterialTheme.typography.titleSmall, color = RoyalGold)
                                            Text(soloEventStatus.type.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                        Text(CalendarEventEngine.formatDuration(soloEventStatus.remainingMs), style = MonoStatStyle, color = RoyalGold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ProgressBarThin(
                                        progress = (game.soloEventPoints / soloEventStatus.tierThresholds.last().toFloat()).coerceIn(0f, 1f),
                                        fillColor = RoyalGold,
                                        height = 6.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        soloEventStatus.tierThresholds.forEachIndexed { idx, threshold ->
                                            val reached = game.soloEventPoints >= threshold
                                            val claimed = idx in game.soloEventClaimedTiers
                                            OutlinedButton(
                                                onClick = { onClaimSoloTier(idx) },
                                                enabled = reached && !claimed,
                                                border = BorderStroke(1.dp, if (claimed) EmeraldActive else if (reached) RoyalGold else CharcoalBorder),
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                            ) {
                                                Text(
                                                    when {
                                                        claimed -> "T${idx + 1} ✓"
                                                        reached -> "Claim"
                                                        else -> "${game.soloEventPoints}/$threshold"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = if (claimed) EmeraldActive else if (reached) RoyalGold else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Daily Royal Contracts
                        item {
                            Text("Daily Royal Contracts", style = MaterialTheme.typography.titleSmall, color = RoyalGold)
                        }
                        items(game.dailyContracts) { contract ->
                            val ready = contract.progress >= contract.target && !contract.claimed
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CharcoalElevated,
                                border = BorderStroke(1.dp, if (ready) EmeraldActive else CharcoalBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(contract.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = "Progress: ${contract.progress}/${contract.target} • +${contract.rewardGold}g & +${contract.rewardCrowns} Cr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = { onClaimContract(contract.id) },
                                        enabled = ready,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (contract.claimed) EmeraldDim else RoyalGold,
                                            contentColor = if (contract.claimed) EmeraldActive else ObsidianBg
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(if (contract.claimed) "Done" else if (ready) "Claim" else "${contract.progress}/${contract.target}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Monthly Kingdom Festival
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VioletDim,
                                border = BorderStroke(1.dp, VioletForge.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("MONTHLY FESTIVAL • ${festivalStatus.name}", style = MaterialTheme.typography.titleSmall, color = VioletForge)
                                    Text("${festivalStatus.bonusLabel} (${festivalStatus.daysRemaining}d left)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                            }
                        }

                        // Weekly Realm Boss
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CharcoalElevated,
                                border = BorderStroke(1.dp, CrimsonCombat.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Weekly Realm Boss (${game.weeklyKey})", style = MaterialTheme.typography.titleSmall, color = CrimsonCombat)
                                        Text(
                                            text = if (game.weeklyBossDefeated) "DEFEATED" else "${formatCompact(game.weeklyBossHpRemaining)}/${formatCompact(game.weeklyBossMaxHp)} HP",
                                            style = MonoStatStyle,
                                            color = if (game.weeklyBossDefeated) EmeraldActive else CrimsonCombat
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ProgressBarThin(
                                        progress = if (game.weeklyBossMaxHp > 0) game.weeklyBossHpRemaining.toFloat() / game.weeklyBossMaxHp.toFloat() else 0f,
                                        fillColor = CrimsonCombat
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onDeployWeeklyBoss()
                                            onDismiss()
                                        },
                                        enabled = !game.weeklyBossDefeated,
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonCombat, contentColor = TextPrimary),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text(if (game.weeklyBossDefeated) "Weekly Spoils Claimed" else "Deploy Squad vs Weekly Realm Boss", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Weekly Grand Tournament (Offline AI Opponents)
                        item {
                            Text("Weekly Grand Tournament (${game.tournamentWinsThisWeek}/5)", style = MaterialTheme.typography.titleSmall, color = RoyalGold)
                        }
                        items(GameCatalog.tournamentOpponents) { opp ->
                            val beaten = opp.rank <= game.tournamentWinsThisWeek
                            val isNext = opp.rank == game.tournamentWinsThisWeek + 1
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CharcoalElevated,
                                border = BorderStroke(1.dp, if (beaten) EmeraldActive.copy(alpha = 0.5f) else if (isNext) RoyalGold else CharcoalBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text("#${opp.rank} ${opp.name}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text("${opp.title} • Power ${opp.powerRating} • +${opp.rewardGold}g, +${opp.rewardCrowns} Cr", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = { onFightTournament(opp.rank) },
                                        enabled = isNext,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (beaten) EmeraldDim else RoyalGold,
                                            contentColor = if (beaten) EmeraldActive else ObsidianBg
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(if (beaten) "Won" else if (isNext) "Duel" else "Locked", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Text(
                                text = if (activeHoliday != null) "Active Holiday: ${activeHoliday.name}"
                                else "Next Real-World Holiday: ${seasonalStatus.nextHoliday.name} in ${seasonalStatus.daysUntilNextHoliday} days",
                                style = MaterialTheme.typography.titleSmall,
                                color = activeHoliday?.primaryAccent ?: RoyalGold
                            )
                            if (activeHoliday != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        onDeploySeasonalBoss(activeHoliday.id, activeHoliday.bossName)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeHoliday.primaryAccent, contentColor = ObsidianBg),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                ) {
                                    Text("Hunt ${activeHoliday.bossName}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        items(GameCatalog.holidays) { holiday ->
                            val isLive = seasonalStatus.activeCalendarHoliday?.id == holiday.id ||
                                seasonalStatus.summonedOffSeasonHoliday?.id == holiday.id
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CharcoalElevated,
                                border = BorderStroke(1.dp, holiday.primaryAccent.copy(alpha = if (isLive) 0.85f else 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(holiday.name, style = MaterialTheme.typography.titleSmall, color = holiday.primaryAccent)
                                        Text(holiday.dateWindowLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Text("Boss: ${holiday.bossName}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    Text(holiday.bossSubtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { onSummonBoss(holiday.id) },
                                        enabled = !isLive && game.chronicleTokens > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = holiday.primaryAccent, contentColor = ObsidianBg),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            when {
                                                isLive -> "Currently Active"
                                                game.chronicleTokens > 0 -> "Summon for 2 Hours (1 Token)"
                                                else -> "Need Chronicle Token"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreasuryModal(
    game: GameState,
    isPlayConnected: Boolean,
    adUiState: AdManagerUiState,
    onPurchase: (Activity?, IapProduct) -> Unit,
    onUseCrownSpeedUp: () -> Unit,
    onBuyBonusApex: () -> Unit,
    onWatchRewardedBlessing: (Activity?, RoyalBlessingType) -> Unit,
    onSimulateDebugAdBlessing: (RoyalBlessingType) -> Unit,
    onActivateBlessingWithCrowns: (RoyalBlessingType) -> Unit,
    onToggleHideRewardedAds: (Boolean) -> Unit,
    onUpdateAdConfig: (Int, Int) -> Unit,
    onOpenPrivacyOptions: (Activity?) -> Unit,
    onDismiss: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Royal Blessings, 1 = Treasury Packs
    val nowMs = System.currentTimeMillis()
    val cooldownSec = ((game.adCooldownRemainingMs(nowMs) + 999L) / 1000L).toInt()
    val dailyLimitReached = game.adsWatchedToday >= game.dailyAdLimit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ObsidianCard(
            borderColor = RoyalGold.copy(alpha = 0.65f),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .widthIn(max = 580.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = RoyalGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Royal Treasury", style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                        Text("Optional Conveniences • 100% Playable Offline", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_treasury_button")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Treasury", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Royal Blessings", "Supporter Packs").forEachIndexed { idx, label ->
                    val selected = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(4.dp),
                        color = if (selected) RoyalGold.copy(alpha = 0.2f) else CharcoalElevated,
                        border = BorderStroke(1.dp, if (selected) RoyalGold else CharcoalBorder),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("treasury_tab_$idx")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) RoyalGold else TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianBg,
                            border = BorderStroke(1.dp, CharcoalBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Crowns: ${game.royalCrowns}", style = MonoStatStyle, color = RoyalGold)
                                    Text(
                                        text = "Daily Ads: ${game.adsWatchedToday}/${game.dailyAdLimit}",
                                        style = MonoStatStyle,
                                        color = if (dailyLimitReached) CrimsonCombat else EmeraldActive
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (cooldownSec > 0 && !game.hideRewardedAdOffers) {
                                        "Next optional ad in ${cooldownSec}s • All blessings also unlock via Crowns"
                                    } else {
                                        adUiState.statusMessage
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Hide Rewarded Ad Offers (Free)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text("Play purely with Royal Crowns & offline gameplay", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = game.hideRewardedAdOffers,
                                        onCheckedChange = onToggleHideRewardedAds,
                                        modifier = Modifier.testTag("hide_rewarded_ads_switch")
                                    )
                                }
                                if (adUiState.isPrivacyOptionsRequired && !game.hideRewardedAdOffers) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = { onOpenPrivacyOptions(activity) }) {
                                        Text("Manage Ad Privacy Consent", fontSize = 11.sp, color = RoyalGold)
                                    }
                                }
                                if (!game.hideRewardedAdOffers) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(4, 6, 10).forEach { limit ->
                                            FilterChip(
                                                selected = game.dailyAdLimit == limit,
                                                onClick = { onUpdateAdConfig(limit, game.adCooldownSeconds) },
                                                label = { Text("$limit/day", fontSize = 10.sp) }
                                            )
                                        }
                                        listOf(30, 90).forEach { cd ->
                                            FilterChip(
                                                selected = game.adCooldownSeconds == cd,
                                                onClick = { onUpdateAdConfig(game.dailyAdLimit, cd) },
                                                label = { Text("${cd}s cd", fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(RoyalBlessingType.entries) { blessing ->
                        val activeRemMs = game.blessingRemainingMs(blessing, nowMs)
                        val isActive = activeRemMs > 0L
                        val adReadyToClick = !game.hideRewardedAdOffers && !dailyLimitReached && cooldownSec == 0 &&
                            (adUiState.availability == AdAvailability.READY || adUiState.availability == AdAvailability.CONSENT_REQUIRED)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CharcoalElevated,
                            border = BorderStroke(1.dp, if (isActive) EmeraldActive.copy(alpha = 0.7f) else CharcoalBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(blessing.displayName, style = MaterialTheme.typography.titleSmall, color = if (isActive) EmeraldActive else RoyalGold)
                                    if (isActive) {
                                        Text(
                                            text = "Active ${CalendarEventEngine.formatDuration(activeRemMs)}",
                                            style = MonoStatStyle.copy(fontSize = 10.sp),
                                            color = EmeraldActive
                                        )
                                    }
                                }
                                Text(blessing.benefitSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!game.hideRewardedAdOffers) {
                                        Button(
                                            onClick = { onWatchRewardedBlessing(activity, blessing) },
                                            enabled = adReadyToClick,
                                            colors = ButtonDefaults.buttonColors(containerColor = VioletForge, contentColor = TextPrimary),
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("watch_ad_${blessing.name.lowercase()}")
                                        ) {
                                            Text(
                                                text = when {
                                                    dailyLimitReached -> "Daily Cap"
                                                    cooldownSec > 0 -> "Wait ${cooldownSec}s"
                                                    adUiState.availability == AdAvailability.OFFLINE -> "Offline"
                                                    adUiState.availability == AdAvailability.LOADING -> "Loading..."
                                                    else -> "Watch Ad"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (BuildConfig.DEBUG) {
                                            OutlinedButton(
                                                onClick = { onSimulateDebugAdBlessing(blessing) },
                                                enabled = !dailyLimitReached && cooldownSec == 0,
                                                border = BorderStroke(1.dp, EmeraldActive.copy(alpha = 0.55f)),
                                                modifier = Modifier.heightIn(min = 48.dp).testTag("sim_ad_${blessing.name.lowercase()}")
                                            ) {
                                                Text("Sim Ad", fontSize = 10.sp, color = EmeraldActive)
                                            }
                                        }
                                    }
                                    if (blessing.crownAlternativeCost > 0) {
                                        OutlinedButton(
                                            onClick = { onActivateBlessingWithCrowns(blessing) },
                                            enabled = game.royalCrowns >= blessing.crownAlternativeCost,
                                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        ) {
                                            Text("${blessing.crownAlternativeCost} Cr", fontSize = 11.sp, color = RoyalGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianBg,
                    border = BorderStroke(1.dp, CharcoalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Crowns: ${game.royalCrowns}", style = MonoStatStyle, color = RoyalGold)
                            Text("Tokens: ${game.chronicleTokens}", style = MonoStatStyle, color = VioletForge)
                            Text(
                                text = if (isPlayConnected) "Play Ready" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPlayConnected) EmeraldActive else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onUseCrownSpeedUp,
                        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("10m Warp (15 Cr)", fontSize = 11.sp, color = RoyalGold)
                    }
                    OutlinedButton(
                        onClick = onBuyBonusApex,
                        border = BorderStroke(1.dp, CrimsonCombat.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("+1 Apex Try (20 Cr)", fontSize = 11.sp, color = CrimsonCombat)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(IapProduct.entries) { product ->
                        val owned = when (product) {
                            IapProduct.SOVEREIGN_SUPPORTER_PACK -> game.supporterPackOwned
                            IapProduct.VAULT_EXPANSION_PACK -> game.vaultExpansionOwned
                            else -> false
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CharcoalElevated,
                            border = BorderStroke(1.dp, if (owned) EmeraldActive.copy(alpha = 0.6f) else CharcoalBorder)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(product.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (product.isConsumable) "Consumable" else "Permanent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (product.isConsumable) VioletForge else RoyalGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(product.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Button(
                                    onClick = { onPurchase(activity, product) },
                                    enabled = !owned,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (owned) EmeraldDim else RoyalGold,
                                        contentColor = if (owned) EmeraldActive else ObsidianBg
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("buy_${product.productId}")
                                ) {
                                    Text(if (owned) "Owned" else product.fallbackPrice, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveBackupModal(
    exportJson: String,
    onImportSave: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var pasteText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ObsidianCard(
            borderColor = RoyalGold.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Local Save Backup", style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Backup", tint = TextSecondary)
                }
            }
            Text(
                text = "100% offline JSON save export & restore — no cloud account or storage permissions needed.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(exportJson))
                    statusText = "Save JSON copied to clipboard (${exportJson.length} chars)."
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = ObsidianBg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("copy_save_json_button")
            ) {
                Text("Copy Current Save JSON", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = pasteText,
                onValueChange = { pasteText = it },
                label = { Text("Paste Save JSON to Restore") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            if (statusText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(statusText!!, style = MaterialTheme.typography.bodySmall, color = RoyalGold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (!onImportSave(pasteText)) {
                        statusText = "Invalid JSON save string."
                    }
                },
                enabled = pasteText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianBg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("restore_save_json_button")
            ) {
                Text("Restore Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AssignSquadModal(
    slotIndex: Int,
    game: GameState,
    seasonalStatus: SeasonalStatus,
    onAssign: (SquadTaskType, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val activeHoliday = seasonalStatus.effectiveHoliday
    val maxApex = 3 + game.apexBonusAttempts
    val apexRemaining = (maxApex - game.apexAttemptsUsed).coerceAtLeast(0)
    var selectedCategory by remember { mutableStateOf(SkillCategory.GATHERING) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ObsidianCard(
            borderColor = EmeraldActive.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.84f)
                .widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Assign Squad ${slotIndex + 1}", style = MaterialTheme.typography.titleLarge, color = EmeraldActive)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GameCatalog.skillNodes.filter { it.skill.category == selectedCategory }) { node ->
                    AssignOptionRow(
                        title = "${node.skill.displayName} (Lv.${game.skillProgress(node.skill).level})",
                        subtitle = "${node.actionTitle} • ${node.yieldDescription}",
                        accent = EmeraldActive,
                        onClick = { onAssign(SquadTaskType.ARTISAN_SKILL, node.skill.name, node.skill.displayName) }
                    )
                }

                if (activeHoliday != null) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${activeHoliday.name} Seasonal Orders", style = MaterialTheme.typography.titleSmall, color = activeHoliday.primaryAccent)
                    }
                    items(activeHoliday.nodes) { sNode ->
                        AssignOptionRow(
                            title = sNode.name,
                            subtitle = sNode.yieldSummary,
                            accent = activeHoliday.primaryAccent,
                            onClick = { onAssign(SquadTaskType.SEASONAL_NODE, sNode.id, sNode.name) }
                        )
                    }
                    item {
                        AssignOptionRow(
                            title = "Hunt ${activeHoliday.bossName}",
                            subtitle = activeHoliday.bossSubtitle,
                            accent = activeHoliday.secondaryAccent,
                            onClick = { onAssign(SquadTaskType.SEASONAL_BOSS, activeHoliday.id.name, activeHoliday.bossName) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dungeon Floors (Pasture to Vibranium Core)", style = MaterialTheme.typography.titleSmall, color = EmeraldActive)
                }
                items(GameCatalog.dungeonFloors.filter { it.floor <= game.highestDungeonFloor }) { floor ->
                    val monstersList = floor.monsters.joinToString(", ") { it.name }
                    AssignOptionRow(
                        title = floor.name,
                        subtitle = "$monstersList • Drops: ${floor.monsters.first().dropsSummary}",
                        accent = EmeraldActive,
                        onClick = { onAssign(SquadTaskType.DUNGEON_FLOOR, floor.floor.toString(), "Floor ${floor.floor}") }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hunting Grounds & Regions", style = MaterialTheme.typography.titleSmall, color = CrimsonCombat)
                }
                items(GameCatalog.regions.filter {
                    game.warlordLevel >= it.reqWarlordLevel && game.buildingLevel(BuildingType.CITADEL) >= it.reqCitadelLevel
                }) { region ->
                    val displayName = if (region.id == "village_outskirts") "${game.villageName} Outskirts" else region.name
                    AssignOptionRow(
                        title = displayName,
                        subtitle = "${region.enemyName} (${region.enemyTroopType.displayName}) • +${region.goldReward}g",
                        accent = CrimsonCombat,
                        onClick = { onAssign(SquadTaskType.RAID_REGION, region.id, displayName) }
                    )
                }

                if (game.buildingLevel(BuildingType.CITADEL) >= 2 && !game.weeklyBossDefeated) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Weekly Realm Boss", style = MaterialTheme.typography.titleSmall, color = CrimsonCombat)
                        Spacer(modifier = Modifier.height(4.dp))
                        AssignOptionRow(
                            title = "Weekly Realm Titan",
                            subtitle = "${formatCompact(game.weeklyBossHpRemaining)} HP left • +15 Crowns & Epic Relic",
                            accent = CrimsonCombat,
                            onClick = { onAssign(SquadTaskType.WEEKLY_REALM_BOSS, "weekly_boss", "Weekly Realm Boss") }
                        )
                    }
                }

                if (game.buildingLevel(BuildingType.CITADEL) >= 3) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Daily Apex Siege ($apexRemaining left)", style = MaterialTheme.typography.titleSmall, color = VioletForge)
                    }
                    items(GameCatalog.apexBosses.filter { game.buildingLevel(BuildingType.CITADEL) >= it.reqCitadelLevel }) { boss ->
                        AssignOptionRow(
                            title = boss.name,
                            subtitle = "${boss.title} • +${boss.goldReward}g & +${boss.crownBonus} Crowns",
                            accent = if (apexRemaining > 0) VioletForge else TextMuted,
                            enabled = apexRemaining > 0,
                            onClick = { onAssign(SquadTaskType.APEX_BOSS, boss.id, boss.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignOptionRow(
    title: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        color = CharcoalElevated,
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.45f) else CharcoalDivider),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) TextPrimary else TextMuted)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                text = if (enabled) "Deploy" else "Locked",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) accent else TextMuted
            )
        }
    }
}
