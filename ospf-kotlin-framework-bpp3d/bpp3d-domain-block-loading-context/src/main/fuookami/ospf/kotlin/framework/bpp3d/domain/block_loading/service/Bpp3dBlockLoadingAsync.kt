package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val bpp3dBlockLoadingAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)