package com.example.profiler_overlay.models

data class MemoryStats (
    val usedBytes: Long,
    val availableBytes: Long,
    val totalBytes: Long
)