package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.data.GameRepository
import com.example.engine.GameSimulationEngine
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class OfflineBattleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = GameRepository(applicationContext)
            val currentState = repository.loadState()
            val nowMs = System.currentTimeMillis()
            val simResult = GameSimulationEngine.simulateOfflineProgress(
                initialState = currentState,
                nowMs = nowMs,
                today = LocalDate.now()
            )
            repository.saveState(simResult.state)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK = "obsidian_crown_offline_battle_periodic"
        private const val UNIQUE_IMMEDIATE_WORK = "obsidian_crown_offline_battle_catchup"

        fun schedulePeriodicOfflineSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<OfflineBattleWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueDelayedOfflineSimulation(context: Context, delayMinutes: Long = 5L) {
            val request = OneTimeWorkRequestBuilder<OfflineBattleWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_IMMEDIATE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
