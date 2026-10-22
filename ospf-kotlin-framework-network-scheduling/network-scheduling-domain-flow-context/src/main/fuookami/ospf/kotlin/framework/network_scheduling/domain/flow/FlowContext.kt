package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*

/** 商品和服务弧的稳定键。 / Stable commodity-service key.
 * @property commodity 商品标识 / Commodity identifier
 * @property arc 服务弧标识 / Service arc identifier
 */
data class FlowKey(val commodity: String, val arc: NetworkArcId)

/** 商品流配置，整数步长不是整单不可拆分约束。 / Commodity flow options; integer steps do not imply unsplittable orders.
 * @property commodity 商品供需 / Commodity balances
 * @property unit 商品归一化单位 / Commodity normalization unit
 * @property integerStep 整数流每步数量，空表示连续 / Amount per integer step, null for continuous flow
 * @property costs 每单位商品的弧费用 / Arc costs per commodity unit
 * @property allowedArcs 可用服务弧，空表示全部 / Allowed services, null for all
 */
class CommodityFlow<V : RealNumber<V>>(
    val commodity: FlowCommodity<V>,
    val unit: PhysicalUnit,
    val integerStep: Quantity<V>? = null,
    costs: Map<NetworkArcId, Quantity<V>> = emptyMap(),
    allowedArcs: Set<NetworkArcId>? = null
) {
    val costs = costs.toMap()
    val allowedArcs = allowedArcs?.toSet()
}

/** 显式共享资源行；用量按一个商品归一化单位声明。 / Explicit shared-resource row; usage is per normalized commodity unit.
 * @property id 资源标识 / Resource identifier
 * @property bounds 资源容量 / Resource bounds
 * @property usage 单位商品的资源消耗 / Resource consumption per commodity unit
 */
class FlowCapacity<V : RealNumber<V>>(
    val id: String,
    val bounds: CapacityBounds<V>,
    usage: Map<FlowKey, Quantity<V>>
) {
    val usage = usage.toMap()
}

/** 有界供需松弛，只能显式启用。 / Bounded balance relaxation, enabled explicitly.
 * @property commodity 商品 / Commodity
 * @property node 节点 / Node
 * @property unusedSupply 最大弃供 / Maximum unused supply
 * @property unmetDemand 最大缺货 / Maximum unmet demand
 * @property penalty 每商品单位罚费用 / Penalty per commodity unit
 */
data class FlowSlack<V : RealNumber<V>>(
    val commodity: String,
    val node: NetworkNodeId,
    val unusedSupply: Quantity<V>,
    val unmetDemand: Quantity<V>,
    val penalty: Quantity<V>
)

/** 不持有 solver 状态的流问题快照。 / Flow problem snapshot without solver state.
 * @property graph 网络拓扑 / Network topology
 * @property arcs 服务弧及容量 / Services and capacities
 * @property commodities 商品配置 / Commodity options
 * @property zero 数值零 / Numeric zero
 * @property costUnit 费用单位 / Cost unit
 * @property capacityUsage 默认弧容量的异量纲消耗覆盖 / Cross-dimensional consumption overrides
 * @property resources 额外共享资源或节点吞吐行 / Additional shared-resource or node-throughput rows
 * @property slacks 显式供需松弛 / Explicit balance relaxation
 */
class FlowProblem<V : RealNumber<V>>(
    val graph: NetworkGraph<NetworkNode<V>, NetworkArc<V>>,
    arcs: List<FlowArc<V>>,
    commodities: List<CommodityFlow<V>>,
    val zero: V,
    val costUnit: PhysicalUnit = NoneUnit,
    capacityUsage: Map<FlowKey, Quantity<V>> = emptyMap(),
    resources: List<FlowCapacity<V>> = emptyList(),
    slacks: List<FlowSlack<V>> = emptyList()
) {
    val arcs = arcs.toList()
    val commodities = commodities.toList()
    val capacityUsage = capacityUsage.toMap()
    val resources = resources.toList()
    val slacks = slacks.toList()

    /** 构建默认弧资源行，不将不同量纲裸值相加。 / Build arc resource rows without mixing raw dimensions.
     * @return 资源行或单位错误 / Resource rows or unit errors
     */
    fun capacities(): Ret<List<FlowCapacity<V>>> {
        val rows = mutableListOf<FlowCapacity<V>>()
        for (arc in arcs) {
            val usage = mutableMapOf<FlowKey, Quantity<V>>()
            for (commodity in commodities) {
                if (commodity.allowedArcs?.contains(arc.networkArc.id) == false) continue
                val key = FlowKey(commodity.commodity.id, arc.networkArc.id)
                val amount = capacityUsage[key] ?: Quantity(zero.constants.one, commodity.unit)
                if (amount.convertTo(arc.capacity.upper.unit) == null) {
                    return networkSchedulingFailure("共享容量需要显式同量纲消耗：$key / Explicit compatible capacity usage required: $key")
                }
                usage[key] = amount
            }
            rows.add(FlowCapacity(
                id = "arc:${arc.networkArc.id.value}",
                bounds = arc.capacity,
                usage = usage
            ))
        }
        return ok(rows + resources)
    }
}

