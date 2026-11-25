package com.example.model

import com.squareup.moshi.JsonClass
import kotlin.random.Random

enum class BottomTab(val label: String) {
    FRONTIER("Frontier"),
    STRONGHOLD("Stronghold"),
    ARTISAN("Artisan"),
    WARLORD("Warlord")
}

enum class MaterialAge(
    val displayName: String,
    val reqCitadelLevel: Int,
    val tier: Int,
    val summary: String
) {
    WOOD("Wood Age", 1, 1, "Timber clubs, bark shields & village campfires"),
    STONE("Stone Age", 2, 2, "Quarried slate tools, cobble helms & palisades"),
    IRON("Iron Age", 3, 3, "Smelted iron ingots, broadswords & chainmail"),
    GOLD("Gold Age", 5, 4, "Gilded sovereign blades & royal signets"),
    DIAMOND("Diamond Age", 8, 5, "Crystalline edge weapons & prism armor"),
    OBSIDIAN("Obsidian Age", 11, 6, "Volcanic forged steel & bastion warplate"),
    VIBRANIUM("Vibranium Age", 15, 7, "Starfall vibranium weave & mythic sovereignty");

    fun next(): MaterialAge? = entries.firstOrNull { it.tier == tier + 1 }
}

enum class SkillCategory(val displayName: String) {
    GATHERING("Gathering"),
    PRODUCTION("Production"),
    COMBAT("Combat"),
    SPECIALIZATION("Specialization")
}

enum class SkillType(
    val displayName: String,
    val category: SkillCategory,
    val maxLevel: Int = 150,
    val cycleMs: Long = 2500L
) {
    // Gathering (6)
    WOODCUTTING("Woodcutting", SkillCategory.GATHERING),
    MINING("Mining", SkillCategory.GATHERING),
    FISHING("Fishing", SkillCategory.GATHERING),
    FARMING("Farming", SkillCategory.GATHERING),
    HUNTING("Hunting", SkillCategory.GATHERING),
    FORAGING("Foraging", SkillCategory.GATHERING),

    // Production (6)
    COOKING("Cooking", SkillCategory.PRODUCTION),
    SMITHING("Smithing", SkillCategory.PRODUCTION),
    CRAFTING("Crafting", SkillCategory.PRODUCTION),
    ALCHEMY("Alchemy", SkillCategory.PRODUCTION),
    TAILORING("Tailoring", SkillCategory.PRODUCTION),
    CONSTRUCTION("Construction", SkillCategory.PRODUCTION),

    // Combat (6)
    ATTACK("Attack", SkillCategory.COMBAT),
    STRENGTH("Strength", SkillCategory.COMBAT),
    DEFENCE("Defence", SkillCategory.COMBAT),
    ARCHERY("Archery", SkillCategory.COMBAT),
    SORCERY("Sorcery", SkillCategory.COMBAT),
    VITALITY("Vitality", SkillCategory.COMBAT),

    // Specialization (3)
    DIVINITY("Divinity", SkillCategory.SPECIALIZATION),
    THIEVING("Thieving", SkillCategory.SPECIALIZATION),
    ENCHANTING("Enchanting", SkillCategory.SPECIALIZATION);

    companion object {
        fun assignableArtisanSkills(): List<SkillType> =
            entries.filter { it.category != SkillCategory.COMBAT }
    }
}

enum class BuildingCategory(val displayName: String) {
    CORE("Core"),
    ECONOMY("Economy"),
    MASTERY("Mastery")
}

