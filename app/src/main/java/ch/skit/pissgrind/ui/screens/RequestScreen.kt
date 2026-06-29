package ch.skit.pissgrind.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ch.skit.pissgrind.managers.settings.AppearanceSettingsManager	
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
    fun RequestScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val username = AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")
    
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
            text = "Music Request",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Request a song or album to be added",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Song / Album") },
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
                        val result = sendRequest(artist, title, info, username.value)
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
            enabled = !isSending
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
    username: String
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("https://register.pissgrind.ch/request")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

    val params = "artist=${URLEncoder.encode(artist, "UTF-8")}" +
            "&title=${URLEncoder.encode(title, "UTF-8")}" +
            "&info=${URLEncoder.encode(info, "UTF-8")}" +
            "&username=${URLEncoder.encode(username, "UTF-8")}"

    conn.outputStream.use { it.write(params.toByteArray()) }

    conn.responseCode == 200
}
