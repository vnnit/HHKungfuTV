package com.hhkungfu.tv.ui.screens.history

import android.text.format.DateUtils
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhkungfu.tv.data.history.HistoryManager
import com.hhkungfu.tv.data.history.WatchHistoryItem
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.components.TvSidebar
import com.hhkungfu.tv.ui.theme.NetflixBlack
import com.hhkungfu.tv.ui.theme.NetflixCardBg
import com.hhkungfu.tv.ui.theme.NetflixGold
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary

@Composable
fun HistoryScreen(
    onMovieClick: (String) -> Unit, // movieUrl or search
    onPlayHistoryItem: (WatchHistoryItem) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    var historyList by remember { mutableStateOf(HistoryManager.getWatchHistory(context)) }

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
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lịch Sử Xem Phim (Lưu 30 ngày)",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (historyList.isNotEmpty()) {
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

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bạn chưa xem tập phim nào",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lịch sử xem các tập phim sẽ tự động lưu ở đây trong vòng 30 ngày.",
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    items(historyList, key = { it.movieTitle + it.episodeSlug + it.sv }) { item ->
                        val timeAgo = DateUtils.getRelativeTimeSpanString(
                            item.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()

                        FocusableTvItem(
                            onClick = { onPlayHistoryItem(item) },
                            shape = RoundedCornerShape(10.dp),
                            focusedScale = 1.02f
                        ) { isFocused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isFocused) Color(0xFF2A2A2A) else NetflixCardBg)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NetflixRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = item.movieTitle,
                                            color = TextPrimary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Đã xem: ${item.episodeName}",
                                                color = NetflixGold,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (item.sv == "2") NetflixRed else Color(0xFF3B82F6))
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = if (item.sv == "2") "Thuyết Minh" else "Việt Sub",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = "• $timeAgo",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isFocused) NetflixRed else Color(0xFF333333))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Xem Tiếp",
                                        color = Color.White,
                                        fontSize = 13.sp,
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
}
