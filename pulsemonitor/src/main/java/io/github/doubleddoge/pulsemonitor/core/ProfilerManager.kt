package com.example.profiler_overlay.core

import android.content.Context
import android.util.Log

import com.example.profiler_overlay.metrics.memory.MemoryCollector
import com.example.profiler_overlay.models.PerformanceSnapshot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProfilerManager(context: Context) {
    // Fetches the RAM information
    private val memoryCollector = MemoryCollector(context.applicationContext)

    // Mutable state flow - an object that holds the latest performance Snapshot.
    // Kept private from the overlay so that the overlay doesn't
    // accidentally modify the data.
    private val _performance = MutableStateFlow<PerformanceSnapshot?>(null)

    // State flow - a read-only version of the data.
    // Set as public so that the overlay can read this.
    val performance: StateFlow<PerformanceSnapshot?> = _performance.asStateFlow()

    // Runs on a background dispatcher
    private val scope = CoroutineScope(Dispatchers.Default)

    // This stored a reference to the coroutine.
    // Job allows us to check the status of the coroutine.
    // Can be null - no job assigned yet.
    private var collectionJob: Job? = null

    fun start() {
        if (collectionJob?.isActive == true) return
        collectionJob = scope.launch {
            while (isActive) {
                val memoryStats = memoryCollector.collect()

                val snapshot = PerformanceSnapshot(
                    memory = memoryStats
                )

                _performance.value = snapshot

                delay(500)
            }
        }
    }

    fun stop() {
        // Cancel the running collection loop.
        collectionJob?.cancel()

        // Remove the reference to the cancelled job.
        collectionJob = null
    }

    // Use to collect all stats
    fun collectSnapshot(): PerformanceSnapshot {
        return PerformanceSnapshot(memory = memoryCollector.collect())
    }
}