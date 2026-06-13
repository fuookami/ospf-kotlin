/**
 * BPP3D 货物模型异步作用域。
 * BPP3D item model async scope.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*

internal val bpp3dItemModelAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
