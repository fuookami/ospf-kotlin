@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.intermediate_model.minus
import fuookami.ospf.kotlin.core.intermediate_model.geq
import fuookami.ospf.kotlin.core.intermediate_model.leq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.utils.functional.*

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

                    is Fatal -> {
                        return Fatal(result.errors)
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

                        is Fatal -> {
                            return Fatal(result.errors)
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

                        is Fatal -> {
                            return Fatal(result.errors)
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

                        is Fatal -> {
                            return Fatal(result.errors)
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
