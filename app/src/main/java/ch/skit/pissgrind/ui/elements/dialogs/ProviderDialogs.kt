package ch.skit.pissgrind.ui.elements.dialogs

import android.net.Uri
import androidx.compose.material3.TextButton
import android.content.Intent
import android.content.Context
import android.util.Patterns
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import ch.skit.pissgrind.R
import ch.skit.pissgrind.data.NavidromeProvider
import ch.skit.pissgrind.data.model.Screen
import ch.skit.pissgrind.managers.LocalProviderManager
import ch.skit.pissgrind.managers.NavidromeManager
import ch.skit.pissgrind.managers.settings.AppearanceSettingsManager
import ch.skit.pissgrind.managers.settings.MediaProviderSettingsManager
import ch.skit.pissgrind.providers.navidrome.getNavidromeStatus
import ch.skit.pissgrind.providers.navidrome.navidromeStatus
import ch.skit.pissgrind.ui.elements.bounceClick
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

//region PREVIEWS
@Preview(showBackground = true, device = "id:tv_1080p")
@Preview(showBackground = true)
@Composable
fun PreviewProviderDialog() {
    CreateMediaProviderDialog(setShowDialog = { })
}

@Preview(showBackground = true)
@Composable
fun PreviewLrcLibDialog() {
    EditLrcLibUrlDialog(setShowDialog = { })
}

@Preview(showBackground = true)
@Composable
fun PreviewNoMediaProvidersDialog() {
    NoMediaProvidersDialog(setShowDialog = { }, NavHostController(LocalContext.current))
}
//endregion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditLrcLibUrlDialog(
    setShowDialog: (Boolean) -> Unit,
    context: Context = LocalContext.current
) {
    val settingsManager = remember { MediaProviderSettingsManager(context) }

    var url by remember { mutableStateOf("https://lrclib.net") }

    // Launch a coroutine to collect the flow once and update url
    LaunchedEffect(settingsManager) {
        settingsManager.lrcLibEndpointFlow.collect { value ->
            url = value
        }
    }

    var isValidUrl: Boolean by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable()
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.Dialog_LRCLIB_Url),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    isValidUrl = Patterns.WEB_URL.matcher(url).matches()
                },
                label = { Text(stringResource(R.string.Dialog_LRCLIB_Url)) },
                singleLine = true,
                isError = !isValidUrl
            )

            Button(
                onClick = {
                    if (isValidUrl)
                        runBlocking {
                            MediaProviderSettingsManager(context).setLrcLibEndpoint(url)
                        }

                    setShowDialog(false)
                },
                modifier = Modifier
                    .bounceClick(),
                enabled = isValidUrl
            ) {
                Text(stringResource(R.string.Action_Done))
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CreateMediaProviderDialog(
    setShowDialog: (Boolean) -> Unit,
    context: Context = LocalContext.current
) {
    var url: String by remember { mutableStateOf("https://fvck.pissgrind.ch") }
    var username: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }
    var allowCerts: Boolean by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .widthIn(max = 320.dp)
                .dialogFocusable()
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.Settings_Header_Media),
                style = MaterialTheme.typography.titleLarge
            )

            //region Navidrome
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                /* USERNAME */
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.Label_Navidrome_Username)) },
                    singleLine = true,
                    isError = navidromeStatus.value == "Wrong username or password"
                )
                /* PASSWORD */
                var passwordVisible by remember { mutableStateOf(false) }
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

                        val description =
                            if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = image),
                                description
                            )
                        }
                    },
                    isError = navidromeStatus.value == "Wrong username or password"
                )

                if (navidromeStatus.value != "") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Status: ${navidromeStatus.value}",
                            fontWeight = FontWeight.Medium,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                Crossfade(
                    navidromeStatus.value == "ok"
                ) {
                    if (it) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val server = NavidromeProvider(
                                        url,
                                        url,
                                        username,
                                        password,
                                        true,
                                        allowCerts
                                    )
                                    NavidromeManager.addServer(server)
                                    AppearanceSettingsManager(context).setUsername(username)

                                    navidromeStatus.value = ""

                                    setShowDialog(false)
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick(),
                            enabled = navidromeStatus.value == "ok"
                        ) {
                            Text(
                                stringResource(R.string.Action_Add)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val server = NavidromeProvider(
                                    url,
                                    url,
                                    username,
                                    password,
                                    true,
                                    allowCerts
                                )
                                coroutineScope.launch {
                                    getNavidromeStatus(server)
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .bounceClick()
                        ) {
                            Text(
                                stringResource(R.string.Action_Login)
                            )
                        }
                    }
                }

                // Register Button
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://register.pissgrind.ch")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.bounceClick()
                ) {
                    Text("Don't have an account? Register")
                }
            }
            //endregion
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoMediaProvidersDialog(setShowDialog: (Boolean) -> Unit, navController: NavHostController) {

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.Settings_Header_Media),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.No_Providers_Splash),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    navController.navigate(Screen.S_Providers.route) {
                        launchSingleTop = true
                    }; setShowDialog(false)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .bounceClick()
            ) {
                Text(
                    stringResource(R.string.Action_Go)
                )
            }
        }
    }
}
