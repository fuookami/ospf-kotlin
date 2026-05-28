package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Capacity
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentZero
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

class BinLoadingOrderConstraint(
    private val bins: List<Bin<BinLayer>>,
    private val assignment: PreciseAssignment,
    private val capacity: Capacity,
    val name: String = "bin_loading_order_constraint"
) {
    fun invoke(model: MetaModel<Flt64>): Try {
        val linearModel = model as AbstractLinearMetaModel<Flt64>
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


