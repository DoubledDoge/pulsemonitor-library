package com.example.profiler_overlay.metrics

interface MetricCollector<T> {

    fun collect(): T

}