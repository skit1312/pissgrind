package ch.pissgrind.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.border
import ch.pissgrind.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ch.pissgrind.R
import ch.pissgrind.data.recommendation.AlgorithmLogger
import ch.pissgrind.data.recommendation.TrainingDataManager
import ch.pissgrind.data.recommendation.TrainingModeManager
import ch.pissgrind.ui.theme.PissBlack
import ch.pissgrind.ui.theme.PissGreen
import ch.pissgrind.ui.theme.PissWhite
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import ch.pissgrind.managers.settings.ExcludedAlbumsManager
import ch.pissgrind.data.recommendation.RecommendationCache
import ch.pissgrind.ui.theme.LocalLightBackground

@Composable
fun AlgorithmSettingsScreen(
    navHostController: NavHostController,
    algorithmLogger: AlgorithmLogger,
    trainingDataManager: TrainingDataManager,
    recommendationCache: RecommendationCache,
    excludedAlbumsManager: ExcludedAlbumsManager
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDark.current

    // Theme colors
    val textColor = if (isDark) PissGreen else PissBlack
    val subtleColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
    val lightBg = LocalLightBackground.current
    val primaryButtonContainer = if (isDark) PissGreen else PissBlack
    val primaryButtonContent = if (isDark) PissBlack else PissWhite
    val outlinedButtonContainer = if (isDark) PissBlack else PissWhite
    val outlinedButtonContent = if (isDark) PissGreen else PissBlack
    val borderColor = if (isDark) PissGreen else PissBlack
    val dividerColor = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack.copy(alpha = 0.9f)
    val statsBackground = if (isDark) PissBlack else PissBlack
    val statsText = if (isDark) PissGreen else PissWhite
    val statsSubtle = if (isDark) PissGreen.copy(alpha = 0.9f) else PissWhite.copy(alpha = 0.9f)
    val statsBorder = if (isDark) PissGreen.copy(alpha = 0.9f) else PissBlack
    val statsDivider = if (isDark) PissGreen.copy(alpha = 0.9f) else PissWhite.copy(alpha = 0.9f)

    val buttonHeight = 48.dp

    // File content to save -> Temporary Holder
    var pendingFileContent by remember { mutableStateOf<ByteArray?>(null) }

    // SAF launcher for saving files
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            pendingFileContent?.let { content ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content)
                }
                Toast.makeText(context, "Log saved", Toast.LENGTH_SHORT).show()
            }
        }
        pendingFileContent = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
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
                text = "Algorithm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== TRAINING MODE SECTION =====
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Training Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the 'Training' chip on the Now Playing Screen to show Thumbs Up/Down Buttons during normal Playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleColor
                )
            }
            item {
                HorizontalDivider(color = dividerColor)
            }

            // ===== LOG SECTION =====
            item {
                Text(
                    text = "Algorithm Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View how your Actions influence Recommendations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleColor
                )
            }

            // Save Full Log
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val file = algorithmLogger.exportFullLog(context)
                            pendingFileContent = file.readBytes()
                            saveFileLauncher.launch("pissgrind_algorithm_full.log")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonContainer,
                        contentColor = primaryButtonContent
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                ) {
                    Text("Full Log", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.floppy_disk_light),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            item {
                HorizontalDivider(color = dividerColor)
            }

            // Clear Log
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            algorithmLogger.clearLog()
                            Toast.makeText(context, "Log cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonContainer,
                        contentColor = primaryButtonContent
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .border(1.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                ) {
                    Text("Clear Log")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.trash_light),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            item {
                HorizontalDivider(color = dividerColor)
            }

            item {
                var showConfirmDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonContainer,
                        contentColor = primaryButtonContent
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .border(1.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                ) {
                    Text("Wipe Algorithm", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.broom_light),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showConfirmDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        containerColor = if (isDark) PissBlack else PissWhite,
                        titleContentColor = textColor,
                        textContentColor = subtleColor,
                        title = { Text("Reset Algorithm?", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("This will reset all of your Recommendations and Training Data, connected to your Account.\n\nThe app will restart automatically.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        trainingDataManager.wipeServerData()
                                        trainingDataManager.clearAll()
                                        algorithmLogger.clearLog()
                                        recommendationCache.clear()
                                        excludedAlbumsManager.clearAll()
                                        val prefs = context.getSharedPreferences(
                                            "pissgrind_recommendations",
                                            android.content.Context.MODE_PRIVATE
                                        )
                                        prefs.edit().clear().apply()

                                        Toast.makeText(
                                            context,
                                            "Algorithm Wiped. Restarting...",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        kotlinx.coroutines.delay(500)
                                        val intent =
                                            context.packageManager.getLaunchIntentForPackage(context.packageName)
                                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        (context as? android.app.Activity)?.finish()
                                        Runtime.getRuntime().exit(0)
                                    }
                                    showConfirmDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryButtonContainer,
                                    contentColor = primaryButtonContent
                                ),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text("Wipe & Restart", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showConfirmDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = outlinedButtonContainer,
                                    contentColor = outlinedButtonContent
                                ),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    borderColor,
                                    RoundedCornerShape(2.dp)
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}       //remove last three } if uncommenting stats section

        /*    // ===== STATS =====
            item {
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            item {
                val stats = algorithmLogger.getStats()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, statsBorder, RoundedCornerShape(2.dp))
                        .background(statsBackground, RoundedCornerShape(2.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Feedback
                    Text(
                        text = "Feedback",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statsSubtle
                    )
                    StatRow("Total Feedback Given", stats.totalFeedback.toString(), statsText, statsSubtle)
                    StatRow("Thumbs Up", stats.thumbsUp.toString(), statsText, statsSubtle)
                    StatRow("Thumbs Down", stats.thumbsDown.toString(), statsText, statsSubtle)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = statsDivider)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Albums
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statsSubtle
                    )
                    StatRow("Albums Excluded (score ≤ -4)", stats.albumsExcluded.toString(), statsText, statsSubtle)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = statsDivider)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Artists
                    Text(
                        text = "Artists",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statsSubtle
                    )
                    StatRow("Artists Boosted (+10%)", stats.artistsBoosted.toString(), statsText, statsSubtle)
                    StatRow("Artists Penalized (Strikes)", stats.artistsPenalized.toString(), statsText, statsSubtle)
                    StatRow("Artists Excluded (3 Strikes)", stats.artistsExcluded.toString(), statsText, statsSubtle)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = statsDivider)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Genres
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statsSubtle
                    )
                    StatRow("Genres Boosted", stats.genresBoosted.toString(), statsText, statsSubtle)
                    StatRow("Genres Penalized", stats.genresPenalized.toString(), statsText, statsSubtle)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, textColor: Color, subtleColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = subtleColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
*/