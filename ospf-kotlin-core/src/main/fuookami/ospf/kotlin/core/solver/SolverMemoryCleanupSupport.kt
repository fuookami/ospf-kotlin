package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.utils.memoryUseOver

/**
 * 统一求解器侧内存压力清理入口，避免在插件中分散直接 `System.gc()` 调用。
 * Unified memory-pressure cleanup entry for solver plugins to avoid scattered direct `System.gc()` calls.
 */
fun cleanupOnSolverMemoryPressure() {
    if (memoryUseOver()) {
        System.gc()
    }
}

/**
 * 求解器主流程结束后统一执行一次清理，保持既有保守行为。
 * Perform one unified cleanup after solver main flow to keep existing conservative behavior.
 */
fun cleanupAfterSolverRun() {
    System.gc()
}
