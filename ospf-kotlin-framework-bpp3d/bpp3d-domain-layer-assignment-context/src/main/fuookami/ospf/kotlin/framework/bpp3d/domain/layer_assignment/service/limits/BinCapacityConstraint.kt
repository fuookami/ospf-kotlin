/**
 * Bin capacity constraint.
 * 箱子容量约束。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

/**
 * Bin capacity constraint, ensures load weight does not exceed bin capacity.
 * 箱子容量约束，确保装载重量不超过箱子承载能力。
 *
 * @property bins 箱子列表 / bin list
 * @property capacity 容量符号 / capacity symbols
 * @property solverValueAdapter 求解器值适配器 / solver value adapter
 * @property name 约束名称 / constraint name
 */
class BinCapacityConstraint(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val capacity: Capacity,
    private val solverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter,
    val name: String = "bin_capacity_constraint"
) {
    /**
     * Add constraint to model.
     * 将约束添加到模型。
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
     */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        for ((i, bin) in bins.withIndex()) {
            val lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(layerAssignmentOne(), capacity.loadWeight[i])),
                constant = layerAssignmentZero()
            )
            val rhs = LinearPolynomial<FltX>(emptyList(), solverValueAdapter.weightToSolver(bin.capacity))
            when (val result = linearModel.addConstraint(
                relation = LinearInequality(lhs, rhs, Comparison.LE),
                name = "${name}_${i}"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
