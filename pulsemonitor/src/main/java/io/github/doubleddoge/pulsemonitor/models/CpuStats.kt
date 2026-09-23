package io.github.doubleddoge.pulsemonitor.models

data class CpuStats(
    val usagePercent: Double,
    val mainThreadPercent: Double,
    val backgroundPercent: Double,
    val cpuTimeMs: Long
)