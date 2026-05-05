package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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

            val poly = MutableLinearPolynomial()
            for (part in zone.parts) {
                val j = positions.indexOf(part.position)
                poly += LinearMonomial(
                    part.weight,
                    load.estimateLoadWeight[j].value
                )
            }
            if (zone.liferaft != null) {
                poly += fuselage.liferaft!!.weight.to(aircraftModel.weightUnit)!!.value
            }
            when (val result = model.addConstraint(
                relation = LinearPolynomial(poly.monomials, poly.constant) leq zone.maxLoadWeight.to(aircraftModel.weightUnit)!!.value,
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

















