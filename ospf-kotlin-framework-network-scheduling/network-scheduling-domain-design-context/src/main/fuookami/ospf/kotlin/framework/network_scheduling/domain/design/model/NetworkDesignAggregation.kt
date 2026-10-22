package fuookami.ospf.kotlin.framework.network_scheduling.domain.design.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.service.limits.NetworkCapacityConstraint

/** 共用结构变量的模型上下文；场景可复用其建设决策。 / Model context owning structural variables shared by scenarios.
 * @property problem 建设问题 / Design problem
 * @property adapter 数值边界 / Numeric boundary
 * @property name 模型命名空间 / Model namespace
 * @property flow 名义流上下文 / Nominal flow context
 * @property construction 建设变量组件 / Construction-variable component
 * @property constructionCost 建设费用表达式 / Construction-cost expression
 */
class NetworkDesignAggregation<V : RealNumber<V>>(
    val problem: NetworkDesignProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "network_design"
) {
    val flow = FlowContext(
        problem = problem.flow,
        adapter = adapter,
        name = "${name}_nominal"
    )
    private var registeredObjective: Boolean? = null
    private var registered = false
    private var model: AbstractLinearMetaModel<Flt64>? = null
    private var attempted = false
    private var structureReady = false
    private val attachedFlows = mutableSetOf<FlowContext<V>>()
    val construction = NetworkConstruction(
        problem = problem,
        adapter = adapter,
        name = name
    )
    private val services get() = construction.services
    private val facilities get() = construction.facilities
    val constructionCost: LinearPolynomial<Flt64> get() = construction.constructionCost

    /** 服务启用表达式。 / Service activation.
     * @param arc 服务弧 / Service arc
     * @return 启用量 / Activation expression
     */
    fun availability(arc: NetworkArcId): LinearPolynomial<Flt64> = construction.availability(arc)

    /** 设施启用表达式。 / Facility activation.
     * @param node 设施节点 / Facility node
     * @return 启用量 / Activation expression
     */
    fun facilityAvailability(node: NetworkNodeId): LinearPolynomial<Flt64> = construction.facilityAvailability(node)

    /** 注册建设、名义流及可选目标。 / Register construction, nominal flows and an optional objective.
     * @param target 元模型 / Meta model
     * @param minimizeCost 是否注册建设加名义运行费用 / Whether to minimize construction plus nominal operation
     * @return 注册结果 / Registration result
     */
    fun register(target: AbstractLinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try {
        if (registered && model === target) {
            return if (registeredObjective == minimizeCost) ok
            else networkSchedulingFailure("重复建设注册选项不一致 / Inconsistent repeated design registration options")
        }
        if (attempted) return networkSchedulingFailure("建设上下文不可复用 / Design context cannot be reused")
        attempted = true
        when (val result = construction.register(target)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        model = target
        structureReady = true
        when (val result = attachFlow(target, flow)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (minimizeCost) {
            when (val result = target.minimize(constructionCost + flow.cost, name = "${name}_cost")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        model = target
        registeredObjective = minimizeCost
        registered = true
        return ok
    }

    /** 将独立场景流连接到同一组建设变量。 / Attach scenario flows to the same construction variables.
     * @param target 元模型 / Meta model
     * @param context 场景流 / Scenario flow
     * @return 注册结果 / Registration result
     */
    fun attachFlow(target: AbstractLinearMetaModel<Flt64>, context: FlowContext<V>): Try {
        if (!structureReady || model !== target || (!registered && context !== flow)) {
            return networkSchedulingFailure("场景必须使用已完成建设注册的同一模型 / Scenarios require completed design registration in the same model")
        }
        if (context in attachedFlows) return ok
        val facilityRows = problem.facilities.flatMap { it.capacityRows }.toSet()
        if (facilityRows.any { id -> context.problem.resources.none { it.id == id } }) {
            return networkSchedulingFailure("设施处理容量行不存在 / Missing facility processing-capacity row")
        }
        val capacityRows = when (val result = context.problem.capacities()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        for (arc in context.problem.arcs) {
            if (arc.networkArc.id !in services && arc.from !in facilities && arc.to !in facilities) continue
            val row = capacityRows.first { it.id == "arc:${arc.networkArc.id.value}" }
            for ((key, usage) in row.usage) {
                val coefficient = adapter.normalize(usage, row.bounds.upper.unit).value
                    ?: return networkSchedulingFailure("通道用量单位无效：$key / Invalid service usage unit: $key")
                if (coefficient leq Flt64.zero) {
                    return networkSchedulingFailure("受控通道需要正容量消耗以推导有限联动上界：$key / Controlled services require positive capacity usage for a finite linking bound: $key")
                }
            }
        }
        val owned = context.problem.arcs.filter { it.networkArc.id in services }
            .map { "arc:${it.networkArc.id.value}" }.toSet() + facilityRows
        when (val result = context.register(
            model = target,
            ownedCapacityRows = owned,
            minimizeCost = false
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        for (arc in context.problem.arcs) {
            val id = arc.networkArc.id
            val scenarioLoad = context.load("arc:${id.value}").value ?: return networkSchedulingFailure("缺失通道流表达式 / Missing service-flow expression")
            val nominal = problem.flow.arcs.firstOrNull { it.networkArc.id == id }
                ?: return networkSchedulingFailure("场景引用未知通道 / Scenario references unknown service")
            if (nominal.from != arc.from || nominal.to != arc.to) {
                return networkSchedulingFailure("场景不能改写稳定通道端点 / Scenarios cannot rewrite stable service endpoints")
            }
            val unit = nominal.capacity.upper.unit
            val loadScale = adapter.normalize(Quantity(problem.flow.zero.constants.one, arc.capacity.upper.unit), unit).value
                ?: return networkSchedulingFailure("场景容量量纲不兼容 / Incompatible scenario capacity dimension")
            val load = loadScale * scenarioLoad
            val nominalUpper = adapter.normalize(nominal.capacity.upper, unit).value ?: return networkSchedulingFailure("名义容量无效 / Invalid nominal capacity")
            val scenarioUpper = adapter.normalize(arc.capacity.upper, unit).value ?: return networkSchedulingFailure("场景容量无效 / Invalid scenario capacity")
            if (scenarioUpper gr nominalUpper) return networkSchedulingFailure("场景容量不能高于名义容量 / Scenario capacity cannot exceed nominal capacity")
            var upper = LinearPolynomial(constant = scenarioUpper)
            val service = problem.services.firstOrNull { it.arc == id }
            if (service != null) {
                upper = LinearPolynomial()
                for ((index, level) in service.levels.withIndex()) {
                    val capacity = adapter.normalize(level.capacity, unit).value ?: return networkSchedulingFailure("等级容量无效 / Invalid level capacity")
                    upper += capacity * services.getValue(id)[index]
                }
                val lower = adapter.normalize(arc.capacity.lower, unit).value ?: return networkSchedulingFailure("容量下界无效 / Invalid lower bound")
                when (val result = NetworkCapacityConstraint(
                    load = load,
                    lower = lower * availability(id),
                    upper = upper,
                    name = "${context.name}_${id.value}_design"
                )(target)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                when (val result = target.addConstraint(load leq scenarioUpper, name = "${context.name}_${id.value}_available_capacity")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            val levelMaximum = service?.levels?.mapNotNull { adapter.normalize(it.capacity, unit).value }?.maxOrNull()
            val maxCapacity = levelMaximum?.let { minOf(it, scenarioUpper) } ?: scenarioUpper
            for (node in setOf(arc.from, arc.to)) {
                val facility = facilities[node] ?: continue
                when (val result = target.addConstraint(load leq maxCapacity * facility, name = "${context.name}_${id.value}_${node.value}_facility")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        for (facility in problem.facilities) {
            for (id in facility.capacityRows) {
                val row = context.problem.resources.firstOrNull { it.id == id }
                    ?: return networkSchedulingFailure("设施容量行不存在 / Missing facility capacity row")
                val load = context.load(id).value
                    ?: return networkSchedulingFailure("设施占用未注册 / Facility load not registered")
                val upper = adapter.normalize(row.bounds.upper, row.bounds.upper.unit).value
                    ?: return networkSchedulingFailure("设施容量无效 / Invalid facility capacity")
                val lower = adapter.normalize(row.bounds.lower, row.bounds.upper.unit).value
                    ?: return networkSchedulingFailure("设施下界无效 / Invalid facility lower bound")
                val activation = facilityAvailability(facility.node)
                when (val result = NetworkCapacityConstraint(
                    load = load,
                    lower = lower * activation,
                    upper = upper * activation,
                    name = "${context.name}_${id}_facility"
                )(target)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        attachedFlows.add(context)
        return ok
    }

    /** 提取建设选择与名义流。 / Extract construction choices and nominal flows.
     * @param target 已求解模型 / Solved model
     * @return 建设结果 / Design solution
     */
    fun extractSolution(target: AbstractLinearMetaModel<Flt64>): Ret<NetworkDesignSolution<V>> {
        if (!registered || model !== target) return networkSchedulingFailure("建设模型未注册 / Design model not registered")
        val decisions = when (val result = construction.extractSolution(target)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val flowSolution = when (val result = flow.extractSolution(target)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(NetworkDesignSolution(
            openFacilities = decisions.openFacilities,
            serviceLevels = decisions.serviceLevels,
            constructionCost = decisions.cost,
            flow = flowSolution
        ))
    }
}
