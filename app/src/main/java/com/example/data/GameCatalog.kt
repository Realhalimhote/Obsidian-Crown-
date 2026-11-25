package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.model.*
import com.example.ui.theme.*
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.pow

data class RegionDef(
    val id: String,
    val name: String,
    val subtitle: String,
    val reqWarlordLevel: Int,
    val reqCitadelLevel: Int,
    val recommendedMight: Long,
    val enemyName: String,
    val enemyTroopType: TroopType,
    val enemyHp: Int,
    val enemyAttack: Int,
    val goldReward: Long,
    val xpReward: Long,
    val dropPoolIds: List<String>
)

data class ApexBossDef(
    val id: String,
    val name: String,
    val title: String,
    val reqCitadelLevel: Int,
    val hp: Int,
    val attack: Int,
    val goldReward: Long,
    val xpReward: Long,
    val crownBonus: Int,
    val dropPoolIds: List<String>
)

data class CompanionDef(
    val id: String,
    val name: String,
    val role: String,
    val bonusText: String,
    val reqSanctuaryLevel: Int,
    val goldCost: Long,
    val skillXpBonusPct: Int = 0,
    val goldBonusPct: Int = 0,
    val lootBonusPct: Int = 0,
    val damageBonusPct: Int = 0
)

data class TournamentOpponentDef(
    val rank: Int,
    val name: String,
    val title: String,
    val powerRating: Int,
    val rewardGold: Long,
    val rewardCrowns: Int
)

data class SeasonalNodeDef(
    val id: String,
    val name: String,
    val holidayId: HolidayId,
    val yieldSummary: String,
    val xpReward: Long
)

data class HolidayDef(
    val id: HolidayId,
    val name: String,
    val dateWindowLabel: String,
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val bossName: String,
    val bossSubtitle: String,
    val bossHp: Int,
    val bossAttack: Int,
    val bossGoldReward: Long,
    val nodes: List<SeasonalNodeDef>,
    val exclusiveDropIds: List<String>
)

data class GearTemplate(
    val templateId: String,
    val name: String,
    val slot: EquipSlot,
    val age: MaterialAge,
    val baseAtk: Int,
    val baseDef: Int,
    val baseHp: Int,
    val baseLootPct: Int = 0,
    val isSeasonal: Boolean = false
)

data class SkillNodeDef(
    val skill: SkillType,
    val actionTitle: String,
    val yieldDescription: String,
    val baseXp: Long
)

data class AgeCraftCost(
    val gold: Long,
    val wood: Long = 0L,
    val stone: Long = 0L,
    val bars: Long = 0L,
    val diamonds: Long = 0L,
    val vibranium: Long = 0L
)

data class DungeonMonsterDef(
    val id: String,
    val name: String,
    val troopType: TroopType,
    val hp: Int,
    val attack: Int,
    val goldReward: Long,
    val xpReward: Long,
    val dropsSummary: String
)

data class DungeonFloorDef(
    val floor: Int,
    val name: String,
    val subtitle: String,
    val clearsToUnlockNext: Int,
    val monsters: List<DungeonMonsterDef>,
    val dropGearPool: List<String>
)

data class ToolCraftCost(
    val wood: Long = 0L,
    val stone: Long = 0L,
    val ironBars: Long = 0L,
    val goldBars: Long = 0L,
    val diamonds: Long = 0L,
    val obsidian: Long = 0L,
    val vibraniumBars: Long = 0L
)

object GameCatalog {

    val squadUnlockCitadelLevels = listOf(1, 3, 6, 10, 15)

