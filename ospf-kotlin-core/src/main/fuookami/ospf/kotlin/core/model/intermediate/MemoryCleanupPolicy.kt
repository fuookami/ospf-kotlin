/**
 * 内存清理策略
 * Memory cleanup policy
*/
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.memoryUseOver

/**
 * 内存清理策略对象，通过系统属性控制何时触发 GC。
 * Memory cleanup policy object controlling when to trigger GC via system properties.
*/
internal object MemoryCleanupPolicy {
    private const val EnabledProperty = "ospf.core.model.memory.cleanup.enabled"
    private const val ThresholdProperty = "ospf.core.model.memory.cleanup.threshold"
    private const val AggressiveProperty = "ospf.core.model.memory.cleanup.aggressive"

    /** 检查内存清理是否启用
     * @return 内存清理是否启用
     * Check whether memory cleanup is enabled
     * @return whether memory cleanup is enabled
    */
    private fun enabled(): Boolean {
        return System.getProperty(EnabledProperty)?.toBoolean() ?: false
    }

    /** 获取内存使用阈值（0.0-1.0）
     * @return 内存使用阈值，默认 0.8
     * Get memory usage threshold (0.0-1.0)
     * @return memory usage threshold, defaults to 0.8
    */
    private fun threshold(): Double {
        return System.getProperty(ThresholdProperty)?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.8
    }

    /** 检查是否启用激进清理模式
     * @return 是否启用激进清理模式
     * Check whether aggressive cleanup mode is enabled
     * @return whether aggressive cleanup mode is enabled
    */
    private fun aggressive(): Boolean {
        return System.getProperty(AggressiveProperty)?.toBoolean() ?: false
    }

    /** 判断当前内存使用是否超过阈值
     * @return 当前内存使用是否超过阈值
     * Check whether current memory usage exceeds the threshold
     * @return whether current memory usage exceeds the threshold
    */
    private fun shouldCleanup(): Boolean {
        return memoryUseOver(threshold())
    }

    /** 根据条件执行 GC
     * @param shouldCleanup 是否需要执行清理
     * Trigger GC if the condition is met
    */
    private fun cleanupIfNeeded(shouldCleanup: Boolean) {
        if (shouldCleanup) {
            System.gc()
        }
    }

    /** 模型构建完成后执行内存清理（激进模式下无条件触发）。 / Perform memory cleanup after model is built (always triggered in aggressive mode). */
    fun cleanupAfterModelBuilt() {
        if (!enabled()) {
            return
        }
        cleanupIfNeeded(aggressive() || shouldCleanup())
    }

    /** 仅在内存压力超过阈值时执行清理。 / Perform cleanup only when memory pressure exceeds the threshold. */
    fun cleanupOnPressure() {
        if (!enabled()) {
            return
        }
        cleanupIfNeeded(shouldCleanup())
    }

    /** 批处理完成后按内存压力策略清理。 / Cleanup after batch processing based on memory pressure policy. */
    fun cleanupAfterBatch() {
        cleanupOnPressure()
    }

    /** 异步批处理完成后按内存压力策略清理。 / Cleanup after async batch processing based on memory pressure policy. */
    fun cleanupAfterAsyncBatch() {
        cleanupOnPressure()
    }

    /** 并发批处理开始前按内存压力策略清理。 / Cleanup before concurrent batch processing based on memory pressure policy. */
    fun cleanupBeforeConcurrentBatch() {
        cleanupOnPressure()
    }

    /** 符号注册完成后执行内存清理（与模型构建策略一致）。 / Cleanup after symbol registration (same policy as model-built cleanup). */
    fun cleanupAfterSymbolRegistration() {
        cleanupAfterModelBuilt()
    }
}
