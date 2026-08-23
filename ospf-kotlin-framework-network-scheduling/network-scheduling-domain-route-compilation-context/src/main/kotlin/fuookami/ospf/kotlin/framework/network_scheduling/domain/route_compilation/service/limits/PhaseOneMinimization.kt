package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*

/**
 * Phase-I 最小化管线。 / Phase-I minimization pipeline.
 *
 * Phase I 最小化人工变量总和：min artificialCost
 * 真实路线列的 Phase-I 目标系数为 0。 / Phase I minimizes the sum of artificial variables: min artificialCost.
 * The Phase-I objective coefficient for real route columns is 0.
 */
class PhaseOneMinimization(
    private val compilation: RouteCompilation<*>,
    override val name: String = "phase_one_minimization"
) : CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> {

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            symbol = compilation.artificialCoverage.artificialCost,
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
