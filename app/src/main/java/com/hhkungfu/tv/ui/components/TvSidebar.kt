package com.hhkungfu.tv.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.CalendarMonth

data class NavMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val slug: String = ""
)

val defaultMenuItems = listOf(
    NavMenuItem("home", "Trang Chủ", Icons.Default.Home, "home"),
    NavMenuItem("history", "Lịch Sử Xem", Icons.Default.History, "history"),
    NavMenuItem("search", "Tìm Kiếm", Icons.Default.Search, "search"),
    NavMenuItem("tu-tien", "Tu Tiên", Icons.Default.AutoAwesome, "tu-tien"),
    NavMenuItem("luyen-cap", "Luyện Cấp", Icons.Default.WorkspacePremium, "luyen-cap"),
    NavMenuItem("kiem-hiep", "Kiếm Hiệp", Icons.Default.Fireplace, "kiem-hiep"),
    NavMenuItem("moi-cap-nhat", "Mới Cập Nhật", Icons.Default.LocalMovies, "moi-cap-nhat"),
    NavMenuItem("top-xem-nhieu", "Xem Nhiều", Icons.Default.Star, "top-xem-nhieu"),
    NavMenuItem("update", "Cập Nhật App", Icons.Default.SystemUpdate, "update")
)

@Composable
fun TvSidebar(
    selectedId: String,
    onItemSelected: (NavMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isExpanded) 200.dp else 68.dp)
            .background(Color(0xE6101010))
            .animateContentSize(animationSpec = tween(180))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // App Logo Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp, start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NetflixRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "H",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "HHKUNGFU",
                    color = NetflixRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }

        // Navigation Items
        defaultMenuItems.forEach { item ->
            val isSelected = item.id == selectedId
            
            FocusableTvItem(
                onClick = { onItemSelected(item) },
                shape = RoundedCornerShape(8.dp),
                focusedScale = 1.05f,
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .height(40.dp)
            ) { isFocused ->
                LaunchedEffect(isFocused) {
                    if (isFocused && !isExpanded) {
                        isExpanded = true
                    }
                }

                Row(
                    modifier = Modifier
                        .background(
                            when {
                                isFocused -> NetflixRed
                                isSelected -> Color(0x33E50914)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isFocused || isSelected) TextPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )

                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            color = if (isFocused || isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
