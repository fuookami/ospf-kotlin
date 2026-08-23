package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*

/**
 * 路线成本最小化管线。 / Route cost minimization pipeline.
 *
 * Phase II 最小化路线成本：min routeCost
 * Phase I 时此管线不激活（由 PhaseOneMinimization 代替）。
 *
 * Phase II minimizes route cost: min routeCost.
 * In Phase I, this pipeline is not active (replaced by PhaseOneMinimization).
 */
class RouteCostMinimization(
    private val compilation: RouteCompilation<*>,
    override val name: String = "route_cost_minimization"
) : CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> {

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            symbol = compilation.routeCost,
            name = name
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    override fun extractor(): ShadowPriceExtractor<VrpShadowPriceArguments, VrpShadowPriceMap>? = null

    override fun refresh(
        shadowPriceMap: VrpShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try = ok
}
