/** 求解器内存清理支持 / Solver memory cleanup support */
package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.utils.memoryUseOver

/**
 * `core.solver` 的插件支持 API：求解器生命周期清理入口。
 * Plugin support APIs in `core.solver` for solver-lifecycle cleanup.
 *
 * 目标：将插件中的清理策略收敛到命名化 helper，避免 `System.gc()` 分散直调。
 * Goal: converge plugin cleanup strategy through named helpers and avoid scattered direct `System.gc()` calls.
 *
 * 非目标：不承诺即时回收，也不替代 JVM/GC 调优参数。
 * Non-goal: does not guarantee immediate reclamation or replace JVM/GC tuning options.
*/

/**
 * 在内存压力触发阈值后执行保守清理。
 * Perform conservative cleanup when memory-pressure threshold is met.
*/
fun cleanupOnSolverMemoryPressure() {
    if (memoryUseOver()) {
        System.gc()
    }
}

/**
 * 在求解主流程结束后执行一次统一清理，保持既有保守策略。
 * Run one unified cleanup after solver main flow to keep existing conservative behavior.
*/
fun cleanupAfterSolverRun() {
    System.gc()
}
