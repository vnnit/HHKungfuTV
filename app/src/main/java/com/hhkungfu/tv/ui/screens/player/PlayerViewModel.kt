package com.hhkungfu.tv.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhkungfu.tv.data.model.ServerOption
import com.hhkungfu.tv.data.model.StreamSource
import com.hhkungfu.tv.data.repository.MovieRepository
import com.hhkungfu.tv.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PlayerUiState {
    data object Loading : PlayerUiState()
    data class Ready(
        val streamSource: StreamSource,
        val movieTitle: String,
        val episodeName: String,
        val currentServer: String,
        val sv: String = "1"
    ) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

class PlayerViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentPostId: String = ""
    private var currentChapterSt: String = ""
    private var currentMovieTitle: String = ""
    private var currentEpisodeName: String = ""
    private var currentServerType: String = Constants.SERVER_PRO
    private var currentSv: String = "1"

    val serverOptions: List<ServerOption> = repository.serverOptions

    fun loadStream(
        postId: String,
        chapterSt: String,
        movieTitle: String,
        episodeName: String,
        serverType: String,
        sv: String = "1"
    ) {
        currentPostId = postId
        currentChapterSt = chapterSt
        currentMovieTitle = movieTitle
        currentEpisodeName = episodeName
        currentServerType = serverType
        currentSv = sv

        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val source = repository.getStreamSource(postId, chapterSt, serverType, sv)
                if (source.embedUrl.isNotEmpty()) {
                    _uiState.value = PlayerUiState.Ready(
                        streamSource = source,
                        movieTitle = movieTitle,
                        episodeName = episodeName,
                        currentServer = serverType,
                        sv = sv
                    )
                } else {
                    _uiState.value = PlayerUiState.Error("Không tìm thấy luồng phát của tập này")
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(e.localizedMessage ?: "Lỗi tải luồng video")
            }
        }
    }

    fun switchServer(serverType: String) {
        if (serverType != currentServerType) {
            loadStream(currentPostId, currentChapterSt, currentMovieTitle, currentEpisodeName, serverType, currentSv)
        }
    }
}
