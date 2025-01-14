package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

class BinLoadingOrderConstraint(
    private val bins: List<Bin<BinLayer>>,
    private val assignment: PreciseAssignment,
    private val capacity: Capacity,
    override val name: String = "bin_loading_order_constraint"
) : Pipeline<AbstractLinearMetaModel> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (i in bins.indices) {
            if (i == 0) {
                continue
            }

            if (bins[i].shape == bins[i - 1].shape) {
                when (val result = model.addConstraint(
                    assignment.v[i - 1] geq assignment.v[i],
                    name = "${name}_${i - 1}_${i}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    capacity.loadVolume[i - 1] geq capacity.loadVolume[i],
                    name = "${name}_volume_${i - 1}_${i}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        return ok
    }
}
