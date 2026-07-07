/**
 * BPP3D item model async scope.
 * BPP3D货物模型异步作用域。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*

internal val bpp3dItemModelAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