enum class BuildingType(
    val displayName: String,
    val category: BuildingCategory,
    val perkSummary: String,
    val maxLevel: Int = 25
) {
    CITADEL("Citadel", BuildingCategory.CORE, "Unlocks Squad Slots (Lv.3,6,10,15) & Material Ages"),
    BARRACKS("Barracks", BuildingCategory.CORE, "Unlocks Tier 2 (Lv.4) & Tier 3 (Lv.9) troops"),
    INFIRMARY("Infirmary", BuildingCategory.CORE, "Boosts wounded bed capacity & Max HP"),
    BLACKSMITH("Blacksmith", BuildingCategory.ECONOMY, "+5% Smithing/Crafting double-craft chance per Lv"),
    LUMBER_MILL("Lumber Mill", BuildingCategory.ECONOMY, "+12% Woodcutting yield per Lv"),
    QUARRY("Quarry", BuildingCategory.ECONOMY, "+12% Mining yield & gem/vibranium chance per Lv"),
    FARMSTEAD("Farmstead", BuildingCategory.ECONOMY, "+12% Farming/Cooking yield per Lv"),
    TREASURY("Treasury", BuildingCategory.ECONOMY, "+6% Gold income & item sell value per Lv"),
    ACADEMY("Academy", BuildingCategory.MASTERY, "+5% global Skill XP per Lv"),
    ROYAL_LIBRARY("Royal Library", BuildingCategory.MASTERY, "+4% Research & Enchanting potency per Lv"),
    HUNTERS_LODGE("Hunter's Lodge", BuildingCategory.MASTERY, "+4% Conquest Mastery & Boss damage per Lv"),
    COLLECTION_HALL("Collection Hall", BuildingCategory.MASTERY, "Boosts Collection Hall discovery bonuses (+3%/Lv)"),
    BEAST_SANCTUARY("Beast Sanctuary", BuildingCategory.MASTERY, "Unlocks & empowers Companions (+6% pet power/Lv)"),
    OBSERVATORY("Observatory", BuildingCategory.MASTERY, "+5% Seasonal, Sorcery & Event points per Lv"),
    DIVINITY_SHRINE("Sanctum Shrine", BuildingCategory.MASTERY, "+8% Divinity Devotion & Prayer Potency per Lv")
}

enum class TroopType(val displayName: String) {
    INFANTRY("Infantry"),
    RANGED("Ranged"),
    CAVALRY("Cavalry"),
    SIEGE("Siege")
}

enum class PhalanxStance(val displayName: String, val troopType: TroopType, val summary: String) {
    INFANTRY_VANGUARD("Infantry Phalanx", TroopType.INFANTRY, "Trains Attack/Defence • +25% vs Ranged"),
    RANGED_VOLLEY("Ranged Volley", TroopType.RANGED, "Trains Archery • +25% vs Cavalry"),
    CAVALRY_CHARGE("Cavalry Wedge", TroopType.CAVALRY, "Trains Strength • +25% vs Infantry"),
    SIEGE_BREACHER("Siege Sorcery", TroopType.SIEGE, "Trains Sorcery/Vitality • +30% vs Bosses")
}

enum class EquipSlot(val displayName: String) {
    WEAPON("Weapon"),
    HELM("Helm"),
    ARMOR("Armor"),
    SHIELD("Shield"),
    SIGNET_RING("Signet Ring")
}

enum class GearRarity(val displayName: String, val rank: Int) {
    COMMON("Common", 0),
    UNCOMMON("Uncommon", 1),
    RARE("Rare", 2),
    EPIC("Epic", 3),
    LEGENDARY("Legendary", 4),
    MYTHIC("Mythic", 5),
    ANCIENT("Ancient", 6),
    DIVINE("Divine", 7),
    CELESTIAL("Celestial", 8),
    ETERNAL("Eternal", 9),
    SOVEREIGN("Sovereign", 10),
    TRANSCENDENT("Transcendent", 11);

    fun next(): GearRarity? = entries.firstOrNull { it.rank == rank + 1 }
}

enum class HolidayId(val displayName: String) {
    SHADOWFALL("Shadowfall"),
    WINTERTIDE("Wintertide"),
    LUNAR_DRAGON("Lunar Dragon Festival"),
    MIDSUMMER_SUNFORGE("Midsummer Sun-Forge")
}

