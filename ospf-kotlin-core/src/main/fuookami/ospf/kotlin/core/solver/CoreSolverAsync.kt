/**
 * 核心求解器异步作用域
 * Core solver async scope
 */
package fuookami.ospf.kotlin.core.solver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 核心求解器共享的异步协程作用域，使用 [SupervisorJob] 和 [Dispatchers.Default]。
 * Shared async coroutine scope for core solvers, using [SupervisorJob] and [Dispatchers.Default].
 */
internal val coreSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
