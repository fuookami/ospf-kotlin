/**
 * 远程求解运行配置
 * Remote solve runtime config
 */
package fuookami.ospf.kotlin.framework.solver.remote.client

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.solver.remote.domain.*

/**
 * 远程求解运行配置。
 * Remote solve runtime config.
 *
 * @property tenantId 默认租户 ID / Default tenant ID
 * @property nodeId 客户端节点 ID / Client node ID
 * @property quantum 远程切片轮询时间片 / Remote slice polling quantum
 * @property maxRounds 最大轮询轮数 / Maximum polling rounds
 * @property taskIdProvider 任务 ID 生成器 / Task ID provider
 * @property sliceIdProvider 切片 ID 生成器 / Slice ID provider
 */
data class RemoteSolverRuntimeConfig(
    val tenantId: TenantId = TenantId.of("default"),
    val nodeId: NodeId = NodeId.of("remote-client"),
    val quantum: Duration = 4000.milliseconds,
    val maxRounds: UInt64 = UInt64(64),
    val taskIdProvider: () -> TaskId = { TaskId.of(UUID.randomUUID().toString()) },
    val sliceIdProvider: () -> SliceId = { SliceId.of(UUID.randomUUID().toString()) }
) {
    init {
        require(quantum > Duration.ZERO) { "quantum must be positive." }
        require(maxRounds > UInt64.zero) { "maxRounds must be positive." }
    }
}