    val skillNodes = listOf(
        // Gathering (6)
        SkillNodeDef(SkillType.WOODCUTTING, "Punch Trees & Chop Timber", "Logs for Wood Tools, Base & Fuel", 14L),
        SkillNodeDef(SkillType.MINING, "Mine Stone & Ore Veins", "Stone, Coal, Iron, Gold, Diamond & Vibranium", 16L),
        SkillNodeDef(SkillType.FISHING, "Cast Fishing Line", "Raw Fish + random Leather, Gold, Gems & Chests", 14L),
        SkillNodeDef(SkillType.FARMING, "Cultivate Grain Plots", "+3–5 Grain & Herbs per 2.5s", 15L),
        SkillNodeDef(SkillType.HUNTING, "Track Woodland Game", "Hides, Leather, Raw Chicken & Mutton", 16L),
        SkillNodeDef(SkillType.FORAGING, "Gather Wild Herbs", "+3–5 Herbs & Rune Dust per 2.5s", 15L),
        // Production (6)
        SkillNodeDef(SkillType.COOKING, "Campfire & Hearth Meals", "Cook Chicken, Eggs, Beef, Mutton, Fish & Bread", 18L),
        SkillNodeDef(SkillType.SMITHING, "Furnace Smelt & Smith", "Smelt Ore Veins -> Ingots + Smith Gear", 22L),
        SkillNodeDef(SkillType.CRAFTING, "Artisan Tool & Leathercraft", "Wood/Leather/Wool -> Gear & Gold", 20L),
        SkillNodeDef(SkillType.ALCHEMY, "Brew War Elixirs", "3 Herbs -> 1 Elixir (+Combat Buff)", 22L),
        SkillNodeDef(SkillType.TAILORING, "Weave Wool & Leather", "Leather/Wool/Hides -> Cloth & Armor", 20L),
        SkillNodeDef(SkillType.CONSTRUCTION, "Mill Timber & Masonry", "Wood + Stone -> Gold & Citadel XP", 22L),
        // Specialization (3)
        SkillNodeDef(SkillType.DIVINITY, "Sanctum Vigil & Offering", "Devotion points for Divine Smite & Prayers", 22L),
        SkillNodeDef(SkillType.THIEVING, "Scout & Pilfer Camps", "+Gold & Rare Gem/Token chance", 24L),
        SkillNodeDef(SkillType.ENCHANTING, "Refine Rune Dust", "2 Herbs + Stone -> Rune Dust + Loot", 26L)
    )

    val companions: List<CompanionDef> = listOf(
        CompanionDef("ashwood_fox", "Ashwood Fox", "Scout Familiar", "+10% Skill XP & +5% Loot", 1, 80L, skillXpBonusPct = 10, lootBonusPct = 5),
        CompanionDef("cobble_golem", "Cobble Golem", "Quarry Guardian", "+15% Mining & +10% Gold", 2, 240L, goldBonusPct = 10, skillXpBonusPct = 10),
        CompanionDef("ironforge_hawk", "Ironforge Hawk", "Warband Spotter", "+12% Combat Dmg & +10% Loot", 3, 600L, damageBonusPct = 12, lootBonusPct = 10),
        CompanionDef("gilded_drake", "Gilded Drake", "Treasury Wyrmling", "+25% Gold & +15% Skill XP", 5, 1600L, goldBonusPct = 25, skillXpBonusPct = 15),
        CompanionDef("diamond_stag", "Diamond Stag", "Prismatic Spirit", "+20% Combat Dmg & +20% Loot", 7, 4200L, damageBonusPct = 20, lootBonusPct = 20),
        CompanionDef("obsidian_shadowcat", "Obsidian Shadowcat", "Nether Stalker", "+30% Combat Dmg & +25% Skill XP", 10, 9500L, damageBonusPct = 30, skillXpBonusPct = 25),
        CompanionDef("vibranium_phoenix", "Vibranium Phoenix", "Sovereign Mythic", "+40% All Yields, Dmg & Loot", 13, 22000L, skillXpBonusPct = 40, goldBonusPct = 40, lootBonusPct = 30, damageBonusPct = 40)
    )

    val tournamentOpponents: List<TournamentOpponentDef> = listOf(
        TournamentOpponentDef(1, "Aldric the Squire", "Hamlet Challenger", 25, 120L, 2),
        TournamentOpponentDef(2, "Ser Vane of Ironcrag", "Valley Knight", 140, 320L, 4),
        TournamentOpponentDef(3, "Warchief Korvak", "Cinder Wastes Champion", 520, 750L, 6),
        TournamentOpponentDef(4, "Grand Magister Elyra", "Diamond Citadel Duelist", 1600, 1650L, 10),
        TournamentOpponentDef(5, "Lord Kaelen Vibran", "Obsidian Throne Sovereign", 4200, 3500L, 15)
    )

