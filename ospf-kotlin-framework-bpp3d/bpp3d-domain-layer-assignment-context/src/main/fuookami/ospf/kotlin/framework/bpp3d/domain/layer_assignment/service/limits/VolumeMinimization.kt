package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

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
            coefficient * assignment.volume,
            "volume"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
