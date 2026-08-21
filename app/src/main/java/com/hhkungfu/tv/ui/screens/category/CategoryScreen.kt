package com.hhkungfu.tv.ui.screens.category

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.components.MovieCard
import com.hhkungfu.tv.ui.components.TvSidebar
import com.hhkungfu.tv.ui.theme.NetflixBlack
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary

@Composable
fun CategoryScreen(
    slug: String,
    title: String,
    onMovieClick: (MovieItem) -> Unit,
    onNavigateHome: () -> Unit,
    onSearchClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onSelectOtherCategory: (String, String) -> Unit,
    viewModel: CategoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(slug) {
        viewModel.loadCategory(slug, 1)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // Sidebar
        TvSidebar(
            selectedId = slug,
            onItemSelected = { item ->
                when (item.id) {
                    "home" -> onNavigateHome()
                    "history" -> onHistoryClick()
                    "search" -> onSearchClick()
                    else -> onSelectOtherCategory(item.slug, item.title)
                }
            }
        )

        // Category Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusableTvItem(
                    onClick = onNavigateHome,
                    shape = RoundedCornerShape(50),
                    focusedScale = 1.1f
                ) { isFocused ->
                    Box(
                        modifier = Modifier
                            .background(if (isFocused) NetflixRed else Color(0x66000000))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title.uppercase(),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid Movies
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is CategoryUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = NetflixRed)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Đang tải danh sách phim...", color = TextSecondary)
                        }
                    }

                    is CategoryUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message, color = NetflixRed, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            FocusableTvItem(
                                onClick = { viewModel.loadCategory(slug) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(NetflixRed)
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(text = "Thử lại", color = Color.White)
                                }
                            }
                        }
                    }

                    is CategoryUiState.Success -> {
                        if (state.movies.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Không có phim nào trong mục này",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Vui lòng chọn danh mục khác hoặc quay lại Trang Chủ.",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                FocusableTvItem(
                                    onClick = onNavigateHome,
                                    shape = RoundedCornerShape(6.dp)
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (isFocused) Color.White else NetflixRed)
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Về Trang Chủ",
                                            color = if (isFocused) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 150.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(state.movies, key = { it.url }) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie) }
                                        )
                                    }
                                }

                                // Pagination Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.currentPage > 1) {
                                        FocusableTvItem(
                                            onClick = { viewModel.prevPage() },
                                            shape = RoundedCornerShape(6.dp)
                                        ) { isFocused ->
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isFocused) NetflixRed else Color(0xFF333333))
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(text = "← Trang trước", color = Color.White, fontSize = 13.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }

                                    Text(
                                        text = "Trang ${state.currentPage}",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    FocusableTvItem(
                                        onClick = { viewModel.nextPage() },
                                        shape = RoundedCornerShape(6.dp)
                                    ) { isFocused ->
                                        Box(
                                            modifier = Modifier
                                                .background(if (isFocused) NetflixRed else Color(0xFF333333))
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(text = "Trang sau →", color = Color.White, fontSize = 13.sp)
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
