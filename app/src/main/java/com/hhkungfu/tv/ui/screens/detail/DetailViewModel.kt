package com.hhkungfu.tv.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhkungfu.tv.data.model.Episode
import com.hhkungfu.tv.data.model.MovieDetail
import com.hhkungfu.tv.data.model.ServerOption
import com.hhkungfu.tv.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val movie: MovieDetail,
        val serverOptions: List<ServerOption>,
        val selectedServer: ServerOption
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentMovieUrl: String = ""

    fun loadMovieDetail(movieUrl: String) {
        currentMovieUrl = movieUrl
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val detail = repository.getMovieDetail(movieUrl)
                val servers = repository.serverOptions
                val defaultServer = servers.first()
                _uiState.value = DetailUiState.Success(
                    movie = detail,
                    serverOptions = servers,
                    selectedServer = defaultServer
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.localizedMessage ?: "Không thể tải chi tiết phim")
            }
        }
    }

    fun selectServer(server: ServerOption) {
        val currentState = _uiState.value
        if (currentState is DetailUiState.Success) {
            _uiState.value = currentState.copy(selectedServer = server)
        }
    }
}
