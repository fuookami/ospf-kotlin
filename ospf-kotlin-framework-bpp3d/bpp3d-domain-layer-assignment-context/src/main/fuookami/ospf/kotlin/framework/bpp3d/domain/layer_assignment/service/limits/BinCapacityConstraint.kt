package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Capacity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

class BinCapacityConstraint(
    private val bins: List<Bin<BinLayer>>,
    private val capacity: Capacity,
    val name: String = "bin_capacity_constraint"
) {
    fun invoke(model: MetaModel<Flt64>): Try {
        val linearModel = model as AbstractLinearMetaModel<Flt64>
        for ((i, bin) in bins.withIndex()) {
            val lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, capacity.loadWeight[i])),
                constant = Flt64.zero
            )
            val rhs = LinearPolynomial<Flt64>(emptyList(), bin.capacity.toFlt64())
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
