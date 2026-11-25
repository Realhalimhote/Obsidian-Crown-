package com.example

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BottomTab
import com.example.model.SquadTaskType
import com.example.ui.components.*
import com.example.ui.screens.ArtisanScreen
import com.example.ui.screens.FrontierScreen
import com.example.ui.screens.StrongholdScreen
import com.example.ui.screens.WarlordScreen
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isPlayConnected by viewModel.billingManager.isPlayConnected.collectAsStateWithLifecycle()
            val adUiState by viewModel.adUiState.collectAsStateWithLifecycle()

            val activeHoliday = uiState.seasonalStatus.effectiveHoliday
            val seasonalAccents = remember(activeHoliday) {
                if (activeHoliday != null) {
                    SeasonalAccentPalette(
                        primary = activeHoliday.primaryAccent,
                        secondary = activeHoliday.secondaryAccent
                    )
                } else {
                    SeasonalAccentPalette()
                }
            }

            ObsidianCrownTheme(seasonalAccents = seasonalAccents) {
                ObsidianCrownApp(
                    uiState = uiState,
                    isPlayConnected = isPlayConnected,
                    adUiState = adUiState,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppPaused()
    }
}

@Composable
private fun ObsidianCrownApp(
    uiState: com.example.viewmodel.GameUiState,
    isPlayConnected: Boolean,
    adUiState: com.example.ads.AdManagerUiState,
    viewModel: GameViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        viewModel.clearSnackbar()
    }

    LaunchedEffect(uiState.showTreasuryModal) {
        if (uiState.showTreasuryModal) {
            viewModel.refreshAdReadiness(activity)
        }
    }

    if (uiState.selectedTab != BottomTab.FRONTIER) {
        BackHandler {
            viewModel.selectTab(BottomTab.FRONTIER)
        }
    }

    if (!uiState.isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Founding Your Village...", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        contentColor = TextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        BottomTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = CharcoalElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BottomTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = CharcoalElevated,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopWarlordHeader(
                        game = uiState.game,
                        seasonalStatus = uiState.seasonalStatus,
                        soloEventStatus = uiState.soloEventStatus,
                        realmSurgeStatus = uiState.realmSurgeStatus,
                        isSquadStripExpanded = uiState.isSquadStripExpanded,
                        onToggleSquadStrip = viewModel::toggleSquadStrip,
                        onOpenTreasury = { viewModel.openTreasury(true) },
                        onOpenEvents = { viewModel.openEventsModal(true) },
                        onAssignSquadClick = { slot -> viewModel.openAssignPicker(slot) },
                        onRecallSquadClick = { slot -> viewModel.recallSquad(slot) }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 820.dp)
                            .fillMaxWidth()
                    ) {
                        when (uiState.selectedTab) {
                            BottomTab.FRONTIER -> FrontierScreen(
                                game = uiState.game,
                                seasonalStatus = uiState.seasonalStatus,
                                soloEventStatus = uiState.soloEventStatus,
                                combatLogs = uiState.combatLogs,
                                onDeployRegion = viewModel::selectRegion,
                                onDeployDungeonFloor = viewModel::selectDungeonFloor,
                                onAttackDungeonFloor = viewModel::attackDungeonInstant,
                                onEatBestFood = viewModel::eatBestFood,
                                onPunchTree = { viewModel.performInstantSkill(com.example.model.SkillType.WOODCUTTING) },
                                onSelectPhalanx = viewModel::selectPhalanx,
                                onOpenEventsModal = { viewModel.openEventsModal(true) },
                                onDeployApexBoss = { bossId, bossName ->
                                    viewModel.assignFirstIdleOrSlot(SquadTaskType.APEX_BOSS, bossId, bossName)
                                },
                                onBuyBonusApex = viewModel::buyBonusApexAttempt,
                                onClaimContract = viewModel::claimContract
                            )

                            BottomTab.STRONGHOLD -> StrongholdScreen(
                                game = uiState.game,
                                onUpgradeBuilding = viewModel::upgradeBuilding,
                                onTrainTroops = viewModel::trainTroops,
                                onHealWounded = viewModel::healWounded,
                                onUnlockOrEquipCompanion = viewModel::unlockOrEquipCompanion
                            )

                            BottomTab.ARTISAN -> ArtisanScreen(
                                game = uiState.game,
                                seasonalStatus = uiState.seasonalStatus,
                                onAssignSkill = { skill ->
                                    viewModel.assignFirstIdleOrSlot(SquadTaskType.ARTISAN_SKILL, skill.name, skill.displayName)
                                },
                                onInstantSkill = viewModel::performInstantSkill,
                                onTogglePrayer = viewModel::toggleDivinePrayer,
                                onCraftTool = viewModel::craftTool,
                                onSmeltOre = viewModel::smeltOre,
                                onCookFood = viewModel::cookFood,
                                onEatFood = viewModel::eatFood,
                                onAssignSeasonalNode = { nodeId, nodeName ->
                                    viewModel.assignFirstIdleOrSlot(SquadTaskType.SEASONAL_NODE, nodeId, nodeName)
                                },
                                onFuseGear = viewModel::fuseGear,
                                onFuseAllGear = viewModel::fuseAllGear,
                                onCraftAgeGear = viewModel::craftAgeGear,
                                onInstantUpgradeSlot = viewModel::instantUpgradeEquipped,
                                onEnchantSlot = viewModel::enchantSlot
                            )

                            BottomTab.WARLORD -> WarlordScreen(
                                game = uiState.game,
                                onEquipItem = viewModel::equipItem,
                                onInstantUpgradeSlot = viewModel::instantUpgradeEquipped,
                                onEnchantSlot = viewModel::enchantSlot,
                                onUpdateAutoEat = viewModel::updateAutoEatThreshold,
                                onEatRationNow = viewModel::eatRationNow,
                                onEatFood = viewModel::eatFood,
                                onSellJunk = viewModel::sellJunk,
                                onToggleAutoSalvage = viewModel::toggleAutoSalvage,
                                onSelectTitle = viewModel::selectWarlordTitle,
                                onOpenBackupModal = { viewModel.openBackupModal(true) },
                                onStartNewVillage = viewModel::startNewVillage
                            )
                        }
                    }
                }
            }
        }
    }

    // Modals
    uiState.offlineReport?.let { report ->
        OfflineReportModal(
            report = report,
            onDismiss = viewModel::dismissOfflineReport
        )
    }

    if (uiState.showTreasuryModal) {
        TreasuryModal(
            game = uiState.game,
            isPlayConnected = isPlayConnected,
            adUiState = adUiState,
            onPurchase = viewModel::purchaseIap,
            onUseCrownSpeedUp = viewModel::useCrownSpeedUp,
            onBuyBonusApex = viewModel::buyBonusApexAttempt,
            onWatchRewardedBlessing = viewModel::watchRewardedBlessing,
            onSimulateDebugAdBlessing = viewModel::simulateDebugAdBlessing,
            onActivateBlessingWithCrowns = viewModel::activateBlessingWithCrowns,
            onToggleHideRewardedAds = viewModel::toggleHideRewardedAdOffers,
            onUpdateAdConfig = viewModel::updateAdConfig,
            onOpenPrivacyOptions = viewModel::showAdPrivacyOptions,
            onDismiss = { viewModel.openTreasury(false) }
        )
    }

    if (uiState.showEventsModal) {
        EventsAndAlmanacModal(
            game = uiState.game,
            seasonalStatus = uiState.seasonalStatus,
            soloEventStatus = uiState.soloEventStatus,
            realmSurgeStatus = uiState.realmSurgeStatus,
            festivalStatus = uiState.festivalStatus,
            onClaimSoloTier = viewModel::claimSoloEventTier,
            onClaimContract = viewModel::claimContract,
            onDeployWeeklyBoss = {
                viewModel.assignFirstIdleOrSlot(SquadTaskType.WEEKLY_REALM_BOSS, "weekly_boss", "Weekly Realm Boss")
            },
            onFightTournament = viewModel::fightTournament,
            onSummonBoss = viewModel::summonSeasonalBoss,
            onDeploySeasonalBoss = { holidayId, bossName ->
                viewModel.assignFirstIdleOrSlot(SquadTaskType.SEASONAL_BOSS, holidayId.name, bossName)
            },
            onDismiss = { viewModel.openEventsModal(false) }
        )
    }

    if (uiState.showBackupModal) {
        SaveBackupModal(
            exportJson = viewModel.exportSaveJson(),
            onImportSave = viewModel::importSaveJson,
            onDismiss = { viewModel.openBackupModal(false) }
        )
    }

    uiState.assigningSquadSlot?.let { slotIdx ->
        AssignSquadModal(
            slotIndex = slotIdx,
            game = uiState.game,
            seasonalStatus = uiState.seasonalStatus,
            onAssign = { taskType, targetId, label ->
                viewModel.assignSquad(slotIdx, taskType, targetId, label)
            },
            onDismiss = { viewModel.openAssignPicker(null) }
        )
    }
}

private fun tabIcon(tab: BottomTab, selected: Boolean) = when (tab) {
    BottomTab.FRONTIER -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
    BottomTab.STRONGHOLD -> if (selected) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance
    BottomTab.ARTISAN -> if (selected) Icons.Filled.Hardware else Icons.Outlined.Hardware
    BottomTab.WARLORD -> if (selected) Icons.Filled.Shield else Icons.Outlined.Shield
}
