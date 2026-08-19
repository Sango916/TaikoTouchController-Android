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

    private val dateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        }
    }
    private val reusableDate = object : ThreadLocal<Date>() {
        override fun initialValue(): Date = Date()
    }

    fun log(message: String) {
        val now = System.currentTimeMillis()
        val date = reusableDate.get() ?: Date()
        date.time = now
        val df = dateFormatThreadLocal.get() ?: SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = df.format(date)
        val formatted = "[$timestamp] $message"
        _logs.update { current ->
            (current + formatted).takeLast(100) // Keep the last 100 log entries to avoid memory bloat
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
