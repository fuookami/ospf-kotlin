package fuookami.ospf.kotlin.framework.persistence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val frameworkPluginAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)