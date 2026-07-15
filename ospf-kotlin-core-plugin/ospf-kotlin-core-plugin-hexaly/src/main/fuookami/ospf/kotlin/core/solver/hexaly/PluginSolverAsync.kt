/**
 * Hexaly plugin async solver coroutine scope
 * Hexaly 插件异步求解协程作用域
*/
package fuookami.ospf.kotlin.core.solver.hexaly

import kotlinx.coroutines.*

/**
 * Hexaly plugin async solver coroutine scope
 * Hexaly 插件异步求解协程作用域
*/
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
