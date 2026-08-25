package com.mnemosyne.feed

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DualActionBottomBar(
    isRecordingMemory: Boolean,
    isProcessingMemory: Boolean,
    onMemoryPressStart: () -> Unit,
    onMemoryPressEnd: () -> Unit,
    isRecordingSearch: Boolean,
    isProcessingSearch: Boolean,
    onSearchPressStart: () -> Unit,
    onSearchPressEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Record Memory
            LargeActionButton(
                label = if (isRecordingMemory) "Recording..." else if (isProcessingMemory) "Saving..." else "Record Memory",
                sublabel = "Hold to Record",
                icon = Icons.Default.Mic,
                isRecording = isRecordingMemory,
                isProcessing = isProcessingMemory,
                activeColor = MaterialTheme.colorScheme.error,
                idleColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onPressStart = onMemoryPressStart,
                onPressEnd = onMemoryPressEnd,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Right Button: Voice Search
            LargeActionButton(
                label = if (isRecordingSearch) "Listening..." else if (isProcessingSearch) "Searching..." else "Voice Search",
                sublabel = "Hold to Search",
                icon = Icons.Default.Search,
                isRecording = isRecordingSearch,
                isProcessing = isProcessingSearch,
                activeColor = MaterialTheme.colorScheme.tertiary,
                idleColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onPressStart = onSearchPressStart,
                onPressEnd = onSearchPressEnd,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun LargeActionButton(
    label: String,
    sublabel: String,
    icon: ImageVector,
    isRecording: Boolean,
    isProcessing: Boolean,
    activeColor: Color,
    idleColor: Color,
    contentColor: Color,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val backgroundColor = when {
        isRecording -> activeColor
        isProcessing -> MaterialTheme.colorScheme.surfaceVariant
        else -> idleColor
    }

    val textColor = when {
        isRecording -> Color.White
        isProcessing -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> contentColor
    }

    Box(
        modifier = modifier
            .scale(if (isRecording) pulseScale else 1.0f)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .pointerInput(isProcessing) {
                if (!isProcessing) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    )
                }
            }
            .semantics { contentDescription = "$label, $sublabel" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = textColor,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
