package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 约束每个机身区域内的总载荷重量到最大允许值。Constrains the total load weight within each fuselage zone to the maximum allowed value.
 *
 * @property aircraftModel 提供单位配置的飞机型号 / The aircraft model providing unit configuration
 * @property fuselage 提供救生筏重量数据的机身 / The fuselage providing liferaft weight data
 * @property maxZoneLoadWeight 各区域最大载荷重量限制 / The maximum zone load weight limits
 * @property positions 货物位置列表 / The list of cargo positions
 * @property load 载荷估算模型 / The load estimation model
*/
class ZoneLoadWeightLimit(
    private val aircraftModel: AircraftModel,
    private val fuselage: Fuselage,
    private val maxZoneLoadWeight: MaxZoneLoadWeight,
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "zone_load_weight_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (zone in maxZoneLoadWeight.limitZones) {
            if (zone.parts.none { it.position.status.available }) {
                continue
            }

            val poly = sum(zone.parts.map { part ->
                val j = positions.indexOf(part.position)
                part.weight * load.estimateLoadWeight[j].value
            }) + (zone.liferaft?.let {
                fuselage.liferaft!!.weight.to(aircraftModel.weightUnit)!!.value
            } ?: Flt64.zero)
            when (val result = model.addConstraint(
                relation = poly leq zone.maxLoadWeight.to(aircraftModel.weightUnit)!!.value,
                name = "${name}_${zone.name}"
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
