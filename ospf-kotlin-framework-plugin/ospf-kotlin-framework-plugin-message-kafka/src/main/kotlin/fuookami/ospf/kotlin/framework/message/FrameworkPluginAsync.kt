package fuookami.ospf.kotlin.framework.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val frameworkPluginAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)