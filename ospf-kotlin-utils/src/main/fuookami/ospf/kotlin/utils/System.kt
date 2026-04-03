package fuookami.ospf.kotlin.utils

fun memoryUseOver(threshold: Double = 0.8): Boolean {
    val memoryThreshold = Runtime.getRuntime().maxMemory().toDouble() * threshold
    val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toDouble()
    return usedMemory >= memoryThreshold
}