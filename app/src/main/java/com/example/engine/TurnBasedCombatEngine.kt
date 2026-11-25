package com.example.engine

import com.example.model.PhalanxStance
import com.example.model.TroopType
import kotlin.math.ceil
import kotlin.math.min

data class CombatSquad(
    val squadId: Int,
    val name: String,
    val troopType: TroopType,
    val unitCount: Int,
    val attackPower: Int,
    val defensePower: Int,
    val currentHp: Int,
    val maxHp: Int,
    val rationsAvailable: Int = 0,
    val autoEatThresholdPct: Int = 40
) {
    val isDefeated: Boolean get() = currentHp <= 0
}

data class CombatEnemy(
    val enemyId: String,
    val name: String,
    val troopType: TroopType,
    val attackPower: Int,
    val defensePower: Int,
    val currentHp: Int,
    val maxHp: Int,
    val isFortressWall: Boolean = false,
    val wallIntegrity: Int = 0,
    val goldReward: Long = 0L,
    val xpReward: Long = 0L
) {
    val isDefeated: Boolean get() = currentHp <= 0 && (!isFortressWall || wallIntegrity <= 0)
}

data class CombatTurnLog(
    val turnNumber: Int,
    val summary: String
)

data class TurnBasedBattleResult(
    val victory: Boolean,
    val turnsElapsed: Int,
    val finalSquad: CombatSquad,
    val finalEnemy: CombatEnemy,
    val rationsConsumed: Int,
    val goldEarned: Long,
    val xpEarned: Long,
    val logs: List<CombatTurnLog>
)

object TurnBasedCombatEngine {

    /**
     * Phalanx Triangle: Infantry > Ranged > Cavalry > Infantry.
     * Siege units gain a 1.45x bonus against fortress walls (isFortressWall = true)
     * and 1.30x against enemy Siege / Boss fortifications.
     */
    fun calculatePhalanxMultiplier(
        attacker: TroopType,
        defender: TroopType,
        isFortressWall: Boolean = false
    ): Float {
        if (attacker == TroopType.SIEGE) {
            return when {
                isFortressWall -> 1.45f
                defender == TroopType.SIEGE -> 1.30f
                else -> 1.05f
            }
        }
        return when {
            attacker == TroopType.INFANTRY && defender == TroopType.RANGED -> 1.25f
            attacker == TroopType.RANGED && defender == TroopType.CAVALRY -> 1.25f
            attacker == TroopType.CAVALRY && defender == TroopType.INFANTRY -> 1.25f
            attacker == TroopType.INFANTRY && defender == TroopType.CAVALRY -> 0.85f
            attacker == TroopType.RANGED && defender == TroopType.INFANTRY -> 0.85f
            attacker == TroopType.CAVALRY && defender == TroopType.RANGED -> 0.85f
            else -> 1.0f
        }
    }

    fun calculatePhalanxMultiplier(
        playerStance: PhalanxStance,
        defender: TroopType,
        isFortressWall: Boolean = false
    ): Float = calculatePhalanxMultiplier(playerStance.troopType, defender, isFortressWall)

    fun simulateBattle(
        initialSquad: CombatSquad,
        initialEnemy: CombatEnemy,
        maxTurns: Int = 50
    ): TurnBasedBattleResult {
        var squad = initialSquad
        var enemy = initialEnemy
        var rationsUsed = 0
        val logs = mutableListOf<CombatTurnLog>()

        var turn = 0
        while (turn < maxTurns && !squad.isDefeated && !enemy.isDefeated) {
            turn++

            // 1. Squad Turn
            val mult = calculatePhalanxMultiplier(squad.troopType, enemy.troopType, enemy.isFortressWall)
            val rawDamage = ceil(squad.attackPower * mult).toInt()
            val mitigatedDamage = (rawDamage - (enemy.defensePower / 4)).coerceAtLeast(4)

            if (enemy.isFortressWall && enemy.wallIntegrity > 0) {
                val wallDmg = if (squad.troopType == TroopType.SIEGE) ceil(mitigatedDamage * 1.25).toInt() else mitigatedDamage
                val remainingWall = (enemy.wallIntegrity - wallDmg).coerceAtLeast(0)
                enemy = enemy.copy(wallIntegrity = remainingWall)
                logs.add(CombatTurnLog(turn, "${squad.name} battered ${enemy.name} walls for $wallDmg dmg (Wall: $remainingWall)"))
            } else {
                val remainingHp = (enemy.currentHp - mitigatedDamage).coerceAtLeast(0)
                enemy = enemy.copy(currentHp = remainingHp)
                logs.add(CombatTurnLog(turn, "${squad.name} struck ${enemy.name} for $mitigatedDamage dmg (${(mult * 100).toInt()}%)"))
            }

            if (enemy.isDefeated) break

            // 2. Enemy Counter-Attack Turn
            val enemyMult = calculatePhalanxMultiplier(enemy.troopType, squad.troopType, isFortressWall = false)
            val incoming = (ceil(enemy.attackPower * enemyMult).toInt() - (squad.defensePower / 3)).coerceAtLeast(2)
            var nextHp = (squad.currentHp - incoming).coerceAtLeast(0)

            // 3. Auto-Eat War Rations check
            val thresholdHp = (squad.maxHp * squad.autoEatThresholdPct) / 100
            if (nextHp in 1..thresholdHp && squad.rationsAvailable > 0) {
                val healAmt = (squad.maxHp * 0.45f).toInt().coerceAtLeast(35)
                nextHp = min(squad.maxHp, nextHp + healAmt)
                squad = squad.copy(currentHp = nextHp, rationsAvailable = squad.rationsAvailable - 1)
                rationsUsed++
                logs.add(CombatTurnLog(turn, "Auto-consumed War Ration (+$healAmt HP)"))
            } else {
                squad = squad.copy(currentHp = nextHp)
            }
        }

        val victory = enemy.isDefeated && !squad.isDefeated
        return TurnBasedBattleResult(
            victory = victory,
            turnsElapsed = turn,
            finalSquad = squad,
            finalEnemy = enemy,
            rationsConsumed = rationsUsed,
            goldEarned = if (victory) enemy.goldReward else 0L,
            xpEarned = if (victory) enemy.xpReward else 0L,
            logs = logs
        )
    }
}
