package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.frontend.model.mechanism.geq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Capacity
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.utils.functional.*

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

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}
