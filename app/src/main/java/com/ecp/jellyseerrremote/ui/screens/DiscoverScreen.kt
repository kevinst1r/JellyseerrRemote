package com.ecp.jellyseerrremote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecp.jellyseerrremote.data.SearchResult
import com.ecp.jellyseerrremote.ui.PosterUrl
import com.ecp.jellyseerrremote.vm.MainViewModel
import com.ecp.jellyseerrremote.vm.RequestResult
import com.ecp.jellyseerrremote.repo.DiscoverCategory

private const val PREVIEW_ITEMS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onSeeMore: (DiscoverCategory) -> Unit
) {
    val s by vm.state.collectAsState()
    val requestResult by vm.requestResult.collectAsState()
    var selectedResult by remember { mutableStateOf<SearchResult?>(null) }

    LaunchedEffect(Unit) {
        DiscoverCategory.entries.forEach { vm.loadDiscoverCategory(it) }
    }

    LaunchedEffect(requestResult != null) {
        if (requestResult is RequestResult.Success || requestResult is RequestResult.Error) {
            selectedResult = null
            vm.clearRequestResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (s.discoverError != null) {
                item {
                    Text(
                        s.discoverError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            DiscoverCategory.entries.forEach { category ->
                val items = s.discoverData[category] ?: emptyList()
                val loading = category in s.discoverLoading
                item(key = category.name) {
                    DiscoverSection(
                        category = category,
                        items = items.take(PREVIEW_ITEMS),
                        isLoading = loading,
                        onItemClick = { selectedResult = it },
                        onSeeMore = { onSeeMore(category) }
                    )
                }
            }
        }
    }

    selectedResult?.let { result ->
        DetailsBottomSheet(
            result = result,
            onDismiss = { selectedResult = null },
            onRequest = { vm.requestMedia(result) },
            requestInProgress = requestResult is RequestResult.Loading || requestResult is RequestResult.Success,
            isRequestedOrInLibrary = result.isRequestedOrInLibrary
        )
    }
}

@Composable
private fun DiscoverSection(
    category: DiscoverCategory,
    items: List<SearchResult>,
    isLoading: Boolean,
    onItemClick: (SearchResult) -> Unit,
    onSeeMore: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                category.label,
                style = MaterialTheme.typography.titleMedium
            )
            androidx.compose.material3.TextButton(onClick = onSeeMore) {
                Text("See more")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading && items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { result ->
                    DiscoverPosterCard(
                        result = result,
                        onClick = { onItemClick(result) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverPosterCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box {
            Column {
                val posterUrl = PosterUrl.forPath(result.posterPath)
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .width(100.dp)
                            .height(150.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(150.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            result.mediaType.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    result.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    modifier = Modifier.padding(6.dp)
                )
            }
            if (result.isRequestedOrInLibrary) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsBottomSheet(
    result: SearchResult,
    onDismiss: () -> Unit,
    onRequest: () -> Unit,
    requestInProgress: Boolean = false,
    isRequestedOrInLibrary: Boolean = false
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val posterUrl = PosterUrl.forPath(result.posterPath)
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp, 180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "${result.year} · ${result.mediaType.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        result.statusLabel,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !requestInProgress && !isRequestedOrInLibrary,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    when {
                        !isRequestedOrInLibrary -> "Request"
                        result.statusLabel == "Requested" -> "Requested"
                        else -> "In library"
                    }
                )
            }
        }
    }
}
