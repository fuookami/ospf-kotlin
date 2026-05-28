package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.utils.functional.*

class BinAmountMinimization(
    private val bins: List<Bin<BinLayer>>,
    private val assignment: PreciseAssignment,
    private val coefficient: (Bin<BinLayer>) -> InfraNumber,
    val name: String = "bin_amount_minimization"
) {
    fun invoke(model: MetaModel<InfraNumber>): Try {
        val linearModel = model as AbstractLinearMetaModel<InfraNumber>
        when (val result = linearModel.minimize(
            polynomial = sum(bins.mapIndexed { i, bin ->
                LinearMonomial(coefficient(bin), assignment.v[i])
            }),
            name = "bin amount"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}





