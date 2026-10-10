package io.github.doubleddoge.pulsemonitor.metrics.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import io.github.doubleddoge.pulsemonitor.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.MemoryStats

/**
 * Collects exactly 5 memory metrics, without ActivityManager.getProcessMemoryInfo().
 *
 *  Process PSS                 -> Debug.getPss()
 *  Java/Kotlin heap used       -> Runtime.totalMemory() - Runtime.freeMemory()
 *  Native heap allocated       -> Debug.getNativeHeapAllocatedSize()
 *  Java heap utilization (%)   -> heap used / Runtime.maxMemory() * 100
 *  System available RAM        -> ActivityManager.getMemoryInfo().availMem
 */
class MemoryCollector(context: Context) : MetricCollector<MemoryStats> {

    private val activityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val runtime = Runtime.getRuntime()

    // Reused on every call to avoid allocating twice a second.
    private val deviceMemoryInfo = ActivityManager.MemoryInfo()

    private var _sessionPeakPssMB = 0.00

    @Synchronized
    override fun collect(): MemoryStats {

        // Process PSS (Debug.getPss() returns KB)
        val _totalPssMB = Debug.getPss() * 1024.00    // *1024 to convert to from KB to MB

        // Update Peak PSS for this session
        if (_totalPssMB > _sessionPeakPssMB){
            _sessionPeakPssMB = _totalPssMB
        }

        // Java/Kotlin heap used + utilization against the configured maximum
        val javaHeapUsedBytes = runtime.totalMemory() - runtime.freeMemory()
        val javaHeapMaxBytes = runtime.maxMemory()
        val javaHeapUtilizationPercent =
            if (javaHeapMaxBytes > 0) javaHeapUsedBytes * 100.0 / javaHeapMaxBytes else 0.0

        // Native heap allocated (already in bytes)
        val nativeHeapAllocatedBytes = Debug.getNativeHeapAllocatedSize()

        // System available RAM
        activityManager.getMemoryInfo(deviceMemoryInfo)
        val systemAvailableBytes = deviceMemoryInfo.availMem

        return MemoryStats(
            totalPssMb = _totalPssMB,
            sessionPeakPss = _sessionPeakPssMB,
            javaHeapUsedMb = javaHeapUsedBytes.toMb(),
            nativeHeapAllocatedMb = nativeHeapAllocatedBytes.toMb(),
            javaHeapUtilizationPercent = javaHeapUtilizationPercent,
            systemAvailableRamMb = systemAvailableBytes.toMb()
        )
    }

    private fun Long.toMb(): Double = this / (1024.0 * 1024.0)
}