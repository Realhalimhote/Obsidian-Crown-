package com.example.engine

import com.example.data.GameCatalog
import com.example.model.*
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

data class TickResult(
    val state: GameState,
    val logs: List<CombatLogEntry> = emptyList()
)

data class OfflineSimResult(
    val state: GameState,
    val report: OfflineReport?
)

object GameSimulationEngine {

    private const val SKILL_CYCLE_MS = 2500L
    private const val COMBAT_ROUND_MS = 1800L
    private const val TWELVE_HOURS_MS = 12L * 3600L * 1000L
    private const val TWENTY_FOUR_HOURS_MS = 24L * 3600L * 1000L

    fun syncClocks(
        state: GameState,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now()
    ): GameState {
        var updated = state
        val todayStr = today.toString()
        if (updated.apexDateKey != todayStr) {
            updated = updated.copy(
                apexDateKey = todayStr,
                apexAttemptsUsed = 0,
                apexKillsToday = 0
            )
        }
        if (updated.contractsDateKey != todayStr || updated.dailyContracts.isEmpty()) {
            val scale = updated.warlordLevel.coerceAtLeast(1)
            val contracts = listOf(
                RoyalContract("c_slay", "Slay ${8 + scale * 2} Frontier Foes", ContractType.SLAY_FOES, 8 + scale * 2, 0, 90L * scale, 2),
                RoyalContract("c_skill", "Complete ${12 + scale * 2} Artisan Cycles", ContractType.COMPLETE_SKILLS, 12 + scale * 2, 0, 80L * scale, 2),
                RoyalContract("c_forge", "Craft or Fuse ${2 + (scale / 5)} Gear", ContractType.FUSE_OR_CRAFT, 2 + (scale / 5), 0, 110L * scale, 3)
            )
            updated = updated.copy(contractsDateKey = todayStr, dailyContracts = contracts)
        }
        val weekKey = CalendarEventEngine.currentWeekKey(today)
        if (updated.weeklyKey != weekKey) {
            val bossHp = 4000L + (updated.buildingLevel(BuildingType.CITADEL) * 1500L)
            updated = updated.copy(
                weeklyKey = weekKey,
                weeklyBossHpRemaining = bossHp,
                weeklyBossMaxHp = bossHp,
                weeklyBossDefeated = false,
                tournamentWinsThisWeek = 0
            )
        }
        val currentWindow = CalendarEventEngine.getCurrentSoloEvent(nowMs).windowId
        if (updated.soloEventWindowId != currentWindow) {
            updated = updated.copy(
                soloEventWindowId = currentWindow,
                soloEventPoints = 0L,
                soloEventClaimedTiers = emptyList()
            )
        }
        if (updated.adsDateKey != todayStr) {
            updated = updated.copy(
                adsDateKey = todayStr,
                adsWatchedToday = 0
            )
        }
        val activeBlessings = updated.activeBlessingExpiries.filterValues { it > nowMs }
        if (activeBlessings.size != updated.activeBlessingExpiries.size) {
            updated = updated.copy(activeBlessingExpiries = activeBlessings)
        }
        if (updated.summonedHolidayId != null && updated.summonedBossExpiresAtMs <= nowMs) {
            updated = updated.copy(
                summonedHolidayId = null,
                summonedBossExpiresAtMs = 0L
            )
        }
        return updated
    }

    fun phalanxMultiplier(
        playerStance: PhalanxStance,
        enemyType: TroopType,
        isBoss: Boolean = false
    ): Float {
        if (playerStance == PhalanxStance.SIEGE_BREACHER) {
            return if (isBoss || enemyType == TroopType.SIEGE) 1.30f else 1.0f
        }
        val player = playerStance.troopType
        return when {
            player == TroopType.INFANTRY && enemyType == TroopType.RANGED -> 1.25f
            player == TroopType.RANGED && enemyType == TroopType.CAVALRY -> 1.25f
            player == TroopType.CAVALRY && enemyType == TroopType.INFANTRY -> 1.25f
            player == TroopType.INFANTRY && enemyType == TroopType.CAVALRY -> 0.85f
            player == TroopType.RANGED && enemyType == TroopType.INFANTRY -> 0.85f
            player == TroopType.CAVALRY && enemyType == TroopType.RANGED -> 0.85f
            else -> 1.0f
        }
    }

    fun conquestMasteryBonusPct(regionKills: Int, huntersLodgeLevel: Int = 1): Int =
        (regionKills / 100) * (5 + (huntersLodgeLevel - 1))

    private fun activeCompanion(state: GameState) =
        state.activeCompanionId?.let { id -> GameCatalog.companions.firstOrNull { it.id == id } }

    private fun companionScale(state: GameState): Double =
        1.0 + ((state.buildingLevel(BuildingType.BEAST_SANCTUARY) - 1) * 0.06)

    fun advanceTick(
        initialState: GameState,
        deltaMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
        random: Random = Random.Default,
        isOfflineStep: Boolean = false
    ): TickResult {
        var s = syncClocks(initialState, nowMs, today)
        val logs = mutableListOf<CombatLogEntry>()
        val unlockedCount = s.unlockedSquadSlots()
        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs)
        val surge = CalendarEventEngine.getCurrentRealmSurge(nowMs)
        val seasonal = CalendarEventEngine.getSeasonalStatus(today, nowMs, s)
        val academyBonus = (s.buildingLevel(BuildingType.ACADEMY) - 1) * 0.05
        val petBonus = ((activeCompanion(s)?.skillXpBonusPct ?: 0) / 100.0) * companionScale(s)
        val supporterBonus = if (s.supporterPackOwned) 0.15 else 0.0
        val xpBlessingBonus = if (s.isBlessingActive(RoyalBlessingType.SKILL_XP_SURGE, nowMs)) 0.35 else 0.0
        val prayerXpBonus = if (s.currentPrayer() == DivinePrayer.ASCENDANT_LITANY && s.devotion >= DivinePrayer.ASCENDANT_LITANY.devotionCostPerAction) {
            0.30 + ((s.buildingLevel(BuildingType.DIVINITY_SHRINE) - 1) * 0.03)
        } else 0.0
        val offlineBlessingMult = if (isOfflineStep && s.isBlessingActive(RoyalBlessingType.OFFLINE_FORTUNE, nowMs)) 1.35 else 1.0
        val gatheringBlessingMult = if (s.isBlessingActive(RoyalBlessingType.GATHERING_BOUNTY, nowMs)) 1.40 else 1.0
        val harvestHymnMult = if (s.currentPrayer() == DivinePrayer.HARVEST_HYMN && s.devotion >= 1L) 1.25 else 1.0
        val lootBlessingActive = s.isBlessingActive(RoyalBlessingType.COMBAT_LOOT_LUCK, nowMs)
        val hasteActive = s.isBlessingActive(RoyalBlessingType.BUILD_TRAINING_HASTE, nowMs)
        val xpMult = (1.0 + academyBonus + petBonus + supporterBonus + xpBlessingBonus + prayerXpBonus) * offlineBlessingMult
        val newSquads = s.squads.toMutableList()

        for (idx in 0 until unlockedCount) {
            val sq = newSquads[idx]
            if (sq.taskType == SquadTaskType.IDLE) continue

            val baseDuration = when (sq.taskType) {
                SquadTaskType.ARTISAN_SKILL, SquadTaskType.SEASONAL_NODE -> SKILL_CYCLE_MS
                SquadTaskType.RAID_REGION, SquadTaskType.DUNGEON_FLOOR, SquadTaskType.SEASONAL_BOSS,
                SquadTaskType.APEX_BOSS, SquadTaskType.WEEKLY_REALM_BOSS -> COMBAT_ROUND_MS
                SquadTaskType.IDLE -> continue
            }
            val cycleDuration = if (hasteActive) (baseDuration * 3L) / 4L else baseDuration

            var accumulated = sq.progressMs + deltaMs
            var keepTask = true
            while (accumulated >= cycleDuration && keepTask) {
                accumulated -= cycleDuration
                when (sq.taskType) {
                    SquadTaskType.ARTISAN_SKILL -> {
                        val skill = SkillType.entries.firstOrNull { it.name == sq.targetId }
                        if (skill != null) {
                            val res = executeSkillCycle(
                                state = s,
                                skill = skill,
                                surge = surge.type,
                                soloEvent = soloEvent.type,
                                xpMult = xpMult,
                                gatheringMult = gatheringBlessingMult * harvestHymnMult * offlineBlessingMult,
                                random = random
                            )
                            s = res.state
                            logs.addAll(res.logs)
                        }
                    }
                    SquadTaskType.SEASONAL_NODE -> {
                        val activeHoliday = seasonal.effectiveHoliday
                        val node = activeHoliday?.nodes?.firstOrNull { it.id == sq.targetId }
                        if (node != null) {
                            val res = executeSeasonalNodeCycle(s, node.id, node.holidayId, xpMult)
                            s = res.state
                            logs.addAll(res.logs)
                        } else {
                            keepTask = false
                        }
                    }
                    SquadTaskType.RAID_REGION -> {
                        val region = GameCatalog.regionById(sq.targetId)
                        val res = executeRegionCombatRound(
                            state = s,
                            region = region,
                            surge = surge.type,
                            soloEvent = soloEvent.type,
                            xpMult = xpMult,
                            lootBlessingMult = (if (lootBlessingActive) 1.35 else 1.0) * offlineBlessingMult,
                            lootBlessingDropBonus = if (lootBlessingActive) 0.10f else 0f,
                            random = random
                        )
                        s = res.state
                        logs.addAll(res.logs)
                    }
                    SquadTaskType.DUNGEON_FLOOR -> {
                        val floorNum = sq.targetId.toIntOrNull() ?: s.currentDungeonFloor
                        if (floorNum <= s.highestDungeonFloor) {
                            val res = executeDungeonFloorRound(
                                state = s,
                                floorNumber = floorNum,
                                surge = surge.type,
                                soloEvent = soloEvent.type,
                                xpMult = xpMult,
                                lootBlessingMult = (if (lootBlessingActive) 1.35 else 1.0) * offlineBlessingMult,
                                lootBlessingDropBonus = if (lootBlessingActive) 0.10f else 0f,
                                random = random
                            )
                            s = res.state
                            logs.addAll(res.logs)
                        } else {
                            keepTask = false
                        }
                    }
                    SquadTaskType.SEASONAL_BOSS -> {
                        val activeHoliday = seasonal.effectiveHoliday
                        if (activeHoliday != null) {
                            val res = executeSeasonalBossRound(s, activeHoliday.id, surge.type, soloEvent.type, xpMult, random)
                            s = res.state
                            logs.addAll(res.logs)
                        } else {
                            keepTask = false
                        }
                    }
                    SquadTaskType.APEX_BOSS -> {
                        val boss = GameCatalog.apexBosses.firstOrNull { it.id == sq.targetId }
                        val maxAttempts = 3 + s.apexBonusAttempts
                        if (boss != null && s.apexAttemptsUsed < maxAttempts) {
                            val res = executeApexBossRound(s, boss.id, surge.type, soloEvent.type, xpMult, random)
                            s = res.first
                            logs.addAll(res.second)
                            if (res.third) keepTask = false
                        } else {
                            keepTask = false
                        }
                    }
                    SquadTaskType.WEEKLY_REALM_BOSS -> {
                        if (!s.weeklyBossDefeated) {
                            val res = executeWeeklyRealmBossRound(s, surge.type, xpMult, random)
                            s = res.first
                            logs.addAll(res.second)
                            if (res.third) keepTask = false
                        } else {
                            keepTask = false
                        }
                    }
                    SquadTaskType.IDLE -> keepTask = false
                }
            }

            newSquads[idx] = if (keepTask) {
                sq.copy(progressMs = accumulated)
            } else {
                SquadAssignment(slotIndex = idx)
            }
        }