enum class SoloEventType(val displayName: String, val subtitle: String) {
    ARMS_RACE("Arms Race", "Train troops & fuse gear for chests"),
    CITADEL_EXPANSION("Citadel Expansion", "Upgrade buildings & complete artisan tasks"),
    MONSTER_HUNT("Monster Hunt", "Slay frontier enemies & bosses")
}

enum class RealmSurgeType(val displayName: String, val bonusSummary: String) {
    MINING_SURGE("Veins of Obsidian", "+50% Mining & Ore Yield"),
    COMBAT_LOOT_SURGE("Spoils of Conquest", "+40% Combat Loot & Gold"),
    ARTISAN_SPEED_SURGE("Masterwork Tempo", "+35% Artisan Skilling Yield & XP"),
    ARMY_MIGHT_SURGE("Warlord's Muster", "+25% Combat Damage & Mastery XP")
}

enum class SquadTaskType {
    IDLE,
    RAID_REGION,
    DUNGEON_FLOOR,
    ARTISAN_SKILL,
    SEASONAL_NODE,
    SEASONAL_BOSS,
    APEX_BOSS,
    WEEKLY_REALM_BOSS
}

enum class ContractType {
    SLAY_FOES,
    COMPLETE_SKILLS,
    FUSE_OR_CRAFT
}

enum class DivinePrayer(
    val displayName: String,
    val bonusSummary: String,
    val reqDivinityLevel: Int,
    val devotionCostPerAction: Long
) {
    DIVINE_SMITE("Divine Smite", "+20% Combat Damage & +10% Gold", 1, 1L),
    IRON_WARD("Sanctuary Ward", "+25% Defense & -15% Damage Taken", 3, 1L),
    HARVEST_HYMN("Harvest Hymn", "+25% Gathering & Production Yield", 6, 1L),
    ASCENDANT_LITANY("Ascendant Litany", "+30% Skill & Warlord XP", 10, 2L)
}

enum class MasteryRank(val label: String, val minLevel: Int, val milestoneBonusPct: Int) {
    INITIATE("Initiate", 1, 0),
    APPRENTICE("Apprentice", 10, 5),
    ARTISAN("Artisan", 25, 12),
    MASTER("Master", 50, 22),
    GRANDMASTER("Grandmaster", 100, 35),
    SOVEREIGN("Sovereign", 150, 50);

    companion object {
        fun forLevel(level: Int): MasteryRank =
            entries.lastOrNull { level >= it.minLevel } ?: INITIATE
    }
}

data class SkillMasteryDetail(
    val skill: SkillType,
    val level: Int,
    val rank: MasteryRank,
    val bonusPct: Int,
    val bonusLabel: String
)

data class UnitMasteryBonuses(
    val smithingBonusPct: Int,
    val cookingBonusPct: Int,
    val divinityBonusPct: Int,
    val unitAttackPct: Int,
    val unitHpPct: Int,
    val unitDefensePct: Int,
    val unitMightPct: Int
)

enum class FoodType(
    val displayName: String,
    val healHp: Int,
    val rationsYield: Int,
    val xpReward: Long,
    val ingredientSummary: String
) {
    ROAST_CHICKEN("Roast Chicken", 35, 2, 16L, "1 Raw Chicken + 1 Wood"),
    CAMPFIRE_EGGS("Campfire Eggs", 25, 1, 12L, "2 Eggs"),
    SEARED_STEAK("Seared Beef Steak", 60, 3, 24L, "1 Raw Beef + 1 Wood"),
    ROAST_MUTTON("Herb Roast Mutton", 50, 3, 22L, "1 Raw Mutton + 1 Herb"),
    GRILLED_FISH("Grilled River Fish", 35, 2, 16L, "1 Raw Fish + 1 Wood"),
    HEARTH_BREAD("Hearth Bread", 30, 2, 14L, "2 Grain"),
    WARLORD_STEW("Hearty Warlord Stew", 105, 5, 36L, "1 Chicken + 1 Fish + 1 Herb")
}

enum class ToolType(val displayName: String) {
    AXE("Woodcutting Axe"),
    PICKAXE("Mining Pickaxe"),
    FISHING_ROD("Fishing Rod");

