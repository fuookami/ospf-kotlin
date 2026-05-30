/**
 * BPP3D BLA 异步作用域。
 * BPP3D BLA async scope.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.bla.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val bpp3dBlaAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)