package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits.*

/** 每模型独立的流聚合及注册入口。 / Per-model flow aggregate and registration entry.
 * @property problem 不可变业务问题 / Immutable business problem
 * @property adapter 数值边界 / Numeric boundary
 * @property name 模型命名空间 / Model namespace
 */
class FlowAggregation<V : RealNumber<V>>(
    val problem: FlowProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "network_flow"
) {
    private var registeredOptions: Pair<Set<String>, Boolean>? = null
    private var attempted = false
    private var registeredModel: AbstractLinearMetaModel<Flt64>? = null
    private val variables = mutableMapOf<FlowKey, AbstractVariableItem<*, *>>()
    private val stepValues = mutableMapOf<String, Flt64>()
    private val expressions = mutableMapOf<FlowKey, LinearPolynomial<Flt64>>()
    private val slackVariables = mutableMapOf<Pair<String, NetworkNodeId>, Pair<URealVar, URealVar>>()
    private var rows: List<FlowCapacity<V>> = emptyList()
    private val loads = mutableMapOf<String, LinearPolynomial<Flt64>>()
    private val productCosts = mutableMapOf<String, LinearPolynomial<Flt64>>()
    private var relaxationCost = LinearPolynomial()

    /** 已归一化费用表达式。 / Normalized cost expression. */
    var cost = LinearPolynomial()
        private set

    /** 商品单位下的流表达式。 / Flow expression in the commodity unit.
     * @param key 商品服务键 / Commodity-service key
     * @return 表达式或错误 / Expression or error
     */
    fun flow(key: FlowKey): Ret<LinearPolynomial<Flt64>> {
        return expressions[key]?.let { ok(it) }
            ?: networkSchedulingFailure("流变量不存在：$key / Flow variable does not exist: $key")
    }

    /** 资源单位下的占用表达式。 / Resource load expression.
     * @param id 资源标识 / Resource identity
     * @return 表达式或错误 / Expression or error
     */
    fun load(id: String): Ret<LinearPolynomial<Flt64>> {
        return loads[id]?.let { ok(it) }
            ?: networkSchedulingFailure("资源未注册：$id / Resource not registered: $id")
    }

    /** 注册变量、守恒、资源和费用；设计组件可接管指定容量行。 / Register flows; design components may own selected capacity rows.
     * @param model 元模型 / Meta model
     * @param ownedCapacityRows 被上层结构决策接管的资源行 / Resource rows owned by structural decisions
     * @param minimizeCost 是否注册费用目标 / Whether to register a cost objective
     * @return 注册结果 / Registration result
     */
    fun register(
        model: AbstractLinearMetaModel<Flt64>,
        ownedCapacityRows: Set<String> = emptySet(),
        minimizeCost: Boolean = true
    ): Try {
        if (registeredModel === model) {
            return if (registeredOptions == (ownedCapacityRows to minimizeCost)) ok
            else networkSchedulingFailure("重复注册选项不一致 / Inconsistent repeated registration options")
        }
        if (attempted) return networkSchedulingFailure("流上下文不可跨模型或失败后复用 / Flow context cannot be reused across models or after failure")
        attempted = true
        val products = problem.commodities
        val arcIds = problem.arcs.map { it.networkArc.id }.toSet()
        if (adapter.toSolverValue(problem.zero).value != Flt64.zero ||
            products.map { it.commodity.id }.toSet().size != products.size ||
            arcIds.size != problem.arcs.size || arcIds != problem.graph.arcs.map { it.id }.toSet() ||
            problem.arcs.any { problem.graph.from(it.networkArc) != it.from || problem.graph.to(it.networkArc) != it.to } ||
            problem.capacityUsage.keys.any { it.arc !in arcIds || products.none { product -> product.commodity.id == it.commodity } }) {
            return networkSchedulingFailure("商品或服务弧定义无效 / Invalid commodities or services")
        }
        rows = when (val result = problem.capacities()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (rows.map { it.id }.toSet().size != rows.size || !ownedCapacityRows.all { id -> rows.any { it.id == id } }) {
            return networkSchedulingFailure("资源行重复或缺失 / Duplicate or missing capacity row")
        }
        for (product in products) {
            var productCost = LinearPolynomial()
            val step = product.integerStep?.let { adapter.normalize(it, product.unit).value } ?: Flt64.one
            if (!step.isFinite() || step leq Flt64.zero ||
                (product.integerStep != null && adapter.normalize(product.integerStep, product.unit).value == null) ||
                product.commodity.supplyDemands.any { problem.graph.node(it.nodeId) == null || it.net.convertTo(product.unit) == null } ||
                product.allowedArcs?.any { it !in arcIds } == true || product.costs.keys.any { it !in arcIds }) {
                return networkSchedulingFailure("商品单位、步长或引用无效 / Invalid commodity unit, step or reference")
            }
            stepValues[product.commodity.id] = step
            for (arc in problem.arcs) {
                if (product.allowedArcs?.contains(arc.networkArc.id) == false) continue
                val key = FlowKey(product.commodity.id, arc.networkArc.id)
                val variable: AbstractVariableItem<*, *> = if (product.integerStep == null) {
                    URealVar("${name}_${product.commodity.id}_${arc.networkArc.id.value}")
                } else {
                    UIntVar("${name}_${product.commodity.id}_${arc.networkArc.id.value}")
                }
                when (val result = model.add(variable)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                variables[key] = variable
                expressions[key] = LinearPolynomial(listOf(LinearMonomial(step, variable)), Flt64.zero)
                val coefficient = adapter.normalize(product.costs[arc.networkArc.id] ?: arc.cost, problem.costUnit).value
                    ?: return networkSchedulingFailure("商品费用单位不兼容 / Incompatible commodity cost unit")
                cost += coefficient * expressions.getValue(key)
                productCost += coefficient * expressions.getValue(key)
            }
            productCosts[product.commodity.id] = productCost
        }
        for (row in rows) {
            var expression = LinearPolynomial()
            for ((key, usage) in row.usage) {
                val flow = expressions[key] ?: return networkSchedulingFailure("资源引用不可用服务：$key / Resource references unavailable service: $key")
                val coefficient = adapter.normalize(usage, row.bounds.upper.unit).value
                    ?: return networkSchedulingFailure("资源消耗单位不兼容 / Incompatible resource usage unit")
                if (coefficient ls Flt64.zero) return networkSchedulingFailure("资源消耗不能为负 / Resource usage must be nonnegative")
                expression += coefficient * flow
            }
            loads[row.id] = expression
            if (row.id in ownedCapacityRows) continue
            val lower = adapter.normalize(row.bounds.lower, row.bounds.upper.unit).value
                ?: return networkSchedulingFailure("资源下界单位无效 / Invalid capacity lower unit")
            val upper = adapter.normalize(row.bounds.upper, row.bounds.upper.unit).value
                ?: return networkSchedulingFailure("资源上界无效 / Invalid capacity upper bound")
            when (val result = CapacityBoundConstraint(
                expression = expression,
                lower = lower,
                upper = upper,
                name = "${name}_${row.id}"
            )(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (slack in problem.slacks) {
            val key = slack.commodity to slack.node
            val product = products.firstOrNull { it.commodity.id == slack.commodity }
                ?: return networkSchedulingFailure("松弛引用缺失商品 / Relaxation references missing commodity")
            if (key in slackVariables || problem.graph.node(slack.node) == null) return networkSchedulingFailure("松弛节点重复或缺失 / Duplicate or missing relaxation node")
            val unused = URealVar("${name}_unused_${slack.commodity}_${slack.node.value}")
            val unmet = URealVar("${name}_unmet_${slack.commodity}_${slack.node.value}")
            val unusedBound = adapter.normalize(slack.unusedSupply, product.unit).value
                ?: return networkSchedulingFailure("弃供单位无效 / Invalid unused-supply unit")
            val unmetBound = adapter.normalize(slack.unmetDemand, product.unit).value
                ?: return networkSchedulingFailure("缺货单位无效 / Invalid unmet-demand unit")
            val penalty = adapter.normalize(slack.penalty, problem.costUnit).value
                ?: return networkSchedulingFailure("松弛罚成本单位无效 / Invalid relaxation penalty unit")
            if (unusedBound ls Flt64.zero || unmetBound ls Flt64.zero || penalty ls Flt64.zero) return networkSchedulingFailure("松弛上界和罚成本不能为负 / Relaxation bounds and penalty must be nonnegative")
            val balance = product.commodity.supplyDemands.firstOrNull { it.nodeId == slack.node }
            val net = if (balance == null) Flt64.zero else adapter.normalize(balance.net, product.unit).value
                ?: return networkSchedulingFailure("松弛供需单位无效 / Invalid relaxation balance unit")
            if (unusedBound gr maxOf(net, Flt64.zero) || unmetBound gr maxOf(-net, Flt64.zero)) {
                return networkSchedulingFailure("松弛上界不能超过对应方向的供给或需求 / Relaxation bounds cannot exceed supply or demand in the corresponding direction")
            }
            for (variable in listOf(unused, unmet)) {
                when (val result = model.add(variable)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            for (constraint in listOf(unused leq unusedBound, unmet leq unmetBound)) {
                when (val result = model.addConstraint(constraint)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            cost += penalty * unused + penalty * unmet
            relaxationCost += penalty * unused + penalty * unmet
            slackVariables[key] = unused to unmet
        }
        for (product in products) {
            for (node in problem.graph.nodes) {
                var balance = LinearPolynomial()
                for (arc in problem.arcs) {
                    val expression = expressions[FlowKey(product.commodity.id, arc.networkArc.id)] ?: continue
                    if (arc.from == node.id) balance += expression
                    if (arc.to == node.id) balance -= expression
                }
                slackVariables[product.commodity.id to node.id]?.let { (unused, unmet) -> balance += unused; balance -= unmet }
                val demand = product.commodity.supplyDemands.firstOrNull { it.nodeId == node.id }
                val net = if (demand == null) Flt64.zero else adapter.normalize(demand.net, product.unit).value
                    ?: return networkSchedulingFailure("供需归一化失败 / Balance normalization failed")
                when (val result = FlowConservationConstraint(
                    balance = balance,
                    net = net,
                    name = "${name}_balance_${product.commodity.id}_${node.id.value}"
                )(model)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        if (minimizeCost) {
            when (val result = MinCostFlowObjective(cost, "${name}_cost")(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        registeredOptions = ownedCapacityRows.toSet() to minimizeCost
        registeredModel = model
        return ok
    }

    /** 从已回填的模型提取业务量。 / Extract business quantities from a populated model.
     * @param model 已求解模型 / Solved model
     * @return 流结果或缺失结果错误 / Flow solution or missing-value error
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<FlowSolution<V>> {
        if (registeredModel !== model) return networkSchedulingFailure("流模型未注册 / Flow model is not registered")
        fun evaluate(polynomial: LinearPolynomial<Flt64>): Flt64? {
            var value = polynomial.constant
            for (term in polynomial.monomials) {
                val token = model.tokens.find(term.symbol as? AbstractVariableItem<*, *> ?: return null)?.result ?: return null
                value += term.coefficient * token
            }
            return value
        }
        val flows = mutableMapOf<FlowKey, Quantity<V>>()
        for ((key, variable) in variables) {
            val solverValue = model.tokens.find(variable)?.result ?: return networkSchedulingFailure("缺失流量结果：$key / Missing flow result: $key")
            val value = adapter.fromSolverValue(solverValue * stepValues.getValue(key.commodity)).value
                ?: return networkSchedulingFailure("流量转换失败 / Flow conversion failed")
            flows[key] = Quantity(value, problem.commodities.first { it.commodity.id == key.commodity }.unit)
        }
        val utilization = mutableMapOf<String, Quantity<V>>()
        for (row in rows) {
            val scalar = evaluate(loads.getValue(row.id)) ?: return networkSchedulingFailure("缺失资源结果 / Missing resource result")
            val value = adapter.fromSolverValue(scalar).value ?: return networkSchedulingFailure("资源结果转换失败 / Resource result conversion failed")
            utilization[row.id] = Quantity(value, row.bounds.upper.unit)
        }
        val unused = mutableMapOf<Pair<String, NetworkNodeId>, Quantity<V>>()
        val unmet = mutableMapOf<Pair<String, NetworkNodeId>, Quantity<V>>()
        for ((key, pair) in slackVariables) {
            val unit = problem.commodities.first { it.commodity.id == key.first }.unit
            val a = model.tokens.find(pair.first)?.result?.let { adapter.fromSolverValue(it).value }
                ?: return networkSchedulingFailure("缺失弃供结果 / Missing unused-supply result")
            val b = model.tokens.find(pair.second)?.result?.let { adapter.fromSolverValue(it).value }
                ?: return networkSchedulingFailure("缺失缺货结果 / Missing unmet-demand result")
            unused[key] = Quantity(a, unit)
            unmet[key] = Quantity(b, unit)
        }
        val total = evaluate(cost)?.let { adapter.fromSolverValue(it).value }
            ?: return networkSchedulingFailure("缺失费用结果 / Missing cost result")
        val commodityCosts = mutableMapOf<String, Quantity<V>>()
        for ((id, expression) in productCosts) {
            val value = evaluate(expression)?.let { adapter.fromSolverValue(it).value }
                ?: return networkSchedulingFailure("商品费用提取失败 / Commodity cost extraction failed")
            commodityCosts[id] = Quantity(value, problem.costUnit)
        }
        val penalty = evaluate(relaxationCost)?.let { adapter.fromSolverValue(it).value }
            ?: return networkSchedulingFailure("罚费用提取失败 / Penalty extraction failed")
        val balances = mutableMapOf<Pair<String, NetworkNodeId>, Quantity<V>>()
        for (product in problem.commodities) {
            for (node in problem.graph.nodes) {
                val key = product.commodity.id to node.id
                val balance = product.commodity.supplyDemands.firstOrNull { it.nodeId == node.id }
                var net = if (balance == null) Flt64.zero else adapter.normalize(balance.net, product.unit).value
                    ?: return networkSchedulingFailure("供需结果换算失败 / Balance result conversion failed")
                val pair = slackVariables[key]
                if (pair != null) {
                    net -= model.tokens.find(pair.first)?.result ?: return networkSchedulingFailure("缺失弃供结果 / Missing unused supply")
                    net += model.tokens.find(pair.second)?.result ?: return networkSchedulingFailure("缺失缺货结果 / Missing unmet demand")
                }
                val value = adapter.fromSolverValue(net).value
                    ?: return networkSchedulingFailure("满足量结果转换失败 / Fulfillment result conversion failed")
                balances[key] = Quantity(value, product.unit)
            }
        }
        return ok(FlowSolution(
            flows = flows.toMap(),
            cost = Quantity(total, problem.costUnit),
            capacityUsage = utilization.toMap(),
            unusedSupply = unused.toMap(),
            unmetDemand = unmet.toMap(),
            commodityCosts = commodityCosts.toMap(),
            relaxationCost = Quantity(penalty, problem.costUnit),
            satisfiedBalances = balances.toMap()
        ))
    }
}
