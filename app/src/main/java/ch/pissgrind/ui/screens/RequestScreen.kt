package ch.pissgrind.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.PissGreenDark


@Composable
    fun RequestScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val username = AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")
        val isDark = LocalIsDark.current
        var artist by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var info by remember { mutableStateOf("") }
        var statusMessage by remember { mutableStateOf("") }
        var isSending by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Request Music",
            style = MaterialTheme.typography.headlineLarge,
            color = if (isDark) PissGreen else PissBlack
        )
        var requestType by remember { mutableStateOf("Song") }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Request a Song or Album to be added",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDark) PissGreen.copy(alpha = 0.8f) else PissBlack.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(32.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = requestType == "Song",
                onClick = { requestType = "Song" },
                shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {
                Text("Song")
            }
            SegmentedButton(
                selected = requestType == "Album",
                onClick = { requestType = "Album" },
                shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 2.dp, bottomEnd = 2.dp)
            ) {
                Text("Album")
            }
        }

        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (requestType == "Song") "Song Title" else "Album Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = info,
            onValueChange = { info = it },
            label = { Text("Additional Info (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (artist.isBlank() || title.isBlank()) {
                    statusMessage = "Please fill in Artist and Song/Album."
                    return@Button
                }
                isSending = true
                statusMessage = ""
                coroutineScope.launch {
                    try {
                        val result = sendRequest(artist, title, info, username.value, requestType)
                        if (result) {
                            statusMessage = "Request sent! ✓"
                            artist = ""
                            title = ""
                            info = ""
                        } else {
                            statusMessage = "Failed to send request."
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isSending = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isSending,
            shape = RoundedCornerShape(2.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp

                )
            } else {
                Text("Send Request")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusMessage.contains("✓"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

private suspend fun sendRequest(
    artist: String,
    title: String,
    info: String,
    username: String,
    type: String
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("https://register.pissgrind.rip/request")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

    val params = "artist=${URLEncoder.encode(artist, "UTF-8")}" +
            "&title=${URLEncoder.encode(title, "UTF-8")}" +
            "&info=${URLEncoder.encode(info, "UTF-8")}" +
            "&username=${URLEncoder.encode(username, "UTF-8")}" +
            "&type=${URLEncoder.encode(type, "UTF-8")}"

    conn.outputStream.use { it.write(params.toByteArray()) }

    conn.responseCode == 200
}

