package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Capacity
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.LayerAssignmentScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.utils.functional.*

class TailBinLoadingRateMinimization(
    private val bins: List<Bin<BinLayer>>,
    private val assignment: PreciseAssignment,
    private val capacity: Capacity,
    private val coefficient: (Bin<BinLayer>) -> LayerAssignmentScalar,
    val name: String = "tail_bin_loading_rate_minimization"
) {
    fun invoke(model: MetaModel<LayerAssignmentScalar>): Try {
        val linearModel = model as AbstractLinearMetaModel<LayerAssignmentScalar>
        when (val result = linearModel.minimize(
            polynomial = sum(bins.mapIndexed { i, bin ->
                LinearMonomial(coefficient(bin), capacity.tailLoadingRate[i])
            }),
            name = "tail bin loading rate"
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
