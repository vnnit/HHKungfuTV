package com.hhkungfu.tv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hhkungfu.tv.data.history.HistoryManager
import com.hhkungfu.tv.data.model.Episode
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.theme.NetflixBlack
import com.hhkungfu.tv.ui.theme.NetflixCardBg
import com.hhkungfu.tv.ui.theme.NetflixGold
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary
import com.hhkungfu.tv.utils.Constants

@Composable
fun DetailScreen(
    movieUrl: String,
    onBackClick: () -> Unit,
    onPlayEpisode: (Episode, String, String, String) -> Unit, // episode, movieTitle, serverType, sv
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedGroupIndex by remember(movieUrl) { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(movieUrl) {
        if (movieUrl.isNotEmpty()) {
            viewModel.loadMovieDetail(movieUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Đang tải thông tin phim & danh sách tập...",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FocusableTvItem(
                            onClick = { viewModel.loadMovieDetail(movieUrl) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(NetflixRed)
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(text = "Thử lại", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        FocusableTvItem(onClick = onBackClick) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF333333))
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(text = "Quay lại", color = Color.White)
                            }
                        }
                    }
                }
            }

            is DetailUiState.Success -> {
                val movie = state.movie
                val currentGroup = movie.episodeGroups.getOrNull(selectedGroupIndex) 
                    ?: movie.episodeGroups.firstOrNull()

                val lastWatched = remember(movie.title, selectedGroupIndex) {
                    HistoryManager.getLastWatchedEpisode(context, movie.title)
                }

                // Background Poster with Gradient
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.posterUrl)
                        .addHeader("User-Agent", Constants.USER_AGENT)
                        .addHeader("Referer", Constants.BASE_URL)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xF8141414),
                                    Color(0xEE141414),
                                    Color(0xCC141414)
                                )
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 24.dp)
                ) {
                    // Back button & Title Header
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            FocusableTvItem(
                                onClick = onBackClick,
                                shape = RoundedCornerShape(50),
                                focusedScale = 1.1f
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .background(if (isFocused) NetflixRed else Color(0x66000000))
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Quay lại",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "CHI TIẾT PHIM",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // Movie Info section
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Poster Card
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NetflixCardBg)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(movie.posterUrl)
                                        .addHeader("User-Agent", Constants.USER_AGENT)
                                        .addHeader("Referer", Constants.BASE_URL)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Meta details
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = movie.title,
                                    color = TextPrimary,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black
                                )

                                if (movie.originalTitle.isNotEmpty()) {
                                    Text(
                                        text = movie.originalTitle,
                                        color = TextSecondary,
                                        fontSize = 15.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NetflixGold)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = movie.quality,
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (movie.episodeCount.isNotEmpty()) {
                                        Text(
                                            text = movie.episodeCount,
                                            color = NetflixRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (movie.year.isNotEmpty()) {
                                        Text(
                                            text = "• ${movie.year}",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Genres
                                if (movie.genres.isNotEmpty()) {
                                    Text(
                                        text = "Thể loại: " + movie.genres.joinToString(", "),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }

                                // Description
                                if (movie.description.isNotEmpty()) {
                                    Text(
                                        text = movie.description,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Smart Quick Play / Continue Watch Buttons
                                val targetEp = if (lastWatched != null) {
                                    currentGroup?.episodes?.firstOrNull { it.slug == lastWatched.episodeSlug } 
                                        ?: currentGroup?.episodes?.firstOrNull() 
                                        ?: movie.episodes.firstOrNull()
                                } else {
                                    currentGroup?.episodes?.firstOrNull() ?: movie.episodes.firstOrNull()
                                }

                                // Find Next Episode (Tập Tiếp Theo)
                                val currentEpNum = targetEp?.let { Regex("""\d+""").find(it.name)?.value?.toIntOrNull() }
                                val nextEp = if (targetEp != null) {
                                    if (currentEpNum != null) {
                                        val nextEpNum = currentEpNum + 1
                                        currentGroup?.episodes?.firstOrNull { ep ->
                                            val num = Regex("""\d+""").find(ep.name)?.value?.toIntOrNull()
                                            num == nextEpNum
                                        } ?: run {
                                            val idx = currentGroup?.episodes?.indexOfFirst { it.slug == targetEp.slug || it.name == targetEp.name } ?: -1
                                            if (idx > 0) currentGroup?.episodes?.getOrNull(idx - 1)
                                            else null
                                        }
                                    } else {
                                        val idx = currentGroup?.episodes?.indexOfFirst { it.slug == targetEp.slug || it.name == targetEp.name } ?: -1
                                        if (idx > 0) currentGroup?.episodes?.getOrNull(idx - 1)
                                        else null
                                    }
                                } else null

                                if (targetEp != null) {
                                    val isContinue = lastWatched != null
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nút 1: Tập Đã Xem / Xem Ngay
                                        FocusableTvItem(
                                            onClick = {
                                                onPlayEpisode(
                                                    targetEp,
                                                    movie.title,
                                                    state.selectedServer.type,
                                                    if (isContinue) lastWatched?.sv ?: currentGroup?.sv ?: targetEp.sv else currentGroup?.sv ?: targetEp.sv
                                                )
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            focusedScale = 1.05f
                                        ) { isFocused ->
                                            Row(
                                                modifier = Modifier
                                                    .background(if (isFocused) Color.White else NetflixRed)
                                                    .padding(horizontal = 18.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isFocused) Color.Black else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isContinue) {
                                                        "Tập Đã Xem (${targetEp.name} - ${if (lastWatched?.sv == "2") "Thuyết Minh" else "Việt Sub"})"
                                                    } else {
                                                        "Xem Ngay (${targetEp.name} - ${if ((currentGroup?.sv ?: targetEp.sv) == "2") "Thuyết Minh" else "Việt Sub"})"
                                                    },
                                                    color = if (isFocused) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        // Nút 2: Xem Tập Tiếp Theo (Tập ..)
                                        if (nextEp != null) {
                                            FocusableTvItem(
                                                onClick = {
                                                    onPlayEpisode(
                                                        nextEp,
                                                        movie.title,
                                                        state.selectedServer.type,
                                                        if (isContinue) lastWatched?.sv ?: currentGroup?.sv ?: nextEp.sv else currentGroup?.sv ?: nextEp.sv
                                                    )
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                focusedScale = 1.05f
                                            ) { isFocused ->
                                                Row(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isFocused) NetflixGold else Color(0xFF2E2E2E)
                                                        )
                                                        .padding(horizontal = 18.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SkipNext,
                                                        contentDescription = null,
                                                        tint = if (isFocused) Color.Black else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Xem Tập Tiếp Theo (${nextEp.name})",
                                                        color = if (isFocused) Color.Black else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Server Quality Selection Tabs (1080P V1, 1080P V2, 4K V1, 4K V2)
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(
                                text = "Chọn Chất Lượng Phát (Server):",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.serverOptions.forEach { server ->
                                    val isSelected = server.type == state.selectedServer.type
                                    FocusableTvItem(
                                        onClick = { viewModel.selectServer(server) },
                                        shape = RoundedCornerShape(6.dp),
                                        focusedScale = 1.05f
                                    ) { isFocused ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when {
                                                        isFocused -> NetflixRed
                                                        isSelected -> Color(0xFF383838)
                                                        else -> Color(0xFF222222)
                                                    }
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = server.displayName,
                                                color = if (isSelected || isFocused) Color.White else TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2 OPTION BUTTONS: [THUYẾT MINH] & [VIỆT SUB] TAB SELECTOR
                    if (movie.episodeGroups.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)) {
                                Text(
                                    text = "Tùy Chọn Bản Dịch:",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    movie.episodeGroups.forEachIndexed { index, group ->
                                        val isSelected = selectedGroupIndex == index
                                        FocusableTvItem(
                                            onClick = { selectedGroupIndex = index },
                                            shape = RoundedCornerShape(8.dp),
                                            focusedScale = 1.06f
                                        ) { isFocused ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when {
                                                            isFocused -> Color.White
                                                            isSelected -> NetflixRed
                                                            else -> Color(0xFF262626)
                                                        }
                                                    )
                                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (group.isThuyetMinh) Icons.Default.RecordVoiceOver else Icons.Default.Subtitles,
                                                        contentDescription = null,
                                                        tint = when {
                                                            isFocused -> Color.Black
                                                            isSelected -> Color.White
                                                            else -> TextSecondary
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = if (group.isThuyetMinh) "🎙️ Thuyết Minh" else "📝 Việt Sub",
                                                        color = when {
                                                            isFocused -> Color.Black
                                                            isSelected -> Color.White
                                                            else -> TextSecondary
                                                        },
                                                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 14.sp
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                when {
                                                                    isFocused -> Color(0x33000000)
                                                                    isSelected -> Color(0x33FFFFFF)
                                                                    else -> Color(0x22FFFFFF)
                                                                }
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${group.episodes.size} tập",
                                                            color = when {
                                                                isFocused -> Color.Black
                                                                isSelected -> Color.White
                                                                else -> TextSecondary
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Render the Episode List for the Selected Option ONLY (with Watched status indicator)
                        if (currentGroup != null) {
                            items(
                                currentGroup.episodes.chunked(6),
                                key = { chunk -> "${currentGroup.title}_${chunk.firstOrNull()?.slug ?: ""}" }
                            ) { epChunk ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    epChunk.forEach { ep ->
                                        val isWatched = HistoryManager.isEpisodeWatched(
                                            context = context,
                                            movieTitle = movie.title,
                                            episodeSlug = ep.slug,
                                            sv = currentGroup.sv
                                        )

                                        FocusableTvItem(
                                            onClick = {
                                                onPlayEpisode(ep, movie.title, state.selectedServer.type, currentGroup.sv)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            focusedScale = 1.08f,
                                            modifier = Modifier.weight(1f)
                                        ) { isFocused ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        when {
                                                            isFocused -> NetflixRed
                                                            isWatched -> Color(0xFF1B2818) // Distinct subtle dark green for watched
                                                            else -> Color(0xFF242424)
                                                        }
                                                    )
                                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = ep.name,
                                                        color = when {
                                                            isFocused -> Color.White
                                                            isWatched -> NetflixGold
                                                            else -> TextPrimary
                                                        },
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isWatched) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isWatched) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "✓",
                                                            color = if (isFocused) Color.White else NetflixGold,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    for (i in 0 until (6 - epChunk.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
