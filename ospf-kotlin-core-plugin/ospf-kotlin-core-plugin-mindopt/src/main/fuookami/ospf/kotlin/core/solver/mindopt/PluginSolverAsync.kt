/** MindOPT 插件异步求解协程作用域 / MindOPT plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.mindopt

import kotlinx.coroutines.*

/** MindOPT 插件异步求解协程作用域 / MindOPT plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)