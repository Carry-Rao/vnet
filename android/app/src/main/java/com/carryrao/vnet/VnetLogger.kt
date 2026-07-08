package com.carryrao.vnet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object VnetLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val buffer = CopyOnWriteArrayList<String>()
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private const val MAX_LINES = 200

    fun log(tag: String, msg: String) {
        val line = "${sdf.format(Date())} [$tag] $msg"
        buffer.add(line)
        while (buffer.size > MAX_LINES) {
            buffer.removeAt(0)
        }
        _logs.value = buffer.toList()
    }

    fun clear() {
        buffer.clear()
        _logs.value = emptyList()
    }
}
