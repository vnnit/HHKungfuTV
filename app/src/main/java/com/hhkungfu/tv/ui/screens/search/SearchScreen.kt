package com.hhkungfu.tv.ui.screens.search

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.components.MovieCard
import com.hhkungfu.tv.ui.components.TvSidebar
import com.hhkungfu.tv.ui.theme.NetflixBlack
import com.hhkungfu.tv.ui.theme.NetflixCardBg
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    onMovieClick: (MovieItem) -> Unit,
    onNavigateHome: () -> Unit,
    onCategoryClick: (String, String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()

    val popularKeywords = listOf(
        "Đấu Phá", "Hoàn Mỹ Thế Giới", "Thôn Phệ Tinh Không",
        "Tiên Nghịch", "Kiếm Lai", "Già Thiên", "Phàm Nhân Tu Tiên",
        "Thần Lan Kỳ Vực", "Vũ Canh Kỷ", "Bách Luyện Thành Thần"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // Sidebar
        TvSidebar(
            selectedId = "search",
            onItemSelected = { item ->
                when (item.id) {
                    "home" -> onNavigateHome()
                    "search" -> {}
                    else -> onCategoryClick(item.slug, item.title)
                }
            }
        )

        // Main Search Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            // Search Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(NetflixCardBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NetflixRed,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Nhập tên phim hoạt hình 3D (VD: Đấu Phá, Tiên Nghịch...)",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(NetflixRed),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (query.isNotEmpty()) {
                    FocusableTvItem(
                        onClick = { viewModel.onQueryChange("") },
                        shape = RoundedCornerShape(50),
                        focusedScale = 1.1f
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) NetflixRed else Color.Transparent)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Quick Search Suggestion Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gợi ý:",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                popularKeywords.take(5).forEach { tag ->
                    FocusableTvItem(
                        onClick = {
                            viewModel.onQueryChange(tag)
                            viewModel.performSearch(tag)
                        },
                        shape = RoundedCornerShape(16.dp),
                        focusedScale = 1.05f
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) NetflixRed else Color(0xFF2B2B2B))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (isFocused) Color.White else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Search Results Grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                when (val state = uiState) {
                    is SearchUiState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chọn từ khóa gợi ý hoặc nhập tên phim để tìm kiếm",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }

                    is SearchUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = NetflixRed)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Đang tìm phim...", color = TextSecondary)
                        }
                    }

                    is SearchUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = state.message, color = NetflixRed, fontSize = 16.sp)
                        }
                    }

                    is SearchUiState.Success -> {
                        if (state.results.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Không tìm thấy phim phù hợp với từ khóa \"$query\"",
                                    color = TextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.results, key = { it.url }) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = { onMovieClick(movie) }
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
