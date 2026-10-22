package fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.*

/** 独立辅助连通流组件，不承诺故障后需求容量。 / Auxiliary connectivity-flow component, not demand-capacity assurance.
 * @property design 共享结构决策 / Shared structural decisions
 * @property requirement 连通要求 / Connectivity requirement
 * @property index 要求索引 / Requirement index
 * @property name 命名空间 / Namespace
 */
class ConnectivityFlow<V : RealNumber<V>>(
    val design: NetworkDesignContext<V>,
    val requirement: ConnectivityRequirement,
    val index: Int,
    val name: String
) {
    internal val variables = mutableMapOf<NetworkArcId, URealVar>()
    private var attempted = false

    /** 注册单位容量连通流与内部节点吞吐。 / Register unit-capacity connectivity and internal-node throughput.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (attempted) return networkSchedulingFailure("连通流组件不可复用 / Connectivity component cannot be reused")
        attempted = true
        val graph = design.problem.flow.graph
        if (requirement.source == requirement.target || requirement.paths < 1 ||
            graph.node(requirement.source) == null || graph.node(requirement.target) == null ||
            (requirement.kind == ConnectivityKind.Reachable && requirement.paths != 1)) {
            return networkSchedulingFailure("连通要求无效 / Invalid connectivity requirement")
        }
        val amount = Flt64(requirement.paths.toDouble())
        for (arc in graph.arcs) {
            val variable = URealVar("${name}_connect_${index}_${arc.id.value}")
            variables[arc.id] = variable
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val bound = if (requirement.kind == ConnectivityKind.Reachable) amount else Flt64.one
            val gates = listOf(
                design.availability(arc.id),
                design.facilityAvailability(arc.from),
                design.facilityAvailability(arc.to)
            )
            for (gate in gates) {
                when (val result = model.addConstraint(variable leq bound * gate)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        for (node in graph.nodes) {
            var outgoing = LinearPolynomial()
            var incoming = LinearPolynomial()
            graph.outgoing(node.id).forEach { outgoing += variables.getValue(it.id) }
            graph.incoming(node.id).forEach { incoming += variables.getValue(it.id) }
            val balance = when (node.id) {
                requirement.source -> amount
                requirement.target -> -amount
                else -> Flt64.zero
            }
            when (val result = model.addConstraint(outgoing - incoming eq balance)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (requirement.kind == ConnectivityKind.NodeDisjoint && node.id != requirement.source && node.id != requirement.target) {
                // 中间节点吞吐至多一，等价于节点拆分的单位容量弧。 / Unit internal-node throughput equals node splitting.
                when (val result = model.addConstraint(outgoing leq Flt64.one)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }
}
