/**
 * BPP3D BLA 异步作用域。
 * BPP3D BLA async scope.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.bla.service

import kotlinx.coroutines.*

internal val bpp3dBlaAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
