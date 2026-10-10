package io.github.doubleddoge.pulsemonitor.models

/**
 * The 5 memory metrics collected by Pulse Monitor.
 * Memory values are in MB (1 MB = 1024 * 1024 bytes).
 */
data class MemoryStats(

    /** Process PSS (MB) - overall process memory consumption. */
    val totalPssMb: Double,
    val sessionPeakPss: Double,

    /** Java/Kotlin heap used (MB) - object allocation and potential leaks. */
    val javaHeapUsedMb: Double,

    /** Native heap allocated (MB) - native allocation growth. */
    val nativeHeapAllocatedMb: Double,

    /** Java heap utilization (%) - heap pressure relative to the configured maximum. */
    val javaHeapUtilizationPercent: Double,

    /** System available RAM (MB) - overall device memory pressure. */
    val systemAvailableRamMb: Double
)