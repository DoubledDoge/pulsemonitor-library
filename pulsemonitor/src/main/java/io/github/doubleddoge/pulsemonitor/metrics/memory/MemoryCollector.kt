package com.example.profiler_overlay.metrics.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log
import com.example.profiler_overlay.metrics.MetricCollector
import com.example.profiler_overlay.models.MemoryStats

class MemoryCollector(context: Context) : MetricCollector<MemoryStats> {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun collect(): MemoryStats {

        val runtime = Runtime.getRuntime()

        val usedHeapBytes =
            runtime.totalMemory() - runtime.freeMemory()

        return MemoryStats(
            usedBytes = usedHeapBytes,
            availableBytes = 0,
            totalBytes = runtime.maxMemory()
        )
    }
}