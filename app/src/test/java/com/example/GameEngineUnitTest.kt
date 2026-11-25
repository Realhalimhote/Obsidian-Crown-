package com.example

import com.example.data.GameCatalog
import com.example.engine.CalendarEventEngine
import com.example.engine.GameSimulationEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class GameEngineUnitTest {

    @Test
    fun seasonalCalendarDetectsRealWorldHolidaysAndCountdowns() {
        val baseState = GameState()
        val halloween = CalendarEventEngine.getSeasonalStatus(LocalDate.of(2026, 10, 25), 0L, baseState)
        assertEquals(HolidayId.SHADOWFALL, halloween.activeCalendarHoliday?.id)

        val newYearJan = CalendarEventEngine.getSeasonalStatus(LocalDate.of(2026, 1, 3), 0L, baseState)
        assertEquals(HolidayId.WINTERTIDE, newYearJan.activeCalendarHoliday?.id)

        val lunar = CalendarEventEngine.getSeasonalStatus(LocalDate.of(2026, 2, 10), 0L, baseState)
        assertEquals(HolidayId.LUNAR_DRAGON, lunar.activeCalendarHoliday?.id)

        val midsummer = CalendarEventEngine.getSeasonalStatus(LocalDate.of(2026, 7, 15), 0L, baseState)
        assertEquals(HolidayId.MIDSUMMER_SUNFORGE, midsummer.activeCalendarHoliday?.id)

        val offSeason = CalendarEventEngine.getSeasonalStatus(LocalDate.of(2026, 11, 25), 0L, baseState)
        assertNull(offSeason.activeCalendarHoliday)
        assertEquals(HolidayId.WINTERTIDE, offSeason.nextHoliday.id)
        assertEquals(15L, offSeason.daysUntilNextHoliday)
    }

    @Test
    fun materialAgeProgressesFromWoodToVibranium() {
        assertEquals(MaterialAge.WOOD, GameState(buildings = mapOf(BuildingType.CITADEL.name to 1)).currentMaterialAge())
        assertEquals(MaterialAge.STONE, GameState(buildings = mapOf(BuildingType.CITADEL.name to 2)).currentMaterialAge())
        assertEquals(MaterialAge.IRON, GameState(buildings = mapOf(BuildingType.CITADEL.name to 3)).currentMaterialAge())
        assertEquals(MaterialAge.GOLD, GameState(buildings = mapOf(BuildingType.CITADEL.name to 5)).currentMaterialAge())
        assertEquals(MaterialAge.DIAMOND, GameState(buildings = mapOf(BuildingType.CITADEL.name to 8)).currentMaterialAge())
        assertEquals(MaterialAge.OBSIDIAN, GameState(buildings = mapOf(BuildingType.CITADEL.name to 11)).currentMaterialAge())
        assertEquals(MaterialAge.VIBRANIUM, GameState(buildings = mapOf(BuildingType.CITADEL.name to 15)).currentMaterialAge())
    }

    @Test
    fun phalanxTriangleAndBossMatchupsWork() {
        assertEquals(1.25f, GameSimulationEngine.phalanxMultiplier(PhalanxStance.INFANTRY_VANGUARD, TroopType.RANGED))
        assertEquals(1.25f, GameSimulationEngine.phalanxMultiplier(PhalanxStance.RANGED_VOLLEY, TroopType.CAVALRY))
        assertEquals(1.25f, GameSimulationEngine.phalanxMultiplier(PhalanxStance.CAVALRY_CHARGE, TroopType.INFANTRY))
        assertEquals(0.85f, GameSimulationEngine.phalanxMultiplier(PhalanxStance.INFANTRY_VANGUARD, TroopType.CAVALRY))
        assertEquals(1.30f, GameSimulationEngine.phalanxMultiplier(PhalanxStance.SIEGE_BREACHER, TroopType.INFANTRY, isBoss = true))
    }

    @Test
    fun fusionForgeMergesAcrossAllTwelveRaritiesToTranscendent() {
        assertEquals(21, SkillType.entries.size)
        assertEquals(15, BuildingType.entries.size)
        assertEquals(12, GearRarity.entries.size)

        val vib1 = GameCatalog.createEquipment("vibranium_spear", GearRarity.SOVEREIGN)
        val vib2 = GameCatalog.createEquipment("vibranium_spear", GearRarity.SOVEREIGN)
        val vib3 = GameCatalog.createEquipment("vibranium_spear", GearRarity.SOVEREIGN)
        val state = GameState(vaultItems = listOf(vib1, vib2, vib3))

        val (after, msg) = GameSimulationEngine.fuseThreeItems(state, "vibranium_spear", GearRarity.SOVEREIGN)
        assertEquals(1, after.vaultItems.size)
        val forged = after.vaultItems.first()
        assertEquals(GearRarity.TRANSCENDENT, forged.rarity)
        assertEquals(MaterialAge.VIBRANIUM, forged.age)
        assertTrue(forged.attackBonus > vib1.attackBonus)
        assertTrue(msg.contains("Transcendent"))
        assertTrue("${forged.templateId}:${forged.rarity.name}" in after.discoveredGearIds)
    }

    @Test
    fun offlineSimulationRespects12hAnd24hCapsAndTimedBlessings() {
        val nowMs = 1_700_000_000_000L
        val thirtyHoursAgo = nowMs - (30L * 3600L * 1000L)
        val squads = listOf(
            SquadAssignment(0, SquadTaskType.RAID_REGION, "village_outskirts", "Village Outskirts"),
            SquadAssignment(1, SquadTaskType.ARTISAN_SKILL, SkillType.WOODCUTTING.name, "Woodcutting")
        ) + List(3) { SquadAssignment(it + 2) }

        val standardState = GameState(
            buildings = mapOf(BuildingType.CITADEL.name to 3),
            squads = squads,
            supporterPackOwned = false,
            lastSavedTimestampMs = thirtyHoursAgo
        )
        val standardSim = GameSimulationEngine.simulateOfflineProgress(standardState, nowMs, LocalDate.of(2026, 11, 25), Random(42))
        val standardReport = requireNotNull(standardSim.report)
        assertEquals(12, standardReport.cappedHours)
        assertEquals(720L, standardReport.elapsedMinutes)
        assertTrue(standardReport.goldEarned > 0)
        assertTrue(standardSim.state.wood > standardState.wood)

        val blessedState = standardState.copy(
            activeBlessingExpiries = mapOf(
                RoyalBlessingType.OFFLINE_FORTUNE.name to (thirtyHoursAgo + 12L * 3600L * 1000L),
                RoyalBlessingType.GATHERING_BOUNTY.name to (thirtyHoursAgo + 12L * 3600L * 1000L)
            )
        )
        val blessedSim = GameSimulationEngine.simulateOfflineProgress(blessedState, nowMs, LocalDate.of(2026, 11, 25), Random(42))
        val blessedReport = requireNotNull(blessedSim.report)
        assertTrue(blessedReport.goldEarned > standardReport.goldEarned)
        assertTrue(blessedSim.state.wood > standardSim.state.wood)
        // Expired blessing should be pruned once offline window finishes past expiry
        assertFalse(blessedSim.state.isBlessingActive(RoyalBlessingType.OFFLINE_FORTUNE, nowMs))

        val supporterState = standardState.copy(supporterPackOwned = true)
        val supporterSim = GameSimulationEngine.simulateOfflineProgress(supporterState, nowMs, LocalDate.of(2026, 11, 25), Random(42))
        val supporterReport = requireNotNull(supporterSim.report)
        assertEquals(24, supporterReport.cappedHours)
        assertEquals(1440L, supporterReport.elapsedMinutes)
        assertTrue(supporterReport.goldEarned > standardReport.goldEarned)
    }

    @Test
    fun royalBlessingsEnforceDailyLimitCooldownAndCrownAlternative() {
        val today = LocalDate.of(2026, 11, 25)
        val t0 = 1_700_000_000_000L
        val initial = GameState(royalCrowns = 30, dailyAdLimit = 2, adCooldownSeconds = 60, adsDateKey = today.toString())

        val (afterAd1, msg1) = GameSimulationEngine.grantRoyalBlessingFromAd(initial, RoyalBlessingType.SKILL_XP_SURGE, t0, today)
        assertTrue(afterAd1.isBlessingActive(RoyalBlessingType.SKILL_XP_SURGE, t0))
        assertEquals(1, afterAd1.adsWatchedToday)
        assertTrue(msg1.contains("Scholar's Blessing"))

        // Immediate second ad blocked by cooldown
        val (cooldownBlocked, msgCooldown) = GameSimulationEngine.grantRoyalBlessingFromAd(afterAd1, RoyalBlessingType.GATHERING_BOUNTY, t0 + 10_000L, today)
        assertEquals(1, cooldownBlocked.adsWatchedToday)
        assertTrue(msgCooldown.contains("cooldown"))

        // After cooldown expires, second ad succeeds
        val (afterAd2, _) = GameSimulationEngine.grantRoyalBlessingFromAd(afterAd1, RoyalBlessingType.ROYAL_CROWN_CACHE, t0 + 65_000L, today)
        assertEquals(2, afterAd2.adsWatchedToday)
        assertEquals(45, afterAd2.royalCrowns)

        // Third ad blocked by daily limit
        val (limitBlocked, msgLimit) = GameSimulationEngine.grantRoyalBlessingFromAd(afterAd2, RoyalBlessingType.GATHERING_BOUNTY, t0 + 130_000L, today)
        assertEquals(2, limitBlocked.adsWatchedToday)
        assertTrue(msgLimit.contains("Daily optional ad limit"))

        // Ordinary gameplay Crown alternative works regardless of ad limit
        val (crownBlessed, _) = GameSimulationEngine.activateRoyalBlessingWithCrowns(limitBlocked, RoyalBlessingType.GATHERING_BOUNTY, t0 + 130_000L, today)
        assertTrue(crownBlessed.isBlessingActive(RoyalBlessingType.GATHERING_BOUNTY, t0 + 130_000L))
        assertEquals(33, crownBlessed.royalCrowns)
    }

    @Test
    fun citadelLevelsUnlockAllFiveSquadSlots() {
        assertEquals(1, GameState(buildings = mapOf(BuildingType.CITADEL.name to 1)).unlockedSquadSlots())
        assertEquals(2, GameState(buildings = mapOf(BuildingType.CITADEL.name to 3)).unlockedSquadSlots())
        assertEquals(3, GameState(buildings = mapOf(BuildingType.CITADEL.name to 6)).unlockedSquadSlots())
        assertEquals(4, GameState(buildings = mapOf(BuildingType.CITADEL.name to 10)).unlockedSquadSlots())
        assertEquals(5, GameState(buildings = mapOf(BuildingType.CITADEL.name to 15)).unlockedSquadSlots())
    }

    @Test
    fun divinityCookingSmithingAndInstantUpgradesWorkImmediately() {
        val base = GameState(
            gold = 500L,
            wood = 50L,
            stone = 50L,
            ore = 10L,
            rawFish = 3L,
            ironBars = 10L,
            equipped = mapOf(
                EquipSlot.WEAPON.name to GameCatalog.createEquipment("wood_club", GearRarity.COMMON)
            )
        )

        // Instant Cooking
        val (afterCook, cookMsg) = GameSimulationEngine.performInstantSkillAction(base, SkillType.COOKING)
        assertEquals(2L, afterCook.rawFish)
        assertTrue(afterCook.warRations > base.warRations)
        assertTrue(cookMsg.contains("Cooked"))

        // Instant Smithing
        val (afterSmith, smithMsg) = GameSimulationEngine.performInstantSkillAction(afterCook, SkillType.SMITHING)
        assertEquals(8L, afterSmith.ore)
        assertTrue(afterSmith.ironBars > afterCook.ironBars)
        assertTrue(smithMsg.contains("Smithed"))

        // Instant Divinity & Prayer Toggle
        val (afterPray, prayMsg) = GameSimulationEngine.performInstantSkillAction(afterSmith, SkillType.DIVINITY)
        assertTrue(afterPray.devotion > afterSmith.devotion)
        assertTrue(prayMsg.contains("Devotion"))

        val (withSmite, smiteMsg) = GameSimulationEngine.toggleDivinePrayer(afterPray, DivinePrayer.DIVINE_SMITE)
        assertEquals(DivinePrayer.DIVINE_SMITE, withSmite.currentPrayer())
        assertTrue(smiteMsg.contains("invoked"))

        // Instant Gear Rarity Upgrade
        val (upgradedGearState, upgMsg) = GameSimulationEngine.instantUpgradeEquippedRarity(withSmite, EquipSlot.WEAPON)
        assertEquals(GearRarity.UNCOMMON, upgradedGearState.equipped[EquipSlot.WEAPON.name]?.rarity)
        assertTrue(upgMsg.contains("Uncommon"))

        // Instant Building Upgrade
        val (upgradedCitadel, bldgMsg) = GameSimulationEngine.upgradeBuilding(upgradedGearState, BuildingType.CITADEL)
        assertEquals(2, upgradedCitadel.buildingLevel(BuildingType.CITADEL))
        assertEquals(MaterialAge.STONE, upgradedCitadel.currentMaterialAge())
        assertTrue(bldgMsg.orEmpty().contains("Lv.2"))
    }

    @Test
    fun skillMasteryProvidesPermanentUnitStatBonuses() {
        val base = GameState()
        val baseAtk = base.attackPower()
        val baseHp = base.maxHp()
        val baseDef = base.defensePower()
        val baseMight = base.effectiveArmyMight()

        // Training Smithing permanently increases Unit ATK & Army Might
        val withSmithing = GameSimulationEngine.grantSkillXp(base, SkillType.SMITHING, 8000L)
        assertTrue(withSmithing.skillProgress(SkillType.SMITHING).level >= 10)
        assertTrue(withSmithing.unitMasteryBonuses().unitAttackPct > 0)
        assertTrue(withSmithing.attackPower() > baseAtk)
        assertTrue(withSmithing.effectiveArmyMight() > baseMight)

        // Training Cooking permanently increases Unit HP & Army Might
        val withCooking = GameSimulationEngine.grantSkillXp(withSmithing, SkillType.COOKING, 8000L)
        assertTrue(withCooking.skillProgress(SkillType.COOKING).level >= 10)
        assertTrue(withCooking.unitMasteryBonuses().unitHpPct > 0)
        assertTrue(withCooking.maxHp() > baseHp)
        assertTrue(withCooking.effectiveArmyMight() > withSmithing.effectiveArmyMight())

        // Training Divinity permanently increases Unit DEF & Army Might
        val withDivinity = GameSimulationEngine.grantSkillXp(withCooking, SkillType.DIVINITY, 8000L)
        assertTrue(withDivinity.skillProgress(SkillType.DIVINITY).level >= 10)
        assertTrue(withDivinity.unitMasteryBonuses().unitDefensePct > 0)
        assertTrue(withDivinity.defensePower() > baseDef)
        assertTrue(withDivinity.effectiveArmyMight() > withCooking.effectiveArmyMight())
    }

    @Test
    fun freshVillageStartsFromNothingAndFollowsMinecraftProgression() {
        var state = GameState.freshVillage(Random(1))
        assertEquals(0L, state.gold)
        assertEquals(0L, state.wood)
        assertEquals(0L, state.stone)
        assertEquals(0L, state.ore)
        assertEquals(0, state.totalCookedFoodCount())
        assertEquals(0, state.axeTier)
        assertEquals(0, state.pickaxeTier)
        assertEquals(0, state.fishingRodTier)

        // Punch trees with Bare Hands to gather enough Wood for first tools
        repeat(6) {
            state = GameSimulationEngine.performInstantSkillAction(state, SkillType.WOODCUTTING, random = Random(it)).first
        }
        assertTrue(state.wood >= 4L)

        // Craft Wood Pickaxe (costs 4 Wood)
        val (withPick, pickMsg) = GameSimulationEngine.craftOrUpgradeTool(state, ToolType.PICKAXE)
        assertEquals(1, withPick.pickaxeTier)
        assertTrue(pickMsg.contains("Wood"))

        // Mine Stone, Coal & Raw Ore Veins using Wood Pickaxe
        var minedState = withPick
        repeat(5) {
            minedState = GameSimulationEngine.performInstantSkillAction(minedState, SkillType.MINING, random = Random(it + 10)).first
        }
        assertTrue(minedState.stone > 0L)
        assertTrue(minedState.ore >= 2L)

        // Smelt raw ore in Furnace with Coal/Wood fuel into Iron Ingots
        val withFuel = minedState.copy(wood = minedState.wood + 2L)
        val (smelted, smeltMsg) = GameSimulationEngine.smeltOre(withFuel, FurnaceRecipe.SMELT_IRON)
        assertEquals(1L, smelted.ironBars)
        assertTrue(smeltMsg.contains("Iron Ingot"))
    }

    @Test
    fun dungeonFloorOneDropsChickenEggBeefLeatherMuttonWoolAndUnlocksFloorTwo() {
        var state = GameState.freshVillage(Random(2)).copy(warlordLevel = 5)
        assertEquals(1, state.highestDungeonFloor)

        repeat(6) { idx ->
            state = GameSimulationEngine.attackDungeonFloor(state, 1, random = Random(idx + 100)).first
        }
        assertTrue((state.dungeonFloorKills["1"] ?: 0) >= 5)
        assertTrue(state.highestDungeonFloor >= 2)
        assertTrue(state.rawChicken > 0L)
        assertTrue(state.rawBeef > 0L)
        assertTrue(state.leather > 0L)
        assertTrue(state.rawMutton > 0L)
        assertTrue(state.wool > 0L)
    }

    @Test
    fun multipleFoodCookingEatingAndRandomFishingDropsWork() {
        var state = GameState.freshVillage(Random(3)).copy(
            wood = 10L,
            rawChicken = 2L,
            eggs = 2L,
            rawBeef = 1L,
            rawMutton = 1L,
            rawFish = 2L,
            herbs = 2L,
            currentHp = 10
        )

        state = GameSimulationEngine.cookFoodRecipe(state, FoodType.ROAST_CHICKEN).first
        state = GameSimulationEngine.cookFoodRecipe(state, FoodType.CAMPFIRE_EGGS).first
        state = GameSimulationEngine.cookFoodRecipe(state, FoodType.SEARED_STEAK).first
        state = GameSimulationEngine.cookFoodRecipe(state, FoodType.ROAST_MUTTON).first
        state = GameSimulationEngine.cookFoodRecipe(state, FoodType.WARLORD_STEW).first

        assertEquals(1, state.cookedFoods[FoodType.ROAST_CHICKEN.name])
        assertEquals(1, state.cookedFoods[FoodType.CAMPFIRE_EGGS.name])
        assertEquals(1, state.cookedFoods[FoodType.SEARED_STEAK.name])
        assertEquals(1, state.cookedFoods[FoodType.ROAST_MUTTON.name])
        assertEquals(1, state.cookedFoods[FoodType.WARLORD_STEW.name])

        val (afterEat, eatMsg) = GameSimulationEngine.eatCookedFood(state, FoodType.SEARED_STEAK)
        assertEquals(0, afterEat.cookedFoods[FoodType.SEARED_STEAK.name] ?: 0)
        assertTrue(afterEat.currentHp > 10)
        assertTrue(eatMsg.contains("Seared Beef Steak"))

        // Fishing grants Fish + random drops (wood / leather / gold / dust)
        var fishState = GameState.freshVillage(Random(4)).copy(fishingRodTier = 2)
        repeat(25) { idx ->
            fishState = GameSimulationEngine.performInstantSkillAction(fishState, SkillType.FISHING, random = Random(idx + 50)).first
        }
        assertTrue(fishState.rawFish > 0L)
        assertTrue(fishState.wood > 0L || fishState.leather > 0L || fishState.gold > 0L || fishState.diamonds > 0L)
    }
}
