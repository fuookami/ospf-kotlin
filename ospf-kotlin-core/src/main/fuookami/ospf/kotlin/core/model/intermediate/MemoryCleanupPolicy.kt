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

    private fun enabled(): Boolean {
        return System.getProperty(EnabledProperty)?.toBoolean() ?: false
    }

    private fun threshold(): Double {
        return System.getProperty(ThresholdProperty)?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.8
    }

    private fun aggressive(): Boolean {
        return System.getProperty(AggressiveProperty)?.toBoolean() ?: false
    }

    private fun shouldCleanup(): Boolean {
        return memoryUseOver(threshold())
    }

    private fun cleanupIfNeeded(shouldCleanup: Boolean) {
        if (shouldCleanup) {
            System.gc()
        }
    }

    fun cleanupAfterModelBuilt() {
        if (!enabled()) {
            return
        }
        cleanupIfNeeded(aggressive() || shouldCleanup())
    }

    fun cleanupOnPressure() {
        if (!enabled()) {
            return
        }
        cleanupIfNeeded(shouldCleanup())
    }

    fun cleanupAfterBatch() {
        cleanupOnPressure()
    }

    fun cleanupAfterAsyncBatch() {
        cleanupOnPressure()
    }

    fun cleanupBeforeConcurrentBatch() {
        cleanupOnPressure()
    }

    fun cleanupAfterSymbolRegistration() {
        cleanupAfterModelBuilt()
    }
}
