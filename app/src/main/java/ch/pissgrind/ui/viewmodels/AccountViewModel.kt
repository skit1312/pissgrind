package ch.pissgrind.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.pissgrind.data.datasource.account.AccountApiService
import ch.pissgrind.data.datasource.account.AccountResult
import ch.pissgrind.managers.NavidromeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val username: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountApiService: AccountApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private fun getCredentials(): Pair<String, String>? {
        val server = NavidromeManager.getCurrentServer() ?: return null
        return Pair(server.username, server.password)
    }

    fun loadAccountInfo() {
        viewModelScope.launch {
            val (username, password) = getCredentials() ?: return@launch
            _uiState.value = _uiState.value.copy(username = username)

            val email = accountApiService.getEmail(username, password)
            _uiState.value = _uiState.value.copy(email = email ?: "")
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            val (username, _) = getCredentials() ?: run {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "No server configured")
                return@launch
            }

            when (val result = accountApiService.changePassword(username, oldPassword, newPassword)) {
                is AccountResult.Success -> {
                    // Update stored password in NavidromeManager
                    val server = NavidromeManager.getCurrentServer()
                    if (server != null) {
                        val updatedServer = server.copy(password = newPassword)
                        NavidromeManager.removeServer(server.id)
                        NavidromeManager.addServer(updatedServer, isPing = true)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Password changed successfully"
                    )
                    onSuccess()
                }
                is AccountResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun changeEmail(password: String, newEmail: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            val (username, _) = getCredentials() ?: run {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "No server configured")
                return@launch
            }

            when (val result = accountApiService.changeEmail(username, password, newEmail)) {
                is AccountResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        email = newEmail,
                        successMessage = "Email changed successfully"
                    )
                }
                is AccountResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
