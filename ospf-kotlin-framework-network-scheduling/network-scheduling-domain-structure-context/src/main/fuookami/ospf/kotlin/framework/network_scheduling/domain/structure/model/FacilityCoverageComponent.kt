package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.NetworkDesignContext
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.NetworkStructureProblem
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.service.limits.FacilityPartitionAssignmentConstraint
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.service.limits.FacilityPartitionQuotaConstraint

/** 分区到合格设施的显式归属组件。 / Explicit partition assignments to eligible facilities.
 * @property problem 结构问题 / Structure problem
 * @property designContext 建设上下文 / Construction context
 * @property name 模型命名空间 / Model namespace
 */
class FacilityCoverageComponent<V : RealNumber<V>>(
    val problem: NetworkStructureProblem<V>,
    val designContext: NetworkDesignContext<V>,
    val name: String
) {
    private val assignments = mutableMapOf<Pair<String, NetworkNodeId>, BinVar>()
    private var attempted = false
    private var registeredModel: AbstractLinearMetaModel<Flt64>? = null

    /** 注册唯一覆盖、设施启用联动和设施分区配额。 / Register unique coverage, facility activation and quotas.
     * @param model 目标元模型 / Target meta model
     * @return 注册结果 / Registration result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (attempted) return networkSchedulingFailure("设施覆盖组件不可复用 / Facility coverage component cannot be reused")
        attempted = true
        for (partition in problem.partitions) {
            var expression = LinearPolynomial()
            for (facility in partition.eligibleFacilities) {
                val variable = BinVar("${name}_partition_${partition.id}_facility_${facility.value}")
                when (val result = model.add(variable)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                assignments[partition.id to facility] = variable
                expression += variable
                when (val result = model.addConstraint(
                    assignmentExpression(variable) leq designContext.aggregation.construction.facilityAvailability(facility),
                    name = "${name}_partition_${partition.id}_open_${facility.value}"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            when (val result = FacilityPartitionAssignmentConstraint(
                assignments = expression,
                required = partition.required,
                name = "${name}_partition_${partition.id}_coverage"
            )(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (quota in problem.quotas) {
            var expression = LinearPolynomial()
            for ((key, variable) in assignments) {
                if (key.second == quota.facility) expression += variable
            }
            when (val result = FacilityPartitionQuotaConstraint(
                assignments = expression,
                minimum = quota.minimum,
                maximum = quota.maximum,
                name = "${name}_facility_${quota.facility.value}_quota"
            )(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        registeredModel = model
        return ok
    }

    /** 读取分区-设施二元归属。 / Read a binary partition-facility assignment.
     * @param partition 分区标识 / Partition identity
     * @param facility 设施节点 / Facility node
     * @return 归属表达式或不合格组合错误 / Assignment expression or ineligible-pair error
     */
    fun assignment(partition: String, facility: NetworkNodeId): Ret<LinearPolynomial<Flt64>> {
        if (registeredModel == null) return networkSchedulingFailure("设施覆盖尚未注册 / Facility coverage is not registered")
        val variable = assignments[partition to facility]
            ?: return networkSchedulingFailure("设施不具备分区资格 / Facility is not eligible for the partition")
        return ok(assignmentExpression(variable))
    }

    /** 提取已归属分区并校验唯一所有者。 / Extract assigned partitions and verify unique ownership.
     * @param model 已求解元模型 / Solved meta model
     * @return 分区所有者或缺失/冲突错误 / Partition owners or missing/conflicting-result error
     */
    internal fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<Map<String, NetworkNodeId>> {
        if (registeredModel !== model) return networkSchedulingFailure("设施覆盖模型未注册 / Facility-coverage model is not registered")
        val owners = mutableMapOf<String, NetworkNodeId>()
        for (partition in problem.partitions) {
            for (facility in partition.eligibleFacilities) {
                val variable = assignments.getValue(partition.id to facility)
                val value = model.tokens.find(variable)?.result
                    ?: return networkSchedulingFailure("缺失分区归属结果 / Missing partition-assignment result")
                if (value gr Flt64(0.5) && owners.put(partition.id, facility) != null) {
                    return networkSchedulingFailure("分区被分配给多个设施 / Partition is assigned to multiple facilities")
                }
            }
            if (partition.required && partition.id !in owners) {
                return networkSchedulingFailure("必需分区没有设施归属 / Required partition has no facility owner")
            }
        }
        return ok(owners.toMap())
    }

    private fun assignmentExpression(variable: BinVar): LinearPolynomial<Flt64> {
        var expression = LinearPolynomial()
        expression += variable
        return expression
    }
}
