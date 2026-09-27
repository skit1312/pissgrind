package ch.pissgrind.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ch.pissgrind.R
import ch.pissgrind.managers.NavidromeManager
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.viewmodels.AccountViewModel

@Composable
fun S_AccountScreen(
    navHostController: NavHostController,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalIsDark.current

    // Theme-aware colors
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.7f) else PissBlack.copy(alpha = 0.7f)
    val buttonContainer = if (isDark) PissGreen else PissBlack
    val lightBg = LocalLightBackground.current
    val buttonContent = if (isDark) PissBlack else lightBg
    val borderColor = if (isDark) PissGreen else PissBlack
    val dividerColor = if (isDark) PissGreen.copy(alpha = 0.7f) else PissBlack.copy(alpha = 0.7f)

    // Load email on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAccountInfo()
    }

    // Show toasts for success/error
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    // Password fields
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    // Email fields
    var newEmail by remember { mutableStateOf("") }
    var emailPassword by remember { mutableStateOf("") }
    var showEmailPassword by remember { mutableStateOf(false) }
    var showCurrentEmail by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = { navHostController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Text(
                text = "Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ===== CHANGE PASSWORD =====
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))

            PasswordField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = "Current Password",
                showPassword = showOldPassword,
                onToggleVisibility = { showOldPassword = !showOldPassword },
                isDark = isDark
            )

            PasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                showPassword = showNewPassword,
                onToggleVisibility = { showNewPassword = !showNewPassword },
                isDark = isDark
            )

            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm New Password",
                showPassword = showNewPassword,
                onToggleVisibility = { showNewPassword = !showNewPassword },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    when {
                        oldPassword.isBlank() -> Toast.makeText(context, "Enter current Password", Toast.LENGTH_SHORT).show()
                        newPassword.length < 8 -> Toast.makeText(context, "New Password must be at least 8 Characters", Toast.LENGTH_SHORT).show()
                        newPassword != confirmPassword -> Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        else -> {
                            viewModel.changePassword(oldPassword, newPassword) {
                                oldPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            }
                        }
                    }
                },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainer,
                    contentColor = buttonContent
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = buttonContent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Change Password", fontWeight = FontWeight.Bold)
                }
            }

            // ===== CHANGE EMAIL =====
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Change Email",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            if (uiState.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCurrentEmail){
                        Text(
                            text = "Email: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = uiState.email,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "hide",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { showCurrentEmail = false }
                        )
                    } else {
                        Text(
                            text = "Email ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "show",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { showCurrentEmail = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("New Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = pissgrindTextFieldColors(isDark),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            )

            PasswordField(
                value = emailPassword,
                onValueChange = { emailPassword = it },
                label = "Confirm with Password",
                showPassword = showEmailPassword,
                onToggleVisibility = { showEmailPassword = !showEmailPassword },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    when {
                        newEmail.isBlank() || !newEmail.contains("@") ->
                            Toast.makeText(context, "Enter a valid Email", Toast.LENGTH_SHORT).show()
                        emailPassword.isBlank() ->
                            Toast.makeText(context, "Enter your Password to confirm", Toast.LENGTH_SHORT).show()
                        else -> {
                            viewModel.changeEmail(emailPassword, newEmail)
                            newEmail = ""
                            emailPassword = ""
                        }
                    }
                },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainer,
                    contentColor = buttonContent
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Email", fontWeight = FontWeight.Bold)
            }

            // ===== LOGOUT =====
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    NavidromeManager.getCurrentServer()?.let { server ->
                        NavidromeManager.removeServer(server.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PissBlack,
                    contentColor = lightBg
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.sign_out_light),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    isDark: Boolean
) {
    val iconTint = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = if (showPassword)
                        ImageVector.vectorResource(R.drawable.eye_slash_light)
                    else
                        ImageVector.vectorResource(R.drawable.eye_light),
                    contentDescription = if (showPassword) "Hide" else "Show",
                    tint = iconTint
                )
            }
        },
        colors = pissgrindTextFieldColors(isDark),
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun pissgrindTextFieldColors(isDark: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isDark) PissGreen else PissBlack,
    unfocusedBorderColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f),
    focusedLabelColor = if (isDark) PissGreen else PissBlack,
    unfocusedLabelColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f),
    cursorColor = if (isDark) PissGreen else PissBlack,
    focusedTextColor = if (isDark) PissGreen else PissBlack,
    unfocusedTextColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
)
