package com.nfcupi.pay.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcupi.pay.data.PreferencesRepository
import com.nfcupi.pay.data.UserProfile
import com.nfcupi.pay.nfc.UpiDeepLinkBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val upiId: String = "",
    val displayName: String = "",
    val redirectSite: String = "",
    val isSaved: Boolean = false,
    val upiIdError: String? = null,
    val redirectSiteError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.userProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        upiId = profile.upiId,
                        displayName = profile.displayName,
                        redirectSite = profile.redirectBaseUrl
                    )
                }
            }
        }
    }

    fun onUpiIdChange(v: String)      { _uiState.update { it.copy(upiId = v, upiIdError = null, isSaved = false) } }
    fun onDisplayNameChange(v: String) { _uiState.update { it.copy(displayName = v, isSaved = false) } }
    fun onRedirectSiteChange(v: String) {
        _uiState.update { it.copy(redirectSite = v, redirectSiteError = null, isSaved = false) }
    }

    fun save() {
        val s = _uiState.value
        if (!s.upiId.contains("@")) {
            _uiState.update { it.copy(upiIdError = "Enter a valid UPI ID (e.g. name@upi)") }
            return
        }

        val normalizedRedirectBaseUrl = try {
            UpiDeepLinkBuilder.normalizeRedirectBaseUrl(s.redirectSite)
        } catch (_: IllegalArgumentException) {
            _uiState.update {
                it.copy(redirectSiteError = "Enter your deployed NFC-redirect domain or full URL")
            }
            return
        }

        viewModelScope.launch {
            prefsRepo.saveProfile(
                UserProfile(
                    upiId = s.upiId.trim(),
                    displayName = s.displayName.trim(),
                    redirectBaseUrl = normalizedRedirectBaseUrl
                )
            )
            _uiState.update { it.copy(isSaved = true, redirectSite = normalizedRedirectBaseUrl) }
        }
    }
}
