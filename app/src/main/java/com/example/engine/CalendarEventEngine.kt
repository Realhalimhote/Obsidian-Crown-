package com.example.engine

import com.example.data.GameCatalog
import com.example.data.HolidayDef
import com.example.model.GameState
import com.example.model.HolidayId
import com.example.model.RealmSurgeType
import com.example.model.SoloEventType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

data class SeasonalStatus(
    val activeCalendarHoliday: HolidayDef?,
    val calendarDaysRemaining: Long,
    val nextHoliday: HolidayDef,
    val daysUntilNextHoliday: Long,
    val summonedOffSeasonHoliday: HolidayDef?,
    val summonedRemainingMs: Long
) {
    val effectiveHoliday: HolidayDef?
        get() = activeCalendarHoliday ?: summonedOffSeasonHoliday
}

data class SoloEventStatus(
    val windowId: Long,
    val type: SoloEventType,
    val remainingMs: Long,
    val tierThresholds: List<Long> = listOf(120L, 350L, 750L)
)

data class RealmSurgeStatus(
    val type: RealmSurgeType,
    val remainingMs: Long
)

data class KingdomFestivalStatus(
    val name: String,
    val bonusLabel: String,
    val daysRemaining: Long
)

object CalendarEventEngine {

    private const val SIX_HOURS_MS = 6L * 3600L * 1000L
    private const val TWO_HOURS_MS = 2L * 3600L * 1000L

    fun currentWeekKey(today: LocalDate = LocalDate.now()): String {
        val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = today.get(IsoFields.WEEK_BASED_YEAR)
        return "$year-W$week"
    }

    fun getMonthlyKingdomFestival(today: LocalDate = LocalDate.now()): KingdomFestivalStatus {
        val monthIdx = (today.monthValue - 1) % 4
        val daysLeft = today.lengthOfMonth().toLong() - today.dayOfMonth + 1L
        return when (monthIdx) {
            0 -> KingdomFestivalStatus("Festival of Iron & Oak", "+15% Gathering & Smithing XP", daysLeft)
            1 -> KingdomFestivalStatus("Sovereign's Bounty", "+15% Gold & Conquest Loot", daysLeft)
            2 -> KingdomFestivalStatus("Arcane Solstice", "+20% Enchanting & Alchemy Yield", daysLeft)
            else -> KingdomFestivalStatus("Warlord's Muster", "+15% Army Damage & Mastery", daysLeft)
        }
    }

    fun getSeasonalStatus(
        today: LocalDate = LocalDate.now(),
        nowMs: Long = System.currentTimeMillis(),
        state: GameState
    ): SeasonalStatus {
        val year = today.year
        var activeHoliday: HolidayDef? = null
        var activeDaysLeft = 0L

        for (holiday in GameCatalog.holidays) {
            val endDate = activeEndDateFor(holiday, today)
            if (endDate != null) {
                activeHoliday = holiday
                activeDaysLeft = ChronoUnit.DAYS.between(today, endDate) + 1L
                break
            }
        }

        var bestNextHoliday = GameCatalog.holidays.first()
        var minDaysUntil = Long.MAX_VALUE

        for (holiday in GameCatalog.holidays) {
            var candidateStart = LocalDate.of(year, holiday.startMonth, holiday.startDay)
            if (!candidateStart.isAfter(today)) {
                candidateStart = LocalDate.of(year + 1, holiday.startMonth, holiday.startDay)
            }
            val diff = ChronoUnit.DAYS.between(today, candidateStart)
            if (diff < minDaysUntil) {
                minDaysUntil = diff
                bestNextHoliday = holiday
            }
        }

        val summonedHoliday = state.summonedHolidayId
            ?.let { idStr -> HolidayId.entries.firstOrNull { it.name == idStr } }
            ?.takeIf { state.summonedBossExpiresAtMs > nowMs }
            ?.let { GameCatalog.holidayById(it) }

        val summonedRemaining = if (summonedHoliday != null) {
            (state.summonedBossExpiresAtMs - nowMs).coerceAtLeast(0L)
        } else {
            0L
        }

        return SeasonalStatus(
            activeCalendarHoliday = activeHoliday,
            calendarDaysRemaining = activeDaysLeft,
            nextHoliday = bestNextHoliday,
            daysUntilNextHoliday = minDaysUntil,
            summonedOffSeasonHoliday = summonedHoliday,
            summonedRemainingMs = summonedRemaining
        )
    }

    private fun activeEndDateFor(holiday: HolidayDef, today: LocalDate): LocalDate? {
        val y = today.year
        return if (holiday.startMonth <= holiday.endMonth) {
            val start = LocalDate.of(y, holiday.startMonth, holiday.startDay)
            val end = LocalDate.of(y, holiday.endMonth, holiday.endDay)
            if (!today.isBefore(start) && !today.isAfter(end)) end else null
        } else {
            val decStart = LocalDate.of(y, holiday.startMonth, holiday.startDay)
            val nextJanEnd = LocalDate.of(y + 1, holiday.endMonth, holiday.endDay)
            val prevDecStart = LocalDate.of(y - 1, holiday.startMonth, holiday.startDay)
            val janEnd = LocalDate.of(y, holiday.endMonth, holiday.endDay)
            when {
                !today.isBefore(decStart) && !today.isAfter(nextJanEnd) -> nextJanEnd
                !today.isBefore(prevDecStart) && !today.isAfter(janEnd) -> janEnd
                else -> null
            }
        }
    }

    fun getCurrentSoloEvent(nowMs: Long = System.currentTimeMillis()): SoloEventStatus {
        val windowId = nowMs / SIX_HOURS_MS
        val types = SoloEventType.entries
        val type = types[(windowId % types.size).toInt()]
        val nextBoundaryMs = (windowId + 1L) * SIX_HOURS_MS
        return SoloEventStatus(
            windowId = windowId,
            type = type,
            remainingMs = (nextBoundaryMs - nowMs).coerceAtLeast(0L)
        )
    }

    fun getCurrentRealmSurge(nowMs: Long = System.currentTimeMillis()): RealmSurgeStatus {
        val windowId = nowMs / TWO_HOURS_MS
        val types = RealmSurgeType.entries
        val type = types[(windowId % types.size).toInt()]
        val nextBoundaryMs = (windowId + 1L) * TWO_HOURS_MS
        return RealmSurgeStatus(
            type = type,
            remainingMs = (nextBoundaryMs - nowMs).coerceAtLeast(0L)
        )
    }

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val hrs = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return String.format("%02d:%02d:%02d", hrs, mins, secs)
    }
}
