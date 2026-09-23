package io.github.doubleddoge.pulsemonitor.metrics.cpu

import com.example.profiler_overlay.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.CpuStats
import java.io.File
import android.os.SystemClock
import android.os.Process

// Collects CPU data and uses MetricCollector
class CpuCollector : MetricCollector<CpuStats> {

    // Number of CPU cores on the device, used to keep usage between 0 and 100%
    private val cores = Runtime.getRuntime().availableProcessors()

    // How much CPU time the app had used when this collector was created
    private val startCpuTime = Process.getElapsedCpuTime()

    // Values from the previous reading, so we can compare against them
    private var lastCpuTime = startCpuTime                   // App CPU time last reading
    private var lastWallTime = SystemClock.elapsedRealtime() // Real clock time last reading
    private var lastMainTicks = 0L                           // Main thread work last reading
    private var lastTotalTicks = 0L                          // Whole app work last reading

    override fun collect(): CpuStats {

        // How much CPU time the app has used in total so far (milliseconds)
        val cpuTime = Process.getElapsedCpuTime()

        // Current real time on the clock (milliseconds)
        val wallTime = SystemClock.elapsedRealtime()

        // CPU usage % = CPU time used since last reading ÷ real time passed
        // Divided by cores so a phone with 8 cores still tops out at 100%
        // maxOf(1, ...) stops dividing by zero if called twice instantly
        val usage = (cpuTime - lastCpuTime) * 100.0 /
                (maxOf(1, wallTime - lastWallTime) * cores)

        // Work done by the main thread (its ID matches the app's process ID)
        val mainTicks = readTicks("/proc/self/task/${Process.myPid()}/stat")

        // Work done by the whole app (all threads together)
        val totalTicks = readTicks("/proc/self/stat")

        // Share of the work the main thread did since last reading (0.0 to 1.0)
        // coerceIn keeps it in range in case a number comes out odd
        val mainShare = ((mainTicks - lastMainTicks).toDouble() /
                maxOf(1, totalTicks - lastTotalTicks)).coerceIn(0.0, 1.0)

        // Save this reading so the next collect() can compare against it
        lastCpuTime = cpuTime
        lastWallTime = wallTime
        lastMainTicks = mainTicks
        lastTotalTicks = totalTicks

        // Split total usage into main thread and background
        val mainPercent = usage * mainShare

        // Send back the finished reading
        return CpuStats(
            usagePercent = usage,
            mainThreadPercent = mainPercent,
            backgroundPercent = usage - mainPercent,  // Whatever isn't main thread
            cpuTimeMs = cpuTime - startCpuTime        // CPU time used this session
        )
    }

    // Reads a Linux "stat" file and returns how much CPU work was done
    // (user time + system time, in "ticks")
    private fun readTicks(path: String): Long = runCatching {

        // Grab the text after the ")" since the app name before it can contain spaces
        // Then split the rest into separate values
        val fields = File(path).readText().substringAfterLast(")").trim().split(" ")

        // Position 11 = time spent running app code
        // Position 12 = time spent in the system on the app's behalf
        fields[11].toLong() + fields[12].toLong()

    }.getOrDefault(0L)  // If the file can't be read, return 0 instead of crashing

}