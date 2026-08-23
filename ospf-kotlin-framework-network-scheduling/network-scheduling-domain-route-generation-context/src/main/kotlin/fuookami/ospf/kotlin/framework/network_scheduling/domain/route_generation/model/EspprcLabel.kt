package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

/**
 * ESPPRC 标签。 / ESPPRC label.
 *
 * 不可变资源标签，包含：
 * - reducedCost：当前累计 reduced cost
 * - time：当前时间（solver 数值）
 * - load：当前负载（solver 数值）
 * - currentNode：当前节点
 * - visited：已访问客户
 * - forbidden：不可达客户
 * - predecessor：前驱索引（用于回溯路线）
 *
 * Immutable resource label containing:
 * - reducedCost: accumulated reduced cost so far
 * - time: current time (solver numeric)
 * - load: current load (solver numeric)
 * - currentNode: current node
 * - visited: visited customers
 * - forbidden: unreachable customers
 * - predecessor: predecessor index (for route backtracking)
 *
 * @property reducedCost 累计 reduced cost / Accumulated reduced cost
 * @property time 当前时间 / Current time
 * @property load 当前负载 / Current load
 * @property currentNode 当前节点 ID / Current node ID
 * @property visited 已访问客户 / Visited customers
 * @property forbidden 不可达客户 / Forbidden customers
 * @property predecessor 前驱标签索引 / Predecessor label index (-1 for root)
 * @property routeIndex 路线在迭代中的索引 / Route index within iteration (for variable naming)
 */
data class EspprcLabel(
    val reducedCost: Flt64,
    val time: Flt64,
    val load: Flt64,
    val currentNode: NetworkNodeId,
    val visited: VisitedCustomers,
    val forbidden: ForbiddenCustomers,
    val predecessor: Int,
    val routeIndex: Int = -1
)
