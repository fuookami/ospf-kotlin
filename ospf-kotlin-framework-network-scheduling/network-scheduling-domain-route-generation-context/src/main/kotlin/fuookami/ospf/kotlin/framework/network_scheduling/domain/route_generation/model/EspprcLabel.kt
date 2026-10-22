package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

/**
 * ESPPRC 标签。 / ESPPRC label.
 *
 * 不可变资源标签，包含：
 * - reducedCost：当前累计 reduced cost
 * - time：当前时间（领域数值）
 * - load：当前负载（领域数值）
 * - currentNode：当前节点
 * - visited：已访问客户
 * - forbidden：不可达客户
 * - predecessor：前驱索引（用于回溯路线）
 * - arcCosts：路线上的原始弧成本，用于终点处计算策略成本
 * - additiveObjectiveCost：逐弧累加的目标成本，用于终点修正
 * - routeCost：终点处由路线成本策略计算的实际路线成本
 *
 * Immutable resource label containing:
 * - reducedCost: accumulated reduced cost so far
 * - time: current time (domain numeric)
 * - load: current load (domain numeric)
 * - currentNode: current node
 * - visited: visited customers
 * - forbidden: unreachable customers
 * - predecessor: predecessor index (for route backtracking)
 * - arcCosts: raw arc costs used to evaluate the route-cost policy at the terminal
 * - additiveObjectiveCost: accumulated arc-decomposable objective cost for terminal correction
 * - routeCost: actual route cost computed by the route-cost policy at the terminal
 *
 * @property reducedCost 累计 reduced cost / Accumulated reduced cost
 * @property time 当前时间 / Current time
 * @property load 当前负载 / Current load
 * @property currentNode 当前节点 ID / Current node ID
 * @property visited 已访问客户 / Visited customers
 * @property forbidden 不可达客户 / Forbidden customers
 * @property predecessor 前驱标签索引 / Predecessor label index (-1 for root)
 * @property routeIndex 路线在迭代中的索引 / Route index within iteration (for variable naming)
 * @property arcCosts 沿途原始弧成本 / Raw arc costs along the route
 * @property additiveObjectiveCost 用于 reduced cost 修正的逐弧累加目标成本 / Accumulated arc-decomposable objective cost used to correct reduced cost
 * @property routeCost 终点处策略计算出的实际路线成本 / Actual route cost evaluated at the terminal
 */
data class EspprcLabel<V : RealNumber<V>>(
    val reducedCost: V,
    val time: V,
    val load: V,
    val currentNode: NetworkNodeId,
    val visited: VisitedCustomers,
    val forbidden: ForbiddenCustomers,
    val predecessor: Int,
    val routeIndex: Int = -1,
    val arcCosts: List<Quantity<V>> = emptyList(),
    val additiveObjectiveCost: V? = null,
    val routeCost: V? = null
)
