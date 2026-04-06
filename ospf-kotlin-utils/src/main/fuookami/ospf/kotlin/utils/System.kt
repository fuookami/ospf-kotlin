/**
 * 系统工具函数
 *
 * System utility functions for memory management and monitoring.
 * 用于内存管理和监控的系统工具函数。
 */
package fuookami.ospf.kotlin.utils

/**
 * 检查内存使用是否超过阈值
 *
 * Checks if the current memory usage exceeds the specified threshold.
 * 检查当前内存使用是否超过指定的阈值。
 *
 * This function calculates the used memory as (total memory - free memory)
 * and compares it against the maximum memory multiplied by the threshold.
 * 此函数将已用内存计算为（总内存 - 空闲内存），
 * 并将其与最大内存乘以阈值进行比较。
 *
 * @param threshold 内存使用阈值，默认 0.8（80%）/ Memory usage threshold, default 0.8 (80%)
 * @return 如果内存使用超过阈值则返回 true，否则返回 false /
 *         True if memory usage exceeds threshold, false otherwise
 */
fun memoryUseOver(threshold: Double = 0.8): Boolean {
    val memoryThreshold = Runtime.getRuntime().maxMemory().toDouble() * threshold
    val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toDouble()
    return usedMemory >= memoryThreshold
}