package com.hhkungfu.tv.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoryUiState {
    data object Loading : CategoryUiState()
    data class Success(val movies: List<MovieItem>, val currentPage: Int) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

class CategoryViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private var currentSlug: String = ""
    private var currentPage: Int = 1

    fun loadCategory(slug: String, page: Int = 1) {
        currentSlug = slug
        currentPage = page
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading
            try {
                val movies = repository.getCategoryMovies(slug, page)
                _uiState.value = CategoryUiState.Success(movies, page)
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(e.localizedMessage ?: "Lỗi tải thể loại")
            }
        }
    }

    fun nextPage() {
        loadCategory(currentSlug, currentPage + 1)
    }

    fun prevPage() {
        if (currentPage > 1) {
            loadCategory(currentSlug, currentPage - 1)
        }
    }
}