    val gearTemplates: Map<String, GearTemplate> = listOf(
        // Wood Age (Citadel Lv.1)
        GearTemplate("wood_club", "Oak War Club", EquipSlot.WEAPON, MaterialAge.WOOD, baseAtk = 4, baseDef = 0, baseHp = 6),
        GearTemplate("wood_buckler", "Barkwood Buckler", EquipSlot.SHIELD, MaterialAge.WOOD, baseAtk = 0, baseDef = 3, baseHp = 12),
        GearTemplate("wood_tunic", "Forester Leather Tunic", EquipSlot.ARMOR, MaterialAge.WOOD, baseAtk = 0, baseDef = 4, baseHp = 16),

        // Stone Age (Citadel Lv.2)
        GearTemplate("stone_axe", "Flint Battleaxe", EquipSlot.WEAPON, MaterialAge.STONE, baseAtk = 7, baseDef = 1, baseHp = 10),
        GearTemplate("stone_helm", "Cobble Bascinet", EquipSlot.HELM, MaterialAge.STONE, baseAtk = 1, baseDef = 5, baseHp = 18),
        GearTemplate("stone_shield", "Slate Kite Shield", EquipSlot.SHIELD, MaterialAge.STONE, baseAtk = 1, baseDef = 6, baseHp = 22),

        // Iron Age (Citadel Lv.3)
        GearTemplate("iron_broadsword", "Iron Broadsword", EquipSlot.WEAPON, MaterialAge.IRON, baseAtk = 12, baseDef = 2, baseHp = 14),
        GearTemplate("iron_plate", "Ironcrag Brigandine", EquipSlot.ARMOR, MaterialAge.IRON, baseAtk = 2, baseDef = 10, baseHp = 40),
        GearTemplate("iron_helm", "Iron Vanguard Helm", EquipSlot.HELM, MaterialAge.IRON, baseAtk = 2, baseDef = 8, baseHp = 28),

        // Gold Age (Citadel Lv.5)
        GearTemplate("gold_claymore", "Gilded Claymore", EquipSlot.WEAPON, MaterialAge.GOLD, baseAtk = 18, baseDef = 3, baseHp = 20, baseLootPct = 5),
        GearTemplate("gold_signet", "Sovereign Gold Signet", EquipSlot.SIGNET_RING, MaterialAge.GOLD, baseAtk = 6, baseDef = 5, baseHp = 22, baseLootPct = 8),
        GearTemplate("gold_cuirass", "Royal Gilded Cuirass", EquipSlot.ARMOR, MaterialAge.GOLD, baseAtk = 4, baseDef = 15, baseHp = 60, baseLootPct = 4),

        // Diamond Age (Citadel Lv.8)
        GearTemplate("diamond_blade", "Diamond Edge Greatsword", EquipSlot.WEAPON, MaterialAge.DIAMOND, baseAtk = 27, baseDef = 4, baseHp = 30),
        GearTemplate("diamond_aegis", "Prism Diamond Aegis", EquipSlot.SHIELD, MaterialAge.DIAMOND, baseAtk = 4, baseDef = 20, baseHp = 75),
        GearTemplate("diamond_armor", "Diamond Bastion Plate", EquipSlot.ARMOR, MaterialAge.DIAMOND, baseAtk = 6, baseDef = 24, baseHp = 95),

        // Obsidian Age (Citadel Lv.11)
        GearTemplate("obsidian_greatsword", "Obsidian Netherblade", EquipSlot.WEAPON, MaterialAge.OBSIDIAN, baseAtk = 38, baseDef = 6, baseHp = 42),
        GearTemplate("obsidian_helm", "Obsidian Crown Helm", EquipSlot.HELM, MaterialAge.OBSIDIAN, baseAtk = 8, baseDef = 22, baseHp = 80),
        GearTemplate("obsidian_mail", "Netherforged Warplate", EquipSlot.ARMOR, MaterialAge.OBSIDIAN, baseAtk = 9, baseDef = 32, baseHp = 135),

        // Vibranium Age (Citadel Lv.15)
        GearTemplate("vibranium_spear", "Vibranium Sovereign Spear", EquipSlot.WEAPON, MaterialAge.VIBRANIUM, baseAtk = 55, baseDef = 10, baseHp = 65, baseLootPct = 10),
        GearTemplate("vibranium_weave", "Vibranium Kinetic Weave", EquipSlot.ARMOR, MaterialAge.VIBRANIUM, baseAtk = 14, baseDef = 45, baseHp = 200, baseLootPct = 6),
        GearTemplate("vibranium_aegis", "Vibranium Star-Shield", EquipSlot.SHIELD, MaterialAge.VIBRANIUM, baseAtk = 10, baseDef = 40, baseHp = 160),
        GearTemplate("vibranium_ring", "Heart-Shaped Vibranium Band", EquipSlot.SIGNET_RING, MaterialAge.VIBRANIUM, baseAtk = 18, baseDef = 16, baseHp = 85, baseLootPct = 14),

        // Seasonal Holiday Relics
        GearTemplate("reaper_scythe", "Reaper Scythe", EquipSlot.WEAPON, MaterialAge.IRON, baseAtk = 22, baseDef = 2, baseHp = 25, baseLootPct = 5, isSeasonal = true),
        GearTemplate("pumpkin_lantern_ring", "Pumpkin Lantern Ring", EquipSlot.SIGNET_RING, MaterialAge.IRON, baseAtk = 8, baseDef = 5, baseHp = 32, baseLootPct = 10, isSeasonal = true),
        GearTemplate("frostfire_armor", "Frostfire Armor", EquipSlot.ARMOR, MaterialAge.GOLD, baseAtk = 6, baseDef = 20, baseHp = 85, isSeasonal = true),
        GearTemplate("starlight_amulet", "Starlight Amulet", EquipSlot.SIGNET_RING, MaterialAge.GOLD, baseAtk = 9, baseDef = 7, baseHp = 35, baseLootPct = 10, isSeasonal = true),
        GearTemplate("dragonfang_blade", "Dragonfang Blade", EquipSlot.WEAPON, MaterialAge.DIAMOND, baseAtk = 28, baseDef = 4, baseHp = 32, baseLootPct = 6, isSeasonal = true),
        GearTemplate("imperial_jade_helm", "Imperial Jade Helm", EquipSlot.HELM, MaterialAge.DIAMOND, baseAtk = 7, baseDef = 16, baseHp = 60, isSeasonal = true),
        GearTemplate("sunforged_aegis", "Sunforged Aegis", EquipSlot.SHIELD, MaterialAge.OBSIDIAN, baseAtk = 7, baseDef = 22, baseHp = 75, isSeasonal = true),
        GearTemplate("solstice_crown", "Solstice Crown", EquipSlot.HELM, MaterialAge.OBSIDIAN, baseAtk = 10, baseDef = 15, baseHp = 60, baseLootPct = 8, isSeasonal = true)
    ).associateBy { it.templateId }

