package fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.model.NetworkResilienceAggregation

/** 连通要求类型。 / Connectivity requirement kind. */
enum class ConnectivityKind { Reachable, EdgeDisjoint, NodeDisjoint }

/** 业务连通要求，不等同于故障后的容量保障。 / Structural connectivity, distinct from post-failure capacity assurance.
 * @property source 起点 / Source
 * @property target 终点 / Target
 * @property paths 路径数量 / Required path count
 * @property kind 不相交类型 / Disjointness kind
 */
data class ConnectivityRequirement(
    val source: NetworkNodeId,
    val target: NetworkNodeId,
    val paths: Int = 1,
    val kind: ConnectivityKind = ConnectivityKind.Reachable
)

/** 显式故障场景，不自动免除失效节点供需。 / Explicit failure scenario; failed-node demand is not automatically waived.
 * @property id 场景标识 / Scenario identity
 * @property closedArcs 关闭服务 / Closed services
 * @property failedNodes 失效节点 / Failed nodes
 * @property upperCapacities 降级后的容量上界 / Degraded upper capacities
 * @property balances 显式覆盖商品供需 / Explicit commodity-balance overrides
 * @property slacks 场景专用有界松弛，空值继承名义配置 / Scenario relaxation, null inherits nominal configuration
 */
class FailureScenario<V : RealNumber<V>>(
    val id: String,
    closedArcs: Set<NetworkArcId> = emptySet(),
    failedNodes: Set<NetworkNodeId> = emptySet(),
    upperCapacities: Map<NetworkArcId, Quantity<V>> = emptyMap(),
    balances: Map<String, List<SupplyDemand<V>>> = emptyMap(),
    slacks: List<FlowSlack<V>>? = null
) {
    val closedArcs = closedArcs.toSet()
    val failedNodes = failedNodes.toSet()
    val upperCapacities = upperCapacities.toMap()
    val balances = balances.mapValues { it.value.toList() }
    val slacks = slacks?.toList()

    companion object {
        /** 构造独立单弧故障场景，不枚举组合。 / Build independent single-arc failures, not combinations.
         * @param arcs 服务集合 / Service identities
         * @return 单故障场景 / Single-failure scenarios
         */
        fun <V : RealNumber<V>> singleArcFailures(arcs: Set<NetworkArcId>): List<FailureScenario<V>> {
            return arcs.map { FailureScenario(id = "arc:${it.value}", closedArcs = setOf(it)) }
        }

        /** 构造独立单节点故障场景。 / Build independent single-node failures.
         * @param nodes 节点集合 / Node identities
         * @return 单故障场景 / Single-failure scenarios
         */
        fun <V : RealNumber<V>> singleNodeFailures(nodes: Set<NetworkNodeId>): List<FailureScenario<V>> {
            return nodes.map { FailureScenario(id = "node:${it.value}", failedNodes = setOf(it)) }
        }
    }
}

/** 韧性模型结果，结论仅适用于显式场景集合。 / Resilience result valid only for the explicit scenario set.
 * @property design 共享建设选择 / Shared construction choices
 * @property scenarios 场景流量与缺口 / Scenario flows and shortfalls
 * @property worstOperatingCost 最坏运行成本 / Worst operating cost
 * @property connectivityFlows 连通要求的辅助流见证 / Auxiliary connectivity-flow witnesses
 * @property effectiveCapacities 各场景建成且有效的通道容量 / Built and effective service capacities per scenario
 * @property worstScenario 实际运行费用最高的场景 / Scenario with highest realized operating cost
 */
data class NetworkResilienceSolution<V : RealNumber<V>>(
    val design: NetworkDesignSolution<V>,
    val scenarios: Map<String, FlowSolution<V>>,
    val worstOperatingCost: Quantity<V>,
    val connectivityFlows: List<Map<NetworkArcId, V>>,
    val effectiveCapacities: Map<String, Map<NetworkArcId, Quantity<V>>>,
    val worstScenario: String
)

/** 共享建设与显式场景的上下文入口。 / Context entry for shared construction and explicit scenarios.
 * @property design 共享建设上下文 / Shared design context
 * @property scenarios 显式故障场景 / Explicit scenarios
 * @property connectivity 连通要求 / Connectivity requirements
 * @property name 模型命名空间 / Model namespace
 * @property aggregation 场景及连通聚合 / Scenario and connectivity aggregation
 */
class NetworkResilienceContext<V : RealNumber<V>>(
    val design: NetworkDesignContext<V>,
    scenarios: List<FailureScenario<V>>,
    connectivity: List<ConnectivityRequirement> = emptyList(),
    val name: String = "network_resilience"
) {
    val scenarios = scenarios.toList()
    val connectivity = connectivity.toList()
    val aggregation = NetworkResilienceAggregation(
        design = design,
        scenarios = this.scenarios,
        connectivity = this.connectivity,
        name = name
    )

    /** 注册共享结构、场景流和辅助连通流。 / Register shared structure, recourse and connectivity flows.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try = aggregation.register(model)

    /** 获取已注册的独立场景流，供下游添加场景规则。 / Access registered recourse for downstream scenario rules.
     * @param scenario 场景标识，名义场景为 nominal / Scenario identity, nominal for the base case
     * @return 场景上下文或错误 / Scenario context or error
     */
    fun flow(scenario: String): Ret<FlowContext<V>> = aggregation.flow(scenario)

    /** 提取所有显式场景结果。 / Extract all explicit scenario results.
     * @param model 已求解模型 / Solved model
     * @return 业务结果 / Business solution
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<NetworkResilienceSolution<V>> = aggregation.extractSolution(model)
}
