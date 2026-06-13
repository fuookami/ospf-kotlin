/**
 * 尾箱分配约束。
 * Tail bin assignment constraint.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentZero
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * 尾箱分配约束，确保尾箱标记与分配一致。
 * Tail bin assignment constraint, ensures tail bin marking is consistent with assignment.
 *
 * @property bins 箱子列表 / bin list
 * @property assignment 精确赋值 / precise assignment
 * @property name 约束名称 / constraint name
 */
class TailBinAssignmentConstraint(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val assignment: PreciseAssignment,
    val name: String = "tail_bin_assignment_constraint"
) {
    /**
     * 将约束添加到模型。
     * Add constraint to model.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
     */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        for ((i, bin) in bins.withIndex()) {
            if (i == bins.lastIndex) {
                val lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), assignment.v[i])),
                    constant = layerAssignmentZero()
                )
                val rhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), assignment.tail[i])),
                    constant = layerAssignmentZero()
                )
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
            } else {
                val lhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), assignment.tail[i])),
                    constant = layerAssignmentZero()
                )
                val rhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), assignment.v[i + 1])),
                    constant = layerAssignmentZero()
                )
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
        }

        return ok
    }
}