    fun ageCraftCost(age: MaterialAge): AgeCraftCost = when (age) {
        MaterialAge.WOOD -> AgeCraftCost(gold = 10L, wood = 10L)
        MaterialAge.STONE -> AgeCraftCost(gold = 25L, wood = 8L, stone = 12L)
        MaterialAge.IRON -> AgeCraftCost(gold = 60L, bars = 4L)
        MaterialAge.GOLD -> AgeCraftCost(gold = 140L, bars = 8L)
        MaterialAge.DIAMOND -> AgeCraftCost(gold = 260L, bars = 10L, diamonds = 4L)
        MaterialAge.OBSIDIAN -> AgeCraftCost(gold = 480L, bars = 16L, diamonds = 6L)
        MaterialAge.VIBRANIUM -> AgeCraftCost(gold = 900L, bars = 20L, vibranium = 5L)
    }

    fun templatesForAge(age: MaterialAge): List<GearTemplate> =
        gearTemplates.values.filter { !it.isSeasonal && it.age == age }

    val regions: List<RegionDef> = listOf(
        RegionDef("village_outskirts", "Village Outskirts", "Wild beasts near your hamlet", 1, 1, 10L, "Wild Timber Wolf", TroopType.CAVALRY, 24, 4, 8L, 12L, listOf("wood_club", "wood_buckler", "wood_tunic")),
        RegionDef("ashwood_border", "Ashwood Border", "Stone Age timberlands & bow raiders", 3, 2, 95L, "Ashwood Bow-Raider", TroopType.RANGED, 60, 9, 18L, 24L, listOf("stone_axe", "stone_helm", "stone_shield")),
        RegionDef("thornwall_caves", "Thornwall Caves", "Subterranean quarry goblins", 5, 3, 240L, "Slate Tunnel Stalker", TroopType.INFANTRY, 105, 14, 30L, 38L, listOf("stone_axe", "iron_helm", "iron_broadsword")),
        RegionDef("ironcrag_valley", "Ironcrag Valley", "Iron Age outriders guarding ore passes", 7, 4, 460L, "Ironcrag Lancers", TroopType.CAVALRY, 165, 20, 48L, 56L, listOf("iron_broadsword", "iron_plate", "gold_signet")),
        RegionDef("gilded_steppes", "Gilded Steppes", "Mercenary companies of the Gold Age", 10, 5, 900L, "Gilded Freeblade", TroopType.INFANTRY, 245, 27, 74L, 84L, listOf("gold_claymore", "gold_cuirass", "gold_signet")),
        RegionDef("cinder_wastes", "Cinder Wastes", "Molten iron bastions & ashen legions", 14, 7, 1650L, "Cinder Phalanx Guard", TroopType.INFANTRY, 370, 38, 115L, 125L, listOf("gold_claymore", "diamond_blade", "diamond_aegis")),
        RegionDef("prism_hollow", "Prism Hollow", "Diamond Age crystalline caverns", 18, 9, 2800L, "Prism Construct", TroopType.SIEGE, 540, 52, 175L, 185L, listOf("diamond_blade", "diamond_armor", "diamond_aegis")),
        RegionDef("frostwatch_peaks", "Frostwatch Peaks", "Highland marksmen atop frozen bastions", 22, 11, 4500L, "Frostwatch Crossbowmen", TroopType.RANGED, 780, 68, 250L, 260L, listOf("obsidian_greatsword", "obsidian_helm", "diamond_armor")),
        RegionDef("obsidian_throne", "Obsidian Throne", "Volcanic nether-forged imperial guard", 28, 13, 7800L, "Obsidian Siege Vanguard", TroopType.SIEGE, 1250, 95, 420L, 420L, listOf("obsidian_greatsword", "obsidian_mail", "vibranium_aegis")),
        RegionDef("vibranium_spire", "Vibranium Spire", "Starfall sanctuary of mythic sovereigns", 35, 15, 14000L, "Starfall Vibranium Guard", TroopType.CAVALRY, 2100, 135, 760L, 740L, listOf("vibranium_spear", "vibranium_weave", "vibranium_aegis", "vibranium_ring"))
    )

