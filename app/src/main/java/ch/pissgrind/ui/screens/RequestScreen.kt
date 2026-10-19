package ch.pissgrind.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.pissgrind.R
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.ui.theme.LocalIsDark
import ch.pissgrind.ui.theme.LocalLightBackground
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.TimeZone

private const val INFO_MAX_LENGTH = 300

private enum class SendState { IDLE, SENDING, SUCCESS }

@Composable
fun RequestScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val username by AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")

    val isDark = LocalIsDark.current
    val accent = if (isDark) PissGreen else PissBlack
    val surfaceCol = if (isDark) PissBlack else LocalLightBackground.current

    var requestType by remember { mutableStateOf("Song") }
    var artist by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    var sendState by remember { mutableStateOf(SendState.IDLE) }
    var errorMessage by remember { mutableStateOf("") }
    var showFieldErrors by remember { mutableStateOf(false) }

    // Für das Ticket nach dem Senden
    var sentType by remember { mutableStateOf("") }
    var sentTitle by remember { mutableStateOf("") }
    var sentArtist by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = accent,
        unfocusedTextColor = accent,
        focusedBorderColor = accent,
        unfocusedBorderColor = accent.copy(alpha = 0.5f),
        focusedLabelColor = accent,
        unfocusedLabelColor = accent.copy(alpha = 0.7f),
        cursorColor = accent
    )

    fun submit() {
        if (sendState == SendState.SENDING) return
        if (artist.isBlank() || title.isBlank()) {
            showFieldErrors = true
            errorMessage = "Please fill in Artist and ${requestType} Title."
            return
        }
        focusManager.clearFocus()
        sendState = SendState.SENDING
        errorMessage = ""
        coroutineScope.launch {
            val ok = try {
                sendRequest(artist.trim(), title.trim(), info.trim(), username, requestType)
            } catch (e: Exception) {
                false
            }
            if (ok) {
                sentType = requestType
                sentTitle = title.trim()
                sentArtist = artist.trim()
                artist = ""; title = ""; info = ""
                showFieldErrors = false
                sendState = SendState.SUCCESS
            } else {
                errorMessage = "Could not send your request. Check your connection and try again."
                sendState = SendState.IDLE
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            //.imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        RequestHeader(requestType, accent, surfaceCol)

        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = sendState == SendState.SUCCESS,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "request form"
        ) { showTicket ->
            if (showTicket) {
                RequestTicket(
                    type = sentType,
                    title = sentTitle,
                    artist = sentArtist,
                    accent = accent,
                    surfaceCol = surfaceCol,
                    onRequestAnother = { sendState = SendState.IDLE }
                )
            } else {
                Column {
                    RequestTypeToggle(
                        selected = requestType,
                        enabled = sendState != SendState.SENDING,
                        accent = accent,
                        surfaceCol = surfaceCol,
                        onSelect = { requestType = it }
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("$requestType Title") },
                        singleLine = true,
                        isError = showFieldErrors && title.isBlank(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(2.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text("Artist") },
                        singleLine = true,
                        isError = showFieldErrors && artist.isBlank(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(2.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = info,
                        onValueChange = { info = it.take(INFO_MAX_LENGTH) },
                        label = { Text("Additional Info (optional)") },
                        placeholder = {
                            Text(
                                "e.g. release year, edition, link…",
                                color = accent.copy(alpha = 0.4f)
                            )
                        },
                        minLines = 2,
                        colors = fieldColors,
                        shape = RoundedCornerShape(2.dp),
                        supportingText = {
                            Text(
                                text = "${info.length} / $INFO_MAX_LENGTH",
                                color = accent.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { submit() },
                        enabled = sendState != SendState.SENDING,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = surfaceCol,
                            disabledContainerColor = accent.copy(alpha = 0.6f),
                            disabledContentColor = surfaceCol
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (sendState == SendState.SENDING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = surfaceCol,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "SEND REQUEST",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ===== COMPONENTS =====

@Composable
private fun RequestHeader(requestType: String, accent: Color, surfaceCol: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "// REQUEST",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = surfaceCol,
            modifier = Modifier
                .background(accent, RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Request Music",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(48.dp)
                .height(3.dp)
                .background(accent)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                append(if (requestType == "Album") "Missing an " else "Missing a ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(requestType.lowercase())
                }
                append("? Request it and it will be added to the library.")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = accent
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.clock_counter_clockwise_light),
                contentDescription = null,
                tint = accent.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Requests are reviewed 3× daily",
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RequestTypeToggle(
    selected: String,
    enabled: Boolean,
    accent: Color,
    surfaceCol: Color,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, accent, RoundedCornerShape(2.dp))
    ) {
        listOf("Song", "Album").forEachIndexed { index, type ->
            val isSelected = selected == type
            val bg by animateColorAsState(
                targetValue = if (isSelected) accent else surfaceCol,
                animationSpec = tween(200),
                label = "segment bg"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) surfaceCol else accent,
                animationSpec = tween(200),
                label = "segment fg"
            )

            if (index > 0) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(accent))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(bg)
                    .clickable(enabled = enabled) { onSelect(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = fg
                )
            }
        }
    }
}

@Composable
private fun RequestTicket(
    type: String,
    title: String,
    artist: String,
    accent: Color,
    surfaceCol: Color,
    onRequestAnother: () -> Unit
) {
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) }
    val nextReview = remember { nextReviewTime() }

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = accent,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = dash),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
                .padding(20.dp)
        ) {
            Text(
                text = "REQUEST QUEUED ✓",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = surfaceCol,
                modifier = Modifier
                    .background(accent, RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = accent.copy(alpha = 0.7f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = "by $artist",
                style = MaterialTheme.typography.bodyLarge,
                color = accent
            )

            Spacer(Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(
                    color = accent.copy(alpha = 0.6f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Next review: $nextReview (Swiss time)",
                style = MaterialTheme.typography.bodySmall,
                color = accent.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRequestAnother,
            shape = RoundedCornerShape(2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "REQUEST ANOTHER",
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = accent
            )
        }
    }
}

private fun nextReviewTime(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Zurich"))
    val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val next = listOf(10, 16, 22).firstOrNull { it * 60 > minutesNow }
    return if (next != null) "today, %02d:00".format(next) else "tomorrow, 10:00"
}

private suspend fun sendRequest(
    artist: String,
    title: String,
    info: String,
    username: String,
    type: String
): Boolean = withContext(Dispatchers.IO) {
    val conn = (URL("https://register.pissgrind.rip/request").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 10_000
        readTimeout = 15_000
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        setRequestProperty("X-Pissgrind-Client", "android")
    }

    try {
        val params = "artist=${URLEncoder.encode(artist, "UTF-8")}" +
                "&title=${URLEncoder.encode(title, "UTF-8")}" +
                "&info=${URLEncoder.encode(info, "UTF-8")}" +
                "&username=${URLEncoder.encode(username, "UTF-8")}" +
                "&type=${URLEncoder.encode(type, "UTF-8")}"

        conn.outputStream.use { it.write(params.toByteArray()) }
        conn.responseCode == 200
    } finally {
        conn.disconnect()
    }
}
