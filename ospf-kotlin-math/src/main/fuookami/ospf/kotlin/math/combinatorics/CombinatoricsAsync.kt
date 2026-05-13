package fuookami.ospf.kotlin.math.combinatorics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val combinatoricsAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)