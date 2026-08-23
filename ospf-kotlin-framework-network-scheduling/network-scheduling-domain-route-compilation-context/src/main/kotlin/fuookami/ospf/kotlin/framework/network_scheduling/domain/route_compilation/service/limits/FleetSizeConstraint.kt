package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.shadowPriceKeyOf

/**
 * 车队数量约束管线。 / Fleet size constraint pipeline.
 *
 * 对每个车辆类型 k 添加约束：fleetUsage[k] <= amount[k]。
 * solver 返回 framework 约定的有符号 LP 对偶值（当前最小化实现和测试快照为非正），
 * 在 reduced cost 公式 `reducedCost = cost - customerDual - fleetDual` 中直接使用。
 * The solver returns framework-signed LP dual values (non-positive for the current minimization
 * implementation and test snapshots); the reduced-cost formula uses them without normalization.
 *
 * For each vehicle type k, adds constraint: fleetUsage[k] <= amount[k].
 * The solver returns framework-signed LP dual values (non-positive for the current minimization implementation).
 * In the reduced cost formula `reducedCost = cost - customerDual - fleetDual`, the signed value is used directly.
 * This follows the framework/Gantt convention; no additional normalization is applied.
 */
class FleetSizeConstraint(
    private val vehicleTypes: List<VehicleType<*>>,
    private val compilation: RouteCompilation<*>,
    override val name: String = "fleet_size"
) : CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> {

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((index, vehicleType) in vehicleTypes.withIndex()) {
            val usage = compilation.fleetUsage[index]
            when (val result = model.addConstraint(
                usage leq Flt64(vehicleType.amount.toDouble()),
                name = "${name}_${vehicleType.id.value}",
                args = FleetSizeShadowPriceKey(vehicleType.id)
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    override fun extractor(): ShadowPriceExtractor<VrpShadowPriceArguments, VrpShadowPriceMap>? {
        return object : ShadowPriceExtractor<VrpShadowPriceArguments, VrpShadowPriceMap> {
            override fun invoke(
                map: AbstractShadowPriceMap<VrpShadowPriceArguments, VrpShadowPriceMap>,
                args: VrpShadowPriceArguments
            ): Flt64 {
                return map.map[FleetSizeShadowPriceKey(args.vehicleTypeId)]?.price ?: Flt64.zero
            }
        }
    }

    override fun refresh(
        shadowPriceMap: VrpShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val key = shadowPriceKeyOf<FleetSizeShadowPriceKey>(constraint.args) ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(key, price))
            }
        }
        return ok
    }
}