    val dungeonFloors: List<DungeonFloorDef> = listOf(
        DungeonFloorDef(
            floor = 1,
            name = "Floor 1: Village Pasture",
            subtitle = "Chicken, Cow & Sheep — ideal for food, leather & wool",
            clearsToUnlockNext = 5,
            monsters = listOf(
                DungeonMonsterDef("pasture_hen", "Pasture Hen", TroopType.RANGED, 10, 1, 3L, 8L, "Raw Chicken, Egg, Feathers"),
                DungeonMonsterDef("grazing_cow", "Grazing Cow", TroopType.INFANTRY, 18, 2, 5L, 12L, "Raw Beef, Leather"),
                DungeonMonsterDef("woolly_sheep", "Woolly Sheep", TroopType.CAVALRY, 14, 1, 4L, 10L, "Raw Mutton, Wool")
            ),
            dropGearPool = listOf("wood_club", "wood_tunic")
        ),
        DungeonFloorDef(
            floor = 2,
            name = "Floor 2: Stone Cavern",
            subtitle = "Wild Boars, Bats & Quarry Slimes",
            clearsToUnlockNext = 8,
            monsters = listOf(
                DungeonMonsterDef("cave_boar", "Cave Boar", TroopType.CAVALRY, 42, 6, 12L, 20L, "Raw Beef, Leather, Stone"),
                DungeonMonsterDef("quarry_slime", "Quarry Slime", TroopType.INFANTRY, 52, 8, 15L, 24L, "Coal, Iron Ore, Stone Gear")
            ),
            dropGearPool = listOf("stone_axe", "stone_helm", "stone_shield")
        ),
        DungeonFloorDef(
            floor = 3,
            name = "Floor 3: Iron Mine Shafts",
            subtitle = "Tunnel Kobolds & Iron Sentinels",
            clearsToUnlockNext = 12,
            monsters = listOf(
                DungeonMonsterDef("tunnel_kobold", "Tunnel Kobold", TroopType.RANGED, 95, 13, 28L, 38L, "Iron Ore, Coal, Gold"),
                DungeonMonsterDef("iron_sentinel", "Iron Sentinel", TroopType.INFANTRY, 130, 17, 36L, 48L, "Iron Ore, Iron Ingots, Iron Gear")
            ),
            dropGearPool = listOf("iron_broadsword", "iron_plate", "iron_helm")
        ),
        DungeonFloorDef(
            floor = 4,
            name = "Floor 4: Gilded Crypt",
            subtitle = "Crypt Raiders & Gilded Constructs",
            clearsToUnlockNext = 15,
            monsters = listOf(
                DungeonMonsterDef("crypt_skeleton", "Crypt Archer", TroopType.RANGED, 210, 24, 65L, 76L, "Gold Ore, Rune Dust, Devotion"),
                DungeonMonsterDef("gilded_mimic", "Gilded Mimic", TroopType.SIEGE, 260, 29, 85L, 95L, "Gold Ore, Gold Gear")
            ),
            dropGearPool = listOf("gold_claymore", "gold_signet", "gold_cuirass")
        ),
        DungeonFloorDef(
            floor = 5,
            name = "Floor 5: Crystal Abyss",
            subtitle = "Prism Golems & Diamond Stalkers",
            clearsToUnlockNext = 20,
            monsters = listOf(
                DungeonMonsterDef("crystal_stalker", "Crystal Stalker", TroopType.CAVALRY, 460, 46, 150L, 170L, "Diamonds, Rune Dust, Diamond Gear"),
                DungeonMonsterDef("prism_golem", "Prism Golem", TroopType.SIEGE, 580, 56, 195L, 210L, "Diamonds, Gold Ore")
            ),
            dropGearPool = listOf("diamond_blade", "diamond_aegis", "diamond_armor")
        ),
        DungeonFloorDef(
            floor = 6,
            name = "Floor 6: Obsidian Bastion",
            subtitle = "Volcanic Nether Guards & Magma Imps",
            clearsToUnlockNext = 25,
            monsters = listOf(
                DungeonMonsterDef("magma_imp", "Magma Imp", TroopType.RANGED, 890, 74, 290L, 310L, "Obsidian, Coal, Diamonds"),
                DungeonMonsterDef("nether_knight", "Nether Knight", TroopType.INFANTRY, 1180, 92, 380L, 390L, "Obsidian, Obsidian Gear")
            ),
            dropGearPool = listOf("obsidian_greatsword", "obsidian_helm", "obsidian_mail")
        ),
        DungeonFloorDef(
            floor = 7,
            name = "Floor 7: Vibranium Core",
            subtitle = "Starfall Guardians & Vibranium Titans",
            clearsToUnlockNext = 999,
            monsters = listOf(
                DungeonMonsterDef("starfall_sentinel", "Starfall Sentinel", TroopType.SIEGE, 1950, 128, 680L, 690L, "Vibranium Ore, Vibranium Gear")
            ),
            dropGearPool = listOf("vibranium_spear", "vibranium_weave", "vibranium_aegis", "vibranium_ring")
        )
    )

