@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.intermediate_model.times
import fuookami.ospf.kotlin.core.intermediate_model.sum
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Capacity
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class TailBinLoadingRateMinimization(
    private val bins: List<Bin<BinLayer>>,
    private val capacity: Capacity,
    private val coefficient: (Bin<BinLayer>) -> Flt64,
    override val name: String = "tail_bin_loading_rate_minimization"
) : Pipeline<AbstractLinearMetaModel> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        when (val result = model.minimize(
            polynomial = sum(bins.mapIndexed { i, bin ->
                coefficient(bin) * capacity.tailLoadingRate[i]
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



