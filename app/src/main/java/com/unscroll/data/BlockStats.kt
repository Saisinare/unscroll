package com.unscroll.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Counts how many times something was blocked today. */
object BlockStats {
    private lateinit var prefs: SharedPreferences
    private val _today = MutableStateFlow(0)
    val today: StateFlow<Int> = _today

    private fun key() = "blocks_${LocalDate.now()}"

    fun init(context: Context) {
        prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
        refresh()
    }

    fun refresh() {
        _today.value = prefs.getInt(key(), 0)
    }

    fun record() {
        val n = prefs.getInt(key(), 0) + 1
        prefs.edit().putInt(key(), n).apply()
        _today.value = n
    }
}
