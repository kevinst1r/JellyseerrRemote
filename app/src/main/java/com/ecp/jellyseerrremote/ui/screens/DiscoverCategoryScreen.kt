package com.ecp.jellyseerrremote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ecp.jellyseerrremote.R
import com.ecp.jellyseerrremote.data.SearchResult
import com.ecp.jellyseerrremote.ui.PosterUrl
import com.ecp.jellyseerrremote.vm.MainViewModel
import com.ecp.jellyseerrremote.vm.RequestResult
import com.ecp.jellyseerrremote.repo.DiscoverCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverCategoryScreen(
    category: DiscoverCategory,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val s by vm.state.collectAsState()
    val requestResult by vm.requestResult.collectAsState()
    var selectedResult by remember { mutableStateOf<SearchResult?>(null) }
    var allItems by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var loadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val initialItems = s.discoverData[category] ?: emptyList()
    LaunchedEffect(initialItems) {
        if (allItems.isEmpty() && initialItems.isNotEmpty()) allItems = initialItems
    }

    LaunchedEffect(Unit) {
        if (initialItems.isEmpty()) {
            loadingMore = true
            vm.loadDiscoverCategoryPage(category, 1)
                .onSuccess { list ->
                    allItems = list
                    currentPage = 1
                    hasMore = list.size >= 20
                }
                .onFailure { hasMore = false }
            loadingMore = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        loadingMore = true
        scope.launch {
            vm.loadDiscoverCategoryPage(category, currentPage + 1)
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        allItems = allItems + list
                        currentPage += 1
                    }
                    hasMore = list.size >= 20
                }
                .onFailure { hasMore = false }
            loadingMore = false
        }
    }

    LaunchedEffect(requestResult) {
        if (requestResult is RequestResult.Success || requestResult is RequestResult.Error) {
            selectedResult = null
            vm.clearRequestResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.label) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (allItems.isEmpty() && !loadingMore) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allItems, key = { it.id }) { result ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        CategoryResultItem(
                            result = result,
                            onClick = { selectedResult = result }
                        )
                    }
                }
                item {
                    if (loadingMore) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (hasMore && allItems.isNotEmpty()) {
                        androidx.compose.material3.TextButton(
                            onClick = { loadMore() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load more")
                        }
                    }
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
private fun CategoryResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val posterUrl = PosterUrl.forPath(result.posterPath)
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp, 108.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp, 108.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (result.year.isNotEmpty()) {
                        Text(
                            result.year,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            result.mediaType.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        result.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (result.isRequestedOrInLibrary) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "In library or requested",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color(0xFF2E7D32), CircleShape)
                        .padding(4.dp),
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
