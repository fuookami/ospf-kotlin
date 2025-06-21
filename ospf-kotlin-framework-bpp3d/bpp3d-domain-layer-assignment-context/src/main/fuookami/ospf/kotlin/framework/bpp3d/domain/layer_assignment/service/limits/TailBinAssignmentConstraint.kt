package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

class TailBinAssignmentConstraint(
    private val bins: List<Bin<BinLayer>>,
    private val assignment: PreciseAssignment,
    override val name: String = "tail_bin_assignment_constraint"
) : Pipeline<AbstractLinearMetaModel> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (i in bins.indices) {
            if (i == bins.lastIndex) {
                when (val result = model.addConstraint(
                    assignment.tail[i] geq assignment.v[i],
                    name = "${name}_lb_${i}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else if (i != 0) {
                if (bins[i - 1].shape == bins[i].shape && bins[i].shape != bins[i + 1].shape) {
                    when (val result = model.addConstraint(
                        assignment.tail[i] geq assignment.v[i],
                        name = "${name}_lb_${i}"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                } else if (bins[i - 1].shape == bins[i].shape && bins[i].shape == bins[i + 1].shape) {
                    when (val result = model.addConstraint(
                        assignment.tail[i] geq (assignment.v[i] - assignment.v[i + 1]),
                        name = "${name}_lb_${i}"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                } else if (bins[i - 1].shape != bins[i].shape && bins[i].shape != bins[i + 1].shape) {
                    when (val result = model.addConstraint(
                        assignment.tail[i] geq assignment.v[i],
                        name = "${name}_lb_${i}"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                }
            }
        }

        for (i in bins.indices) {
            when (val result = model.addConstraint(
                assignment.tail[i] leq assignment.v[i],
                name = "${name}_ub_${i}"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}
