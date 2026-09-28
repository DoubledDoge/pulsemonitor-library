package io.github.doubleddoge.pulsemonitor.metrics.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import io.github.doubleddoge.pulsemonitor.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.MemoryStats

class MemoryCollector(context: Context) : MetricCollector<MemoryStats> {

    private val context = context.applicationContext
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // ---------------------------------------------------------
    // Session statistics
    // ---------------------------------------------------------

    private var firstPssBytes: Long? = null

    private var peakPssBytes = 0L

    private var minimumPssBytes = Long.MAX_VALUE

    private var totalPssSamples = 0L

    private var sampleCount = 0L


    override fun collect(): MemoryStats {

        // -----------------------------------------------------
        // Get information about THIS application's process
        // -----------------------------------------------------

        val processMemoryInfo =
            activityManager.getProcessMemoryInfo(
                intArrayOf(android.os.Process.myPid())
            )[0]


        // -----------------------------------------------------
        // Application PSS
        // -----------------------------------------------------

        val totalPssBytes =
            processMemoryInfo.totalPss * 1024L

        val privateDirtyBytes =
            processMemoryInfo.totalPrivateDirty * 1024L


        // -----------------------------------------------------
        // Heap information
        // -----------------------------------------------------

        val javaHeapPssBytes =
            processMemoryInfo.dalvikPss * 1024L

        val nativeHeapPssBytes =
            processMemoryInfo.nativePss * 1024L


        // -----------------------------------------------------
        // RSS
        // -----------------------------------------------------

        val rssBytes: Long? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Debug.getRss() * 1024L
            } else {
                null
            }

        // -----------------------------------------------------
        // Update session statistics
        // -----------------------------------------------------

        // Store the first measurement as our baseline.
        if (firstPssBytes == null) {
            firstPssBytes = totalPssBytes
        }

        // Update peak.
        if (totalPssBytes > peakPssBytes) {
            peakPssBytes = totalPssBytes
        }

        // Update minimum.
        if (totalPssBytes < minimumPssBytes) {
            minimumPssBytes = totalPssBytes
        }

        // Add this measurement to the average.
        totalPssSamples += totalPssBytes
        sampleCount++


        val averagePssBytes =
            totalPssSamples / sampleCount


        // Difference from the first measurement.
        val pssChangeBytes =
            totalPssBytes - (firstPssBytes ?: totalPssBytes)


        // -----------------------------------------------------
        // Device-wide memory
        // -----------------------------------------------------

        val deviceMemoryInfo =
            ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(deviceMemoryInfo)

        val deviceAvailableBytes =
            deviceMemoryInfo.availMem

        val deviceTotalBytes =
            deviceMemoryInfo.totalMem

        val deviceLowMemory =
            deviceMemoryInfo.lowMemory


        // -----------------------------------------------------
        // Return the complete memory snapshot
        // -----------------------------------------------------
        return MemoryStats(
            totalPssBytes = totalPssBytes,
            rssBytes = rssBytes,
            privateDirtyBytes = privateDirtyBytes,
            javaHeapPssBytes = javaHeapPssBytes,
            nativeHeapPssBytes = nativeHeapPssBytes,
            peakPssBytes = peakPssBytes,
            minimumPssBytes = minimumPssBytes,
            averagePssBytes = averagePssBytes,
            pssChangeBytes = pssChangeBytes,
            deviceAvailableBytes = deviceAvailableBytes,
            deviceTotalBytes = deviceTotalBytes,
            deviceLowMemory = deviceLowMemory
        )
    }

    /**
     * Resets all session statistics.
     * Useful if we implement a "Reset Session" button
     */
    fun reset() {
        firstPssBytes = null
        peakPssBytes = 0L
        minimumPssBytes = Long.MAX_VALUE
        totalPssSamples = 0L
        sampleCount = 0L
    }
}