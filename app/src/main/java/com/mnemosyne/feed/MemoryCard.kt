package com.mnemosyne.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mnemosyne.data.Memory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryCard(
    memory: Memory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(
        Date(memory.timestampUtcMs)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = memory.transcript,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val locationText = memory.locationName
                val locationDisplay = when {
                    !locationText.isNullOrBlank() -> "📍 $locationText"
                    memory.latitudeDeg != 0.0 || memory.longitudeDeg != 0.0 -> "📍 %.3f, %.3f".format(Locale.US, memory.latitudeDeg, memory.longitudeDeg)
                    else -> null
                }

                if (locationDisplay != null) {
                    Text(
                        text = locationDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
