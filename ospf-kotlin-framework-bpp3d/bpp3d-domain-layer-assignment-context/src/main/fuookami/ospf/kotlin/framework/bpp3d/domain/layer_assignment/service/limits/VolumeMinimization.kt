@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.intermediate_model.times
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DCGPipeline
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class VolumeMinimization<
        Args : AbstractBPP3DShadowPriceArguments<T>,
        T : Cuboid<T>
        >(
    private val assignment: ImpreciseAssignment,
    private val coefficient: Flt64,
    override val name: String = "volume_minimization",
) : AbstractBPP3DCGPipeline<Args, T> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        when (val result = model.minimize(
            monomial = coefficient * assignment.volume,
            name = "volume"
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



