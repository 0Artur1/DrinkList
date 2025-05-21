package com.example.drinkopedia.viewmodel


import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private var timerJob: Job? = null


    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun setMinutes(minutes: Int) {
        if (_timerState.value.isRunning) return

        val newTotalSeconds = minutes * 60 + (_timerState.value.totalSeconds % 60)

        _timerState.value = _timerState.value.copy(
            totalSeconds = newTotalSeconds,
            currentSeconds = newTotalSeconds
        )
    }

    fun setSeconds(seconds: Int) {
        if (_timerState.value.isRunning) return

        val newTotalSeconds = (_timerState.value.totalSeconds / 60) * 60 + seconds

        _timerState.value = _timerState.value.copy(
            totalSeconds = newTotalSeconds,
            currentSeconds = newTotalSeconds
        )
    }

    fun startTimer() {
        if (_timerState.value.isRunning || _timerState.value.currentSeconds <= 0) return

        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob = viewModelScope.launch {
            while (_timerState.value.currentSeconds > 0 && _timerState.value.isRunning) {
                delay(1000)
                _timerState.value = _timerState.value.copy(
                    currentSeconds = _timerState.value.currentSeconds - 1
                )
            }

            if (_timerState.value.currentSeconds <= 0) {
                _timerState.value = _timerState.value.copy(isRunning = false)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            currentSeconds = _timerState.value.totalSeconds
        )
    }

    fun clearTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    data class TimerState(
        val totalSeconds: Int = 0,
        val currentSeconds: Int = 0,
        val isRunning: Boolean = false
    ) {
        val minutes: Int
            get() = currentSeconds / 60

        val seconds: Int
            get() = currentSeconds % 60

        val formattedTime: String
            get() = String.format("%02d:%02d", minutes, seconds)
    }
}