/** 业务流量提取结果。 / Extracted business flows.
 * @property flows 商品服务流量 / Commodity-service quantities
 * @property cost 总费用，包含显式罚成本 / Total cost including explicit penalties
 * @property capacityUsage 资源利用量 / Resource utilization
 * @property unusedSupply 弃供 / Unused supply
 * @property unmetDemand 缺货 / Unmet demand
 * @property commodityCosts 各商品运输费用 / Transportation cost by commodity
 * @property relaxationCost 松弛罚费用 / Relaxation penalties
 * @property satisfiedBalances 实际供需净量 / Realized net balances
 */
data class FlowSolution<V : RealNumber<V>>(
    val flows: Map<FlowKey, Quantity<V>>,
    val cost: Quantity<V>,
    val capacityUsage: Map<String, Quantity<V>>,
    val unusedSupply: Map<Pair<String, NetworkNodeId>, Quantity<V>>,
    val unmetDemand: Map<Pair<String, NetworkNodeId>, Quantity<V>>,
    val commodityCosts: Map<String, Quantity<V>>,
    val relaxationCost: Quantity<V>,
    val satisfiedBalances: Map<Pair<String, NetworkNodeId>, Quantity<V>>
)

/** 每次模型注册使用独立聚合，可追加领域 pipeline。 / Independent per-model aggregation with extension pipelines.
 * @property problem 不可变问题 / Immutable problem
 * @property adapter 数值边界 / Numeric boundary
 * @property name 模型命名空间 / Model namespace
 * @property aggregation 注册态 / Registration state
 * @property cost 已注册的运行费用表达式 / Registered operating-cost expression
 */
class FlowContext<V : RealNumber<V>>(
    val problem: FlowProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "network_flow"
) {
    val aggregation = FlowAggregation(
        problem = problem,
        adapter = adapter,
        name = name
    )
    val cost: LinearPolynomial<Flt64> get() = aggregation.cost

    /** 访问商品流表达式。 / Access a commodity flow expression.
     * @param key 商品弧键 / Commodity-arc key
     * @return 表达式或错误 / Expression or error
     */
    fun flow(key: FlowKey): Ret<LinearPolynomial<Flt64>> = aggregation.flow(key)

    /** 访问资源占用表达式。 / Access a resource load expression.
     * @param id 资源标识 / Resource identity
     * @return 表达式或错误 / Expression or error
     */
    fun load(id: String): Ret<LinearPolynomial<Flt64>> = aggregation.load(id)

    /** 注册流聚合。 / Register the flow aggregation.
     * @param model 元模型 / Meta model
     * @param ownedCapacityRows 外部决策管理的容量行 / Capacity rows managed by external decisions
     * @param minimizeCost 是否添加目标 / Whether to add the objective
     * @return 注册结果 / Registration result
     */
    fun register(
        model: AbstractLinearMetaModel<Flt64>,
        ownedCapacityRows: Set<String> = emptySet(),
        minimizeCost: Boolean = true
    ): Try = aggregation.register(
        model = model,
        ownedCapacityRows = ownedCapacityRows,
        minimizeCost = minimizeCost
    )

    /** 提取已求解结果。 / Extract solved values.
     * @param model 已求解模型 / Solved model
     * @return 业务结果 / Business solution
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<FlowSolution<V>> = aggregation.extractSolution(model)
}
