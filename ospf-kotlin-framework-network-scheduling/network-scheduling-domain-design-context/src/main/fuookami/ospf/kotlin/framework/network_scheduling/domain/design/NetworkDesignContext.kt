package fuookami.ospf.kotlin.framework.network_scheduling.domain.design

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.model.NetworkDesignAggregation

/** 离散建设等级。 / Discrete construction level.
 * @property id 等级标识 / Level identity
 * @property capacity 建设后总容量，不是增量 / Total resulting capacity, not an increment
 * @property cost 建设费用 / Construction cost
 */
data class CapacityLevel<V : RealNumber<V>>(val id: String, val capacity: Quantity<V>, val cost: Quantity<V>)

/** 可选服务通道；未选择等级表示关闭。 / Candidate service; no selected level means closed.
 * @property arc 服务弧 / Service identity
 * @property levels 可选容量等级 / Capacity levels
 * @property required 是否必须选择一个等级 / Whether one level must be selected
 */
class NetworkService<V : RealNumber<V>>(
    val arc: NetworkArcId,
    levels: List<CapacityLevel<V>>,
    val required: Boolean = false
) {
    val levels = levels.toList()
}

/** 候选设施。 / Candidate facility.
 * @property node 设施节点 / Facility node
 * @property cost 开设费用 / Opening cost
 * @property required 是否必须开设 / Whether opening is required
 * @property capacityRows 设施启用管理的共享处理容量行 / Shared processing rows gated by facility activation
 */
class NetworkFacility<V : RealNumber<V>>(
    val node: NetworkNodeId,
    val cost: Quantity<V>,
    val required: Boolean = false,
    capacityRows: Set<String> = emptySet()
) {
    val capacityRows = capacityRows.toSet()
}

/** 结构决策问题。 / Structural decision problem.
 * @property flow 名义流问题 / Nominal flow problem
 * @property services 候选通道 / Candidate services
 * @property facilities 候选设施 / Candidate facilities
 * @property budget 建设预算 / Construction budget
 */
class NetworkDesignProblem<V : RealNumber<V>>(
    val flow: FlowProblem<V>,
    services: List<NetworkService<V>> = emptyList(),
    facilities: List<NetworkFacility<V>> = emptyList(),
    val budget: Quantity<V>? = null
) {
    val services = services.toList()
    val facilities = facilities.toList()
}

/** 结构决策结果。 / Structural decision solution.
 * @property openFacilities 已开设施 / Open facilities
 * @property serviceLevels 已选通道等级 / Selected service levels
 * @property constructionCost 建设成本 / Construction cost
 * @property flow 名义流结果 / Nominal flow solution
 */
data class NetworkDesignSolution<V : RealNumber<V>>(
    val openFacilities: Set<NetworkNodeId>,
    val serviceLevels: Map<NetworkArcId, String>,
    val constructionCost: Quantity<V>,
    val flow: FlowSolution<V>
)

/** 结构决策注册入口，共享模型聚合可供场景和业务规则使用。 / Structural registration entry with a shared model aggregate.
 * @property problem 建设问题 / Design problem
 * @property adapter 数值边界 / Numeric boundary
 * @property name 模型命名空间 / Model namespace
 * @property aggregation 模型聚合 / Model aggregation
 * @property flow 名义流上下文 / Nominal flow context
 * @property constructionCost 建设费用表达式 / Construction-cost expression
 */
class NetworkDesignContext<V : RealNumber<V>>(
    val problem: NetworkDesignProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "network_design"
) {
    val aggregation = NetworkDesignAggregation(
        problem = problem,
        adapter = adapter,
        name = name
    )
    val flow: FlowContext<V> get() = aggregation.flow
    val constructionCost: LinearPolynomial<Flt64> get() = aggregation.constructionCost

    /** 服务启用表达式。 / Service activation.
     * @param arc 服务弧 / Service arc
     * @return 启用表达式 / Activation expression
     */
    fun availability(arc: NetworkArcId): LinearPolynomial<Flt64> = aggregation.availability(arc)

    /** 设施启用表达式。 / Facility activation.
     * @param node 设施节点 / Facility node
     * @return 启用表达式 / Activation expression
     */
    fun facilityAvailability(node: NetworkNodeId): LinearPolynomial<Flt64> = aggregation.facilityAvailability(node)

    /** 注册结构与名义流。 / Register structure and nominal flow.
     * @param target 元模型 / Meta model
     * @param minimizeCost 是否添加建设加运行费用 / Whether to add construction and operating costs
     * @return 注册结果 / Registration result
     */
    fun register(target: AbstractLinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try = aggregation.register(target, minimizeCost)

    /** 注册共享结构决策的场景流。 / Register recourse sharing structural decisions.
     * @param target 同一元模型 / Same meta model
     * @param context 场景流 / Scenario flow
     * @return 注册结果 / Registration result
     */
    fun attachFlow(target: AbstractLinearMetaModel<Flt64>, context: FlowContext<V>): Try = aggregation.attachFlow(target, context)

    /** 提取结构和名义流结果。 / Extract design and nominal flow.
     * @param target 已求解模型 / Solved model
     * @return 业务结果 / Business solution
     */
    fun extractSolution(target: AbstractLinearMetaModel<Flt64>): Ret<NetworkDesignSolution<V>> = aggregation.extractSolution(target)
}
