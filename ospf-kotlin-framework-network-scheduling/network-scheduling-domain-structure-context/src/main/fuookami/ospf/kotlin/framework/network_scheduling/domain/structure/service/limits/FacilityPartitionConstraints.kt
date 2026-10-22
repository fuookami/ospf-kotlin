package fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 对单个分区实施必选/可选唯一归属。 / Enforce mandatory or optional unique ownership for one partition.
 * @property name 约束命名空间 / Constraint namespace
 */
class FacilityPartitionAssignmentConstraint(
    private val assignments: LinearPolynomial<Flt64>,
    private val required: Boolean,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册分区归属约束。 / Register the partition-ownership constraint.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        return if (required) {
            model.addConstraint(assignments eq Flt64.one, name = name)
        } else {
            model.addConstraint(assignments leq Flt64.one, name = name)
        }
    }
}

/** 对设施分区数量实施最小/最大配额。 / Enforce a minimum and optional maximum number of assigned partitions.
 * @property name 约束命名空间 / Constraint namespace
 */
class FacilityPartitionQuotaConstraint(
    private val assignments: LinearPolynomial<Flt64>,
    private val minimum: Int,
    private val maximum: Int?,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    /** 注册设施分区配额。 / Register the facility partition quota.
     * @param model 元模型 / Meta model
     * @return 注册结果 / Registration result
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (minimum > 0) {
            when (val result = model.addConstraint(assignments geq Flt64(minimum.toDouble()), name = "${name}_minimum")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        maximum?.let { limit ->
            return model.addConstraint(assignments leq Flt64(limit.toDouble()), name = "${name}_maximum")
        }
        return ok
    }
}
