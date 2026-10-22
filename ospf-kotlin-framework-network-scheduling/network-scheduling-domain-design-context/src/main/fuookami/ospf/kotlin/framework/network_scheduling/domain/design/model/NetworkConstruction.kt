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
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*

/** 设施与通道变量组件，仅负责共享建设决策。 / Facility/service variable component owning shared construction only.
 * @property problem 建设问题 / Design problem
 * @property adapter 数值边界 / Numeric boundary
 * @property name 模型命名空间 / Model namespace
 * @property constructionCost 建设费用表达式 / Construction-cost expression
 */
class NetworkConstruction<V : RealNumber<V>>(
    val problem: NetworkDesignProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String
) {
    internal val services = mutableMapOf<NetworkArcId, List<BinVar>>()
    internal val facilities = mutableMapOf<NetworkNodeId, BinVar>()
    private var attempted = false
    private var model: AbstractLinearMetaModel<Flt64>? = null
    var constructionCost = LinearPolynomial()
        private set

    /** 服务启用表达式。 / Service activation expression.
     * @param arc 服务标识 / Service identity
     * @return 启用量 / Activation expression
     */
    fun availability(arc: NetworkArcId): LinearPolynomial<Flt64> {
        var expression = LinearPolynomial()
        val variables = services[arc] ?: return LinearPolynomial(constant = Flt64.one)
        for (variable in variables) expression += variable
        return expression
    }

    /** 可选设施启用量；固定设施为一。 / Optional facility activation, one for fixed facilities.
     * @param node 节点 / Node
     * @return 启用量 / Activation expression
     */
    fun facilityAvailability(node: NetworkNodeId): LinearPolynomial<Flt64> {
        val variable = facilities[node] ?: return LinearPolynomial(constant = Flt64.one)
        return LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero)
    }

    /** 注册建设变量、互斥等级和预算。 / Register construction, exclusive levels and budget.
     * @param target 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    fun register(target: AbstractLinearMetaModel<Flt64>): Try {
        if (model === target) return ok
        if (attempted) return networkSchedulingFailure("建设变量组件不可复用 / Construction component cannot be reused")
        attempted = true
        if (problem.services.map { it.arc }.toSet().size != problem.services.size ||
            problem.facilities.map { it.node }.toSet().size != problem.facilities.size) {
            return networkSchedulingFailure("建设资源重复 / Duplicate design asset")
        }
        for (facility in problem.facilities) {
            if (problem.flow.graph.node(facility.node) == null) return networkSchedulingFailure("设施节点不存在 / Facility node does not exist")
            val coefficient = adapter.normalize(facility.cost, problem.flow.costUnit).value
                ?: return networkSchedulingFailure("设施成本单位不兼容 / Incompatible facility cost unit")
            if (coefficient ls Flt64.zero) return networkSchedulingFailure("设施成本不能为负 / Facility cost must be nonnegative")
            val variable = BinVar("${name}_facility_${facility.node.value}")
            when (val result = target.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            facilities[facility.node] = variable
            constructionCost += coefficient * variable
            if (facility.required) {
                when (val result = target.addConstraint(variable eq Flt64.one)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        for (service in problem.services) {
            val arc = problem.flow.arcs.firstOrNull { it.networkArc.id == service.arc }
                ?: return networkSchedulingFailure("建设通道不存在 / Design service does not exist")
            if (service.levels.isEmpty() || service.levels.map { it.id }.toSet().size != service.levels.size || service.levels.any { it.id.isBlank() }) {
                return networkSchedulingFailure("建设等级为空或重复 / Empty or duplicate capacity level")
            }
            val variables = mutableListOf<BinVar>()
            var activation = LinearPolynomial()
            for (level in service.levels) {
                val capacity = adapter.normalize(level.capacity, arc.capacity.upper.unit).value
                    ?: return networkSchedulingFailure("建设容量单位不兼容 / Incompatible construction capacity unit")
                val coefficient = adapter.normalize(level.cost, problem.flow.costUnit).value
                    ?: return networkSchedulingFailure("建设成本单位不兼容 / Incompatible construction cost unit")
                val envelope = adapter.normalize(arc.capacity.upper, arc.capacity.upper.unit).value
                    ?: return networkSchedulingFailure("通道容量包络无效 / Invalid service capacity envelope")
                if (capacity ls Flt64.zero || coefficient ls Flt64.zero || capacity gr envelope) {
                    return networkSchedulingFailure("等级容量必须位于声明包络内且费用非负 / Level capacity must fit the declared envelope and cost must be nonnegative")
                }
                val variable = BinVar("${name}_${service.arc.value}_${level.id}")
                when (val result = target.add(variable)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                variables.add(variable)
                activation += variable
                constructionCost += coefficient * variable
            }
            services[service.arc] = variables.toList()
            val constraint = if (service.required) activation eq Flt64.one else activation leq Flt64.one
            when (val result = target.addConstraint(constraint)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        problem.budget?.let { budget ->
            val amount = adapter.normalize(budget, problem.flow.costUnit).value
                ?: return networkSchedulingFailure("建设预算单位不兼容 / Incompatible construction budget unit")
            if (amount ls Flt64.zero) return networkSchedulingFailure("建设预算不能为负 / Construction budget must be nonnegative")
            when (val result = target.addConstraint(constructionCost leq amount, name = "${name}_budget")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        model = target
        return ok
    }

    /** 提取建设选择。 / Extract construction choices.
     * @param target 已求解模型 / Solved model
     * @return 建设选择和费用 / Choices and cost
     */
    fun extractSolution(target: AbstractLinearMetaModel<Flt64>): Ret<NetworkConstructionSolution<V>> {
        if (model !== target) return networkSchedulingFailure("建设变量未注册 / Construction variables not registered")
        val open = mutableSetOf<NetworkNodeId>()
        val selected = mutableMapOf<NetworkArcId, String>()
        var total = Flt64.zero
        for (facility in problem.facilities) {
            val value = target.tokens.find(facilities.getValue(facility.node))?.result
                ?: return networkSchedulingFailure("缺失设施结果 / Missing facility result")
            if (value gr Flt64(0.5)) open.add(facility.node)
            total += value * (adapter.normalize(facility.cost, problem.flow.costUnit).value ?: return networkSchedulingFailure("设施费用无效 / Invalid facility cost"))
        }
        for (service in problem.services) {
            for ((index, variable) in services.getValue(service.arc).withIndex()) {
                val value = target.tokens.find(variable)?.result ?: return networkSchedulingFailure("缺失通道结果 / Missing service result")
                if (value gr Flt64(0.5)) selected[service.arc] = service.levels[index].id
                total += value * (adapter.normalize(service.levels[index].cost, problem.flow.costUnit).value ?: return networkSchedulingFailure("通道费用无效 / Invalid service cost"))
            }
        }
        val value = adapter.fromSolverValue(total).value
            ?: return networkSchedulingFailure("建设费用转换失败 / Construction cost conversion failed")
        return ok(NetworkConstructionSolution(
            openFacilities = open.toSet(),
            serviceLevels = selected.toMap(),
            cost = Quantity(value, problem.flow.costUnit)
        ))
    }
}

/** 共享建设部分的提取结果。 / Extracted shared construction decisions.
 * @property openFacilities 已开设施 / Open facilities
 * @property serviceLevels 建设等级 / Built levels
 * @property cost 建设成本 / Construction cost
 */
data class NetworkConstructionSolution<V : RealNumber<V>>(
    val openFacilities: Set<NetworkNodeId>,
    val serviceLevels: Map<NetworkArcId, String>,
    val cost: Quantity<V>
)
