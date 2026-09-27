package ch.pissgrind.updater

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen

@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PissBlack,
        shape = RoundedCornerShape(2.dp),
        title = {
            Text(
                text = "Update Available",
                color = PissGreen,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "v${updateInfo.latestVersion}",
                    color = PissGreen,
                    fontWeight = FontWeight.SemiBold
                )
                if (!updateInfo.changelog.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = updateInfo.changelog,
                        color = PissGreen.copy(alpha = 0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    updateInfo.downloadUrl?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PissGreen,
                    contentColor = PissBlack
                ),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = PissGreen.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun WhatsNewDialog(
    changelog: String,
    version: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PissBlack,
        shape = RoundedCornerShape(2.dp),
        title = {
            Text(
                text = "What's New in v$version",
                color = PissGreen,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = changelog,
                    color = PissGreen.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PissGreen,
                    contentColor = PissBlack
                ),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text("Nice!", fontWeight = FontWeight.Bold)
            }
        }
    )
}
