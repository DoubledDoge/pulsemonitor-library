package io.github.doubleddoge.pulsemonitor.models

data class PerformanceSnapshot (
    val memory: MemoryStats,
    val cpu: CpuStats
)