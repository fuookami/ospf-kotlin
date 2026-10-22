package fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.*

/** 共享一阶段建设、二阶段场景流的直接 MILP。 / Direct MILP with shared construction and scenario recourse.
 * @property design 共享建设上下文 / Shared design context
 * @property scenarios 显式故障场景 / Explicit failure scenarios
 * @property connectivity 名义结构连通要求 / Nominal structural connectivity requirements
 * @property name 模型命名空间 / Model namespace
 */
class NetworkResilienceAggregation<V : RealNumber<V>>(
    val design: NetworkDesignContext<V>,
    scenarios: List<FailureScenario<V>>,
    connectivity: List<ConnectivityRequirement> = emptyList(),
    val name: String = "network_resilience"
) {
    val scenarios = scenarios.toList()
    val connectivity = connectivity.toList()
    private val scenarioFlows = mutableMapOf<String, FlowContext<V>>()
    private val witnesses = mutableListOf<ConnectivityFlow<V>>()
    private val worstCost = RealVar("${name}_worst_operation")
    private var registeredModel: AbstractLinearMetaModel<Flt64>? = null
    private var attempted = false

    /** 获取已注册场景流，供业务追加约束。 / Access registered scenario flow for business extensions.
     * @param scenario 场景标识，名义场景为 nominal / Scenario identity, nominal for the base case
     * @return 场景上下文或未注册错误 / Scenario context or registration error
     */
    fun flow(scenario: String): Ret<FlowContext<V>> {
        if (registeredModel == null) return networkSchedulingFailure("韧性模型尚未注册 / Resilience model is not registered")
        return scenarioFlows[scenario]?.let { ok(it) }
            ?: networkSchedulingFailure("场景不存在：$scenario / Unknown scenario: $scenario")
    }

    /** 注册场景模型、连通辅助流和最坏费用目标。 / Register scenarios, connectivity flows and worst-cost objective.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (registeredModel === model) return ok
        if (attempted) return networkSchedulingFailure("韧性上下文不可复用 / Resilience context cannot be reused")
        attempted = true
        if (scenarios.any { it.id.isBlank() || it.id == "nominal" } || scenarios.map { it.id }.toSet().size != scenarios.size) {
            return networkSchedulingFailure("场景标识重复、为空或占用 nominal / Duplicate, blank or reserved scenario identity")
        }
        when (val result = design.register(model, false)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = model.add(worstCost)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        scenarioFlows["nominal"] = design.flow
        for (scenario in scenarios) {
            val problem = when (val result = scenarioProblem(scenario)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val context = FlowContext(
                problem = problem,
                adapter = design.adapter,
                name = "${name}_${scenario.id}"
            )
            when (val result = design.attachFlow(model, context)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            // 降级与硬下界冲突是模型不可行，不是问题定义错误。 / Degradation conflicting with a hard minimum is model infeasibility.
            for (arc in context.problem.arcs) {
                val nominal = design.problem.flow.arcs.first { it.networkArc.id == arc.networkArc.id }
                val lower = design.adapter.normalize(nominal.capacity.lower, arc.capacity.upper.unit).value
                    ?: return networkSchedulingFailure("场景下界单位不兼容 / Incompatible scenario minimum unit")
                val upper = design.adapter.normalize(arc.capacity.upper, arc.capacity.upper.unit).value
                    ?: return networkSchedulingFailure("场景上界无效 / Invalid scenario upper capacity")
                if (lower gr upper) {
                    val load = context.load("arc:${arc.networkArc.id.value}").value
                        ?: return networkSchedulingFailure("场景流表达式缺失 / Missing scenario flow expression")
                    when (val result = model.addConstraint(
                        load geq lower * design.availability(arc.networkArc.id),
                        name = "${context.name}_${arc.networkArc.id.value}_hard_minimum"
                    )) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            }
            scenarioFlows[scenario.id] = context
        }
        for ((id, flow) in scenarioFlows) {
            when (val result = model.addConstraint(flow.cost leq worstCost, name = "${name}_${id}_worst")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for ((index, requirement) in connectivity.withIndex()) {
            val witness = ConnectivityFlow(
                design = design,
                requirement = requirement,
                index = index,
                name = name
            )
            when (val result = witness.register(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            witnesses.add(witness)
        }
        when (val result = model.minimize(design.constructionCost + worstCost, name = "${name}_cost")) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        registeredModel = model
        return ok
    }

    private fun scenarioProblem(scenario: FailureScenario<V>): Ret<FlowProblem<V>> {
        val nominal = design.problem.flow
        val arcIds = nominal.arcs.map { it.networkArc.id }.toSet()
        val commodityIds = nominal.commodities.map { it.commodity.id }.toSet()
        if ((scenario.closedArcs + scenario.upperCapacities.keys).any { it !in arcIds } ||
            scenario.failedNodes.any { nominal.graph.node(it) == null } || scenario.balances.keys.any { it !in commodityIds }) {
            return networkSchedulingFailure("故障场景引用不存在 / Failure scenario references unknown assets")
        }
        val arcs = mutableListOf<FlowArc<V>>()
        for (arc in nominal.arcs) {
            val upper = scenario.upperCapacities[arc.networkArc.id] ?: arc.capacity.upper
            val newValue = design.adapter.normalize(upper, arc.capacity.upper.unit).value
                ?: return networkSchedulingFailure("降级容量单位不兼容 / Incompatible degraded capacity unit")
            val oldValue = design.adapter.normalize(arc.capacity.upper, arc.capacity.upper.unit).value
                ?: return networkSchedulingFailure("名义容量无效 / Invalid nominal capacity")
            if (newValue ls Flt64.zero || newValue gr oldValue) {
                return networkSchedulingFailure("故障容量必须非负且不高于名义容量 / Failure capacity must be nonnegative and no greater than nominal capacity")
            }
            if (arc.networkArc.id in scenario.closedArcs || arc.from in scenario.failedNodes || arc.to in scenario.failedNodes) continue
            val minimum = design.adapter.normalize(arc.capacity.lower, arc.capacity.upper.unit).value
                ?: return networkSchedulingFailure("名义容量下界无效 / Invalid nominal capacity minimum")
            val lower = if (minimum gr newValue) {
                Quantity(nominal.zero, upper.unit)
            } else {
                arc.capacity.lower
            }
            val bounds = when (val result = CapacityBounds(lower, upper)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            arcs.add(
                FlowArc(
                    networkArc = arc.networkArc,
                    capacity = bounds
                ).value ?: return networkSchedulingFailure("故障服务无效 / Invalid failed service")
            )
        }
        val active = arcs.map { it.networkArc.id }.toSet()
        val graph = nominal.graph.filtered { it.id in active }
        val commodities = mutableListOf<CommodityFlow<V>>()
        for (product in nominal.commodities) {
            val balances = scenario.balances[product.commodity.id] ?: product.commodity.supplyDemands
            val commodity = FlowCommodity(product.commodity.id, balances).value
                ?: return networkSchedulingFailure("场景供需无效 / Invalid scenario balances")
            commodities.add(CommodityFlow(
                commodity = commodity,
                unit = product.unit,
                integerStep = product.integerStep,
                costs = product.costs.filterKeys { it in active },
                allowedArcs = product.allowedArcs?.intersect(active)
            ))
        }
        return ok(FlowProblem(
            graph = graph,
            arcs = arcs,
            commodities = commodities,
            zero = nominal.zero,
            costUnit = nominal.costUnit,
            capacityUsage = nominal.capacityUsage.filterKeys { it.arc in active },
            resources = nominal.resources.map { row ->
                FlowCapacity(
                    id = row.id,
                    bounds = row.bounds,
                    usage = row.usage.filterKeys { key -> key.arc in active }
                )
            },
            slacks = scenario.slacks ?: nominal.slacks
        ))
    }

    /** 提取所有场景及辅助连通流。 / Extract every scenario and connectivity witness.
     * @param model 已求解模型 / Solved model
     * @return 韧性结果 / Resilience solution
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<NetworkResilienceSolution<V>> {
        if (registeredModel !== model) return networkSchedulingFailure("韧性模型未注册 / Resilience model not registered")
        val designSolution = when (val result = design.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val solutions = mutableMapOf<String, FlowSolution<V>>()
        for ((id, flow) in scenarioFlows) {
            when (val result = flow.extractSolution(model)) {
                is Ok -> solutions[id] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val cost = model.tokens.find(worstCost)?.result?.let { design.adapter.fromSolverValue(it).value }
            ?: return networkSchedulingFailure("缺失最坏费用结果 / Missing worst-cost result")
        val connectivity = mutableListOf<Map<NetworkArcId, V>>()
        for (witness in witnesses) {
            val values = mutableMapOf<NetworkArcId, V>()
            for ((id, variable) in witness.variables) {
                val value = model.tokens.find(variable)?.result?.let { design.adapter.fromSolverValue(it).value }
                    ?: return networkSchedulingFailure("缺失连通见证 / Missing connectivity witness")
                values[id] = value
            }
            connectivity.add(values.toMap())
        }
        val capacities = mutableMapOf<String, Map<NetworkArcId, Quantity<V>>>()
        for ((id, context) in scenarioFlows) {
            val scenarioCapacities = mutableMapOf<NetworkArcId, Quantity<V>>()
            for (arc in design.problem.flow.arcs) {
                val scenarioArc = context.problem.arcs.firstOrNull { it.networkArc.id == arc.networkArc.id }
                val service = design.problem.services.firstOrNull { it.arc == arc.networkArc.id }
                val selected = service?.levels?.firstOrNull { it.id == designSolution.serviceLevels[service.arc] }
                val unit = arc.capacity.upper.unit
                val facilityClosed = design.problem.facilities.any {
                    (it.node == arc.from || it.node == arc.to) && it.node !in designSolution.openFacilities
                }
                val available = if (scenarioArc == null || facilityClosed || (service != null && selected == null)) {
                    design.problem.flow.zero
                } else {
                    val declared = scenarioArc.capacity.upper.convertTo(unit)?.value
                        ?: return networkSchedulingFailure("场景容量结果换算失败 / Scenario capacity result conversion failed")
                    val built = selected?.capacity?.convertTo(unit)?.value ?: declared
                    if (built ls declared) built else declared
                }
                scenarioCapacities[arc.networkArc.id] = Quantity(available, unit)
            }
            capacities[id] = scenarioCapacities.toMap()
        }
        var worstScenario = "nominal"
        for ((id, solution) in solutions) {
            if (solution.cost.value gr solutions.getValue(worstScenario).cost.value) worstScenario = id
        }
        return ok(NetworkResilienceSolution(
            design = designSolution,
            scenarios = solutions.toMap(),
            worstOperatingCost = Quantity(cost, design.problem.flow.costUnit),
            connectivityFlows = connectivity,
            effectiveCapacities = capacities.toMap(),
            worstScenario = worstScenario
        ))
    }
}
