/**
 * BPP3D item service async scope.
 * BPP3D货物服务异步作用域。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlinx.coroutines.*

internal val bpp3dItemServiceAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
