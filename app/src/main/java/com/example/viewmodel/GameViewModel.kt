package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ads.AdManagerUiState
import com.example.ads.RewardedAdManager
import com.example.billing.BillingManager
import com.example.billing.IapProduct
import com.example.data.GameCatalog
import com.example.data.GameRepository
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GameUiState(
    val isLoaded: Boolean = false,
    val game: GameState = GameState.freshVillage(),
    val selectedTab: BottomTab = BottomTab.FRONTIER,
    val isSquadStripExpanded: Boolean = true,
    val seasonalStatus: SeasonalStatus = CalendarEventEngine.getSeasonalStatus(state = GameState.freshVillage()),
    val soloEventStatus: SoloEventStatus = CalendarEventEngine.getCurrentSoloEvent(),
    val realmSurgeStatus: RealmSurgeStatus = CalendarEventEngine.getCurrentRealmSurge(),
    val festivalStatus: KingdomFestivalStatus = CalendarEventEngine.getMonthlyKingdomFestival(),
    val combatLogs: List<CombatLogEntry> = emptyList(),
    val offlineReport: OfflineReport? = null,
    val showTreasuryModal: Boolean = false,
    val showEventsModal: Boolean = false,
    val showBackupModal: Boolean = false,
    val assigningSquadSlot: Int? = null,
    val snackbarMessage: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val billingManager: BillingManager = BillingManager(
        context = application.applicationContext,
        onEntitlementGranted = { product -> grantIapEntitlement(product) },
        onStatusMessage = { msg -> showBanner(msg) }
    )

    val rewardedAdManager: RewardedAdManager = RewardedAdManager(application.applicationContext)
    val adUiState: StateFlow<AdManagerUiState> = rewardedAdManager.uiState

    private var ticksSinceSave = 0

    init {
        billingManager.startConnection()
        viewModelScope.launch {
            val loaded = repository.loadState()
            val nowMs = System.currentTimeMillis()
            val today = LocalDate.now()
            val sim = GameSimulationEngine.simulateOfflineProgress(loaded, nowMs, today)
            repository.saveState(sim.state)

            _uiState.update {
                it.copy(
                    isLoaded = true,
                    game = sim.state,
                    seasonalStatus = CalendarEventEngine.getSeasonalStatus(today, nowMs, sim.state),
                    soloEventStatus = CalendarEventEngine.getCurrentSoloEvent(nowMs),
                    realmSurgeStatus = CalendarEventEngine.getCurrentRealmSurge(nowMs),
                    festivalStatus = CalendarEventEngine.getMonthlyKingdomFestival(today),
                    offlineReport = sim.report
                )
            }
            startTickLoop()
        }
    }

    private fun startTickLoop() {
        viewModelScope.launch {
            while (true) {
                delay(250L)
                val current = _uiState.value
                if (!current.isLoaded) continue
                val nowMs = System.currentTimeMillis()
                val today = LocalDate.now()
                val tick = GameSimulationEngine.advanceTick(
                    initialState = current.game,
                    deltaMs = 250L,
                    nowMs = nowMs,
                    today = today
                )
                val mergedLogs = if (tick.logs.isEmpty()) {
                    current.combatLogs
                } else {
                    (tick.logs + current.combatLogs).take(12)
                }
                _uiState.update {
                    it.copy(
                        game = tick.state,
                        seasonalStatus = CalendarEventEngine.getSeasonalStatus(today, nowMs, tick.state),
                        soloEventStatus = CalendarEventEngine.getCurrentSoloEvent(nowMs),
                        realmSurgeStatus = CalendarEventEngine.getCurrentRealmSurge(nowMs),
                        festivalStatus = CalendarEventEngine.getMonthlyKingdomFestival(today),
                        combatLogs = mergedLogs
                    )
                }
                ticksSinceSave++
                if (ticksSinceSave >= 20) {
                    ticksSinceSave = 0
                    repository.saveState(tick.state)
                }
            }
        }
    }

    fun onAppPaused() {
        val current = _uiState.value
        if (!current.isLoaded) return
        val stamped = current.game.copy(lastSavedTimestampMs = System.currentTimeMillis())
        _uiState.update { it.copy(game = stamped) }
        viewModelScope.launch {
            repository.saveState(stamped)
        }
    }

    fun selectTab(tab: BottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleSquadStrip() {
        _uiState.update { it.copy(isSquadStripExpanded = !it.isSquadStripExpanded) }
    }

    fun dismissOfflineReport() {
        _uiState.update { it.copy(offlineReport = null) }
    }

    fun openTreasury(open: Boolean) {
        _uiState.update { it.copy(showTreasuryModal = open) }
    }

    fun openEventsModal(open: Boolean) {
        _uiState.update { it.copy(showEventsModal = open) }
    }

    fun openBackupModal(open: Boolean) {
        _uiState.update { it.copy(showBackupModal = open) }
    }

    fun openAssignPicker(slotIndex: Int?) {
        _uiState.update { it.copy(assigningSquadSlot = slotIndex) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun showBanner(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    private fun updateGame(persistImmediately: Boolean = true, transform: (GameState) -> GameState) {
        val next = transform(_uiState.value.game).copy(lastSavedTimestampMs = System.currentTimeMillis())
        val nowMs = System.currentTimeMillis()
        val today = LocalDate.now()
        _uiState.update {
            it.copy(
                game = next,
                seasonalStatus = CalendarEventEngine.getSeasonalStatus(today, nowMs, next),
                soloEventStatus = CalendarEventEngine.getCurrentSoloEvent(nowMs),
                realmSurgeStatus = CalendarEventEngine.getCurrentRealmSurge(nowMs),
                festivalStatus = CalendarEventEngine.getMonthlyKingdomFestival(today)
            )
        }
        if (persistImmediately) {
            viewModelScope.launch { repository.saveState(next) }
        }
    }

    fun assignSquad(slotIndex: Int, taskType: SquadTaskType, targetId: String, targetLabel: String) {
        updateGame { s ->
            if (slotIndex !in 0 until s.unlockedSquadSlots()) return@updateGame s
            val updated = s.squads.toMutableList()
            updated[slotIndex] = SquadAssignment(
                slotIndex = slotIndex,
                taskType = taskType,
                targetId = targetId,
                targetLabel = targetLabel,
                progressMs = 0L
            )
            var next = s.copy(squads = updated)
            if (taskType == SquadTaskType.RAID_REGION) {
                val region = GameCatalog.regionById(targetId)
                next = next.copy(
                    activeRegionId = region.id,
                    currentEnemyName = region.enemyName,
                    currentEnemyHp = region.enemyHp,
                    currentEnemyMaxHp = region.enemyHp
                )
            } else if (taskType == SquadTaskType.DUNGEON_FLOOR) {
                val floorNum = targetId.toIntOrNull() ?: 1
                val floorDef = GameCatalog.dungeonFloor(floorNum)
                val firstMonster = floorDef.monsters.first()
                next = next.copy(
                    currentDungeonFloor = floorNum,
                    currentDungeonEnemyName = firstMonster.name,
                    currentDungeonEnemyHp = firstMonster.hp,
                    currentDungeonEnemyMaxHp = firstMonster.hp
                )
            }
            next
        }
        _uiState.update { it.copy(assigningSquadSlot = null) }
        showBanner("Squad ${slotIndex + 1} deployed: $targetLabel")
    }

    fun assignFirstIdleOrSlot(taskType: SquadTaskType, targetId: String, targetLabel: String) {
        val s = _uiState.value.game
        val unlocked = s.unlockedSquadSlots()
        val idleIdx = (0 until unlocked).firstOrNull { s.squads[it].taskType == SquadTaskType.IDLE }
        if (idleIdx != null) {
            assignSquad(idleIdx, taskType, targetId, targetLabel)
        } else {
            assignSquad(0, taskType, targetId, targetLabel)
        }
    }

    fun recallSquad(slotIndex: Int) {
        updateGame { s ->
            val updated = s.squads.toMutableList()
            updated[slotIndex] = SquadAssignment(slotIndex = slotIndex)
            s.copy(squads = updated)
        }
        showBanner("Squad ${slotIndex + 1} recalled")
    }

    fun selectRegion(regionId: String) {
        val region = GameCatalog.regionById(regionId)
        val s = _uiState.value.game
        if (s.warlordLevel < region.reqWarlordLevel || s.buildingLevel(BuildingType.CITADEL) < region.reqCitadelLevel) {
            showBanner("Requires Warlord Lv.${region.reqWarlordLevel} & Citadel Lv.${region.reqCitadelLevel}")
            return
        }
        val label = if (region.id == "village_outskirts") "${s.villageName} Outskirts" else region.name
        assignFirstIdleOrSlot(SquadTaskType.RAID_REGION, region.id, label)
    }

    fun selectPhalanx(stance: PhalanxStance) {
        updateGame { it.copy(activePhalanx = stance) }
        showBanner("Formation: ${stance.displayName}")
    }

    fun upgradeBuilding(type: BuildingType) {
        val result = GameSimulationEngine.upgradeBuilding(_uiState.value.game, type)
        updateGame { result.first }
        result.second?.let { showBanner(it) }
    }

    fun trainTroops(type: TroopType, tier: Int) {
        val result = GameSimulationEngine.trainTroopBatch(_uiState.value.game, type, tier)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun healWounded() {
        val result = GameSimulationEngine.healWounded(_uiState.value.game)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun fuseGear(templateId: String, rarity: GearRarity) {
        val result = GameSimulationEngine.fuseThreeItems(_uiState.value.game, templateId, rarity)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun craftAgeGear(age: MaterialAge) {
        val result = GameSimulationEngine.craftAgeGear(_uiState.value.game, age)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun enchantSlot(slot: EquipSlot) {
        val result = GameSimulationEngine.enchantEquippedSlot(_uiState.value.game, slot)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun performInstantSkill(skill: SkillType) {
        val result = GameSimulationEngine.performInstantSkillAction(_uiState.value.game, skill)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun toggleDivinePrayer(prayer: DivinePrayer) {
        val result = GameSimulationEngine.toggleDivinePrayer(_uiState.value.game, prayer)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun instantUpgradeEquipped(slot: EquipSlot) {
        val result = GameSimulationEngine.instantUpgradeEquippedRarity(_uiState.value.game, slot)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun fuseAllGear() {
        val result = GameSimulationEngine.fuseAllReady(_uiState.value.game)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun unlockOrEquipCompanion(companionId: String) {
        val result = GameSimulationEngine.unlockOrEquipCompanion(_uiState.value.game, companionId)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun fightTournament(rank: Int) {
        val result = GameSimulationEngine.fightTournamentChallenger(_uiState.value.game, rank)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun claimContract(contractId: String) {
        val result = GameSimulationEngine.claimRoyalContract(_uiState.value.game, contractId)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun equipItem(item: EquipmentItem) {
        updateGame { GameSimulationEngine.equipItem(it, item) }
        showBanner("Equipped ${item.name}")
    }

    fun sellJunk() {
        val result = GameSimulationEngine.sellAllJunk(_uiState.value.game)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun selectDungeonFloor(floorNumber: Int) {
        val s = _uiState.value.game
        if (floorNumber > s.highestDungeonFloor) {
            showBanner("Clear Floor ${floorNumber - 1} to unlock Floor $floorNumber")
            return
        }
        assignFirstIdleOrSlot(SquadTaskType.DUNGEON_FLOOR, floorNumber.toString(), "Floor $floorNumber")
    }

    fun attackDungeonInstant(floorNumber: Int) {
        val result = GameSimulationEngine.attackDungeonFloor(_uiState.value.game, floorNumber)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun cookFood(food: FoodType) {
        val result = GameSimulationEngine.cookFoodRecipe(_uiState.value.game, food)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun eatFood(food: FoodType) {
        val result = GameSimulationEngine.eatCookedFood(_uiState.value.game, food)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun eatBestFood() {
        val result = GameSimulationEngine.eatBestAvailableFood(_uiState.value.game)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun smeltOre(recipe: FurnaceRecipe) {
        val result = GameSimulationEngine.smeltOre(_uiState.value.game, recipe)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun craftTool(tool: ToolType) {
        val result = GameSimulationEngine.craftOrUpgradeTool(_uiState.value.game, tool)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun updateAutoEatThreshold(pct: Int) {
        updateGame(persistImmediately = true) { it.copy(autoEatThresholdPct = pct.coerceIn(10, 85)) }
    }

    fun eatRationNow() {
        eatBestFood()
    }

    fun selectWarlordTitle(title: String) {
        updateGame { it.copy(warlordTitle = title) }
        showBanner("Title set: $title")
    }

    fun toggleAutoSalvage(enabled: Boolean) {
        if (!_uiState.value.game.vaultExpansionOwned) {
            showBanner("Unlock Vault Expansion Pack in Treasury to enable Auto-Salvage.")
            return
        }
        updateGame { it.copy(autoSalvageJunkEnabled = enabled) }
    }

    fun claimSoloEventTier(tierIndex: Int) {
        val result = GameSimulationEngine.claimSoloEventTier(_uiState.value.game, tierIndex)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun summonSeasonalBoss(holidayId: HolidayId) {
        val result = GameSimulationEngine.summonSeasonalBoss(_uiState.value.game, holidayId)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun useCrownSpeedUp() {
        val result = GameSimulationEngine.useCrownSpeedUp(_uiState.value.game)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun buyBonusApexAttempt() {
        val result = GameSimulationEngine.buyBonusApexAttempt(_uiState.value.game)
        updateGame { result.first }
        showBanner(result.second)
    }

    fun purchaseIap(activity: Activity?, product: IapProduct) {
        billingManager.purchase(activity, product)
    }

    fun refreshAdReadiness(activity: Activity?) {
        rewardedAdManager.refreshState(activity, _uiState.value.game.hideRewardedAdOffers)
    }

    fun watchRewardedBlessing(activity: Activity?, blessing: RoyalBlessingType) {
        val current = _uiState.value.game
        if (current.hideRewardedAdOffers) {
            showBanner("Rewarded ad offers are hidden in your settings.")
            return
        }
        val nowMs = System.currentTimeMillis()
        if (current.adsWatchedToday >= current.dailyAdLimit) {
            showBanner("Daily optional ad limit reached (${current.dailyAdLimit}/${current.dailyAdLimit}). Use Crowns or wait for reset.")
            return
        }
        val cooldown = current.adCooldownRemainingMs(nowMs)
        if (cooldown > 0L) {
            showBanner("Blessing cooldown: ${(cooldown + 999L) / 1000L}s remaining")
            return
        }
        rewardedAdManager.showRewardedAd(
            activity = activity,
            hideOffers = current.hideRewardedAdOffers,
            onRewardEarned = {
                val res = GameSimulationEngine.grantRoyalBlessingFromAd(_uiState.value.game, blessing)
                updateGame(persistImmediately = true) { res.first }
                showBanner(res.second)
            },
            onStatusMessage = { msg -> showBanner(msg) }
        )
    }

    fun simulateDebugAdBlessing(blessing: RoyalBlessingType) {
        if (!BuildConfig.DEBUG) return
        val current = _uiState.value.game
        if (current.hideRewardedAdOffers) {
            showBanner("Rewarded ad offers are hidden.")
            return
        }
        rewardedAdManager.simulateDebugRewardedAd(
            onRewardEarned = {
                val res = GameSimulationEngine.grantRoyalBlessingFromAd(_uiState.value.game, blessing)
                updateGame(persistImmediately = true) { res.first }
                showBanner("[Debug Ad] ${res.second}")
            },
            onStatusMessage = { msg -> showBanner(msg) }
        )
    }

    fun activateBlessingWithCrowns(blessing: RoyalBlessingType) {
        val res = GameSimulationEngine.activateRoyalBlessingWithCrowns(_uiState.value.game, blessing)
        updateGame(persistImmediately = true) { res.first }
        showBanner(res.second)
    }

    fun toggleHideRewardedAdOffers(hide: Boolean) {
        updateGame(persistImmediately = true) { it.copy(hideRewardedAdOffers = hide) }
        rewardedAdManager.refreshState(null, hide)
        showBanner(if (hide) "Rewarded ad offers hidden" else "Optional Royal Blessings ads enabled")
    }

    fun updateAdConfig(dailyLimit: Int, cooldownSeconds: Int) {
        updateGame(persistImmediately = true) {
            it.copy(
                dailyAdLimit = dailyLimit.coerceIn(1, 12),
                adCooldownSeconds = cooldownSeconds.coerceIn(0, 600)
            )
        }
    }

    fun showAdPrivacyOptions(activity: Activity?) {
        rewardedAdManager.showPrivacyOptions(activity) { msg -> showBanner(msg) }
    }

    fun exportSaveJson(): String = repository.exportSaveJson(_uiState.value.game)

    fun importSaveJson(rawJson: String): Boolean {
        val imported = repository.importSaveJson(rawJson) ?: return false
        val synced = GameSimulationEngine.syncClocks(imported)
        _uiState.update { it.copy(game = synced, showBackupModal = false) }
        viewModelScope.launch { repository.saveState(synced) }
        showBanner("Save restored for ${synced.villageName}!")
        return true
    }

    fun startNewVillage() {
        val current = _uiState.value.game
        val fresh = GameState.freshVillage().copy(
            supporterPackOwned = current.supporterPackOwned,
            vaultExpansionOwned = current.vaultExpansionOwned,
            royalObsidianTitleUnlocked = current.royalObsidianTitleUnlocked
        )
        _uiState.update { it.copy(game = fresh, combatLogs = emptyList(), selectedTab = BottomTab.FRONTIER) }
        viewModelScope.launch { repository.saveState(fresh) }
        showBanner("Founded new hamlet: ${fresh.villageName}!")
    }

    private fun grantIapEntitlement(product: IapProduct) {
        updateGame { s ->
            when (product) {
                IapProduct.SOVEREIGN_SUPPORTER_PACK -> s.copy(supporterPackOwned = true)
                IapProduct.VAULT_EXPANSION_PACK -> s.copy(vaultExpansionOwned = true)
                IapProduct.CHRONICLE_FESTIVAL_BUNDLE -> s.copy(
                    chronicleTokens = s.chronicleTokens + 5,
                    royalObsidianTitleUnlocked = true,
                    warlordTitle = "Royal Obsidian Warlord"
                )
                IapProduct.ROYAL_CROWN_POUCH -> s.copy(royalCrowns = s.royalCrowns + 250)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }
}
