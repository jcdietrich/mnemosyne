package com.mnemosyne.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mnemosyne.data.Memory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.asinh
import kotlin.math.tan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memory: Memory,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit = {}
) {
    val dateOnlyFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val timeOnlyFormat = SimpleDateFormat("h:mm:ss a z", Locale.getDefault())

    val memoryDate = Date(memory.timestampUtcMs)
    val dateString = dateOnlyFormat.format(memoryDate)
    val timeString = timeOnlyFormat.format(memoryDate)

    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val openMaps = {
        val geoUri = Uri.parse("geo:${memory.latitudeDeg},${memory.longitudeDeg}?q=${memory.latitudeDeg},${memory.longitudeDeg}(Memory+Location)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://maps.google.com/?q=${memory.latitudeDeg},${memory.longitudeDeg}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Memory") },
            text = { Text("Are you sure you want to delete this memory? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(memory.id)
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Memory",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            // Clean Date and Time
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Map preview if GPS coordinates are present
            if (memory.latitudeDeg != 0.0 || memory.longitudeDeg != 0.0) {
                Spacer(modifier = Modifier.height(16.dp))

                val locationText = memory.locationName
                if (!locationText.isNullOrBlank()) {
                    Text(
                        text = "📍 $locationText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val lat = memory.latitudeDeg
                val lon = memory.longitudeDeg
                val zoom = 16
                val n = 1 shl zoom
                val latRad = Math.toRadians(lat)
                val tileX = ((lon + 180.0) / 360.0 * n).toInt()
                val tileY = ((1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * n).toInt()
                val tileUrl = "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = openMaps),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tileUrl)
                            .addHeader("User-Agent", "MnemosyneApp/1.0 (Android; On-Device Memory Store)")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Map preview for coordinates $lat, $lon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Centered Pin Marker
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pin",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // Transcript text
            Text(
                text = memory.transcript,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
