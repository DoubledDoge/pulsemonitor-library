package io.github.doubleddoge.pulsemonitor.metrics.network

import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import io.github.doubleddoge.pulsemonitor.metrics.MetricCollector
import io.github.doubleddoge.pulsemonitor.models.NetworkStats

class NetworkCollector : MetricCollector<NetworkStats>{

    companion object {
        private const val MS_PER_SECOND = 1000L
        private const val MIN_ELAPSED_MS = 1L       // Stops dividing by zero
        private const val NO_BYTES = 0L             //lowest a reading can be
        private val UNSUPPORTED = TrafficStats.UNSUPPORTED.toLong() //Android's can't tell value (-1)
    }

    //Apps ID
    private val uid = Process.myUid()

    // starting counter , count when collector started
    private val startReceived = TrafficStats.getUidRxBytes(uid).coerceAtLeast(NO_BYTES)
    private var startSent = SystemClock.elapsedRealtime().coerceAtLeast(NO_BYTES)

    //last reading, used to work out speed
    private var lastReceived = startReceived
    private var lastSent = startSent
    private var lastTotalBytes = startReceived + startSent
    private var lastTime = SystemClock.elapsedRealtime()

    override fun collect(): NetworkStats {

        //read the numbers now
        val received = readOrLast(TrafficStats.getUidRxBytes(uid), lastReceived)
        val sent = readOrLast(TrafficStats.getUidTxBytes(uid), lastSent)
        val now = SystemClock.elapsedRealtime()

        //Speed = bytes moved since last time / time passed
        val totalBytes = received + sent
        val bytesMoved = totalBytes - lastTotalBytes
        val timePassed = maxOf(MIN_ELAPSED_MS, now - lastTime)
        val speed = bytesMoved * MS_PER_SECOND / timePassed

        //Remember this reading for next time
        lastReceived = received
        lastSent = sent
        lastTotalBytes = totalBytes
        lastTime = now

        //Fill the box
        return NetworkStats(
            receivedBytes = (received - startReceived).coerceAtLeast(NO_BYTES),
            sentBytes = (sent - startSent).coerceAtLeast(NO_BYTES),
            bytesPerSecond = speed.coerceAtLeast(NO_BYTES)
        )
    }
    //Returns the new reading, or the last good one if Android returned -1
    private fun readOrLast(value: Long, last: Long): Long =
        if (value == UNSUPPORTED) last else value

}