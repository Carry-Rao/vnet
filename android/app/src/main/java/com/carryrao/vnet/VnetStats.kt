package com.carryrao.vnet

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VnetStats {
    const val HISTORY_SIZE = 30

    data class Snapshot(
        val txBytes: Long = 0,
        val rxBytes: Long = 0,
        val txSpeed: Long = 0,
        val rxSpeed: Long = 0,
        val latencyMs: Int = 0,
        val connected: Boolean = false,
        val txSpeedHistory: List<Long> = emptyList(),
        val rxSpeedHistory: List<Long> = emptyList()
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot

    private var txTotal = 0L
    private var rxTotal = 0L
    private var txWindow = 0L
    private var rxWindow = 0L
    private var latency = 0
    private var connected = false

    private var txSpeedHistory = mutableListOf<Long>()
    private var rxSpeedHistory = mutableListOf<Long>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var speedJob: Job? = null

    fun start() {
        txTotal = 0; rxTotal = 0; txWindow = 0; rxWindow = 0; latency = 0; connected = true
        txSpeedHistory.clear()
        rxSpeedHistory.clear()
        emit()
        speedJob = scope.launch {
            while (isActive) {
                delay(1000)
                val txs = txWindow
                val rxs = rxWindow
                txWindow = 0
                rxWindow = 0
                txSpeedHistory.add(txs)
                rxSpeedHistory.add(rxs)
                if (txSpeedHistory.size > HISTORY_SIZE) txSpeedHistory.removeAt(0)
                if (rxSpeedHistory.size > HISTORY_SIZE) rxSpeedHistory.removeAt(0)
                _snapshot.value = Snapshot(txTotal, rxTotal, txs, rxs, latency, true, txSpeedHistory.toList(), rxSpeedHistory.toList())
            }
        }
    }

    fun stop() {
        speedJob?.cancel()
        connected = false
        emit()
    }

    fun addTx(n: Int) {
        txTotal += n
        txWindow += n
    }

    fun addRx(n: Int) {
        rxTotal += n
        rxWindow += n
    }

    fun setLatency(ms: Int) {
        latency = ms
    }

    private fun emit() {
        _snapshot.value = Snapshot(txTotal, rxTotal, txWindow, rxWindow, latency, connected, txSpeedHistory.toList(), rxSpeedHistory.toList())
    }
}
