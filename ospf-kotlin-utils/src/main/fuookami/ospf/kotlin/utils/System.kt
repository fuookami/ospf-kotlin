package fuookami.ospf.kotlin.utils

import fuookami.ospf.kotlin.utils.math.*

fun memoryUseOver(threshold: Flt64 = Flt64(0.9)): Boolean {
    val memoryThreshold = Flt64(Runtime.getRuntime().maxMemory().toDouble()) * threshold
    val usedMemory = Flt64((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toDouble())
    return usedMemory geq memoryThreshold
}
