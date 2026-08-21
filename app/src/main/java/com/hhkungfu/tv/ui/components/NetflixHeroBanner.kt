package com.hhkungfu.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.ui.theme.NetflixGold
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary
import com.hhkungfu.tv.utils.Constants

@Composable
fun NetflixHeroBanner(
    movie: MovieItem?,
    onPlayClick: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (movie == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // Poster / Backdrop image
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

        // Gradient Left-to-Right and Top-to-Bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xF0141414),
                            Color(0xCC141414),
                            Color(0x55141414),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1400f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x88141414),
                            Color(0xFF141414)
                        ),
                        startY = 150f
                    )
                )
        )

        // Movie Info and Action Buttons
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.58f)
                .padding(start = 36.dp, top = 36.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NetflixRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HHKUNGFU ĐẶC BIỆT",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (movie.quality.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = movie.quality,
                        color = NetflixGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (movie.latestEpisode.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${movie.latestEpisode}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = movie.title,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (movie.originalTitle.isNotEmpty()) {
                Text(
                    text = movie.originalTitle,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                FocusableTvItem(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(6.dp),
                    focusedScale = 1.05f
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(if (isFocused) Color.White else NetflixRed)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Xem",
                            tint = if (isFocused) Color.Black else Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Xem Ngay",
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Detail Button
                FocusableTvItem(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(6.dp),
                    focusedScale = 1.05f
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(if (isFocused) Color(0x66FFFFFF) else Color(0x33FFFFFF))
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Chi tiết",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi Tiết",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
