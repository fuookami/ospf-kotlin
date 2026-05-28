package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentZero
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

class TailBinAssignmentConstraint(
    private val bins: List<Bin<BinLayer>>,
    private val assignment: PreciseAssignment,
    val name: String = "tail_bin_assignment_constraint"
) {
    fun invoke(model: MetaModel<InfraNumber>): Try {
        val linearModel = model as AbstractLinearMetaModel<InfraNumber>
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



