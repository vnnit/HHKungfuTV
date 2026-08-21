package com.hhkungfu.tv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhkungfu.tv.data.model.HomeSection
import com.hhkungfu.tv.data.model.MovieItem
import com.hhkungfu.tv.data.model.ScheduleDay
import com.hhkungfu.tv.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val heroMovie: MovieItem?,
        val sections: List<HomeSection>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val scheduleDays: List<ScheduleDay> = repository.scheduleDays

    private val _selectedDayId = MutableStateFlow(getTodayDayId())
    val selectedDayId: StateFlow<String> = _selectedDayId.asStateFlow()

    private val _scheduleMovies = MutableStateFlow<List<MovieItem>>(emptyList())
    val scheduleMovies: StateFlow<List<MovieItem>> = _scheduleMovies.asStateFlow()

    private val _isScheduleLoading = MutableStateFlow(false)
    val isScheduleLoading: StateFlow<Boolean> = _isScheduleLoading.asStateFlow()

    init {
        loadHomeData()
        selectScheduleDay(getTodayDayId())
    }

    private fun getTodayDayId(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "chu-nhat"
            Calendar.MONDAY -> "thu-2"
            Calendar.TUESDAY -> "thu-3"
            Calendar.WEDNESDAY -> "thu-4"
            Calendar.THURSDAY -> "thu-5"
            Calendar.FRIDAY -> "thu-6"
            Calendar.SATURDAY -> "thu-7"
            else -> "thu-4"
        }
    }

    fun selectScheduleDay(dayId: String) {
        _selectedDayId.value = dayId
        viewModelScope.launch {
            _isScheduleLoading.value = true
            try {
                val movies = repository.getSchedule(dayId)
                _scheduleMovies.value = movies
            } catch (_: Exception) {
                _scheduleMovies.value = emptyList()
            } finally {
                _isScheduleLoading.value = false
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val (hero, sections) = repository.getHomePage()
                _uiState.value = HomeUiState.Success(heroMovie = hero, sections = sections)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Lỗi tải trang chủ")
            }
        }
    }
}
