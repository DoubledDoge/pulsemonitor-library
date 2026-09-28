package io.github.doubleddoge.pulsemonitor.models

data class MemoryStats (

    // ---------------------------------------------------------
    // Application memory
    // ---------------------------------------------------------

    // Proportional Set Size
    // The main overall app RAM
    val totalPssBytes: Long,

    // Resident Set Size
    // The amount of the app's memory currently in RAM.
    val rssBytes: Long?,

    // Memory mainly private to this process.
    val privateDirtyBytes: Long,

    // ---------------------------------------------------------
    // Heap memory
    // ---------------------------------------------------------

    // PSS attributed to the Dalvik/ART heap.
    val javaHeapPssBytes: Long,

    // PSS attributed to the native heap.
    val nativeHeapPssBytes: Long,

    // ---------------------------------------------------------
    // Session statistics
    // ---------------------------------------------------------

    // Highest PSS recorded since collection started.
    val peakPssBytes: Long,

    // Lowest PSS recorded since collection started.
    val minimumPssBytes: Long,

    // Average PSS recorded since collection started.
    val averagePssBytes: Long,

    // Difference between current PSS and the first measurement.
    val pssChangeBytes: Long,

    // ---------------------------------------------------------
    // Device memory
    // ---------------------------------------------------------

    // RAM currently available to the entire device.
    val deviceAvailableBytes: Long,

    // Total physical RAM on the device.
    val deviceTotalBytes: Long,

    // Whether Android currently considers the device
    // to be in a low-memory condition.
    val deviceLowMemory: Boolean
)