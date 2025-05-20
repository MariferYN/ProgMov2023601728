package com.example.productos

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val PRIMARY_COLOR = intPreferencesKey("primary_color")
    }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: false }
    val colorFlow: Flow<Int> = context.dataStore.data.map { it[PRIMARY_COLOR] ?: 0xFF6200EE.toInt() }

    suspend fun saveDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
    }

    suspend fun saveColor(color: Int) {
        context.dataStore.edit { it[PRIMARY_COLOR] = color }
    }
}