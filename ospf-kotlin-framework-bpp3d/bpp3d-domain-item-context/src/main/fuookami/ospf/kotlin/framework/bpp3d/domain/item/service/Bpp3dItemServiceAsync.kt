/**
 * BPP3D 货物服务异步作用域。
 * BPP3D item service async scope.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val bpp3dItemServiceAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
