package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GameState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "obsidian_crown_save"
)

class GameRepository(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(GameState::class.java)
    private val keyGameState = stringPreferencesKey("game_state_v3_json")

    suspend fun loadState(): GameState {
        return context.gameDataStore.data.map { prefs ->
            val raw = prefs[keyGameState]
            if (raw.isNullOrBlank()) {
                GameState.freshVillage()
            } else {
                try {
                    adapter.fromJson(raw) ?: GameState.freshVillage()
                } catch (_: Exception) {
                    GameState.freshVillage()
                }
            }
        }.first()
    }

    suspend fun saveState(state: GameState) {
        val json = adapter.toJson(state)
        context.gameDataStore.edit { prefs ->
            prefs[keyGameState] = json
        }
    }

    fun exportSaveJson(state: GameState): String = adapter.toJson(state)

    fun importSaveJson(rawJson: String): GameState? {
        return try {
            adapter.fromJson(rawJson.trim())
        } catch (_: Exception) {
            null
        }
    }
}
