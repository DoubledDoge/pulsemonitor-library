package io.github.doubleddoge.pulsemonitor.metrics

interface MetricCollector<T> {

    fun collect(): T

}