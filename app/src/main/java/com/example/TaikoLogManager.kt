package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TaikoLogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val formatted = "[$timestamp] $message"
        _logs.update { current ->
            (current + formatted).takeLast(100) // Keep the last 100 log entries to avoid memory bloat
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
