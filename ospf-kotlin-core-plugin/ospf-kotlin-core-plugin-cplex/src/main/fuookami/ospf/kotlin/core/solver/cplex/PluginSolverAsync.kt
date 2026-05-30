/** CPLEX 插件异步求解协程作用域 / CPLEX plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.cplex

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** CPLEX 插件异步求解协程作用域 / CPLEX plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)