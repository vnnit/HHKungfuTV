package com.hhkungfu.tv.ui.screens.home

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.data.updater.AppUpdater
import com.hhkungfu.tv.data.updater.UpdateInfo
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.components.MovieCard
import com.hhkungfu.tv.ui.components.MovieRow
import com.hhkungfu.tv.ui.components.NetflixHeroBanner
import com.hhkungfu.tv.ui.components.TvSidebar
import com.hhkungfu.tv.ui.theme.NetflixBlack
import com.hhkungfu.tv.ui.theme.NetflixCardBg
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onMovieClick: (MovieItem) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDayId by viewModel.selectedDayId.collectAsState()
    val scheduleMovies by viewModel.scheduleMovies.collectAsState()
    val isScheduleLoading by viewModel.isScheduleLoading.collectAsState()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var isCheckingUpdateManually by remember { mutableStateOf(false) }
    val updateButtonFocusRequester = remember { FocusRequester() }

    // Silently check for app update in the background on startup
    LaunchedEffect(Unit) {
        val info = AppUpdater.checkForUpdate(context)
        if (info != null) {
            updateInfo = info
        }
    }

    // Auto-focus on "Cập Nhật Ngay" button whenever dialog opens
    LaunchedEffect(updateInfo) {
        if (updateInfo != null) {
            delay(200)
            try {
                updateButtonFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    fun startDownload(url: String) {
        isDownloadingUpdate = true
        downloadProgress = 0
        scope.launch {
            val success = AppUpdater.downloadAndInstallApk(context, url) { progress ->
                downloadProgress = progress
            }
            isDownloadingUpdate = false
            if (!success) {
                Toast.makeText(context, "Không thể tải bản cập nhật", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(NetflixBlack)
        ) {
            // Left TV Navigation Sidebar
            TvSidebar(
                selectedId = "home",
                onItemSelected = { item ->
                    when (item.id) {
                        "home" -> { /* Already on Home */ }
                        "history" -> onHistoryClick()
                        "search" -> onSearchClick()
                        "update" -> {
                            isCheckingUpdateManually = true
                            scope.launch {
                                val info = AppUpdater.checkForUpdate(context)
                                isCheckingUpdateManually = false
                                if (info != null) {
                                    updateInfo = info
                                } else {
                                    Toast.makeText(context, "Bạn đang dùng phiên bản mới nhất!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else -> onCategoryClick(item.slug, item.title)
                    }
                }
            )

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NetflixBlack)
            ) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
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
                                text = "Đang tải dữ liệu HHKungfu TV...",
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                        }
                    }

                    is HomeUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Không thể kết nối đến máy chủ",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            FocusableTvItem(
                                onClick = { viewModel.loadHomeData() }
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .background(if (isFocused) Color.White else NetflixRed)
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Thử lại",
                                        color = if (isFocused) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    is HomeUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            // Hero Feature Banner at the top
                            item {
                                NetflixHeroBanner(
                                    movie = state.heroMovie,
                                    onPlayClick = {
                                        state.heroMovie?.let { onMovieClick(it) }
                                    },
                                    onDetailClick = {
                                        state.heroMovie?.let { onMovieClick(it) }
                                    }
                                )
                            }

                            // 📅 LỊCH CHIẾU PHIM HÀNG NGÀY (7 DAYS TABS & MOVIE ROW)
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    // Section Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 36.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color(0xFF00AAFF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "📅 LỊCH CHIẾU PHIM HÀNG NGÀY",
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // 7 Days Tabs (Sun, Mon, Tue, Wed, Thu, Fri, Sat)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 36.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        viewModel.scheduleDays.forEach { day ->
                                            val isSelected = selectedDayId == day.id
                                            FocusableTvItem(
                                                onClick = { viewModel.selectScheduleDay(day.id) },
                                                shape = RoundedCornerShape(8.dp),
                                                focusedScale = 1.06f,
                                                modifier = Modifier.weight(1f)
                                            ) { isFocused ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            when {
                                                                isFocused -> Color.White
                                                                isSelected -> Color(0xFF0088CC)
                                                                else -> Color(0xFF242424)
                                                            }
                                                        )
                                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = day.code,
                                                            color = if (isFocused) Color.Black else if (isSelected) Color.White else TextSecondary,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = day.name,
                                                            color = if (isFocused) Color.Black else Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected || isFocused) FontWeight.Black else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Schedule Movies List for Selected Day
                                    if (isScheduleLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF00AAFF),
                                                modifier = Modifier.size(36.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    } else if (scheduleMovies.isNotEmpty()) {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(scheduleMovies, key = { "sched_${selectedDayId}_${it.url}" }) { movie ->
                                                MovieCard(
                                                    movie = movie,
                                                    onClick = { onMovieClick(movie) }
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Không có phim nào chiếu trong ngày này",
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontal Movie Rows for each category
                            items(state.sections, key = { it.slug }) { section ->
                                MovieRow(
                                    title = section.title,
                                    movies = section.movies,
                                    onMovieClick = onMovieClick,
                                    onSeeAllClick = {
                                        onCategoryClick(section.slug, section.title)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Auto-Update Dialog (with proper TV Focus Trapping)
        if (updateInfo != null) {
            val info = updateInfo!!
            Dialog(
                onDismissRequest = { updateInfo = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(500.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NetflixCardBg)
                            .padding(28.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = NetflixRed,
                                modifier = Modifier.size(52.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Có Bản Cập Nhật Mới (${info.versionName.ifEmpty { "v${info.versionCode}" }})",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (info.changelog.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = info.changelog,
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isDownloadingUpdate) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = NetflixRed,
                                        trackColor = Color(0xFF333333)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Đang tải: $downloadProgress%",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Cancel Button
                                    FocusableTvItem(
                                        onClick = { updateInfo = null },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { isFocused ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isFocused) Color(0xFF555555) else Color(0xFF333333))
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Để Sau",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    // Update Button
                                    FocusableTvItem(
                                        onClick = { startDownload(info.apkUrl) },
                                        shape = RoundedCornerShape(8.dp),
                                        focusedScale = 1.05f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(updateButtonFocusRequester)
                                    ) { isFocused ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isFocused) Color.White else NetflixRed)
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Cập Nhật Ngay",
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
        }
    }
}
