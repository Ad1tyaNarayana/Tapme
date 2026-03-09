package com.nfcupi.pay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_prefs")

data class UserProfile(
    val upiId: String = "",
    val displayName: String = "",
    val redirectBaseUrl: String = ""
)

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_UPI_ID = stringPreferencesKey("upi_id")
    private val KEY_NAME   = stringPreferencesKey("display_name")
    private val KEY_REDIRECT_BASE_URL = stringPreferencesKey("redirect_base_url")

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            upiId           = prefs[KEY_UPI_ID] ?: "",
            displayName     = prefs[KEY_NAME]   ?: "",
            redirectBaseUrl = prefs[KEY_REDIRECT_BASE_URL] ?: ""
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UPI_ID] = profile.upiId
            prefs[KEY_NAME]   = profile.displayName
            prefs[KEY_REDIRECT_BASE_URL] = profile.redirectBaseUrl
        }
    }
}
