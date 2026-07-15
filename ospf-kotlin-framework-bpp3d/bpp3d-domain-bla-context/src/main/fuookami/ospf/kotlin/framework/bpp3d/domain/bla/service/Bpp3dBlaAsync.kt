/**
 * BPP3D BLA async scope.
 * BPP3D BLA 异步作用域。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.bla.service

import kotlinx.coroutines.*

internal val bpp3dBlaAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
