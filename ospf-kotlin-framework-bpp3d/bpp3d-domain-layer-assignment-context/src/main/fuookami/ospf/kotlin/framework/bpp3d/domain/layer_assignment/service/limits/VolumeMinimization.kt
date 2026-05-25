package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.ImpreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.LayerAssignmentScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution

class VolumeMinimization<
        Args : AbstractBPP3DShadowPriceArguments<T>,
        T : Cuboid<T>
        >(
    private val assignment: ImpreciseAssignment,
    private val coefficient: LayerAssignmentScalar,
    override val name: String = "volume_minimization",
) : CGPipeline<Args, AbstractLinearMetaModel<LayerAssignmentScalar>, AbstractBPP3DShadowPriceMap<Args, T>> {
    override fun invoke(model: AbstractLinearMetaModel<LayerAssignmentScalar>): Try {
        when (val result = model.minimize(
            monomial = LinearMonomial(coefficient, assignment.volume),
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
