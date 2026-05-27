package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val bpp3dItemModelAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
