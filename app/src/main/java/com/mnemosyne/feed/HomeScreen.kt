package com.mnemosyne.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mnemosyne.capture.CaptureState
import com.mnemosyne.capture.CaptureViewModel
import com.mnemosyne.data.Memory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    feedViewModel: FeedViewModel = hiltViewModel(),
    captureViewModel: CaptureViewModel = hiltViewModel()
) {
    val displayedMemories by feedViewModel.displayedMemories.collectAsState()
    val searchQuery by feedViewModel.searchQuery.collectAsState()
    val captureState by captureViewModel.captureState.collectAsState()
    val voiceSearchState by feedViewModel.voiceSearchState.collectAsState()

    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var memoryPendingDelete by remember { mutableStateOf<Memory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(captureState) {
        when (val state = captureState) {
            is CaptureState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                captureViewModel.resetState()
            }
            is CaptureState.Done -> {
                snackbarHostState.showSnackbar("Memory saved.")
                captureViewModel.resetState()
            }
            else -> Unit
        }
    }

    if (memoryPendingDelete != null) {
        val memoryToDelete = memoryPendingDelete!!
        AlertDialog(
            onDismissRequest = { memoryPendingDelete = null },
            title = { Text("Delete Memory") },
            text = { Text("Are you sure you want to delete this memory? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedViewModel.deleteMemory(memoryToDelete.id)
                        memoryPendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memoryPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedMemory != null) {
        MemoryDetailScreen(
            memory = selectedMemory!!,
            onBack = { selectedMemory = null },
            onDelete = { id ->
                feedViewModel.deleteMemory(id)
                selectedMemory = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mnemosyne",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top 2/3: Search bar & Memory Feed
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.67f)
                    .padding(horizontal = 16.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { feedViewModel.onSearchQueryChanged(it) },
                    onClearQuery = { feedViewModel.clearSearch() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (displayedMemories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching memories found." else "No memories recorded yet.\nHold the microphone button below to record.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val listState = rememberLazyListState()

                    LaunchedEffect(listState.canScrollForward) {
                        if (!listState.canScrollForward && displayedMemories.isNotEmpty()) {
                            feedViewModel.loadNextPage()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(displayedMemories, key = { it.id }) { memory ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                        memoryPendingDelete = memory
                                        false // Don't auto dismiss until confirmed
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                            else -> Color.Transparent
                                        },
                                        label = "swipeBg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                MemoryCard(
                                    memory = memory,
                                    onClick = { selectedMemory = memory }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom 1/3: 2 Large Action Buttons (Record on Left, Voice Search on Right)
            DualActionBottomBar(
                isRecordingMemory = captureState is CaptureState.Recording,
                isProcessingMemory = captureState is CaptureState.Processing,
                onMemoryPressStart = { captureViewModel.onRecordPressed() },
                onMemoryPressEnd = { captureViewModel.onRecordReleased() },
                isRecordingSearch = voiceSearchState is VoiceSearchState.Recording,
                isProcessingSearch = voiceSearchState is VoiceSearchState.Processing,
                onSearchPressStart = { feedViewModel.startVoiceSearch() },
                onSearchPressEnd = { feedViewModel.stopVoiceSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.33f)
            )
        }
    }
}
