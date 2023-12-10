package com.example.usbchatapp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ChatScreenViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ChatScreenUiState(content = ""))
    val uiState: StateFlow<ChatScreenUiState> = _uiState
}
