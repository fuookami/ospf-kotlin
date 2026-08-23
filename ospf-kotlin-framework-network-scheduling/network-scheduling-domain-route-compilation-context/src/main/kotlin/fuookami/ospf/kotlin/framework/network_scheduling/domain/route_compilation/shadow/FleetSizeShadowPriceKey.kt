package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow

import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VehicleTypeId

/**
 * 车队数量影子价格键。 / Fleet-size shadow price key.
 *
 * @property vehicleTypeId 车辆类型标识 / Vehicle type identifier
 */
data class FleetSizeShadowPriceKey(
    val vehicleTypeId: VehicleTypeId
) : ShadowPriceKey(FleetSizeShadowPriceKey::class)
