/**
 * Bin loading order constraint.
 * 箱子装载顺序约束。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

/**
 * Bin loading order constraint, ensures same-type bins are used in order.
 * 箱子装载顺序约束，确保同类型箱子按顺序使用。
 *
 * @property bins 箱子列表 / bin list
 * @property assignment 精确赋值 / precise assignment
 * @property capacity 容量符号 / capacity symbols
 * @property name 约束名称 / constraint name
*/
class BinLoadingOrderConstraint(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val assignment: PreciseAssignment,
    private val capacity: Capacity,
    val name: String = "bin_loading_order_constraint"
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
        for (i in bins.indices) {
            if (i == 0) {
                continue
            }

            if (bins[i].shape == bins[i - 1].shape) {
                val assignLhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), assignment.v[i - 1])),
                    constant = layerAssignmentZero()
                )
                val assignRhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), assignment.v[i])),
                    constant = layerAssignmentZero()
                )
                when (val result = linearModel.addConstraint(
                    relation = LinearInequality(assignLhs, assignRhs, Comparison.GE),
                    name = "${name}_${i - 1}_${i}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }

                val volumeLhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), capacity.loadVolume[i - 1])),
                    constant = layerAssignmentZero()
                )
                val volumeRhs = LinearPolynomial(
                    monomials = listOf(LinearMonomial(layerAssignmentOne(), capacity.loadVolume[i])),
                    constant = layerAssignmentZero()
                )
                when (val result = linearModel.addConstraint(
                    relation = LinearInequality(volumeLhs, volumeRhs, Comparison.GE),
                    name = "${name}_volume_${i - 1}_${i}"
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
