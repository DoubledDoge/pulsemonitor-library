package io.github.doubleddoge.pulsemonitor.models

/**
 * The 5 memory metrics Android developers check most often.
 * (Fields marked "context" exist only to make a metric readable.)
 */
data class MemoryStats(

    // 1. Total PSS - the app's real memory footprint (shared pages split fairly)
    val totalPssBytes: Long,
    //val pssChangeBytes: Long,

    // 2. Java heap used - what your Kotlin/Java objects occupy
    val javaHeapUsedBytes: Long,
    val javaHeapMaxBytes: Long,          // context: the OutOfMemoryError limit

    // 3. Native heap allocated - C/C++ allocations, direct buffers, bitmap pixels (API 26+)
    val nativeHeapAllocatedBytes: Long,

    // 4. Peak PSS this session - worst-case footprint, catches short spikes
    val peakPssBytes: Long,

    // 5. Device memory pressure - is the system about to start killing processes?
    val deviceAvailableBytes: Long,
    val deviceTotalBytes: Long,          // context
    val deviceLowMemory: Boolean
)