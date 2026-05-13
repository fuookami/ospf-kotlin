package fuookami.ospf.kotlin.framework.solver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val frameworkAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)