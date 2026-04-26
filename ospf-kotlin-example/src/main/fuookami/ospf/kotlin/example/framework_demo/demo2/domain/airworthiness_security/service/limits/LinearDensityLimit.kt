package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*

class LinearDensityLimit(
    private val aircraftModel: AircraftModel,
    private val linearDensity: LinearDensity,
    private val positions: List<Position>,
    override val name: String = "linear_density_limit"
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        for (line in linearDensity.limitLines) {
            if (line.positions.none { it.status.available }) {
                continue
            }

            val poly = MutableLinearPolynomial()
            for (position in line.positions) {
                val j = positions.indexOf(position)
                poly += LinearMonomial(Flt64.one, linearDensity.linearDensity[j].value)
            }
            when (val result = model.addConstraint(
                relation = LinearPolynomial(poly.monomials, poly.constant) leq line.zone.maxLinearDensity.to(aircraftModel.linearDensityUnit)!!.value,
                name = "${name}_${line.zone.name}_${line.arm.value}"
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


