    companion object {
        fun tierLabel(tier: Int): String = when (tier) {
            0 -> "Bare Hands"
            1 -> "Wood"
            2 -> "Stone"
            3 -> "Iron"
            4 -> "Gold"
            5 -> "Diamond"
            6 -> "Obsidian"
            else -> "Vibranium"
        }
    }
}

enum class FurnaceRecipe(
    val displayName: String,
    val outputName: String,
    val reqPickaxeTier: Int,
    val xpReward: Long
) {
    SMELT_IRON("Smelt Iron Vein", "Iron Ingot", 0, 22L),
    SMELT_GOLD("Smelt Gold Vein", "Gold Ingot", 3, 32L),
    SMELT_VIBRANIUM("Smelt Vibranium Vein", "Vibranium Ingot", 6, 55L)
}

enum class RoyalBlessingType(
    val displayName: String,
    val benefitSummary: String,
    val durationMs: Long,
    val crownAlternativeCost: Int
) {
    SKILL_XP_SURGE(
        displayName = "Scholar's Blessing",
        benefitSummary = "+35% Skill & Warlord XP for 30 min",
        durationMs = 30L * 60_000L,
        crownAlternativeCost = 12
    ),
    GATHERING_BOUNTY(
        displayName = "Harvest Blessing",
        benefitSummary = "+40% Gathering Yields for 30 min",
        durationMs = 30L * 60_000L,
        crownAlternativeCost = 12
    ),
    COMBAT_LOOT_LUCK(
        displayName = "Plunder Blessing",
        benefitSummary = "+35% Combat Gold & Drop Chance for 30 min",
        durationMs = 30L * 60_000L,
        crownAlternativeCost = 12
    ),
    OFFLINE_FORTUNE(
        displayName = "Sentinel's Blessing",
        benefitSummary = "+35% Offline Sim Gold & Yield Bonus for 12h",
        durationMs = 12L * 3600_000L,
        crownAlternativeCost = 15
    ),
    BUILD_TRAINING_HASTE(
        displayName = "Architect & Drill Boon",
        benefitSummary = "-25% Instant Upgrade & Troop Cost, +25% Batch & +25% Speed (30m)",
        durationMs = 30L * 60_000L,
        crownAlternativeCost = 14
    ),
    INSTANT_SQUAD_SURGE(
        displayName = "Warhorn Surge",
        benefitSummary = "Instant 15 min parallel Squad progression",
        durationMs = 0L,
        crownAlternativeCost = 15
    ),
    ROYAL_CROWN_CACHE(
        displayName = "Crown Tribute",
        benefitSummary = "+15 Royal Crowns immediately (Also earned via Contracts)",
        durationMs = 0L,
        crownAlternativeCost = 0
    ),
    APEX_BOSS_ATTEMPT(
        displayName = "Apex Siege Writ",
        benefitSummary = "+1 Bonus Daily Apex Boss Attempt",
        durationMs = 0L,
        crownAlternativeCost = 20
    )
}

