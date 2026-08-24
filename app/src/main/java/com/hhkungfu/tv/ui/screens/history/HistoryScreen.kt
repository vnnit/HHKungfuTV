package com.hhkungfu.tv.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hhkungfu.tv.data.history.HistoryManager
import com.hhkungfu.tv.data.history.WatchHistoryItem
import com.hhkungfu.tv.data.parser.HhKungfuParser
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.components.TvSidebar
import com.hhkungfu.tv.ui.theme.NetflixBlack
import com.hhkungfu.tv.ui.theme.NetflixCardBg
import com.hhkungfu.tv.ui.theme.NetflixGold
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary
import com.hhkungfu.tv.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(
    onMovieClick: (String) -> Unit, // Navigate to movie Detail Screen
    onPlayHistoryItem: ((WatchHistoryItem) -> Unit)? = null,
    onCategoryClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    var historyList by remember { mutableStateOf(HistoryManager.getWatchHistory(context)) }

    // Auto-enrich posters and URLs for any history items missing posterUrl or valid URL
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val parser = HhKungfuParser()
            var hasUpdates = false
            val updatedList = historyList.map { item ->
                if (item.posterUrl.isEmpty() || !item.movieUrl.startsWith("http")) {
                    try {
                        val results = parser.searchMovies(item.movieTitle)
                        val matched = results.firstOrNull { 
                            it.title.equals(item.movieTitle, ignoreCase = true) || 
                            it.title.contains(item.movieTitle, ignoreCase = true) || 
                            item.movieTitle.contains(it.title, ignoreCase = true)
                        } ?: results.firstOrNull()

                        if (matched != null) {
                            hasUpdates = true
                            item.copy(
                                posterUrl = if (item.posterUrl.isEmpty()) matched.posterUrl else item.posterUrl,
                                movieUrl = if (!item.movieUrl.startsWith("http")) matched.url else item.movieUrl
                            )
                        } else item
                    } catch (_: Exception) {
                        item
                    }
                } else item
            }
            if (hasUpdates) {
                withContext(Dispatchers.Main) {
                    historyList = updatedList
                }
                for (item in updatedList) {
                    HistoryManager.saveWatchHistory(
                        context = context,
                        movieUrl = item.movieUrl,
                        movieTitle = item.movieTitle,
                        posterUrl = item.posterUrl,
                        episodeSlug = item.episodeSlug,
                        episodeName = item.episodeName,
                        sv = item.sv
                    )
                }
            }
        }
    }

    // Deduplicate history items by movieTitle so each movie appears once in the Grid with its latest watched episode
    val movieHistoryList = remember(historyList) {
        historyList.distinctBy { it.movieTitle }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // Left TV Navigation Sidebar
        TvSidebar(
            selectedId = "history",
            onItemSelected = { item ->
                when (item.id) {
                    "home" -> onNavigateHome()
                    "search" -> onSearchClick()
                    "history" -> { /* Already on History */ }
                    else -> onCategoryClick(item.slug, item.title)
                }
            }
        )

        // Main Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NetflixRed,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lịch Sử Xem Phim",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (movieHistoryList.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "(${movieHistoryList.size} phim)",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (movieHistoryList.isNotEmpty()) {
                    FocusableTvItem(
                        onClick = {
                            HistoryManager.clearAllHistory(context)
                            historyList = emptyList()
                        },
                        shape = RoundedCornerShape(6.dp),
                        focusedScale = 1.05f
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .background(if (isFocused) NetflixRed else Color(0xFF262626))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Xóa Lịch Sử",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (movieHistoryList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bạn chưa xem bộ phim nào",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lịch sử các bộ phim đã xem sẽ tự động hiển thị ở đây để bạn dễ dàng xem tiếp.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        FocusableTvItem(
                            onClick = onNavigateHome,
                            shape = RoundedCornerShape(6.dp),
                            focusedScale = 1.08f
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .background(if (isFocused) Color.White else NetflixRed)
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Khám Phá Phim Ngay",
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 36.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(movieHistoryList, key = { it.movieTitle }) { item ->
                        HistoryMovieCard(
                            item = item,
                            onClick = {
                                val targetUrl = if (item.movieUrl.startsWith("http")) {
                                    item.movieUrl
                                } else if (item.movieUrl.isNotEmpty() && !item.movieUrl.all { it.isDigit() }) {
                                    "${Constants.BASE_URL}/${item.movieUrl.removePrefix("/")}"
                                } else {
                                    // Search by title if movieUrl is numeric or empty
                                    item.movieTitle
                                }
                                onMovieClick(targetUrl)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryMovieCard(
    item: WatchHistoryItem,
    onClick: () -> Unit
) {
    FocusableTvItem(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        focusedScale = 1.08f
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) Color(0xFF262626) else Color.Transparent)
                .padding(if (isFocused) 6.dp else 0.dp)
        ) {
            // Poster Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NetflixCardBg)
            ) {
                if (item.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.posterUrl)
                            .addHeader("User-Agent", Constants.USER_AGENT)
                            .addHeader("Referer", Constants.BASE_URL)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.movieTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2B2B2B), Color(0xFF141414))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Top Right Badge (Thuyết Minh / Việt Sub)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (item.sv == "2") NetflixRed else Color(0xFF3B82F6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.sv == "2") "Thuyết Minh" else "Việt Sub",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom Gradient Overlay showing Last Watched Episode
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xF5000000))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Đã xem: ${item.episodeName}",
                        color = NetflixGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Movie Title
            Text(
                text = item.movieTitle,
                color = if (isFocused) Color.White else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
