package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*

/**
 * 管线列表生成器。 / Pipeline list generator.
 *
 * 根据定价阶段生成不同的管线列表：
 * - Phase I：客户覆盖约束 + 车队约束 + Phase-I 最小化
 * - Phase II：客户覆盖约束 + 车队约束 + 路线成本最小化
 *
 * Generates different pipeline lists based on the pricing phase:
 * - Phase I: customer coverage constraint + fleet constraint + Phase-I minimization
 * - Phase II: customer coverage constraint + fleet constraint + route cost minimization
 */
object PipelineListGenerator {

    /**
     * 生成 Phase-I 管线列表。 / Generate Phase-I pipeline list.
     */
    fun phaseOne(
        instance: VrptwInstance<*>,
        compilation: RouteCompilation<*>,
        extraPipelines: List<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>> = emptyList(),
        customerCoverageConstraint: CustomerCoverageConstraint = CustomerCoverageConstraint(instance.customers, compilation),
        fleetSizeConstraint: FleetSizeConstraint = FleetSizeConstraint(instance.vehicleTypes, compilation)
    ): CGPipelineList<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> {
        return listOf(
            customerCoverageConstraint,
            fleetSizeConstraint,
            PhaseOneMinimization(compilation)
        ) + extraPipelines
    }

    /**
     * 生成 Phase-II 管线列表。 / Generate Phase-II pipeline list.
     */
    fun phaseTwo(
        instance: VrptwInstance<*>,
        compilation: RouteCompilation<*>,
        extraPipelines: List<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>> = emptyList(),
        customerCoverageConstraint: CustomerCoverageConstraint = CustomerCoverageConstraint(instance.customers, compilation),
        fleetSizeConstraint: FleetSizeConstraint = FleetSizeConstraint(instance.vehicleTypes, compilation)
    ): CGPipelineList<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> {
        return listOf(
            customerCoverageConstraint,
            fleetSizeConstraint,
            RouteCostMinimization(compilation)
        ) + extraPipelines
    }
}
