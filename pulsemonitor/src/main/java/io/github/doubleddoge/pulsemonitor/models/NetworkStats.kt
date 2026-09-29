package io.github.doubleddoge.pulsemonitor.models

data class NetworkStats (
    val receivedBytes: Long,        //Download this session
    val sentBytes: Long,            // Uploaded this session
    val bytesPerSecond: Long        // Current speed
)