/**
 * BPP3D 块装载异步作用域。
 * BPP3D block loading async scope.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val bpp3dBlockLoadingAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)