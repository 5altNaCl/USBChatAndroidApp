package com.example.usbchatapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.usbchatapp.ui.theme.USBChatAppTheme

data class ChatScreenUiState(
    val content: String
)

@Composable
fun ChatScreen(
    viewModel: ChatScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ChatScreen(uiState = uiState)
}

@Composable
private fun ChatScreen(
    uiState: ChatScreenUiState
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.content,
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}

@Preview
@Composable
private fun PreviewChatScreen() {
    val uiState = ChatScreenUiState(content = "test")
    USBChatAppTheme {
        ChatScreen(uiState = uiState)
    }
}