    fun dungeonFloor(floor: Int): DungeonFloorDef =
        dungeonFloors.firstOrNull { it.floor == floor } ?: dungeonFloors.first()

    fun toolCraftCost(targetTier: Int): ToolCraftCost = when (targetTier) {
        1 -> ToolCraftCost(wood = 4L)
        2 -> ToolCraftCost(wood = 3L, stone = 6L)
        3 -> ToolCraftCost(wood = 3L, ironBars = 4L)
        4 -> ToolCraftCost(wood = 4L, goldBars = 4L)
        5 -> ToolCraftCost(wood = 4L, diamonds = 4L)
        6 -> ToolCraftCost(diamonds = 4L, obsidian = 6L)
        else -> ToolCraftCost(obsidian = 6L, vibraniumBars = 4L)
    }

    val apexBosses: List<ApexBossDef> = listOf(
        ApexBossDef("apex_bastion_warden", "Ironclad Bastion Warden", "Tier I Daily Apex Siege", 3, 420, 28, 350L, 180L, 3, listOf("iron_broadsword", "iron_plate", "gold_signet")),
        ApexBossDef("apex_cinder_wyrm", "Cinderhorn Siege Wyrm", "Tier II Daily Apex Siege", 8, 1250, 65, 950L, 460L, 5, listOf("diamond_blade", "diamond_armor", "obsidian_helm")),
        ApexBossDef("apex_obsidian_titan", "Vibranium Dread-Titan", "Tier III Daily Apex Siege", 15, 3200, 120, 2400L, 1100L, 8, listOf("vibranium_spear", "vibranium_weave", "vibranium_aegis"))
    )

