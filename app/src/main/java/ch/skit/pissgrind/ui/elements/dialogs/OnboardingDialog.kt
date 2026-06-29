package ch.skit.pissgrind.ui.elements.dialogs

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
import ch.skit.pissgrind.R
import ch.skit.pissgrind.data.NavidromeProvider
import ch.skit.pissgrind.managers.NavidromeManager
import ch.skit.pissgrind.managers.settings.AppearanceSettingsManager
import ch.skit.pissgrind.providers.navidrome.getNavidromeStatus
import ch.skit.pissgrind.providers.navidrome.navidromeStatus
import ch.skit.pissgrind.ui.elements.bounceClick
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

enum class OnboardingStep { OVERVIEW, PROVIDER_SELECTION, DONE }

@Composable
fun OnboardingDialog(
    setShowDialog: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf("https://fvck.pissgrind.ch") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var registerEmail by remember { mutableStateOf("") }
    var registerPasswordRepeat by remember { mutableStateOf("") }
    var registerStatus by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }


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
//logo
                Image(
                    painter = painterResource(R.drawable.pissgrind_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(275.dp)
                        .padding(top = 0.dp, bottom = 10.dp)
                )
                Text(
                    text = if (showRegister) "Create an account" else "Sign in to pissgrind",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

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
                            R.drawable.round_visibility_24
                        else
                            R.drawable.round_visibility_off_24
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = image),
                                description
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

                Spacer(Modifier.height(8.dp))

                if (!showRegister) {
                    // Login Status
                    if (navidromeStatus.value != "") {
                        Text(
                            text = "Status: ${navidromeStatus.value}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Login Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val server = NavidromeProvider(
                                    url, url, username, password, true, false
                                )
                                NavidromeManager.addServer(server)
                                AppearanceSettingsManager(context).setUsername(username)
                                setShowDialog(true)
                            }
                        },
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
                                registerStatus = "Please fill in all fields."
                                return@Button
                            }
                            if (password != registerPasswordRepeat) {
                                registerStatus = "Passwords do not match."
                                return@Button
                            }
                            if (password.length < 8) {
                                registerStatus = "Password must be at least 8 characters."
                                return@Button
                            }
                            if (username.length < 3) {
                                registerStatus = "Username must be at least 3 characters."
                                return@Button
                            }
                            isRegistering = true
                            registerStatus = ""
                            coroutineScope.launch {
                                try {
                                    val result = submitRegistration(
                                        username,
                                        registerEmail,
                                        password,
                                        registerPasswordRepeat
                                    )
                                    registerStatus =
                                        if (result) "Registration submitted! Check your email for approval." else "Registration failed. Try again."
                                } catch (e: Exception) {
                                    registerStatus = "Error: ${e.message}"
                                }
                                isRegistering = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        enabled = !isRegistering
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Register")
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
    val url = URL("https://register.pissgrind.ch/register")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

    val params = "username=${URLEncoder.encode(username, "UTF-8")}" +
            "&email=${URLEncoder.encode(email, "UTF-8")}" +
            "&password=${URLEncoder.encode(password, "UTF-8")}" +
            "&password_repeat=${URLEncoder.encode(passwordRepeat, "UTF-8")}"

    conn.outputStream.use { it.write(params.toByteArray()) }

    conn.responseCode == 200
}