        s = s.copy(squads = newSquads, lastSavedTimestampMs = nowMs)
        return TickResult(s, logs.takeLast(12))
    }

    fun simulateOfflineProgress(
        initialState: GameState,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
        random: Random = Random.Default
    ): OfflineSimResult {
        val lastSaved = initialState.lastSavedTimestampMs
        if (lastSaved <= 0L || nowMs <= lastSaved) {
            return OfflineSimResult(syncClocks(initialState, nowMs, today).copy(lastSavedTimestampMs = nowMs), null)
        }
        val rawElapsedMs = nowMs - lastSaved
        if (rawElapsedMs < 15_000L) {
            return OfflineSimResult(syncClocks(initialState, nowMs, today).copy(lastSavedTimestampMs = nowMs), null)
        }

        val capHours = if (initialState.supporterPackOwned) 24 else 12
        val capMs = if (initialState.supporterPackOwned) TWENTY_FOUR_HOURS_MS else TWELVE_HOURS_MS
        val effectiveMs = min(rawElapsedMs, capMs)

        val anyActiveSquad = initialState.squads.take(initialState.unlockedSquadSlots())
            .any { it.taskType != SquadTaskType.IDLE }
        if (!anyActiveSquad) {
            return OfflineSimResult(
                syncClocks(initialState, nowMs, today).copy(lastSavedTimestampMs = nowMs),
                null
            )
        }

        val startGold = initialState.gold
        val startSkillXp = initialState.skills.values.sumOf { it.xp + it.level * 100L }
        val startKills = initialState.regionKills.values.sum() + initialState.dungeonFloorKills.values.sum()
        val startRations = initialState.warRations
        val startWood = initialState.wood
        val startStone = initialState.stone
        val startOre = initialState.ore
        val startFish = initialState.rawFish
        val startBars = initialState.ironBars
        val startDiamonds = initialState.diamonds
        val startVibranium = initialState.vibranium
        val startVaultIds = initialState.vaultItems.map { it.id }.toSet()

        var simState = syncClocks(initialState, lastSaved, today)
        val stepMs = when {
            effectiveMs <= 300_000L -> 2500L
            effectiveMs <= 3_600_000L -> 5000L
            else -> 15_000L
        }
        var remaining = effectiveMs
        var stepTimeMs = lastSaved
        while (remaining > 0L) {
            val dt = min(stepMs, remaining)
            stepTimeMs += dt
            simState = advanceTick(simState, dt, stepTimeMs, today, random, isOfflineStep = true).state
            remaining -= dt
        }
        simState = syncClocks(simState, nowMs, today)

        val goldEarned = (simState.gold - startGold).coerceAtLeast(0L)
        val warlordXpGained = (totalWarlordXpApprox(simState) - totalWarlordXpApprox(initialState)).coerceAtLeast(0L)
        val skillXpGained = (simState.skills.values.sumOf { it.xp + it.level * 100L } - startSkillXp).coerceAtLeast(0L)
        val endKills = simState.regionKills.values.sum() + simState.dungeonFloorKills.values.sum()
        val kills = (endKills - startKills).coerceAtLeast(0)
        val rationsEaten = (startRations - simState.warRations).coerceAtLeast(0)

        val summary = buildList {
            val dWood = simState.wood - startWood
            val dStone = simState.stone - startStone
            val dOre = simState.ore - startOre
            val dFish = simState.rawFish - startFish
            val dBars = simState.ironBars - startBars
            val dDiamonds = simState.diamonds - startDiamonds
            val dVib = simState.vibranium - startVibranium
            val dRations = simState.warRations - startRations
            if (dWood > 0) add("+$dWood Wood")
            if (dStone > 0) add("+$dStone Stone")
            if (dOre > 0) add("+$dOre Ore")
            if (dFish > 0) add("+$dFish Fish")
            if (dBars > 0) add("+$dBars Ingots")
            if (dDiamonds > 0) add("+$dDiamonds Diamonds")
            if (dVib > 0) add("+$dVib Vibranium")
            if (dRations > 0) add("+$dRations Rations")
        }

        val newItems = simState.vaultItems.filter { it.id !in startVaultIds }
        val report = OfflineReport(
            elapsedMinutes = (effectiveMs / 60_000L).coerceAtLeast(1L),
            cappedHours = capHours,
            goldEarned = goldEarned,
            warlordXpGained = warlordXpGained,
            skillXpGained = skillXpGained,
            enemiesSlain = kills,
            rationsConsumed = rationsEaten,
            resourcesSummary = summary,
            itemsFound = newItems.take(8)
        )

        return OfflineSimResult(simState.copy(lastSavedTimestampMs = nowMs), report)
    }

    private fun totalWarlordXpApprox(state: GameState): Long {
        var total = state.warlordXp
        for (lvl in 1 until state.warlordLevel) {
            total += GameCatalog.xpRequiredForLevel(lvl)
        }
        return total
    }

    private fun incrementContract(state: GameState, type: ContractType, amount: Int = 1): GameState {
        val updated = state.dailyContracts.map { c ->
            if (c.type == type && !c.claimed && c.progress < c.target) {
                c.copy(progress = (c.progress + amount).coerceAtMost(c.target))
            } else c
        }
        return state.copy(dailyContracts = updated)
    }

    private fun executeSkillCycle(
        state: GameState,
        skill: SkillType,
        surge: RealmSurgeType,
        soloEvent: SoloEventType,
        xpMult: Double,
        gatheringMult: Double = 1.0,
        random: Random
    ): TickResult {
        var s = state
        val logs = mutableListOf<CombatLogEntry>()
        val surgeSpeedBoost = if (surge == RealmSurgeType.ARTISAN_SPEED_SURGE) 1.35 else 1.0
        val miningBoost = if (skill == SkillType.MINING && surge == RealmSurgeType.MINING_SURGE) 1.5 else 1.0
        val lumberMult = 1.0 + (s.buildingLevel(BuildingType.LUMBER_MILL) - 1) * 0.12
        val quarryMult = 1.0 + (s.buildingLevel(BuildingType.QUARRY) - 1) * 0.12
        val farmMult = 1.0 + (s.buildingLevel(BuildingType.FARMSTEAD) - 1) * 0.12
        val treasuryMult = 1.0 + (s.buildingLevel(BuildingType.TREASURY) - 1) * 0.06
        val skillLvl = s.skillProgress(skill).level
        val lvlBonus = 1 + (skillLvl / 8)
        val age = s.currentMaterialAge()

        var baseXp = GameCatalog.skillNodes.firstOrNull { it.skill == skill }?.baseXp ?: 16L
        val eventPoints = if (soloEvent == SoloEventType.CITADEL_EXPANSION) 6L else 1L

        when (skill) {
            SkillType.WOODCUTTING -> {
                val axeBase = if (s.axeTier == 0) random.nextInt(1, 3) else random.nextInt(2, 5) + (s.axeTier * 2)
                val gained = ceil((axeBase + lvlBonus / 2) * lumberMult * surgeSpeedBoost * gatheringMult).toLong()
                s = s.copy(wood = s.wood + gained)
            }
            SkillType.MINING -> {
                val pick = s.pickaxeTier
                val stoneGain = ceil((random.nextInt(1, 4) + pick + lvlBonus / 2) * quarryMult * miningBoost * surgeSpeedBoost * gatheringMult).toLong()
                val coalGain = if (pick >= 1 || random.nextFloat() < 0.45f) random.nextInt(1, 3).toLong() else 0L
                val oreGain = if (pick >= 1 || age.tier >= MaterialAge.STONE.tier || skillLvl >= 2) {
                    ceil((random.nextInt(1, 3) + pick + lvlBonus / 3) * quarryMult * miningBoost * surgeSpeedBoost * gatheringMult).toLong()
                } else 0L
                val goldOreGain = if ((pick >= 3 || age.tier >= MaterialAge.GOLD.tier) && random.nextFloat() < 0.45f) {
                    random.nextInt(1, 3).toLong()
                } else 0L
                val diamondGain = if ((pick >= 4 || age.tier >= MaterialAge.DIAMOND.tier) && random.nextFloat() < 0.35f) 1L else 0L
                val obsidianGain = if ((pick >= 5 || age.tier >= MaterialAge.OBSIDIAN.tier) && random.nextFloat() < 0.30f) 1L else 0L
                val vibOreGain = if ((pick >= 6 || age.tier >= MaterialAge.VIBRANIUM.tier) && random.nextFloat() < 0.25f) 1L else 0L
                s = s.copy(
                    stone = s.stone + stoneGain,
                    coal = s.coal + coalGain,
                    ore = s.ore + oreGain,
                    goldOre = s.goldOre + goldOreGain,
                    diamonds = s.diamonds + diamondGain,
                    obsidianShards = s.obsidianShards + obsidianGain,
                    vibraniumOre = s.vibraniumOre + vibOreGain
                )
            }
            SkillType.FISHING -> {
                val catch = applyFishingCatch(s, lvlBonus, surgeSpeedBoost, gatheringMult, random)
                s = catch.first
                catch.third?.let { logs.add(it) }
            }
            SkillType.FARMING -> {
                val grainGain = ceil((random.nextInt(3, 6) + lvlBonus) * farmMult * surgeSpeedBoost * gatheringMult).toLong()
                s = s.copy(grain = s.grain + grainGain, herbs = s.herbs + 1L)
            }
            SkillType.HUNTING -> {
                val hidesGain = ceil((random.nextInt(2, 4) + lvlBonus / 2) * surgeSpeedBoost * gatheringMult).toLong()
                val roll = random.nextFloat()
                s = when {
                    roll < 0.45f -> s.copy(
                        hides = s.hides + hidesGain,
                        leather = s.leather + 1L,
                        rawChicken = s.rawChicken + 1L,
                        eggs = s.eggs + 1L,
                        warRations = s.warRations + 1
                    )
                    roll < 0.80f -> s.copy(
                        hides = s.hides + hidesGain,
                        leather = s.leather + 1L,
                        rawMutton = s.rawMutton + 1L,
                        warRations = s.warRations + 1
                    )
                    else -> s.copy(
                        hides = s.hides + hidesGain,
                        leather = s.leather + 1L,
                        rawBeef = s.rawBeef + 1L,
                        warRations = s.warRations + 1
                    )
                }
            }
            SkillType.FORAGING -> {
                val herbGain = ceil((random.nextInt(3, 6) + lvlBonus) * surgeSpeedBoost * gatheringMult).toLong()
                val dustGain = if (random.nextFloat() < 0.35f) 1L else 0L
                s = s.copy(herbs = s.herbs + herbGain, runeDust = s.runeDust + dustGain)
            }
            SkillType.COOKING -> {
                when {
                    s.rawChicken >= 1L -> {
                        val map = s.cookedFoods.toMutableMap()
                        map[FoodType.ROAST_CHICKEN.name] = (map[FoodType.ROAST_CHICKEN.name] ?: 0) + 1
                        s = s.copy(rawChicken = s.rawChicken - 1L, cookedFoods = map, warRations = s.warRations + FoodType.ROAST_CHICKEN.rationsYield)
                    }
                    s.rawBeef >= 1L -> {
                        val map = s.cookedFoods.toMutableMap()
                        map[FoodType.SEARED_STEAK.name] = (map[FoodType.SEARED_STEAK.name] ?: 0) + 1
                        s = s.copy(rawBeef = s.rawBeef - 1L, cookedFoods = map, warRations = s.warRations + FoodType.SEARED_STEAK.rationsYield)
                    }
                    s.rawMutton >= 1L -> {
                        val map = s.cookedFoods.toMutableMap()
                        map[FoodType.ROAST_MUTTON.name] = (map[FoodType.ROAST_MUTTON.name] ?: 0) + 1
                        s = s.copy(rawMutton = s.rawMutton - 1L, cookedFoods = map, warRations = s.warRations + FoodType.ROAST_MUTTON.rationsYield)
                    }
                    s.eggs >= 2L -> {
                        val map = s.cookedFoods.toMutableMap()
                        map[FoodType.CAMPFIRE_EGGS.name] = (map[FoodType.CAMPFIRE_EGGS.name] ?: 0) + 1
                        s = s.copy(eggs = s.eggs - 2L, cookedFoods = map, warRations = s.warRations + FoodType.CAMPFIRE_EGGS.rationsYield)
                    }
                    s.rawFish >= 1L -> {
                        val cooked = ceil((if (surge == RealmSurgeType.ARTISAN_SPEED_SURGE) 3 else 2) * farmMult).toInt()
                        val map = s.cookedFoods.toMutableMap()
                        map[FoodType.GRILLED_FISH.name] = (map[FoodType.GRILLED_FISH.name] ?: 0) + 1
                        s = s.copy(rawFish = s.rawFish - 1L, cookedFoods = map, warRations = s.warRations + cooked)
                    }
                    s.grain >= 1L -> {
                        val map = s.cookedFoods.toMutableMap()
                        map[FoodType.HEARTH_BREAD.name] = (map[FoodType.HEARTH_BREAD.name] ?: 0) + 1
                        s = s.copy(grain = s.grain - 1L, cookedFoods = map, warRations = s.warRations + 2)
                    }
                    else -> {
                        s = s.copy(warRations = s.warRations + 1)
                        baseXp = 8L
                    }
                }
            }
            SkillType.SMITHING -> {
                val hasFuel = s.coal >= 1L || s.wood >= 1L
                if (s.ore >= 2L && hasFuel) {
                    val blacksmithBonus = if (random.nextFloat() < (s.buildingLevel(BuildingType.BLACKSMITH) * 0.05f)) 2L else 1L
                    val newCoal = if (s.coal >= 1L) s.coal - 1L else s.coal
                    val newWood = if (s.coal >= 1L) s.wood else s.wood - 1L
                    s = s.copy(ore = s.ore - 2L, coal = newCoal, wood = newWood, ironBars = s.ironBars + blacksmithBonus)
                    if (random.nextFloat() < 0.16f) {
                        val agePool = GameCatalog.templatesForAge(age).map { it.templateId }
                        val rarity = if (random.nextFloat() < 0.22f) GearRarity.UNCOMMON else GearRarity.COMMON
                        val item = GameCatalog.createEquipment(agePool.random(random), rarity)
                        val looted = addLootItem(s, item)
                        s = looted.first
                        if (looted.second != null) logs.add(looted.second!!)
                    }
                } else if (s.goldOre >= 2L && hasFuel) {
                    val newCoal = if (s.coal >= 1L) s.coal - 1L else s.coal
                    val newWood = if (s.coal >= 1L) s.wood else s.wood - 1L
                    s = s.copy(goldOre = s.goldOre - 2L, coal = newCoal, wood = newWood, goldBars = s.goldBars + 1L)
                } else {
                    s = s.copy(ore = s.ore + 1L)
                    baseXp = 6L
                }
            }
            SkillType.CRAFTING -> {
                if (s.wood >= 2L && (s.stone >= 1L || s.leather >= 1L)) {
                    val goldGain = ceil((8L + lvlBonus * 2L) * treasuryMult).toLong()
                    val newStone = if (s.stone >= 1L) s.stone - 1L else s.stone
                    val newLeather = if (s.stone >= 1L) s.leather else s.leather - 1L
                    s = s.copy(wood = s.wood - 2L, stone = newStone, leather = newLeather, gold = s.gold + goldGain)
                    if (random.nextFloat() < 0.12f) {
                        val agePool = GameCatalog.templatesForAge(age).map { it.templateId }
                        val item = GameCatalog.createEquipment(agePool.random(random), GearRarity.COMMON)
                        val looted = addLootItem(s, item)
                        s = looted.first
                        if (looted.second != null) logs.add(looted.second!!)
                    }
                } else {
                    s = s.copy(gold = s.gold + 3L)
                    baseXp = 8L
                }
            }
            SkillType.ALCHEMY -> {
                if (s.herbs >= 3L) {
                    s = s.copy(herbs = s.herbs - 3L, elixirs = s.elixirs + 1)
                } else {
                    s = s.copy(herbs = s.herbs + 1L)
                    baseXp = 8L
                }
            }
            SkillType.TAILORING -> {
                when {
                    s.wool >= 2L -> s = s.copy(wool = s.wool - 2L, cloth = s.cloth + 2L)
                    s.leather >= 2L -> s = s.copy(leather = s.leather - 2L, cloth = s.cloth + 2L)
                    s.hides >= 2L -> s = s.copy(hides = s.hides - 2L, cloth = s.cloth + 2L)
                    else -> {
                        s = s.copy(cloth = s.cloth + 1L)
                        baseXp = 8L
                    }
                }
            }
            SkillType.CONSTRUCTION -> {
                if (s.wood >= 2L && s.stone >= 1L) {
                    val goldGain = ceil((10L + lvlBonus * 2L) * treasuryMult).toLong()
                    s = s.copy(wood = s.wood - 2L, stone = s.stone - 1L, gold = s.gold + goldGain)
                } else {
                    s = s.copy(stone = s.stone + 1L)
                    baseXp = 8L
                }
            }
            SkillType.DIVINITY -> {
                val shrineMult = 1.0 + (s.buildingLevel(BuildingType.DIVINITY_SHRINE) - 1) * 0.08
                val devotionGain = ceil((3 + lvlBonus / 2) * shrineMult * surgeSpeedBoost * gatheringMult).toLong()
                s = s.copy(devotion = s.devotion + devotionGain)
            }
            SkillType.THIEVING -> {
                val goldGain = ceil((12L + lvlBonus * 3L) * treasuryMult).toLong()
                val bonusToken = if (random.nextFloat() < 0.004f) 1 else 0
                s = s.copy(
                    gold = s.gold + goldGain,
                    chronicleTokens = s.chronicleTokens + bonusToken
                )
            }
            SkillType.ENCHANTING -> {
                if (s.herbs >= 2L && s.stone >= 1L) {
                    val libMult = 1 + (s.buildingLevel(BuildingType.ROYAL_LIBRARY) / 5)
                    s = s.copy(herbs = s.herbs - 2L, stone = s.stone - 1L, runeDust = s.runeDust + 2L * libMult)
                } else {
                    s = s.copy(runeDust = s.runeDust + 1L)
                    baseXp = 8L
                }
            }
            else -> {}
        }

        if (s.currentPrayer() == DivinePrayer.HARVEST_HYMN && s.devotion >= 1L && skill != SkillType.DIVINITY) {
            s = s.copy(devotion = (s.devotion - 1L).coerceAtLeast(0L))
        } else if (s.currentPrayer() == DivinePrayer.ASCENDANT_LITANY && s.devotion >= 2L && skill != SkillType.DIVINITY) {
            s = s.copy(devotion = (s.devotion - 1L).coerceAtLeast(0L))
        }

        val gainedXp = ceil(baseXp * xpMult * surgeSpeedBoost).toLong()
        s = grantSkillXp(s, skill, gainedXp)
        s = incrementContract(s, ContractType.COMPLETE_SKILLS, 1)
        if (eventPoints > 0L) s = s.copy(soloEventPoints = s.soloEventPoints + eventPoints)
        return TickResult(s, logs)
    }

    private fun executeSeasonalNodeCycle(
        state: GameState,
        nodeId: String,
        holidayId: HolidayId,
        xpMult: Double
    ): TickResult {
        var s = state
        val obsBonus = 1 + (s.buildingLevel(BuildingType.OBSERVATORY) / 4)
        val essenceGain = (if (holidayId == HolidayId.LUNAR_DRAGON) 4L else 2L) * obsBonus
        when (nodeId) {
            "shadowfall_pumpkin" -> s = s.copy(warRations = s.warRations + 3, seasonalEssence = s.seasonalEssence + essenceGain)
            "shadowfall_wood", "wintertide_frostpine", "lunar_bamboo", "midsummer_ironwood" ->
                s = s.copy(wood = s.wood + if (holidayId == HolidayId.LUNAR_DRAGON) 8L else 5L, seasonalEssence = s.seasonalEssence + essenceGain)
            "wintertide_glacial", "lunar_jade", "midsummer_ignis" ->
                s = s.copy(ore = s.ore + if (holidayId == HolidayId.LUNAR_DRAGON) 7L else 4L, seasonalEssence = s.seasonalEssence + essenceGain)
        }
        val xp = ceil(24L * xpMult).toLong()
        s = grantSkillXp(s, SkillType.WOODCUTTING, xp)
        return TickResult(s)
    }

    private fun grantCombatStanceXp(state: GameState, xpBase: Long): GameState {
        var s = grantSkillXp(state, SkillType.VITALITY, (xpBase / 2).coerceAtLeast(2L))
        s = when (s.activePhalanx) {
            PhalanxStance.INFANTRY_VANGUARD -> {
                val s1 = grantSkillXp(s, SkillType.ATTACK, xpBase)
                grantSkillXp(s1, SkillType.DEFENCE, (xpBase / 2).coerceAtLeast(2L))
            }
            PhalanxStance.RANGED_VOLLEY -> grantSkillXp(s, SkillType.ARCHERY, xpBase)
            PhalanxStance.CAVALRY_CHARGE -> grantSkillXp(s, SkillType.STRENGTH, xpBase)
            PhalanxStance.SIEGE_BREACHER -> {
                val obsBoost = (xpBase * (1.0 + s.buildingLevel(BuildingType.OBSERVATORY) * 0.05)).toLong()
                grantSkillXp(s, SkillType.SORCERY, obsBoost)
            }
        }
        return s
    }

    private fun executeRegionCombatRound(
        state: GameState,
        region: com.example.data.RegionDef,
        surge: RealmSurgeType,
        soloEvent: SoloEventType,
        xpMult: Double,
        lootBlessingMult: Double = 1.0,
        lootBlessingDropBonus: Float = 0f,
        random: Random
    ): TickResult {
        var s = state
        val logs = mutableListOf<CombatLogEntry>()

        var enemyHp = if (s.currentEnemyName == region.enemyName && s.currentEnemyHp > 0) {
            s.currentEnemyHp
        } else {
            region.enemyHp
        }

        val kills = s.regionKills[region.id] ?: 0
        val masteryPct = conquestMasteryBonusPct(kills, s.buildingLevel(BuildingType.HUNTERS_LODGE))
        val phalanxMult = phalanxMultiplier(s.activePhalanx, region.enemyTroopType, isBoss = false)
        val surgeDmgMult = if (surge == RealmSurgeType.ARMY_MIGHT_SURGE) 1.25f else 1.0f
        val petDmgMult = 1f + (((activeCompanion(s)?.damageBonusPct ?: 0) * companionScale(s)).toFloat() / 100f)
        val elixirMult = if (s.elixirs > 0) 1.15f else 1.0f
        val masteryMult = 1f + (masteryPct / 100f)
        val shrineBonus = (s.buildingLevel(BuildingType.DIVINITY_SHRINE) - 1) * 0.02f
        val smiteActive = s.currentPrayer() == DivinePrayer.DIVINE_SMITE && s.devotion >= 1L
        val smiteMult = if (smiteActive) 1.20f + shrineBonus else 1.0f

        val playerDmg = ceil(s.attackPower() * phalanxMult * surgeDmgMult * petDmgMult * elixirMult * masteryMult * smiteMult).toInt().coerceAtLeast(3)
        enemyHp -= playerDmg

        if (enemyHp <= 0) {
            val lootSurgeMult = if (surge == RealmSurgeType.COMBAT_LOOT_SURGE) 1.4 else 1.0
            val treasuryMult = 1.0 + (s.buildingLevel(BuildingType.TREASURY) - 1) * 0.06
            val petGoldMult = 1.0 + (((activeCompanion(s)?.goldBonusPct ?: 0) * companionScale(s)) / 100.0)
            val smiteGoldMult = if (smiteActive) 1.10 else 1.0
            val goldGain = ceil(region.goldReward * lootSurgeMult * treasuryMult * petGoldMult * masteryMult * lootBlessingMult * smiteGoldMult).toLong()
            val xpGain = ceil(region.xpReward * xpMult).toLong()
            val newKills = kills + 1
            val updatedKillsMap = s.regionKills.toMutableMap().apply { put(region.id, newKills) }

            val devotionAfter = if (smiteActive) (s.devotion - 1L).coerceAtLeast(0L) else s.devotion
            s = s.copy(
                gold = s.gold + goldGain,
                devotion = devotionAfter,
                regionKills = updatedKillsMap,
                currentEnemyHp = region.enemyHp,
                currentEnemyMaxHp = region.enemyHp,
                currentEnemyName = region.enemyName,
                soloEventPoints = s.soloEventPoints + if (soloEvent == SoloEventType.MONSTER_HUNT) 14L else 2L
            )
            s = grantWarlordXp(s, xpGain)
            s = grantCombatStanceXp(s, xpGain)
            s = incrementContract(s, ContractType.SLAY_FOES, 1)

            if (region.id == "village_outskirts") {
                val dropRoll = random.nextFloat()
                s = when {
                    dropRoll < 0.40f -> s.copy(leather = s.leather + 1L)
                    dropRoll < 0.72f -> s.copy(rawChicken = s.rawChicken + 1L, eggs = s.eggs + 1L)
                    else -> s.copy(rawBeef = s.rawBeef + 1L)
                }
            }

            logs.add(CombatLogEntry(System.nanoTime(), "Slain ${region.enemyName} (+${goldGain}g, +${xpGain} XP)"))

            val petLootPct = ((activeCompanion(s)?.lootBonusPct ?: 0) * companionScale(s)).toFloat()
            val dropChance = 0.16f + (s.gearLootBonusPct() + masteryPct + petLootPct) / 100f +
                (if (surge == RealmSurgeType.COMBAT_LOOT_SURGE) 0.08f else 0f) + lootBlessingDropBonus
            if (random.nextFloat() < dropChance.coerceAtMost(0.65f)) {
                val rarityRoll = random.nextFloat()
                val rarity = when {
                    rarityRoll < 0.01f -> GearRarity.EPIC
                    rarityRoll < 0.05f -> GearRarity.RARE
                    rarityRoll < 0.28f -> GearRarity.UNCOMMON
                    else -> GearRarity.COMMON
                }
                val item = GameCatalog.createEquipment(region.dropPoolIds.random(random), rarity)
                val looted = addLootItem(s, item)
                s = looted.first
                if (looted.second != null) logs.add(looted.second!!)
            }
        } else {
            val wardActive = s.currentPrayer() == DivinePrayer.IRON_WARD && s.devotion >= 1L
            val effDef = if (wardActive) (s.defensePower() * 1.25f).toInt() else s.defensePower()
            val rawMitigated = (region.enemyAttack - (effDef / 3)).coerceAtLeast(1)
            val mitigated = if (wardActive) (rawMitigated * 0.85f).toInt().coerceAtLeast(1) else rawMitigated
            val hpAfterHit = s.currentHp - mitigated
            s = s.copy(
                devotion = if (wardActive) (s.devotion - 1L).coerceAtLeast(0L) else s.devotion,
                currentEnemyHp = enemyHp,
                currentEnemyMaxHp = region.enemyHp,
                currentEnemyName = region.enemyName
            )
            val autoAte = applyAutoEatIfNeeded(s, hpAfterHit, logs)
            if (autoAte != null) {
                s = autoAte
            } else if (hpAfterHit <= 0) {
                val infCap = 10 + s.buildingLevel(BuildingType.INFIRMARY) * 12
                val woundedAdd = if (s.troops.totalTroops() > 1) min(1, (infCap - s.woundedTroops).coerceAtLeast(0)) else 0
                s = s.copy(
                    currentHp = s.maxHp(),
                    currentEnemyHp = region.enemyHp,
                    woundedTroops = s.woundedTroops + woundedAdd
                )
                logs.add(CombatLogEntry(System.nanoTime(), "Retreated & recovered at ${s.villageName}", isAlert = true))
            } else {
                s = s.copy(currentHp = hpAfterHit)
            }
        }
        return TickResult(s, logs)
    }

    private fun applyFishingCatch(
        state: GameState,
        lvlBonus: Int,
        surgeSpeedBoost: Double,
        gatheringMult: Double,
        random: Random
    ): Triple<GameState, String, CombatLogEntry?> {
        val rod = state.fishingRodTier
        val fishGain = ceil((random.nextInt(1, 3) + rod + lvlBonus / 2) * surgeSpeedBoost * gatheringMult).toLong()
        var s = state.copy(rawFish = state.rawFish + fishGain)
        var dropLabel = "+$fishGain Fish"
        var logEntry: CombatLogEntry? = null

        val dropChance = (0.35f + rod * 0.05f).coerceAtMost(0.75f)
        if (random.nextFloat() < dropChance) {
            val roll = random.nextFloat()
            when {
                roll < 0.25f -> {
                    val w = 1L + random.nextInt(1, 3)
                    s = s.copy(wood = s.wood + w)
                    dropLabel += " & Driftwood (+$w Wood)"
                }
                roll < 0.48f -> {
                    s = s.copy(leather = s.leather + 1L)
                    dropLabel += " & Old Boot (+1 Leather)"
                }
                roll < 0.72f -> {
                    val g = 6L + random.nextInt(4, 15) + rod * 4L
                    s = s.copy(gold = s.gold + g)
                    dropLabel += " & Coin Pouch (+${g}g)"
                }
                roll < 0.87f -> {
                    if (rod >= 2) {
                        s = s.copy(diamonds = s.diamonds + 1L)
                        dropLabel += " & River Pearl (+1 Diamond)"
                        logEntry = CombatLogEntry(System.nanoTime(), "Fishing Treasure: River Pearl (+1 Diamond)!", isLoot = true)
                    } else {
                        s = s.copy(gold = s.gold + 14L)
                        dropLabel += " & Silver Ring (+14g)"
                    }
                }
                roll < 0.95f -> {
                    s = s.copy(runeDust = s.runeDust + 2L)
                    dropLabel += " & Rune Bottle (+2 Dust)"
                }
                else -> {
                    val pool = GameCatalog.templatesForAge(s.currentMaterialAge())
                    val item = GameCatalog.createEquipment(pool.random(random).templateId, GearRarity.UNCOMMON)
                    val looted = addLootItem(s, item)
                    s = looted.first
                    dropLabel += " & Sunken ${item.name}!"
                    logEntry = looted.second
                }
            }
        }
        return Triple(s, dropLabel, logEntry)
    }

    private fun applyAutoEatIfNeeded(
        state: GameState,
        hpAfterHit: Int,
        logs: MutableList<CombatLogEntry>
    ): GameState? {
        val thresholdHp = (state.maxHp() * state.autoEatThresholdPct) / 100
        if (hpAfterHit > thresholdHp) return null

        val bestCooked = state.cookedFoods
            .filterValues { it > 0 }
            .keys
            .mapNotNull { name -> FoodType.entries.firstOrNull { it.name == name } }
            .maxByOrNull { it.healHp }

        if (bestCooked != null) {
            val map = state.cookedFoods.toMutableMap()
            val rem = (map[bestCooked.name] ?: 1) - 1
            if (rem <= 0) map.remove(bestCooked.name) else map[bestCooked.name] = rem
            val healedHp = min(state.maxHp(), hpAfterHit.coerceAtLeast(0) + bestCooked.healHp)
            logs.add(CombatLogEntry(System.nanoTime(), "Auto-ate ${bestCooked.displayName} (+${bestCooked.healHp} HP)"))
            return state.copy(cookedFoods = map, currentHp = healedHp)
        }
        if (state.warRations > 0) {
            val healAmt = (state.maxHp() * 0.45f).toInt().coerceAtLeast(25)
            val healedHp = min(state.maxHp(), hpAfterHit.coerceAtLeast(0) + healAmt)
            logs.add(CombatLogEntry(System.nanoTime(), "Auto-ate War Ration (+$healAmt HP)"))
            return state.copy(warRations = state.warRations - 1, currentHp = healedHp)
        }
        return null
    }

    private fun executeDungeonFloorRound(
        state: GameState,
        floorNumber: Int,
        surge: RealmSurgeType,
        soloEvent: SoloEventType,
        xpMult: Double,
        lootBlessingMult: Double = 1.0,
        lootBlessingDropBonus: Float = 0f,
        random: Random
    ): TickResult {
        var s = state
        val logs = mutableListOf<CombatLogEntry>()
        val floorDef = GameCatalog.dungeonFloor(floorNumber)
        val currentMonster = floorDef.monsters.firstOrNull { it.name == s.currentDungeonEnemyName }
            ?: floorDef.monsters.first()

        var enemyHp = if (s.currentDungeonFloor == floorNumber &&
            s.currentDungeonEnemyName == currentMonster.name &&
            s.currentDungeonEnemyHp > 0
        ) {
            s.currentDungeonEnemyHp
        } else {
            currentMonster.hp
        }

        val floorKills = s.dungeonFloorKills[floorNumber.toString()] ?: 0
        val masteryPct = conquestMasteryBonusPct(floorKills, s.buildingLevel(BuildingType.HUNTERS_LODGE))
        val phalanxMult = phalanxMultiplier(s.activePhalanx, currentMonster.troopType, isBoss = false)
        val surgeDmgMult = if (surge == RealmSurgeType.ARMY_MIGHT_SURGE) 1.25f else 1.0f
        val petDmgMult = 1f + (((activeCompanion(s)?.damageBonusPct ?: 0) * companionScale(s)).toFloat() / 100f)
        val elixirMult = if (s.elixirs > 0) 1.15f else 1.0f
        val masteryMult = 1f + (masteryPct / 100f)
        val shrineBonus = (s.buildingLevel(BuildingType.DIVINITY_SHRINE) - 1) * 0.02f
        val smiteActive = s.currentPrayer() == DivinePrayer.DIVINE_SMITE && s.devotion >= 1L
        val smiteMult = if (smiteActive) 1.20f + shrineBonus else 1.0f

        val playerDmg = ceil(s.attackPower() * phalanxMult * surgeDmgMult * petDmgMult * elixirMult * masteryMult * smiteMult)
            .toInt().coerceAtLeast(3)
        enemyHp -= playerDmg

        if (enemyHp <= 0) {
            val lootSurgeMult = if (surge == RealmSurgeType.COMBAT_LOOT_SURGE) 1.4 else 1.0
            val treasuryMult = 1.0 + (s.buildingLevel(BuildingType.TREASURY) - 1) * 0.06
            val petGoldMult = 1.0 + (((activeCompanion(s)?.goldBonusPct ?: 0) * companionScale(s)) / 100.0)
            val smiteGoldMult = if (smiteActive) 1.10 else 1.0
            val goldGain = ceil(currentMonster.goldReward * lootSurgeMult * treasuryMult * petGoldMult * masteryMult * lootBlessingMult * smiteGoldMult).toLong()
            val xpGain = ceil(currentMonster.xpReward * xpMult).toLong()
            val newKills = floorKills + 1
            val updatedFloorKills = s.dungeonFloorKills.toMutableMap().apply { put(floorNumber.toString(), newKills) }

            val nextMonster = if (floorDef.monsters.size > 1) {
                val curIdx = floorDef.monsters.indexOfFirst { it.id == currentMonster.id }
                floorDef.monsters[(curIdx + 1) % floorDef.monsters.size]
            } else {
                currentMonster
            }

            val unlockedNewFloor = newKills >= floorDef.clearsToUnlockNext &&
                floorNumber < GameCatalog.dungeonFloors.size &&
                s.highestDungeonFloor < floorNumber + 1
            val nextHighestFloor = if (unlockedNewFloor) floorNumber + 1 else s.highestDungeonFloor

            val devotionAfter = if (smiteActive) (s.devotion - 1L).coerceAtLeast(0L) else s.devotion
            s = s.copy(
                gold = s.gold + goldGain,
                devotion = devotionAfter,
                currentDungeonFloor = floorNumber,
                highestDungeonFloor = nextHighestFloor,
                dungeonFloorKills = updatedFloorKills,
                currentDungeonEnemyName = nextMonster.name,
                currentDungeonEnemyHp = nextMonster.hp,
                currentDungeonEnemyMaxHp = nextMonster.hp,
                soloEventPoints = s.soloEventPoints + if (soloEvent == SoloEventType.MONSTER_HUNT) 14L else 3L
            )
            val dropResult = grantDungeonMonsterDrops(s, currentMonster.id, random)
            s = dropResult.first
            s = grantWarlordXp(s, xpGain)
            s = grantCombatStanceXp(s, xpGain)
            s = incrementContract(s, ContractType.SLAY_FOES, 1)

            logs.add(CombatLogEntry(System.nanoTime(), "${currentMonster.name} slain: ${dropResult.second} (+${goldGain}g)", isLoot = true))
            if (unlockedNewFloor) {
                logs.add(CombatLogEntry(System.nanoTime(), "Unlocked Dungeon Floor ${floorNumber + 1}!", isLoot = true))
            }

            val dropChance = 0.14f + (s.gearLootBonusPct() + masteryPct) / 100f + lootBlessingDropBonus
            if (random.nextFloat() < dropChance.coerceAtMost(0.55f)) {
                val item = GameCatalog.createEquipment(floorDef.dropGearPool.random(random), GearRarity.COMMON)
                val looted = addLootItem(s, item)
                s = looted.first
                if (looted.second != null) logs.add(looted.second!!)
            }
        } else {
            val wardActive = s.currentPrayer() == DivinePrayer.IRON_WARD && s.devotion >= 1L
            val effDef = if (wardActive) (s.defensePower() * 1.25f).toInt() else s.defensePower()
            val rawMitigated = (currentMonster.attack - (effDef / 3)).coerceAtLeast(if (floorNumber == 1) 0 else 1)
            val mitigated = if (wardActive) (rawMitigated * 0.85f).toInt().coerceAtLeast(0) else rawMitigated
            val hpAfterHit = s.currentHp - mitigated
            s = s.copy(
                devotion = if (wardActive) (s.devotion - 1L).coerceAtLeast(0L) else s.devotion,
                currentDungeonFloor = floorNumber,
                currentDungeonEnemyName = currentMonster.name,
                currentDungeonEnemyHp = enemyHp,
                currentDungeonEnemyMaxHp = currentMonster.hp
            )
            val autoAte = applyAutoEatIfNeeded(s, hpAfterHit, logs)
            if (autoAte != null) {
                s = autoAte
            } else if (hpAfterHit <= 0) {
                s = s.copy(currentHp = s.maxHp(), currentDungeonEnemyHp = currentMonster.hp)
                logs.add(CombatLogEntry(System.nanoTime(), "Retreated from ${floorDef.name} to recover", isAlert = true))
            } else {
                s = s.copy(currentHp = hpAfterHit)
            }
        }
        return TickResult(s, logs)
    }

    private fun grantDungeonMonsterDrops(
        state: GameState,
        monsterId: String,
        random: Random
    ): Pair<GameState, String> {
        return when (monsterId) {
            "pasture_hen" -> {
                val eggGain = if (random.nextFloat() < 0.80f) 1L else 0L
                val featherGain = random.nextInt(1, 3).toLong()
                val next = state.copy(
                    rawChicken = state.rawChicken + 1L,
                    eggs = state.eggs + eggGain,
                    feathers = state.feathers + featherGain
                )
                val eggText = if (eggGain > 0) ", +1 Egg" else ""
                next to "+1 Raw Chicken$eggText, +$featherGain Feathers"
            }
            "grazing_cow" -> {
                val leatherGain = random.nextInt(1, 3).toLong()
                val next = state.copy(
                    rawBeef = state.rawBeef + 1L,
                    leather = state.leather + leatherGain
                )
                next to "+1 Raw Beef, +$leatherGain Leather"
            }
            "woolly_sheep" -> {
                val woolGain = random.nextInt(1, 3).toLong()
                val next = state.copy(
                    rawMutton = state.rawMutton + 1L,
                    wool = state.wool + woolGain
                )
                next to "+1 Raw Mutton, +$woolGain Wool"
            }
            "cave_boar" -> state.copy(rawBeef = state.rawBeef + 1L, leather = state.leather + 1L, stone = state.stone + 2L) to "+1 Beef, +1 Leather, +2 Stone"
            "quarry_slime" -> state.copy(coal = state.coal + 2L, ore = state.ore + 2L) to "+2 Coal, +2 Ore"
            "tunnel_kobold" -> state.copy(ore = state.ore + 2L, coal = state.coal + 1L) to "+2 Ore, +1 Coal"
            "iron_sentinel" -> state.copy(ore = state.ore + 2L, ironBars = state.ironBars + 1L) to "+2 Ore, +1 Iron Ingot"
            "crypt_skeleton" -> state.copy(goldOre = state.goldOre + 2L, runeDust = state.runeDust + 1L, devotion = state.devotion + 2L) to "+2 Gold Ore, +1 Dust"
            "gilded_mimic" -> state.copy(goldOre = state.goldOre + 3L) to "+3 Gold Ore"
            "crystal_stalker" -> state.copy(diamonds = state.diamonds + 1L, runeDust = state.runeDust + 2L) to "+1 Diamond, +2 Dust"
            "prism_golem" -> state.copy(diamonds = state.diamonds + 1L, goldOre = state.goldOre + 2L) to "+1 Diamond, +2 Gold Ore"
            "magma_imp" -> state.copy(obsidianShards = state.obsidianShards + 2L, coal = state.coal + 2L) to "+2 Obsidian, +2 Coal"
            "nether_knight" -> state.copy(obsidianShards = state.obsidianShards + 3L) to "+3 Obsidian Shards"
            else -> state.copy(vibraniumOre = state.vibraniumOre + 2L) to "+2 Vibranium Ore"
        }
    }

    private fun executeSeasonalBossRound(
        state: GameState,
        holidayId: HolidayId,
        surge: RealmSurgeType,
        soloEvent: SoloEventType,
        xpMult: Double,
        random: Random
    ): TickResult {
        var s = state
        val holiday = GameCatalog.holidayById(holidayId)
        val logs = mutableListOf<CombatLogEntry>()

        var bossHp = if (s.currentEnemyName == holiday.bossName && s.currentEnemyHp > 0) s.currentEnemyHp else holiday.bossHp
        val phalanxMult = phalanxMultiplier(s.activePhalanx, TroopType.SIEGE, isBoss = true)
        val dmg = ceil(s.attackPower() * phalanxMult * (if (surge == RealmSurgeType.ARMY_MIGHT_SURGE) 1.25f else 1.0f)).toInt()
        bossHp -= dmg

        if (bossHp <= 0) {
            val goldGain = holiday.bossGoldReward
            val xpGain = ceil(160L * xpMult).toLong()
            s = s.copy(
                gold = s.gold + goldGain,
                seasonalEssence = s.seasonalEssence + 10L,
                currentEnemyHp = holiday.bossHp,
                currentEnemyMaxHp = holiday.bossHp,
                currentEnemyName = holiday.bossName,
                soloEventPoints = s.soloEventPoints + if (soloEvent == SoloEventType.MONSTER_HUNT) 45L else 10L
            )
            s = grantWarlordXp(s, xpGain)
            s = grantCombatStanceXp(s, xpGain)
            s = incrementContract(s, ContractType.SLAY_FOES, 1)
            val rarity = if (random.nextFloat() < 0.25f) GearRarity.RARE else GearRarity.UNCOMMON
            val drop = GameCatalog.createEquipment(holiday.exclusiveDropIds.random(random), rarity)
            val looted = addLootItem(s, drop)
            s = looted.first
            logs.add(CombatLogEntry(System.nanoTime(), "Slain ${holiday.bossName}! +${goldGain}g & Seasonal Relic!", isLoot = true))
            if (looted.second != null) logs.add(looted.second!!)
        } else {
            val dmgTaken = (holiday.bossAttack - s.defensePower() / 3).coerceAtLeast(4)
            var hp = s.currentHp - dmgTaken
            val threshold = (s.maxHp() * s.autoEatThresholdPct) / 100
            if (hp <= threshold && s.warRations > 0) {
                hp = min(s.maxHp(), hp + (s.maxHp() * 0.45f).toInt())
                s = s.copy(warRations = s.warRations - 1)
            } else if (hp <= 0) {
                hp = s.maxHp()
                bossHp = holiday.bossHp
                logs.add(CombatLogEntry(System.nanoTime(), "Retreated from ${holiday.bossName}!", isAlert = true))
            }
            s = s.copy(
                currentHp = hp,
                currentEnemyHp = bossHp,
                currentEnemyMaxHp = holiday.bossHp,
                currentEnemyName = holiday.bossName
            )
        }
        return TickResult(s, logs)
    }

    private fun executeApexBossRound(
        state: GameState,
        bossId: String,
        surge: RealmSurgeType,
        soloEvent: SoloEventType,
        xpMult: Double,
        random: Random
    ): Triple<GameState, List<CombatLogEntry>, Boolean> {
        var s = state
        val boss = GameCatalog.apexBosses.first { it.id == bossId }
        val logs = mutableListOf<CombatLogEntry>()

        var bossHp = if (s.currentEnemyName == boss.name && s.currentEnemyHp > 0) s.currentEnemyHp else boss.hp
        val phalanxMult = phalanxMultiplier(s.activePhalanx, TroopType.SIEGE, isBoss = true)
        val lodgeBonus = 1f + (s.buildingLevel(BuildingType.HUNTERS_LODGE) - 1) * 0.04f
        val dmg = ceil(s.attackPower() * phalanxMult * lodgeBonus * (if (surge == RealmSurgeType.ARMY_MIGHT_SURGE) 1.25f else 1.0f)).toInt()
        bossHp -= dmg

        if (bossHp <= 0) {
            val goldGain = boss.goldReward
            val xpGain = ceil(boss.xpReward * xpMult).toLong()
            s = s.copy(
                gold = s.gold + goldGain,
                royalCrowns = s.royalCrowns + boss.crownBonus,
                apexAttemptsUsed = s.apexAttemptsUsed + 1,
                apexKillsToday = s.apexKillsToday + 1,
                currentEnemyHp = boss.hp,
                currentEnemyMaxHp = boss.hp,
                currentEnemyName = boss.name,
                soloEventPoints = s.soloEventPoints + if (soloEvent == SoloEventType.MONSTER_HUNT) 60L else 15L
            )
            s = grantWarlordXp(s, xpGain)
            s = grantCombatStanceXp(s, xpGain)
            s = incrementContract(s, ContractType.SLAY_FOES, 1)
            val rarity = if (random.nextFloat() < 0.25f) GearRarity.EPIC else GearRarity.RARE
            val item = GameCatalog.createEquipment(boss.dropPoolIds.random(random), rarity)
            val looted = addLootItem(s, item)
            s = looted.first
            logs.add(CombatLogEntry(System.nanoTime(), "Apex Victory: ${boss.name}! +${goldGain}g, +${boss.crownBonus} Crowns", isLoot = true))
            if (looted.second != null) logs.add(looted.second!!)
            val exhausted = s.apexAttemptsUsed >= (3 + s.apexBonusAttempts)
            return Triple(s, logs, exhausted)
        } else {
            val dmgTaken = (boss.attack - s.defensePower() / 3).coerceAtLeast(6)
            var hp = s.currentHp - dmgTaken
            val threshold = (s.maxHp() * s.autoEatThresholdPct) / 100
            if (hp <= threshold && s.warRations > 0) {
                hp = min(s.maxHp(), hp + (s.maxHp() * 0.45f).toInt())
                s = s.copy(warRations = s.warRations - 1)
            } else if (hp <= 0) {
                s = s.copy(
                    currentHp = s.maxHp(),
                    apexAttemptsUsed = s.apexAttemptsUsed + 1,
                    currentEnemyHp = boss.hp,
                    currentEnemyMaxHp = boss.hp,
                    currentEnemyName = boss.name
                )
                logs.add(CombatLogEntry(System.nanoTime(), "Apex Siege failed — 1 attempt consumed", isAlert = true))
                val exhausted = s.apexAttemptsUsed >= (3 + s.apexBonusAttempts)
                return Triple(s, logs, exhausted)
            }
            s = s.copy(
                currentHp = hp,
                currentEnemyHp = bossHp,
                currentEnemyMaxHp = boss.hp,
                currentEnemyName = boss.name
            )
            return Triple(s, logs, false)
        }
    }

    private fun executeWeeklyRealmBossRound(
        state: GameState,
        surge: RealmSurgeType,
        xpMult: Double,
        random: Random
    ): Triple<GameState, List<CombatLogEntry>, Boolean> {
        var s = state
        val logs = mutableListOf<CombatLogEntry>()
        val phalanxMult = phalanxMultiplier(s.activePhalanx, TroopType.SIEGE, isBoss = true)
        val dmg = ceil(s.attackPower() * phalanxMult * (if (surge == RealmSurgeType.ARMY_MIGHT_SURGE) 1.25f else 1.0f)).toLong()
        val remaining = (s.weeklyBossHpRemaining - dmg).coerceAtLeast(0L)

        s = grantCombatStanceXp(s, ceil(25L * xpMult).toLong())
        if (remaining <= 0L) {
            val rewardGold = 1500L + s.buildingLevel(BuildingType.CITADEL) * 400L
            s = s.copy(
                weeklyBossHpRemaining = 0L,
                weeklyBossDefeated = true,
                gold = s.gold + rewardGold,
                royalCrowns = s.royalCrowns + 15,
                chronicleTokens = s.chronicleTokens + 1
            )
            val age = s.currentMaterialAge()
            val pool = GameCatalog.templatesForAge(age)
            val drop = GameCatalog.createEquipment(pool.random(random).templateId, GearRarity.EPIC)
            val looted = addLootItem(s, drop)
            s = looted.first
            logs.add(CombatLogEntry(System.nanoTime(), "Weekly Realm Boss Defeated! +${rewardGold}g, +15 Crowns, +1 Token!", isLoot = true))
            return Triple(s, logs, true)
        } else {
            s = s.copy(weeklyBossHpRemaining = remaining)
            return Triple(s, logs, false)
        }
    }

    fun recordCollectionDiscovery(state: GameState, item: EquipmentItem): GameState {
        val key = "${item.templateId}:${item.rarity.name}"
        return if (key in state.discoveredGearIds) state
        else state.copy(discoveredGearIds = state.discoveredGearIds + key)
    }

    fun addLootItem(state: GameState, item: EquipmentItem): Pair<GameState, CombatLogEntry?> {
        val withCollection = recordCollectionDiscovery(state, item)
        if (withCollection.vaultExpansionOwned && withCollection.autoSalvageJunkEnabled &&
            item.rarity == GearRarity.COMMON && !item.isSeasonal
        ) {
            return Pair(
                withCollection.copy(gold = withCollection.gold + item.sellGold),
                CombatLogEntry(System.nanoTime(), "Auto-Salvaged ${item.name} (+${item.sellGold}g)", isLoot = true)
            )
        }
        if (withCollection.vaultItems.size >= withCollection.maxVaultCapacity()) {
            return Pair(
                withCollection.copy(gold = withCollection.gold + item.sellGold),
                CombatLogEntry(System.nanoTime(), "Vault full: Sold ${item.name} (+${item.sellGold}g)", isAlert = true)
            )
        }
        return Pair(
            withCollection.copy(vaultItems = withCollection.vaultItems + item),
            CombatLogEntry(System.nanoTime(), "Looted [${item.rarity.displayName}] ${item.name}", isLoot = true)
        )
    }

    fun grantWarlordXp(state: GameState, amount: Long): GameState {
        var lvl = state.warlordLevel
        var xp = state.warlordXp + amount
        var hp = state.currentHp
        while (xp >= GameCatalog.xpRequiredForLevel(lvl)) {
            xp -= GameCatalog.xpRequiredForLevel(lvl)
            lvl++
            hp = state.copy(warlordLevel = lvl).maxHp()
        }
        return state.copy(warlordLevel = lvl, warlordXp = xp, currentHp = hp)
    }

    fun grantSkillXp(state: GameState, skill: SkillType, amount: Long): GameState {
        val current = state.skillProgress(skill)
        if (current.level >= skill.maxLevel) return state
        var lvl = current.level
        var xp = current.xp + amount
        while (lvl < skill.maxLevel && xp >= GameCatalog.xpRequiredForLevel(lvl)) {
            xp -= GameCatalog.xpRequiredForLevel(lvl)
            lvl++
        }
        if (lvl >= skill.maxLevel) xp = 0L
        val updated = state.skills.toMutableMap().apply {
            put(skill.name, SkillProgress(lvl, xp))
        }
        return state.copy(skills = updated)
    }

    fun upgradeBuilding(state: GameState, type: BuildingType, nowMs: Long = System.currentTimeMillis()): Pair<GameState, String?> {
        val curLvl = state.buildingLevel(type)
        if (curLvl >= type.maxLevel) return state to "${type.displayName} is already Max Level"
        val hasteMult = if (state.isBlessingActive(RoyalBlessingType.BUILD_TRAINING_HASTE, nowMs)) 0.75 else 1.0
        val goldCost = ceil(GameCatalog.buildingGoldCost(type, curLvl) * hasteMult).toLong()
        val woodCost = ceil(GameCatalog.buildingWoodCost(type, curLvl) * hasteMult).toLong()
        val stoneCost = ceil(GameCatalog.buildingStoneCost(type, curLvl) * hasteMult).toLong()
        val barCost = ceil(GameCatalog.buildingBarCost(type, curLvl) * hasteMult).toLong()

        if (state.gold < goldCost || state.wood < woodCost || state.stone < stoneCost || state.ironBars < barCost) {
            return state to "Need ${goldCost}g, ${woodCost} Wood" +
                (if (stoneCost > 0) ", $stoneCost Stone" else "") +
                (if (barCost > 0) ", $barCost Ingots" else "")
        }

        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs).type
        val points = if (soloEvent == SoloEventType.CITADEL_EXPANSION) 55L else 10L
        val prevAge = state.currentMaterialAge()
        val updatedBuildings = state.buildings.toMutableMap().apply {
            put(type.name, curLvl + 1)
        }
        var nextState = state.copy(
            gold = state.gold - goldCost,
            wood = state.wood - woodCost,
            stone = state.stone - stoneCost,
            ironBars = state.ironBars - barCost,
            buildings = updatedBuildings,
            soloEventPoints = state.soloEventPoints + points
        )
        nextState = grantSkillXp(nextState, SkillType.CONSTRUCTION, 35L * curLvl)
        val newAge = nextState.currentMaterialAge()
        val ageMsg = if (type == BuildingType.CITADEL && newAge != prevAge) {
            " Advanced to ${newAge.displayName}!"
        } else ""
        return nextState to "${type.displayName} upgraded to Lv.${curLvl + 1}!$ageMsg"
    }

    fun trainTroopBatch(
        state: GameState,
        troopType: TroopType,
        tier: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<GameState, String> {
        val barracksLvl = state.buildingLevel(BuildingType.BARRACKS)
        val citadelLvl = state.buildingLevel(BuildingType.CITADEL)
        if (tier == 2 && barracksLvl < 4) return state to "Requires Barracks Lv.4"
        if (tier == 3 && barracksLvl < 9) return state to "Requires Barracks Lv.9"

        val hasteActive = state.isBlessingActive(RoyalBlessingType.BUILD_TRAINING_HASTE, nowMs)
        val baseBatch = 3 + (citadelLvl - 1)
        val batchSize = if (hasteActive) ceil(baseBatch * 1.25).toInt() else baseBatch
        val costMult = if (hasteActive) 0.75 else 1.0
        val goldCost = ceil(baseBatch * (6L * tier * tier) * costMult).toLong()
        val rationCost = ceil(baseBatch * tier * costMult).toInt().coerceAtLeast(1)
        if (state.gold < goldCost || state.warRations < rationCost) {
            return state to "Need ${goldCost}g & $rationCost War Rations"
        }

        val r = state.troops
        val updatedTroops = when (troopType) {
            TroopType.INFANTRY -> when (tier) {
                1 -> r.copy(t1Infantry = r.t1Infantry + batchSize)
                2 -> r.copy(t2Infantry = r.t2Infantry + batchSize)
                else -> r.copy(t3Infantry = r.t3Infantry + batchSize)
            }
            TroopType.RANGED -> when (tier) {
                1 -> r.copy(t1Ranged = r.t1Ranged + batchSize)
                2 -> r.copy(t2Ranged = r.t2Ranged + batchSize)
                else -> r.copy(t3Ranged = r.t3Ranged + batchSize)
            }
            TroopType.CAVALRY -> when (tier) {
                1 -> r.copy(t1Cavalry = r.t1Cavalry + batchSize)
                2 -> r.copy(t2Cavalry = r.t2Cavalry + batchSize)
                else -> r.copy(t3Cavalry = r.t3Cavalry + batchSize)
            }
            TroopType.SIEGE -> when (tier) {
                1 -> r.copy(t1Siege = r.t1Siege + batchSize)
                2 -> r.copy(t2Siege = r.t2Siege + batchSize)
                else -> r.copy(t3Siege = r.t3Siege + batchSize)
            }
        }

        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs).type
        val points = if (soloEvent == SoloEventType.ARMS_RACE) (batchSize * tier * 4L) else 8L
        return state.copy(
            gold = state.gold - goldCost,
            warRations = state.warRations - rationCost,
            troops = updatedTroops,
            soloEventPoints = state.soloEventPoints + points
        ) to "Recruited +$batchSize Tier $tier ${troopType.displayName}!"
    }

    fun healWounded(state: GameState): Pair<GameState, String> {
        if (state.woundedTroops <= 0) return state to "No wounded soldiers in Infirmary"
        val cost = state.woundedTroops * 10L
        if (state.gold < cost) return state to "Need ${cost}g to heal wounded"
        val healed = state.woundedTroops
        return state.copy(
            gold = state.gold - cost,
            woundedTroops = 0,
            troops = state.troops.copy(t1Infantry = state.troops.t1Infantry + healed)
        ) to "Healed $healed soldiers back to active duty!"
    }

    fun fuseThreeItems(
        state: GameState,
        templateId: String,
        rarity: GearRarity,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<GameState, String> {
        if (rarity == GearRarity.TRANSCENDENT) return state to "Already Transcendent (Tier 12)!"
        val matching = state.vaultItems.filter { it.templateId == templateId && it.rarity == rarity }
        if (matching.size < 3) return state to "Need 3 identical pieces in Vault"

        val toRemoveIds = matching.take(3).map { it.id }.toSet()
        val upgraded = GameCatalog.upgradeEquipment(matching.first())
            ?: return state to "Cannot upgrade further"
        val remainingVault = state.vaultItems.filterNot { it.id in toRemoveIds } + upgraded

        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs).type
        val points = if (soloEvent == SoloEventType.ARMS_RACE) 75L else 15L

        var next = state.copy(
            vaultItems = remainingVault,
            soloEventPoints = state.soloEventPoints + points
        )
        next = recordCollectionDiscovery(next, upgraded)
        next = incrementContract(next, ContractType.FUSE_OR_CRAFT, 1)
        return next to "Forged [${upgraded.rarity.displayName}] ${upgraded.name} (+25% stats)!"
    }

    fun craftAgeGear(state: GameState, age: MaterialAge, random: Random = Random.Default): Pair<GameState, String> {
        if (state.buildingLevel(BuildingType.CITADEL) < age.reqCitadelLevel) {
            return state to "Requires Citadel Lv.${age.reqCitadelLevel} for ${age.displayName}"
        }
        val cost = GameCatalog.ageCraftCost(age)
        if (state.gold < cost.gold || state.wood < cost.wood || state.stone < cost.stone ||
            state.ironBars < cost.bars || state.diamonds < cost.diamonds || state.vibranium < cost.vibranium
        ) {
            return state to "Insufficient materials for ${age.displayName} craft"
        }
        val pool = GameCatalog.templatesForAge(age)
        val chosen = pool.random(random)
        val rarityRoll = random.nextFloat()
        val rarity = when {
            rarityRoll < 0.04f -> GearRarity.RARE
            rarityRoll < 0.25f -> GearRarity.UNCOMMON
            else -> GearRarity.COMMON
        }
        val item = GameCatalog.createEquipment(chosen.templateId, rarity)
        val afterCost = state.copy(
            gold = state.gold - cost.gold,
            wood = state.wood - cost.wood,
            stone = state.stone - cost.stone,
            ironBars = state.ironBars - cost.bars,
            diamonds = state.diamonds - cost.diamonds,
            vibranium = state.vibranium - cost.vibranium
        )
        val added = addLootItem(afterCost, item)
        var next = incrementContract(added.first, ContractType.FUSE_OR_CRAFT, 1)
        next = grantSkillXp(next, SkillType.SMITHING, 25L * age.tier)
        return next to "Crafted [${item.rarity.displayName}] ${item.name}!"
    }

    fun performInstantSkillAction(
        state: GameState,
        skill: SkillType,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
        random: Random = Random.Default
    ): Pair<GameState, String> {
        val s0 = syncClocks(state, nowMs, today)
        val surge = CalendarEventEngine.getCurrentRealmSurge(nowMs).type
        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs).type
        val academyBonus = (s0.buildingLevel(BuildingType.ACADEMY) - 1) * 0.05
        val petBonus = ((activeCompanion(s0)?.skillXpBonusPct ?: 0) / 100.0) * companionScale(s0)
        val supporterBonus = if (s0.supporterPackOwned) 0.15 else 0.0
        val xpBlessingBonus = if (s0.isBlessingActive(RoyalBlessingType.SKILL_XP_SURGE, nowMs)) 0.35 else 0.0
        val prayerXpBonus = if (s0.currentPrayer() == DivinePrayer.ASCENDANT_LITANY && s0.devotion >= 2L) 0.30 else 0.0
        val xpMult = 1.0 + academyBonus + petBonus + supporterBonus + xpBlessingBonus + prayerXpBonus
        val gatheringBlessingMult = if (s0.isBlessingActive(RoyalBlessingType.GATHERING_BOUNTY, nowMs)) 1.40 else 1.0
        val harvestHymnMult = if (s0.currentPrayer() == DivinePrayer.HARVEST_HYMN && s0.devotion >= 1L) 1.25 else 1.0

        val res = executeSkillCycle(
            state = s0,
            skill = skill,
            surge = surge,
            soloEvent = soloEvent,
            xpMult = xpMult,
            gatheringMult = gatheringBlessingMult * harvestHymnMult,
            random = random
        )
        val next = res.state
        val msg = when (skill) {
            SkillType.COOKING -> "Cooked +${(next.warRations - s0.warRations).coerceAtLeast(1)} War Rations!"
            SkillType.SMITHING -> {
                val bars = next.ironBars - s0.ironBars
                val gBars = next.goldBars - s0.goldBars
                when {
                    bars > 0 -> "Smithed +$bars Iron Ingot(s)!"
                    gBars > 0 -> "Smithed +$gBars Gold Ingot(s)!"
                    else -> "Prospected +1 Ore vein for smelting!"
                }
            }
            SkillType.DIVINITY -> "Sanctum Prayer: +${next.devotion - s0.devotion} Devotion!"
            SkillType.ALCHEMY -> if (next.elixirs > s0.elixirs) "Brewed +1 War Elixir!" else "Gathered +1 Herb for brewing!"
            SkillType.ENCHANTING -> "Refined +${next.runeDust - s0.runeDust} Rune Dust!"
            SkillType.TAILORING -> "Wove +${next.cloth - s0.cloth} Cloth!"
            SkillType.THIEVING -> "Pilfered +${next.gold - s0.gold}g!"
            SkillType.WOODCUTTING -> if (s0.axeTier == 0) "Punched trees: +${next.wood - s0.wood} Wood!" else "Chopped +${next.wood - s0.wood} Wood!"
            SkillType.MINING -> buildString {
                append("Mined +${next.stone - s0.stone} Stone")
                if (next.coal > s0.coal) append(", +${next.coal - s0.coal} Coal")
                if (next.ore > s0.ore) append(", +${next.ore - s0.ore} Ore")
                if (next.goldOre > s0.goldOre) append(", +${next.goldOre - s0.goldOre} Gold Ore")
                if (next.diamonds > s0.diamonds) append(", +${next.diamonds - s0.diamonds} Diamond")
                if (next.vibraniumOre > s0.vibraniumOre) append(", +${next.vibraniumOre - s0.vibraniumOre} Vibranium Ore")
            }
            SkillType.FISHING -> {
                val extra = buildList {
                    if (next.wood > s0.wood) add("+${next.wood - s0.wood} Driftwood")
                    if (next.leather > s0.leather) add("+${next.leather - s0.leather} Leather")
                    if (next.gold > s0.gold) add("+${next.gold - s0.gold}g")
                    if (next.diamonds > s0.diamonds) add("+${next.diamonds - s0.diamonds} Pearl")
                    if (next.runeDust > s0.runeDust) add("+${next.runeDust - s0.runeDust} Dust")
                }
                if (extra.isEmpty()) "Caught +${next.rawFish - s0.rawFish} Fish!"
                else "Caught +${next.rawFish - s0.rawFish} Fish & ${extra.joinToString()}!"
            }
            SkillType.FARMING -> "Harvested +${next.grain - s0.grain} Grain!"
            SkillType.HUNTING -> "Hunted +${next.hides - s0.hides} Hides & Meat!"
            SkillType.FORAGING -> "Foraged +${next.herbs - s0.herbs} Herbs!"
            SkillType.CONSTRUCTION -> if (next.gold > s0.gold) "Constructed supplies (+${next.gold - s0.gold}g)!" else "Quarried +1 Stone!"
            else -> "${skill.displayName} action complete!"
        }
        return next to msg
    }

    fun attackDungeonFloor(
        state: GameState,
        floorNumber: Int,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
        random: Random = Random.Default
    ): Pair<GameState, String> {
        if (floorNumber > state.highestDungeonFloor) {
            return state to "Clear Floor ${floorNumber - 1} to unlock Floor $floorNumber!"
        }
        val s0 = syncClocks(state, nowMs, today)
        val surge = CalendarEventEngine.getCurrentRealmSurge(nowMs).type
        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs).type
        val res = executeDungeonFloorRound(
            state = s0,
            floorNumber = floorNumber,
            surge = surge,
            soloEvent = soloEvent,
            xpMult = 1.0,
            random = random
        )
        val summary = res.logs.lastOrNull()?.text ?: "Struck ${res.state.currentDungeonEnemyName} (${res.state.currentDungeonEnemyHp} HP left)"
        return res.state to summary
    }

    fun cookFoodRecipe(state: GameState, food: FoodType): Pair<GameState, String> {
        val afterCost = when (food) {
            FoodType.ROAST_CHICKEN -> if (state.rawChicken >= 1L && state.wood >= 1L) {
                state.copy(rawChicken = state.rawChicken - 1L, wood = state.wood - 1L)
            } else return state to "Need ${food.ingredientSummary}"
            FoodType.CAMPFIRE_EGGS -> if (state.eggs >= 2L) {
                state.copy(eggs = state.eggs - 2L)
            } else return state to "Need ${food.ingredientSummary}"
            FoodType.SEARED_STEAK -> if (state.rawBeef >= 1L && state.wood >= 1L) {
                state.copy(rawBeef = state.rawBeef - 1L, wood = state.wood - 1L)
            } else return state to "Need ${food.ingredientSummary}"
            FoodType.ROAST_MUTTON -> if (state.rawMutton >= 1L && state.herbs >= 1L) {
                state.copy(rawMutton = state.rawMutton - 1L, herbs = state.herbs - 1L)
            } else return state to "Need ${food.ingredientSummary}"
            FoodType.GRILLED_FISH -> if (state.rawFish >= 1L && state.wood >= 1L) {
                state.copy(rawFish = state.rawFish - 1L, wood = state.wood - 1L)
            } else return state to "Need ${food.ingredientSummary}"
            FoodType.HEARTH_BREAD -> if (state.grain >= 2L) {
                state.copy(grain = state.grain - 2L)
            } else return state to "Need ${food.ingredientSummary}"
            FoodType.WARLORD_STEW -> if (state.rawChicken >= 1L && state.rawFish >= 1L && state.herbs >= 1L) {
                state.copy(rawChicken = state.rawChicken - 1L, rawFish = state.rawFish - 1L, herbs = state.herbs - 1L)
            } else return state to "Need ${food.ingredientSummary}"
        }
        val map = afterCost.cookedFoods.toMutableMap()
        map[food.name] = (map[food.name] ?: 0) + 1
        var next = afterCost.copy(
            cookedFoods = map,
            warRations = afterCost.warRations + food.rationsYield
        )
        next = grantSkillXp(next, SkillType.COOKING, food.xpReward)
        next = incrementContract(next, ContractType.COMPLETE_SKILLS, 1)
        return next to "Cooked 1x ${food.displayName} (+${food.rationsYield} Rations, +${food.xpReward} Cook XP)"
    }

    fun eatCookedFood(state: GameState, food: FoodType): Pair<GameState, String> {
        val count = state.cookedFoods[food.name] ?: 0
        if (count <= 0) return state to "No ${food.displayName} cooked yet!"
        if (state.currentHp >= state.maxHp()) return state to "Already at full HP!"
        val map = state.cookedFoods.toMutableMap()
        if (count <= 1) map.remove(food.name) else map[food.name] = count - 1
        val newHp = min(state.maxHp(), state.currentHp + food.healHp)
        return state.copy(cookedFoods = map, currentHp = newHp) to "Ate ${food.displayName} (+${food.healHp} HP)"
    }

    fun eatBestAvailableFood(state: GameState): Pair<GameState, String> {
        if (state.currentHp >= state.maxHp()) return state to "Already at full HP!"
        val best = state.cookedFoods
            .filterValues { it > 0 }
            .keys
            .mapNotNull { key -> FoodType.entries.firstOrNull { it.name == key } }
            .maxByOrNull { it.healHp }
        if (best != null) return eatCookedFood(state, best)
        if (state.warRations > 0) {
            val healAmt = (state.maxHp() * 0.45f).toInt().coerceAtLeast(25)
            val newHp = min(state.maxHp(), state.currentHp + healAmt)
            return state.copy(warRations = state.warRations - 1, currentHp = newHp) to "Ate War Ration (+$healAmt HP)"
        }
        return state to "No food! Hunt Floor 1 (Chicken/Cow/Sheep) or Fish & Cook."
    }

    fun smeltOre(state: GameState, recipe: FurnaceRecipe): Pair<GameState, String> {
        val hasCoal = state.coal >= 1L
        val hasWood = state.wood >= 1L
        if (!hasCoal && !hasWood) {
            return state to "Furnace needs Fuel (1 Coal or 1 Wood)"
        }
        val afterFuel = if (hasCoal) state.copy(coal = state.coal - 1L) else state.copy(wood = state.wood - 1L)
        val afterSmelt = when (recipe) {
            FurnaceRecipe.SMELT_IRON -> {
                if (state.ore < 2L) return state to "Need 2 Iron Ore + 1 Fuel (Coal or Wood)"
                afterFuel.copy(ore = afterFuel.ore - 2L, ironBars = afterFuel.ironBars + 1L)
            }
            FurnaceRecipe.SMELT_GOLD -> {
                if (state.goldOre < 2L) return state to "Need 2 Gold Ore + 1 Fuel"
                afterFuel.copy(goldOre = afterFuel.goldOre - 2L, goldBars = afterFuel.goldBars + 1L)
            }
            FurnaceRecipe.SMELT_VIBRANIUM -> {
                if (state.vibraniumOre < 2L) return state to "Need 2 Vibranium Ore + 1 Fuel"
                afterFuel.copy(vibraniumOre = afterFuel.vibraniumOre - 2L, vibranium = afterFuel.vibranium + 1L)
            }
        }
        var next = grantSkillXp(afterSmelt, SkillType.SMITHING, recipe.xpReward)
        next = incrementContract(next, ContractType.COMPLETE_SKILLS, 1)
        return next to "Smelted 1x ${recipe.outputName} (+${recipe.xpReward} Smithing XP)"
    }

    fun craftOrUpgradeTool(state: GameState, tool: ToolType): Pair<GameState, String> {
        val currentTier = state.toolTier(tool)
        val nextTier = currentTier + 1
        if (nextTier > 7) return state to "${tool.displayName} is already Vibranium Tier!"
        val cost = GameCatalog.toolCraftCost(nextTier)
        if (state.wood < cost.wood || state.stone < cost.stone || state.ironBars < cost.ironBars ||
            state.goldBars < cost.goldBars || state.diamonds < cost.diamonds ||
            state.obsidianShards < cost.obsidian || state.vibranium < cost.vibraniumBars
        ) {
            val missing = buildList {
                if (cost.wood > 0) add("${cost.wood} Wood")
                if (cost.stone > 0) add("${cost.stone} Stone")
                if (cost.ironBars > 0) add("${cost.ironBars} Iron Ingots")
                if (cost.goldBars > 0) add("${cost.goldBars} Gold Ingots")
                if (cost.diamonds > 0) add("${cost.diamonds} Diamonds")
                if (cost.obsidian > 0) add("${cost.obsidian} Obsidian")
                if (cost.vibraniumBars > 0) add("${cost.vibraniumBars} Vibranium")
            }
            return state to "Need ${missing.joinToString(", ")} for ${ToolType.tierLabel(nextTier)} ${tool.displayName}"
        }
        val deducted = state.copy(
            wood = state.wood - cost.wood,
            stone = state.stone - cost.stone,
            ironBars = state.ironBars - cost.ironBars,
            goldBars = state.goldBars - cost.goldBars,
            diamonds = state.diamonds - cost.diamonds,
            obsidianShards = state.obsidianShards - cost.obsidian,
            vibranium = state.vibranium - cost.vibraniumBars
        )
        var next = when (tool) {
            ToolType.AXE -> deducted.copy(axeTier = nextTier)
            ToolType.PICKAXE -> deducted.copy(pickaxeTier = nextTier)
            ToolType.FISHING_ROD -> deducted.copy(fishingRodTier = nextTier)
        }
        next = grantSkillXp(next, SkillType.CRAFTING, 25L * nextTier)
        next = incrementContract(next, ContractType.FUSE_OR_CRAFT, 1)
        return next to "Crafted ${ToolType.tierLabel(nextTier)} ${tool.displayName}!"
    }

    fun toggleDivinePrayer(state: GameState, prayer: DivinePrayer): Pair<GameState, String> {
        val divLvl = state.skillProgress(SkillType.DIVINITY).level
        if (divLvl < prayer.reqDivinityLevel) {
            return state to "Requires Divinity Lv.${prayer.reqDivinityLevel}"
        }
        return if (state.activePrayerId == prayer.name) {
            state.copy(activePrayerId = null) to "${prayer.displayName} deactivated"
        } else {
            state.copy(activePrayerId = prayer.name) to "${prayer.displayName} invoked!"
        }
    }

    fun instantUpgradeEquippedRarity(
        state: GameState,
        slot: EquipSlot,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<GameState, String> {
        val current = state.equipped[slot.name] ?: return state to "No gear equipped in ${slot.displayName}"
        val nextRarity = current.rarity.next() ?: return state to "${current.name} is already Transcendent!"
        val (goldCost, ingotCost) = GameCatalog.rarityUpgradeCost(current)
        if (state.gold < goldCost || state.ironBars < ingotCost) {
            return state to "Need ${goldCost}g & $ingotCost Ingots to upgrade"
        }
        val upgraded = GameCatalog.upgradeEquipment(current)?.copy(id = current.id)
            ?: return state to "Cannot upgrade further"
        val newEquipped = state.equipped.toMutableMap().apply { put(slot.name, upgraded) }
        val soloEvent = CalendarEventEngine.getCurrentSoloEvent(nowMs).type
        val points = if (soloEvent == SoloEventType.ARMS_RACE) 50L else 12L
        var next = state.copy(
            gold = state.gold - goldCost,
            ironBars = state.ironBars - ingotCost,
            equipped = newEquipped,
            soloEventPoints = state.soloEventPoints + points
        )
        next = recordCollectionDiscovery(next, upgraded)
        next = incrementContract(next, ContractType.FUSE_OR_CRAFT, 1)
        next = grantSkillXp(next, SkillType.SMITHING, 30L * (nextRarity.rank + 1))
        return next to "Upgraded ${upgraded.name} to ${upgraded.rarity.displayName}!"
    }

    fun fuseAllReady(state: GameState, nowMs: Long = System.currentTimeMillis()): Pair<GameState, String> {
        var current = state
        var totalFused = 0
        while (true) {
            val readyGroup = current.vaultItems
                .filter { it.rarity != GearRarity.TRANSCENDENT }
                .groupBy { it.templateId to it.rarity }
                .values
                .firstOrNull { it.size >= 3 } ?: break
            val sample = readyGroup.first()
            current = fuseThreeItems(current, sample.templateId, sample.rarity, nowMs).first
            totalFused++
        }
        return if (totalFused == 0) current to "No 3-of-a-kind sets ready in Vault"
        else current to "Instantly fused $totalFused gear sets!"
    }

    fun enchantEquippedSlot(state: GameState, slot: EquipSlot): Pair<GameState, String> {
        val current = state.equipped[slot.name] ?: return state to "No item equipped in ${slot.displayName}"
        val dustCost = 4L + (current.enchantLevel * 4L)
        if (state.runeDust < dustCost) return state to "Need $dustCost Rune Dust (from Foraging/Enchanting)"
        val enchanted = current.copy(
            enchantLevel = current.enchantLevel + 1,
            attackBonus = ceil(current.attackBonus * 1.12).toInt().coerceAtLeast(current.attackBonus + 1),
            defenseBonus = ceil(current.defenseBonus * 1.12).toInt().coerceAtLeast(current.defenseBonus + 1),
            hpBonus = ceil(current.hpBonus * 1.12).toInt()
        )
        val newEquipped = state.equipped.toMutableMap().apply { put(slot.name, enchanted) }
        val next = grantSkillXp(
            state.copy(runeDust = state.runeDust - dustCost, equipped = newEquipped),
            SkillType.ENCHANTING,
            30L
        )
        return next to "Enchanted ${enchanted.name} to +${enchanted.enchantLevel}!"
    }

    fun unlockOrEquipCompanion(state: GameState, companionId: String): Pair<GameState, String> {
        val def = GameCatalog.companions.firstOrNull { it.id == companionId } ?: return state to "Unknown companion"
        if (companionId in state.unlockedCompanions) {
            return state.copy(activeCompanionId = companionId) to "${def.name} is now accompanying you!"
        }
        val sanctuaryLvl = state.buildingLevel(BuildingType.BEAST_SANCTUARY)
        if (sanctuaryLvl < def.reqSanctuaryLevel) {
            return state to "Requires Beast Sanctuary Lv.${def.reqSanctuaryLevel}"
        }
        if (state.gold < def.goldCost) return state to "Need ${def.goldCost}g to bond ${def.name}"
        return state.copy(
            gold = state.gold - def.goldCost,
            unlockedCompanions = state.unlockedCompanions + companionId,
            activeCompanionId = companionId
        ) to "Bonded ${def.name} (${def.bonusText})!"
    }

    fun fightTournamentChallenger(state: GameState, rank: Int): Pair<GameState, String> {
        val opp = GameCatalog.tournamentOpponents.firstOrNull { it.rank == rank } ?: return state to "Invalid opponent"
        if (rank != state.tournamentWinsThisWeek + 1) {
            return state to if (rank <= state.tournamentWinsThisWeek) "Already defeated this week!" else "Defeat Rank ${rank - 1} first!"
        }
        val playerScore = state.attackPower() * 2 + state.defensePower() + (state.effectiveArmyMight() / 15L).toInt()
        if (playerScore < opp.powerRating) {
            return state to "Too weak! Your Power $playerScore vs ${opp.name} (${opp.powerRating})"
        }
        val next = state.copy(
            gold = state.gold + opp.rewardGold,
            royalCrowns = state.royalCrowns + opp.rewardCrowns,
            tournamentWinsThisWeek = state.tournamentWinsThisWeek + 1
        )
        return grantWarlordXp(next, 90L * rank) to "Tournament Victory vs ${opp.name}! +${opp.rewardGold}g, +${opp.rewardCrowns} Crowns"
    }

    fun claimRoyalContract(state: GameState, contractId: String): Pair<GameState, String> {
        val c = state.dailyContracts.firstOrNull { it.id == contractId } ?: return state to "Contract not found"
        if (c.claimed) return state to "Already claimed"
        if (c.progress < c.target) return state to "Progress ${c.progress}/${c.target}"
        val updated = state.dailyContracts.map { if (it.id == contractId) it.copy(claimed = true) else it }
        return state.copy(
            gold = state.gold + c.rewardGold,
            royalCrowns = state.royalCrowns + c.rewardCrowns,
            dailyContracts = updated
        ) to "Contract Complete! +${c.rewardGold}g & +${c.rewardCrowns} Crowns"
    }

    fun equipItem(state: GameState, item: EquipmentItem): GameState {
        val currentEquipped = state.equipped[item.slot.name]
        val newVault = state.vaultItems.filterNot { it.id == item.id }.toMutableList()
        if (currentEquipped != null) {
            newVault.add(currentEquipped)
        }
        val newEquipped = state.equipped.toMutableMap().apply {
            put(item.slot.name, item)
        }
        return state.copy(
            equipped = newEquipped,
            vaultItems = newVault,
            currentHp = min(state.currentHp, state.copy(equipped = newEquipped).maxHp())
        )
    }

    fun sellAllJunk(state: GameState): Pair<GameState, String> {
        val junk = state.vaultItems.filter { it.rarity == GearRarity.COMMON && !it.isSeasonal }
        if (junk.isEmpty()) return state to "No Common non-seasonal junk in Vault"
        val treasuryMult = 1.0 + (state.buildingLevel(BuildingType.TREASURY) - 1) * 0.06
        val goldGain = ceil(junk.sumOf { it.sellGold.toLong() } * treasuryMult).toLong()
        val kept = state.vaultItems.filterNot { it.rarity == GearRarity.COMMON && !it.isSeasonal }
        return state.copy(gold = state.gold + goldGain, vaultItems = kept) to "Sold ${junk.size} junk items for +${goldGain}g"
    }

    fun claimSoloEventTier(state: GameState, tierIndex: Int, nowMs: Long = System.currentTimeMillis()): Pair<GameState, String> {
        val event = CalendarEventEngine.getCurrentSoloEvent(nowMs)
        val threshold = event.tierThresholds.getOrNull(tierIndex) ?: return state to "Invalid tier"
        if (state.soloEventPoints < threshold) return state to "Need $threshold event points"
        if (tierIndex in state.soloEventClaimedTiers) return state to "Tier already claimed"

        val rewardGold = (tierIndex + 1) * 180L
        val rewardBars = (tierIndex + 1) * 4L
        val rewardCrowns = if (tierIndex == 2) 6 else 2

        return state.copy(
            gold = state.gold + rewardGold,
            ironBars = state.ironBars + rewardBars,
            royalCrowns = state.royalCrowns + rewardCrowns,
            soloEventClaimedTiers = state.soloEventClaimedTiers + tierIndex
        ) to "Claimed Chest ${tierIndex + 1}: +${rewardGold}g, +${rewardBars} Ingots, +${rewardCrowns} Crowns!"
    }

    fun summonSeasonalBoss(state: GameState, holidayId: HolidayId, nowMs: Long = System.currentTimeMillis()): Pair<GameState, String> {
        if (state.chronicleTokens < 1) return state to "Need 1 Chronicle Festival Token"
        val twoHoursMs = 2L * 3600L * 1000L
        val holiday = GameCatalog.holidayById(holidayId)
        return state.copy(
            chronicleTokens = state.chronicleTokens - 1,
            summonedHolidayId = holidayId.name,
            summonedBossExpiresAtMs = nowMs + twoHoursMs
        ) to "Summoned ${holiday.name} (${holiday.bossName}) for 2 hours!"
    }

    fun useCrownSpeedUp(state: GameState): Pair<GameState, String> {
        val cost = 15
        if (state.royalCrowns < cost) return state to "Need $cost Royal Crowns"
        var s = state.copy(royalCrowns = state.royalCrowns - cost)
        val res = advanceTick(s, 600_000L, System.currentTimeMillis(), LocalDate.now())
        s = res.state.copy(gold = res.state.gold + 150L, ironBars = res.state.ironBars + 3L)
        return s to "Royal Warp: 10m instant squad progress + 150g + 3 Ingots!"
    }

    fun buyBonusApexAttempt(state: GameState): Pair<GameState, String> {
        val cost = 20
        if (state.royalCrowns < cost) return state to "Need $cost Royal Crowns"
        return state.copy(
            royalCrowns = state.royalCrowns - cost,
            apexBonusAttempts = state.apexBonusAttempts + 1
        ) to "+1 Apex Boss attempt unlocked!"
    }

    fun grantRoyalBlessingFromAd(
        state: GameState,
        blessing: RoyalBlessingType,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now()
    ): Pair<GameState, String> {
        var s = syncClocks(state, nowMs, today)
        if (s.adsWatchedToday >= s.dailyAdLimit) {
            return s to "Daily optional ad limit reached (${s.dailyAdLimit}/${s.dailyAdLimit})"
        }
        val cooldownMs = s.adCooldownRemainingMs(nowMs)
        if (cooldownMs > 0L) {
            val sec = ((cooldownMs + 999L) / 1000L)
            return s to "Blessing cooldown active (${sec}s remaining)"
        }
        s = applyRoyalBlessingEffect(s, blessing, nowMs, today)
        return s.copy(
            adsDateKey = today.toString(),
            adsWatchedToday = s.adsWatchedToday + 1,
            lastAdWatchedAtMs = nowMs
        ) to "Royal Blessing Granted: ${blessing.displayName}!"
    }

    fun activateRoyalBlessingWithCrowns(
        state: GameState,
        blessing: RoyalBlessingType,
        nowMs: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now()
    ): Pair<GameState, String> {
        if (blessing == RoyalBlessingType.ROYAL_CROWN_CACHE) {
            return state to "Earn Royal Crowns via Contracts, Bosses, and Tournaments"
        }
        val cost = blessing.crownAlternativeCost
        if (state.royalCrowns < cost) {
            return state to "Need $cost Royal Crowns"
        }
        val s = applyRoyalBlessingEffect(
            state.copy(royalCrowns = state.royalCrowns - cost),
            blessing,
            nowMs,
            today
        )
        return s to "Activated ${blessing.displayName} with $cost Crowns!"
    }

    private fun applyRoyalBlessingEffect(
        state: GameState,
        blessing: RoyalBlessingType,
        nowMs: Long,
        today: LocalDate
    ): GameState {
        return when (blessing) {
            RoyalBlessingType.SKILL_XP_SURGE,
            RoyalBlessingType.GATHERING_BOUNTY,
            RoyalBlessingType.COMBAT_LOOT_LUCK,
            RoyalBlessingType.OFFLINE_FORTUNE,
            RoyalBlessingType.BUILD_TRAINING_HASTE -> {
                val existingEnd = state.activeBlessingExpiries[blessing.name]?.coerceAtLeast(nowMs) ?: nowMs
                val updated = state.activeBlessingExpiries.toMutableMap().apply {
                    put(blessing.name, existingEnd + blessing.durationMs)
                }
                state.copy(activeBlessingExpiries = updated)
            }
            RoyalBlessingType.INSTANT_SQUAD_SURGE -> {
                val res = advanceTick(state, 900_000L, nowMs, today)
                res.state.copy(gold = res.state.gold + 100L)
            }
            RoyalBlessingType.ROYAL_CROWN_CACHE -> {
                state.copy(royalCrowns = state.royalCrowns + 15)
            }
            RoyalBlessingType.APEX_BOSS_ATTEMPT -> {
                state.copy(apexBonusAttempts = state.apexBonusAttempts + 1)
            }
        }
    }
}
