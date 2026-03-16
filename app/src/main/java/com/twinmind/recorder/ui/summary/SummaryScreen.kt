package com.twinmind.recorder.ui.summary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.twinmind.recorder.data.local.entity.SummaryStatus
import com.twinmind.recorder.ui.theme.*

@Composable
fun SummaryScreen(
    navController: NavController,
    sessionId: String,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle(null)
    val context = LocalContext.current

    // Trigger summary generation when screen opens (idempotent — WorkManager uses KEEP policy)
    LaunchedEffect(sessionId) {
        viewModel.requestSummary()
    }

    // Stable lambda references - wrapped in remember to prevent infinite recomposition loops
    val onBack: () -> Unit = remember { { navController.popBackStack() } }
    val onRetry: () -> Unit = remember { { viewModel.requestSummary() } }
    val onCopyTranscript: (String) -> Unit = remember(session) {
        { transcript: String ->
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        // ─── Top bar ──────────────────────────────────────────────────────────
        SummaryTopBar(
            title = session?.title ?: "Summary",
            onBack = onBack,
            onShare = { /* TODO: share sheet */ }
        )

        // ─── Tabs: Summary | Transcript ───────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Background,
            contentColor = Purple,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Purple
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Summary",
                        color = if (selectedTab == 0) Purple else Muted
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Transcript",
                        color = if (selectedTab == 1) Purple else Muted
                    )
                }
            )
        }

        HorizontalDivider(color = Outline)

        // ─── Tab content ──────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> SummaryTab(
                uiState = uiState,
                onRetry = onRetry
            )
            1 -> TranscriptTab(
                transcript = session?.transcript,
                onCopy = onCopyTranscript
            )
        }
    }
}

@Composable
private fun SummaryTopBar(title: String, onBack: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = OnSurface)
        }
    }
}

@Composable
private fun ColumnScope.SummaryTab(
    uiState: SummaryUiState,
    onRetry: () -> Unit
) {
    when (uiState.status) {
        null,
        SummaryStatus.PENDING -> {
            // Loading state
            SummaryLoadingState()
        }

        SummaryStatus.STREAMING -> {
            // Show whatever is available so far + loading indicator
            SummaryContentList(
                uiState = uiState,
                isStreaming = true
            )
        }

        SummaryStatus.DONE -> {
            SummaryContentList(
                uiState = uiState,
                isStreaming = false
            )
        }

        SummaryStatus.ERROR -> {
            SummaryErrorState(
                message = uiState.errorMessage ?: "An error occurred",
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun ColumnScope.SummaryContentList(
    uiState: SummaryUiState,
    isStreaming: Boolean
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isStreaming) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = Purple,
                trackColor = SurfaceVariant
            )
            Text(
                "Generating summary...",
                style = MaterialTheme.typography.bodySmall.copy(color = Muted)
            )
        }

        // Fix 2: Add explicit keys to preserve identity across recompositions
        key("section_title") {
            TitleSection(title = uiState.title)
        }
        key("section_summary") {
            SummarySection(summary = uiState.summary)
        }
        key("section_action_items") {
            ActionItemsSection(items = uiState.actionItems)
        }
        key("section_key_points") {
            KeyPointsSection(points = uiState.keyPoints)
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ColumnScope.SummaryLoadingState() {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = Purple,
            trackColor = SurfaceVariant
        )
        Text(
            "Generating summary...",
            style = MaterialTheme.typography.bodySmall.copy(color = Muted)
        )
        SkeletonCard(lines = 1)
        SkeletonCard(lines = 4)
        SkeletonCard(lines = 3)
        SkeletonCard(lines = 3)
    }
}

@Composable
private fun ColumnScope.SummaryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface)
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onRetry,
            border = BorderStroke(1.dp, Purple)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Purple)
            Spacer(Modifier.width(6.dp))
            Text("Retry", color = Purple)
        }
    }
}

@Composable
private fun ColumnScope.TranscriptTab(transcript: String?, onCopy: (String) -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full Transcript",
                style = MaterialTheme.typography.labelMedium
            )
            // Copy button
            if (!transcript.isNullOrBlank()) {
                IconButton(onClick = { onCopy(transcript) }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy transcript",
                        tint = Muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(1.dp, Outline, RoundedCornerShape(12.dp))
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = transcript?.takeIf { it.isNotBlank() }
                    ?: "Transcript is being generated. Chunks appear here as they are transcribed.",
                style = if (transcript.isNullOrBlank())
                    MaterialTheme.typography.bodySmall.copy(color = Subtle)
                else
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}
