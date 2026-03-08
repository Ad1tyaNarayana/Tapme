package com.nfcupi.pay.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcupi.pay.data.PreferencesRepository
import com.nfcupi.pay.data.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val upiId: String = "",
    val displayName: String = "",
    val isSaved: Boolean = false,
    val upiIdError: String? = null
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
                _uiState.update { it.copy(upiId = profile.upiId, displayName = profile.displayName) }
            }
        }
    }

    fun onUpiIdChange(v: String)      { _uiState.update { it.copy(upiId = v, upiIdError = null, isSaved = false) } }
    fun onDisplayNameChange(v: String) { _uiState.update { it.copy(displayName = v, isSaved = false) } }

    fun save() {
        val s = _uiState.value
        if (!s.upiId.contains("@")) {
            _uiState.update { it.copy(upiIdError = "Enter a valid UPI ID (e.g. name@upi)") }
            return
        }
        viewModelScope.launch {
            prefsRepo.saveProfile(UserProfile(upiId = s.upiId.trim(), displayName = s.displayName.trim()))
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
