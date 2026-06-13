/**
 * BPP3D 块装载异步作用域。
 * BPP3D block loading async scope.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import kotlinx.coroutines.*

internal val bpp3dBlockLoadingAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