    val holidays: List<HolidayDef> = listOf(
        HolidayDef(
            id = HolidayId.SHADOWFALL,
            name = "Shadowfall",
            dateWindowLabel = "Oct 15 – Nov 2",
            startMonth = 10,
            startDay = 15,
            endMonth = 11,
            endDay = 2,
            primaryAccent = ShadowfallOrange,
            secondaryAccent = ShadowfallPurple,
            bossName = "Headless Dreadlord",
            bossSubtitle = "Spectral World Boss • Drops Reaper Scythe & Pumpkin Lantern Ring",
            bossHp = 650,
            bossAttack = 36,
            bossGoldReward = 420L,
            nodes = listOf(
                SeasonalNodeDef("shadowfall_pumpkin", "Harvest Cursed Pumpkins", HolidayId.SHADOWFALL, "+3 Rations & Shadow Essence", 24L),
                SeasonalNodeDef("shadowfall_wood", "Fell Shadowwood Groves", HolidayId.SHADOWFALL, "+5 Wood & Shadow Essence", 24L)
            ),
            exclusiveDropIds = listOf("reaper_scythe", "pumpkin_lantern_ring")
        ),
        HolidayDef(
            id = HolidayId.WINTERTIDE,
            name = "Wintertide",
            dateWindowLabel = "Dec 10 – Jan 5",
            startMonth = 12,
            startDay = 10,
            endMonth = 1,
            endDay = 5,
            primaryAccent = WintertideCyan,
            secondaryAccent = WintertideGold,
            bossName = "Krampus, The Frost Tyrant",
            bossSubtitle = "Yule Gift Caravan Escort • Drops Frostfire Armor & Starlight Amulet",
            bossHp = 720,
            bossAttack = 38,
            bossGoldReward = 480L,
            nodes = listOf(
                SeasonalNodeDef("wintertide_frostpine", "Chop Frostpine Timber", HolidayId.WINTERTIDE, "+5 Wood & Yule Essence", 25L),
                SeasonalNodeDef("wintertide_glacial", "Mine Glacial Ore", HolidayId.WINTERTIDE, "+4 Ore & Yule Essence", 26L)
            ),
            exclusiveDropIds = listOf("frostfire_armor", "starlight_amulet")
        ),
        HolidayDef(
            id = HolidayId.LUNAR_DRAGON,
            name = "Lunar Dragon Festival",
            dateWindowLabel = "Feb 1 – Feb 20",
            startMonth = 2,
            startDay = 1,
            endMonth = 2,
            endDay = 20,
            primaryAccent = LunarCrimson,
            secondaryAccent = LunarGold,
            bossName = "Jade Dragon Sovereign",
            bossSubtitle = "2x Resource Surge • Drops Dragonfang Blade & Imperial Jade Helm",
            bossHp = 780,
            bossAttack = 40,
            bossGoldReward = 520L,
            nodes = listOf(
                SeasonalNodeDef("lunar_bamboo", "Cut Crimson Bamboo", HolidayId.LUNAR_DRAGON, "+6 Wood & Dragon Essence", 26L),
                SeasonalNodeDef("lunar_jade", "Excavate Jade Vein", HolidayId.LUNAR_DRAGON, "+5 Ore & Dragon Essence", 28L)
            ),
            exclusiveDropIds = listOf("dragonfang_blade", "imperial_jade_helm")
        ),
        HolidayDef(
            id = HolidayId.MIDSUMMER_SUNFORGE,
            name = "Midsummer Sun-Forge",
            dateWindowLabel = "Jul 1 – Jul 21",
            startMonth = 7,
            startDay = 1,
            endMonth = 7,
            endDay = 21,
            primaryAccent = MidsummerAmber,
            secondaryAccent = MidsummerGold,
            bossName = "Solar Colossal Forgemaster",
            bossSubtitle = "2x Forge & Loot Surge • Drops Sunforged Aegis & Solstice Crown",
            bossHp = 820,
            bossAttack = 42,
            bossGoldReward = 560L,
            nodes = listOf(
                SeasonalNodeDef("midsummer_ironwood", "Harvest Sunlit Ironwood", HolidayId.MIDSUMMER_SUNFORGE, "+6 Wood & Solar Essence", 26L),
                SeasonalNodeDef("midsummer_ignis", "Mine Ignis Ore", HolidayId.MIDSUMMER_SUNFORGE, "+5 Ore & Solar Essence", 28L)
            ),
            exclusiveDropIds = listOf("sunforged_aegis", "solstice_crown")
        )
    )

    fun holidayById(id: HolidayId): HolidayDef = holidays.first { it.id == id }

    fun regionById(id: String): RegionDef = regions.firstOrNull { it.id == id } ?: regions.first()

    fun xpRequiredForLevel(level: Int): Long = (60.0 * level.toDouble().pow(1.42)).toLong()