@JsonClass(generateAdapter = true)
data class RoyalContract(
    val id: String,
    val title: String,
    val type: ContractType,
    val target: Int,
    val progress: Int = 0,
    val rewardGold: Long,
    val rewardCrowns: Int,
    val claimed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class EquipmentItem(
    val id: String,
    val templateId: String,
    val name: String,
    val slot: EquipSlot,
    val rarity: GearRarity,
    val age: MaterialAge = MaterialAge.WOOD,
    val attackBonus: Int,
    val defenseBonus: Int,
    val hpBonus: Int,
    val lootBonusPct: Int = 0,
    val enchantLevel: Int = 0,
    val isSeasonal: Boolean = false,
    val sellGold: Int = 15
)

@JsonClass(generateAdapter = true)
data class SquadAssignment(
    val slotIndex: Int,
    val taskType: SquadTaskType = SquadTaskType.IDLE,
    val targetId: String = "",
    val targetLabel: String = "Unassigned",
    val progressMs: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SkillProgress(
    val level: Int = 1,
    val xp: Long = 0L
)

@JsonClass(generateAdapter = true)
data class TroopRoster(
    val t1Infantry: Int = 1,
    val t2Infantry: Int = 0,
    val t3Infantry: Int = 0,
    val t1Ranged: Int = 0,
    val t2Ranged: Int = 0,
    val t3Ranged: Int = 0,
    val t1Cavalry: Int = 0,
    val t2Cavalry: Int = 0,
    val t3Cavalry: Int = 0,
    val t1Siege: Int = 0,
    val t2Siege: Int = 0,
    val t3Siege: Int = 0
) {
    fun totalTroops(): Int =
        t1Infantry + t2Infantry + t3Infantry +
            t1Ranged + t2Ranged + t3Ranged +
            t1Cavalry + t2Cavalry + t3Cavalry +
            t1Siege + t2Siege + t3Siege

    fun totalArmyMight(): Long =
        (t1Infantry * 10L + t2Infantry * 28L + t3Infantry * 70L) +
            (t1Ranged * 11L + t2Ranged * 30L + t3Ranged * 74L) +
            (t1Cavalry * 13L + t2Cavalry * 34L + t3Cavalry * 82L) +
            (t1Siege * 18L + t2Siege * 46L + t3Siege * 110L)
}

@JsonClass(generateAdapter = true)
data class GameState(
    val villageName: String = "Bramblewick",
    val warlordLevel: Int = 1,
    val warlordXp: Long = 0L,
    val warlordTitle: String = "Village Wanderer",
    val currentHp: Int = 55,
    val autoEatThresholdPct: Int = 40,
    val gold: Long = 15L,
    val royalCrowns: Int = 10,
    val chronicleTokens: Int = 1,
    val activeRegionId: String = "village_outskirts",
    val activePhalanx: PhalanxStance = PhalanxStance.INFANTRY_VANGUARD,
    val squads: List<SquadAssignment> = List(5) { SquadAssignment(slotIndex = it) },
    val skills: Map<String, SkillProgress> = SkillType.entries.associate { it.name to SkillProgress(1, 0L) },
    val buildings: Map<String, Int> = BuildingType.entries.associate { it.name to 1 },
    val troops: TroopRoster = TroopRoster(),
    val woundedTroops: Int = 0,
    // Expanded Multi-Skill, Animal Food & Furnace Resources
    val wood: Long = 10L,
    val stone: Long = 0L,
    val coal: Long = 0L,
    val ore: Long = 0L,
    val goldOre: Long = 0L,
    val obsidianShards: Long = 0L,
    val vibraniumOre: Long = 0L,
    val rawFish: Long = 2L,
    val rawChicken: Long = 0L,
    val eggs: Long = 0L,
    val rawBeef: Long = 0L,
    val rawMutton: Long = 0L,
    val feathers: Long = 0L,
    val leather: Long = 0L,
    val wool: Long = 0L,
    val herbs: Long = 2L,
    val grain: Long = 2L,
    val hides: Long = 0L,
    val cloth: Long = 0L,
    val elixirs: Int = 0,
    val runeDust: Long = 0L,
    val warRations: Int = 4,
    val cookedFoods: Map<String, Int> = emptyMap(),
    val ironBars: Long = 0L,
    val goldBars: Long = 0L,
    val diamonds: Long = 0L,
    val vibranium: Long = 0L,
    val devotion: Long = 10L,
    val seasonalEssence: Long = 0L,
    // Gathering Tools (0 = Bare Hands .. 7 = Vibranium)
    val axeTier: Int = 0,
    val pickaxeTier: Int = 0,
    val fishingRodTier: Int = 0,
    // Dungeon Progression
    val currentDungeonFloor: Int = 1,
    val highestDungeonFloor: Int = 1,
    val dungeonFloorKills: Map<String, Int> = emptyMap(),
    val currentDungeonEnemyName: String = "Pasture Hen",
    val currentDungeonEnemyHp: Int = 10,
    val currentDungeonEnemyMaxHp: Int = 10,
    // Divinity, Companions & Collection Hall
    val activePrayerId: String? = null,
    val unlockedCompanions: List<String> = emptyList(),
    val activeCompanionId: String? = null,
    val discoveredGearIds: List<String> = emptyList(),
    // Combat & Mastery
    val regionKills: Map<String, Int> = emptyMap(),
    val equipped: Map<String, EquipmentItem> = emptyMap(),
    val vaultItems: List<EquipmentItem> = emptyList(),
    // Events & Contracts
    val soloEventWindowId: Long = 0L,
    val soloEventPoints: Long = 0L,
    val soloEventClaimedTiers: List<Int> = emptyList(),
    val contractsDateKey: String = "",
    val dailyContracts: List<RoyalContract> = emptyList(),
    val apexDateKey: String = "",
    val apexAttemptsUsed: Int = 0,
    val apexBonusAttempts: Int = 0,
    val apexKillsToday: Int = 0,
    val weeklyKey: String = "",
    val weeklyBossHpRemaining: Long = 5000L,
    val weeklyBossMaxHp: Long = 5000L,
    val weeklyBossDefeated: Boolean = false,
    val tournamentWinsThisWeek: Int = 0,
    val summonedHolidayId: String? = null,
    val summonedBossExpiresAtMs: Long = 0L,
    // Entitlements & Optional Ad Blessings
    val supporterPackOwned: Boolean = false,
    val vaultExpansionOwned: Boolean = false,
    val royalObsidianTitleUnlocked: Boolean = false,
    val autoSalvageJunkEnabled: Boolean = false,
    val activeBlessingExpiries: Map<String, Long> = emptyMap(),
    val adsDateKey: String = "",
    val adsWatchedToday: Int = 0,
    val dailyAdLimit: Int = 6,
    val lastAdWatchedAtMs: Long = 0L,
    val adCooldownSeconds: Int = 90,
    val hideRewardedAdOffers: Boolean = false,
    // Active Enemy
    val currentEnemyHp: Int = 24,
    val currentEnemyMaxHp: Int = 24,
    val currentEnemyName: String = "Wild Timber Wolf",
    val lastSavedTimestampMs: Long = 0L
) {
    companion object {
        private val startingVillages = listOf(
            "Bramblewick",
            "Oakhaven",
            "Alderford",
            "Thornbrook",
            "Mudhollow",
            "Cindervale",
            "Mossbridge",
            "Reedwatch"
        )

        fun freshVillage(random: Random = Random.Default): GameState {
            val chosenVillage = startingVillages.random(random)
            return GameState(
                villageName = chosenVillage,
                gold = 0L,
                wood = 0L,
                stone = 0L,
                coal = 0L,
                ore = 0L,
                rawFish = 0L,
                herbs = 0L,
                grain = 0L,
                warRations = 0,
                devotion = 0L,
                axeTier = 0,
                pickaxeTier = 0,
                fishingRodTier = 0,
                lastSavedTimestampMs = System.currentTimeMillis()
            )
        }
    }

    fun totalCookedFoodCount(): Int = cookedFoods.values.sum() + warRations

    fun toolTier(tool: ToolType): Int = when (tool) {
        ToolType.AXE -> axeTier
        ToolType.PICKAXE -> pickaxeTier
        ToolType.FISHING_ROD -> fishingRodTier
    }

    fun currentMaterialAge(): MaterialAge {
        val citadel = buildingLevel(BuildingType.CITADEL)
        return MaterialAge.entries.lastOrNull { citadel >= it.reqCitadelLevel } ?: MaterialAge.WOOD
    }

    fun settlementStageName(): String {
        val citadel = buildingLevel(BuildingType.CITADEL)
        return when {
            citadel >= 15 -> "$villageName Empire"
            citadel >= 10 -> "$villageName Citadel"
            citadel >= 6 -> "$villageName Fortress"
            citadel >= 3 -> "$villageName Town"
            citadel >= 2 -> "$villageName Palisade"
            else -> "$villageName Hamlet"
        }
    }

    fun buildingLevel(type: BuildingType): Int = buildings[type.name] ?: 1

    fun skillProgress(type: SkillType): SkillProgress = skills[type.name] ?: SkillProgress()

    fun unlockedSquadSlots(): Int {
        val citadel = buildingLevel(BuildingType.CITADEL)
        var slots = 1
        if (citadel >= 3) slots++
        if (citadel >= 6) slots++
        if (citadel >= 10) slots++
        if (citadel >= 15) slots++
        return slots
    }

    fun maxVaultCapacity(): Int = 80 + if (vaultExpansionOwned) 100 else 0

    fun collectionHallBonusPct(): Int {
        val hallLvl = buildingLevel(BuildingType.COLLECTION_HALL)
        return discoveredGearIds.size * (2 + hallLvl)
    }

    private fun coreMasteryPct(level: Int): Int {
        val rankBonus = MasteryRank.forLevel(level).milestoneBonusPct
        return ((level - 1).coerceAtLeast(0) * 2) + rankBonus
    }

    fun skillMasteryDetail(skill: SkillType): SkillMasteryDetail? {
        val lvl = skillProgress(skill).level
        val rank = MasteryRank.forLevel(lvl)
        return when (skill) {
            SkillType.SMITHING -> SkillMasteryDetail(skill, lvl, rank, coreMasteryPct(lvl), "+${coreMasteryPct(lvl)}% Unit ATK")
            SkillType.COOKING -> SkillMasteryDetail(skill, lvl, rank, coreMasteryPct(lvl), "+${coreMasteryPct(lvl)}% Unit HP")
            SkillType.DIVINITY -> SkillMasteryDetail(skill, lvl, rank, coreMasteryPct(lvl), "+${coreMasteryPct(lvl)}% Unit DEF")
            SkillType.ALCHEMY -> {
                val pct = (lvl - 1).coerceAtLeast(0)
                SkillMasteryDetail(skill, lvl, rank, pct, "+$pct% Unit Stamina")
            }
            SkillType.TAILORING -> {
                val pct = (lvl - 1).coerceAtLeast(0)
                SkillMasteryDetail(skill, lvl, rank, pct, "+$pct% Unit Ward")
            }
            SkillType.ENCHANTING -> {
                val pct = (lvl - 1).coerceAtLeast(0)
                SkillMasteryDetail(skill, lvl, rank, pct, "+$pct% Unit Spell ATK")
            }
            else -> null
        }
    }

    fun unitMasteryBonuses(): UnitMasteryBonuses {
        val smithLvl = skillProgress(SkillType.SMITHING).level
        val cookLvl = skillProgress(SkillType.COOKING).level
        val divLvl = skillProgress(SkillType.DIVINITY).level
        val smithPct = coreMasteryPct(smithLvl)
        val cookPct = coreMasteryPct(cookLvl)
        val divPct = coreMasteryPct(divLvl)

        val alchPct = (skillProgress(SkillType.ALCHEMY).level - 1).coerceAtLeast(0)
        val tailPct = (skillProgress(SkillType.TAILORING).level - 1).coerceAtLeast(0)
        val enchPct = (skillProgress(SkillType.ENCHANTING).level - 1).coerceAtLeast(0)

        val unitAtk = smithPct + enchPct
        val unitHp = cookPct + alchPct
        val unitDef = divPct + tailPct
        val mightPct = (smithPct + cookPct + divPct + ((alchPct + tailPct + enchPct) / 2)) / 2
        return UnitMasteryBonuses(
            smithingBonusPct = smithPct,
            cookingBonusPct = cookPct,
            divinityBonusPct = divPct,
            unitAttackPct = unitAtk,
            unitHpPct = unitHp,
            unitDefensePct = unitDef,
            unitMightPct = mightPct
        )
    }

    fun effectiveArmyMight(): Long {
        val rawMight = troops.totalArmyMight()
        val mastery = unitMasteryBonuses()
        return kotlin.math.ceil(rawMight * (1.0 + mastery.unitMightPct / 100.0)).toLong()
    }

    fun maxHp(): Int {
        val gearHp = equipped.values.sumOf { it.hpBonus }
        val vitalityLvl = skillProgress(SkillType.VITALITY).level
        val infBonus = (buildingLevel(BuildingType.INFIRMARY) - 1) * 14
        val citadelBonus = (buildingLevel(BuildingType.CITADEL) - 1) * 16
        val baseHp = 40 + (warlordLevel * 12) + (vitalityLvl * 8) + citadelBonus + infBonus + gearHp
        val hpMult = 1.0 + (unitMasteryBonuses().unitHpPct / 100.0)
        return kotlin.math.ceil(baseHp * hpMult).toInt()
    }

    fun attackPower(): Int {
        val gearAtk = equipped.values.sumOf { it.attackBonus }
        val atkLvl = skillProgress(SkillType.ATTACK).level
        val strLvl = skillProgress(SkillType.STRENGTH).level
        val arcLvl = skillProgress(SkillType.ARCHERY).level
        val sorLvl = skillProgress(SkillType.SORCERY).level
        val combatAvg = (atkLvl + strLvl + arcLvl + sorLvl) / 2
        val troopPower = (effectiveArmyMight() / 10L).toInt().coerceAtLeast(1)
        val collectionMult = 1.0 + (collectionHallBonusPct() / 200.0)
        val masteryAtkMult = 1.0 + (unitMasteryBonuses().unitAttackPct / 100.0)
        return kotlin.math.ceil((4 + (warlordLevel * 2) + combatAvg + gearAtk + troopPower) * collectionMult * masteryAtkMult).toInt()
    }

    fun defensePower(): Int {
        val gearDef = equipped.values.sumOf { it.defenseBonus }
        val defLvl = skillProgress(SkillType.DEFENCE).level
        val citadelDef = (buildingLevel(BuildingType.CITADEL) - 1) * 2
        val baseDef = 1 + defLvl + gearDef + citadelDef
        val masteryDefMult = 1.0 + (unitMasteryBonuses().unitDefensePct / 100.0)
        return kotlin.math.ceil(baseDef * masteryDefMult).toInt()
    }

    fun gearLootBonusPct(): Int = equipped.values.sumOf { it.lootBonusPct }

    fun currentPrayer(): DivinePrayer? =
        activePrayerId?.let { id -> DivinePrayer.entries.firstOrNull { it.name == id } }

    fun isBlessingActive(type: RoyalBlessingType, nowMs: Long = System.currentTimeMillis()): Boolean =
        (activeBlessingExpiries[type.name] ?: 0L) > nowMs

    fun blessingRemainingMs(type: RoyalBlessingType, nowMs: Long = System.currentTimeMillis()): Long =
        ((activeBlessingExpiries[type.name] ?: 0L) - nowMs).coerceAtLeast(0L)

    fun remainingDailyAds(): Int = (dailyAdLimit - adsWatchedToday).coerceAtLeast(0)

    fun adCooldownRemainingMs(nowMs: Long = System.currentTimeMillis()): Long =
        if (lastAdWatchedAtMs <= 0L) 0L
        else ((lastAdWatchedAtMs + adCooldownSeconds * 1000L) - nowMs).coerceAtLeast(0L)
}

data class OfflineReport(
    val elapsedMinutes: Long,
    val cappedHours: Int,
    val goldEarned: Long,
    val warlordXpGained: Long,
    val skillXpGained: Long,
    val enemiesSlain: Int,
    val rationsConsumed: Int,
    val resourcesSummary: List<String>,
    val itemsFound: List<EquipmentItem>
)

data class CombatLogEntry(
    val id: Long,
    val text: String,
    val isAlert: Boolean = false,
    val isLoot: Boolean = false
)
