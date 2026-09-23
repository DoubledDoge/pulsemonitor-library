package io.github.doubleddoge.pulsemonitor.metrics.cpu

import com.example.profiler_overlay.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.CpuStats
import java.io.File
import android.os.SystemClock
import android.os.Process

// Collects CPU data follows the same MetricCollector
class CpuCollector : MetricCollector<CpuStats> {

    companion object {
        // Turns a fraction (0.0 to 1.0) into a percentage
        private const val PERCENT_MULTIPLIER = 100.0

        // Smallest time gap allowed, stops dividing by zero
        private const val MIN_ELAPSED_MS = 1L

        // Smallest tick gap allowed, stops dividing by zero
        private const val MIN_TICK_DELTA = 1L

        // Main thread share can only be between 0% and 100% of the work
        private const val MIN_SHARE = 0.0
        private const val MAX_SHARE = 1.0

        // Positions in the stat file (counted after the ")")
        private const val USER_TIME_INDEX = 11   // Time spent running app code
        private const val SYSTEM_TIME_INDEX = 12 // Time spent in the system for the app

        // Returned if the stat file can't be read
        private const val UNREADABLE_TICKS = 0L

        // Linux stat files
        private const val PROCESS_STAT_PATH = "/proc/self/stat"
        private const val TASK_DIR_PATH = "/proc/self/task"
    }

    // Number of CPU cores on the device, used to keep usage between 0 and 100%
    private val cores = Runtime.getRuntime().availableProcessors()

    // Path to the main thread's stat file (its ID matches the app's process ID)
    private val mainThreadStatPath = "$TASK_DIR_PATH/${Process.myPid()}/stat"

    // How much CPU time the app had used when this collector was created
    private val startCpuTime = Process.getElapsedCpuTime()

    // Values from the previous reading, so we can compare against them
    private var lastCpuTime = startCpuTime
    private var lastWallTime = SystemClock.elapsedRealtime()
    private var lastMainTicks = UNREADABLE_TICKS
    private var lastTotalTicks = UNREADABLE_TICKS

    override fun collect(): CpuStats {

        // How much CPU time the app has used in total so far (milliseconds)
        val cpuTime = Process.getElapsedCpuTime()

        // Current real time on the clock (milliseconds)
        val wallTime = SystemClock.elapsedRealtime()

        // CPU time used and real time passed since the last reading
        val cpuDelta = cpuTime - lastCpuTime
        val wallDelta = maxOf(MIN_ELAPSED_MS, wallTime - lastWallTime)

        // CPU usage % = CPU time used ÷ real time passed, spread across all cores
        val usage = cpuDelta * PERCENT_MULTIPLIER / (wallDelta * cores)

        // Work done by the main thread and by the whole app
        val mainTicks = readTicks(mainThreadStatPath)
        val totalTicks = readTicks(PROCESS_STAT_PATH)

        // Work done since the last reading
        val mainDelta = mainTicks - lastMainTicks
        val totalDelta = maxOf(MIN_TICK_DELTA, totalTicks - lastTotalTicks)

        // Share of the work the main thread did (0.0 to 1.0)
        val mainShare = (mainDelta.toDouble() / totalDelta).coerceIn(MIN_SHARE, MAX_SHARE)

        // Save this reading so the next collect() can compare against it
        lastCpuTime = cpuTime
        lastWallTime = wallTime
        lastMainTicks = mainTicks
        lastTotalTicks = totalTicks

        // Split total usage into main thread and background
        val mainPercent = usage * mainShare

        return CpuStats(
            usagePercent = usage,
            mainThreadPercent = mainPercent,
            backgroundPercent = usage - mainPercent,
            cpuTimeMs = cpuTime - startCpuTime
        )
    }

    // Reads a Linux stat file and returns user time + system time (in ticks)
    private fun readTicks(path: String): Long = runCatching {

        // Take the text after ")" since the app name before it can contain spaces
        val fields = File(path).readText().substringAfterLast(")").trim().split(" ")

        val userTime = fields[USER_TIME_INDEX].toLong()
        val systemTime = fields[SYSTEM_TIME_INDEX].toLong()

        userTime + systemTime

    }.getOrDefault(UNREADABLE_TICKS)  // If the file can't be read, return 0 instead of crashing
}