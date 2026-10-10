package io.github.doubleddoge.pulsemonitor.metrics.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import io.github.doubleddoge.pulsemonitor.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.MemoryStats

class MemoryCollector(context: Context) : MetricCollector<MemoryStats> {

    private val activityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val runtime = Runtime.getRuntime()

    // Reused on every call to avoid allocating twice a second.
    private val deviceMemoryInfo = ActivityManager.MemoryInfo()

    private var _sessionPeakPssMB = 0.00

    @Synchronized
    override fun collect(): MemoryStats {

        // Total Process PSS in KB
        val _totalPssMB = Debug.getPss()  / 1024.00  // convert to MB

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
            sessionPeakPssMB = _sessionPeakPssMB,
            javaHeapUsedMb = javaHeapUsedBytes.toMb(),
            nativeHeapAllocatedMb = nativeHeapAllocatedBytes.toMb(),
            javaHeapUtilizationPercent = javaHeapUtilizationPercent,
            systemAvailableRamMb = systemAvailableBytes.toMb()
        )
    }

    private fun Long.toMb(): Double = this / (1024.0 * 1024.0)
}