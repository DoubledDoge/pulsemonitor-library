package io.github.doubleddoge.pulsemonitor.metrics.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import io.github.doubleddoge.pulsemonitor.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.MemoryStats

/**
 * Collects memory metrics WITHOUT ActivityManager.getProcessMemoryInfo().
 *
 *  PSS            -> Debug.getPss()                         (in-process, no call to system_server)
 *  Java heap      -> Runtime.totalMemory() - freeMemory()   (microseconds)
 *  Java heap max  -> Runtime.maxMemory()
 *  Native heap    -> Debug.getNativeHeapAllocatedSize()     (mallinfo, cheap)
 *  Device memory  -> ActivityManager.getMemoryInfo()        (lightweight, not the smaps-based call)
 */
class MemoryCollector(context: Context) : MetricCollector<MemoryStats> {

    private val activityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val runtime = Runtime.getRuntime()

    // Reused every call so we don't allocate 2 times a second.
    private val deviceMemoryInfo = ActivityManager.MemoryInfo()

    // Session statistic
    private var peakPssBytes = 0L

    @Synchronized
    override fun collect(): MemoryStats {

        // 1. PSS (Debug.getPss() returns KB)
        val pssBytes = Debug.getPss() * 1024L

        // 2. Java heap
        val javaHeapUsedBytes = runtime.totalMemory() - runtime.freeMemory()
        val javaHeapMaxBytes = runtime.maxMemory()

        // 3. Native heap (already in bytes)
        val nativeHeapAllocatedBytes = Debug.getNativeHeapAllocatedSize()

        // 4. Peak PSS
        if (pssBytes > peakPssBytes) peakPssBytes = pssBytes

        // 5. Device pressure
        activityManager.getMemoryInfo(deviceMemoryInfo)

        return MemoryStats(
            totalPssBytes = pssBytes,
            javaHeapUsedBytes = javaHeapUsedBytes,
            javaHeapMaxBytes = javaHeapMaxBytes,
            nativeHeapAllocatedBytes = nativeHeapAllocatedBytes,
            peakPssBytes = peakPssBytes,
            deviceAvailableBytes = deviceMemoryInfo.availMem,
            deviceTotalBytes = deviceMemoryInfo.totalMem,
            deviceLowMemory = deviceMemoryInfo.lowMemory
        )
    }

    /** Resets the session peak (e.g. a "Reset Session" button). */
    @Synchronized
    fun reset() {
        peakPssBytes = 0L
    }
}