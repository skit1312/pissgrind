package ch.pissgrind.ui.elements.dialogs

import androidx.compose.material3.CircularProgressIndicator
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.pissgrind.R
import ch.pissgrind.data.NavidromeProvider
import ch.pissgrind.managers.NavidromeManager
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.providers.navidrome.getNavidromeStatus
import ch.pissgrind.providers.navidrome.navidromeStatus
import ch.pissgrind.ui.elements.bounceClick
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen

enum class OnboardingStep { OVERVIEW, PROVIDER_SELECTION, DONE }

@Composable
fun OnboardingDialog(
    setShowDialog: (Boolean) -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDark.current
    var url by remember { mutableStateOf("https://fvck.pissgrind.rip") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var registerEmail by remember { mutableStateOf("") }
    var registerPasswordRepeat by remember { mutableStateOf("") }
    var registerStatus by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var showRegisterResultDialog by remember { mutableStateOf(false) }
    var registrationSucceeded by remember { mutableStateOf(false) }


    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .offset(y = (-100).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(Modifier.height(100.dp))

                //Logo
                Image(
                    painter = if (isDark) painterResource(R.drawable.logofullpgreen) else painterResource(R.drawable.logofullpblack),
                    "Pissgrind",
                    modifier = Modifier.height(100.dp)
                )

                Spacer(Modifier.height(100.dp))

                Text(
                    text = if (showRegister) "Create an Account" else "Sign in to Pissgrind",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Username (shared)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.Label_Navidrome_Username)) },
                    singleLine = true,
                    isError = !showRegister && navidromeStatus.value == "Wrong username or password",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (showRegister) {
                    // Email
                    OutlinedTextField(
                        value = registerEmail,
                        onValueChange = { registerEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                }

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.Label_Navidrome_Password)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            R.drawable.eye_light
                        else
                            R.drawable.eye_slash_light
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = image),
                                description,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    isError = !showRegister && navidromeStatus.value == "Wrong username or password",
                    modifier = Modifier.fillMaxWidth()
                )

                if (showRegister) {
                    Spacer(Modifier.height(12.dp))

                    // Repeat Password
                    OutlinedTextField(
                        value = registerPasswordRepeat,
                        onValueChange = { registerPasswordRepeat = it },
                        label = { Text("Repeat Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!showRegister) {
                    // Login Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                navidromeStatus.value = "Please enter username and password"
                                return@Button
                            }
                            if (url.isBlank()) {
                                navidromeStatus.value = "Please enter server URL"
                                return@Button
                            }
                            coroutineScope.launch {
                                val server = NavidromeProvider(
                                    url, url, username, password, true, false
                                )
                                NavidromeManager.addServer(server)
                                AppearanceSettingsManager(context).setUsername(username)
                                onLoginSuccess()
                                setShowDialog(true)
                            }
                        },
                        shape = RoundedCornerShape(size = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick()
                    ) {
                        Text(stringResource(R.string.Action_Login))
                    }

                } else {
                    Spacer(Modifier.height(16.dp))

                    // Register Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank() || registerEmail.isBlank()) {
                                registerStatus = "Please fill in all Fields."
                                return@Button
                            }
                            if (password != registerPasswordRepeat) {
                                registerStatus = "Passwords do not match."
                                return@Button
                            }
                            if (password.length < 8) {
                                registerStatus = "Password must be at least 8 Characters."
                                return@Button
                            }
                            if (username.length < 3) {
                                registerStatus = "Username must be at least 3 Characters."
                                return@Button
                            }
                            isRegistering = true
                            registerStatus = ""
                            showRegisterResultDialog = true
                            coroutineScope.launch {
                                try {
                                    val result = submitRegistration(
                                        username, registerEmail, password, registerPasswordRepeat
                                    )
                                    registrationSucceeded = result
                                    registerStatus = if (result)
                                        "Registration submitted"
                                    else
                                        "Registration failed. Try again."
                                } catch (e: Exception) {
                                    registrationSucceeded = false
                                    registerStatus = "Error: ${e.message}"
                                }
                                isRegistering = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        enabled = !isRegistering,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                            Text("Register")
                    }
                    // ===== REGISTRATION RESULT DIALOG =====
                    if (showRegisterResultDialog) {
                        Dialog(
                            onDismissRequest = { },
                            properties = DialogProperties(
                                dismissOnBackPress = !isRegistering,
                                dismissOnClickOutside = false
                            )
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isDark) PissGreen else PissBlack,
                                        RoundedCornerShape(2.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isRegistering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(Modifier.height(20.dp))
                                        Text(
                                            text = "Submitting registration...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    } else {
                                        Text(
                                            text = if (registrationSucceeded)
                                                "Registration submitted"
                                            else
                                                "Registration failed",
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center,
                                            color = if (registrationSucceeded)
                                                MaterialTheme.colorScheme.onBackground
                                            else
                                                MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = if (registrationSucceeded)
                                                "You will receive an Email as soon as your Account has been approved."
                                            else
                                                registerStatus,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(Modifier.height(24.dp))
                                        Button(
                                            onClick = {
                                                showRegisterResultDialog = false
                                                if (registrationSucceeded) {
                                                    showRegister = false
                                                    registerStatus = ""
                                                    registerEmail = ""
                                                    password = ""
                                                    registerPasswordRepeat = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .bounceClick()
                                        ) {
                                            Text("Close")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (registerStatus.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = registerStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (registerStatus.contains("submitted"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Toggle Login/Register
                TextButton(
                    onClick = { showRegister = !showRegister },
                    modifier = Modifier.bounceClick()
                ) {
                    Text(if (showRegister) "Already have an account? Login" else "Don't have an account? Register")
                }
            }
        }
    }
}
private suspend fun submitRegistration(
    username: String,
    email: String,
    password: String,
    passwordRepeat: String
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("https://register.pissgrind.rip/register")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    conn.setRequestProperty("X-Pissgrind-Client", "android")

    val params = "username=${URLEncoder.encode(username, "UTF-8")}" +
            "&email=${URLEncoder.encode(email, "UTF-8")}" +
            "&password=${URLEncoder.encode(password, "UTF-8")}" +
            "&password_repeat=${URLEncoder.encode(passwordRepeat, "UTF-8")}"

    conn.outputStream.use { it.write(params.toByteArray()) }

    conn.responseCode == 200
}