    fun buildingGoldCost(type: BuildingType, currentLevel: Int): Long {
        val base = when (type) {
            BuildingType.CITADEL -> 40L
            BuildingType.BARRACKS -> 35L
            BuildingType.INFIRMARY -> 30L
            BuildingType.LUMBER_MILL, BuildingType.QUARRY, BuildingType.FARMSTEAD -> 32L
            BuildingType.BLACKSMITH, BuildingType.TREASURY -> 45L
            BuildingType.DIVINITY_SHRINE -> 40L
            else -> 55L
        }
        return (base * 1.48.pow(currentLevel - 1)).toLong()
    }

    fun buildingWoodCost(type: BuildingType, currentLevel: Int): Long {
        val base = when (type) {
            BuildingType.CITADEL -> 15L
            BuildingType.LUMBER_MILL -> 12L
            else -> 10L
        }
        return (base * 1.38.pow(currentLevel - 1)).toLong()
    }

    fun buildingStoneCost(type: BuildingType, currentLevel: Int): Long {
        if (currentLevel < 2) return 0L
        val base = if (type == BuildingType.CITADEL || type == BuildingType.QUARRY) 10L else 6L
        return (base * 1.34.pow(currentLevel - 2)).toLong()
    }

    fun buildingBarCost(type: BuildingType, currentLevel: Int): Long {
        return if (currentLevel < 3) 0L else ((currentLevel - 2) * 3L + if (type == BuildingType.CITADEL) 2L else 0L)
    }

    fun createEquipment(templateId: String, rarity: GearRarity): EquipmentItem {
        val t = gearTemplates[templateId] ?: gearTemplates.values.first()
        val mult = 1.25.pow(rarity.rank.toDouble())
        val atk = ceil(t.baseAtk * mult).toInt()
        val def = ceil(t.baseDef * mult).toInt()
        val hp = ceil(t.baseHp * mult).toInt()
        val loot = if (t.baseLootPct > 0) ceil(t.baseLootPct * (1.0 + rarity.rank * 0.25)).toInt() else 0
        val sell = (12 * (rarity.rank + 1) * t.age.tier * (if (t.isSeasonal) 2 else 1))
        return EquipmentItem(
            id = UUID.randomUUID().toString(),
            templateId = t.templateId,
            name = t.name,
            slot = t.slot,
            rarity = rarity,
            age = t.age,
            attackBonus = atk,
            defenseBonus = def,
            hpBonus = hp,
            lootBonusPct = loot,
            isSeasonal = t.isSeasonal,
            sellGold = sell
        )
    }

    fun upgradeEquipment(base: EquipmentItem): EquipmentItem? {
        val nextRarity = base.rarity.next() ?: return null
        return EquipmentItem(
            id = UUID.randomUUID().toString(),
            templateId = base.templateId,
            name = base.name,
            slot = base.slot,
            rarity = nextRarity,
            age = base.age,
            attackBonus = ceil(base.attackBonus * 1.25).toInt(),
            defenseBonus = ceil(base.defenseBonus * 1.25).toInt(),
            hpBonus = ceil(base.hpBonus * 1.25).toInt(),
            lootBonusPct = if (base.lootBonusPct > 0) ceil(base.lootBonusPct * 1.25).toInt() else 0,
            enchantLevel = base.enchantLevel,
            isSeasonal = base.isSeasonal,
            sellGold = ceil(base.sellGold * 1.65).toInt()
        )
    }

    fun rarityUpgradeCost(item: EquipmentItem): Pair<Long, Long> {
        val rankFactor = item.rarity.rank + 1
        val ageFactor = item.age.tier
        val goldCost = (25L * rankFactor * ageFactor)
        val ingotCost = (2L * rankFactor + (ageFactor - 1L)).coerceAtLeast(2L)
        return goldCost to ingotCost
    }

    fun availableTitles(state: GameState): List<String> {
        val list = mutableListOf("Village Wanderer")
        if (state.buildingLevel(BuildingType.CITADEL) >= 2) list.add("Hamlet Reeve")
        if (state.warlordLevel >= 5) list.add("Ironcrag Marshal")
        if (state.warlordLevel >= 12) list.add("Diamond Sovereign")
        if (state.buildingLevel(BuildingType.CITADEL) >= 15) list.add("Vibranium Emperor")
        if (state.royalObsidianTitleUnlocked) list.add("Royal Obsidian Warlord")
        return list
    }
}
