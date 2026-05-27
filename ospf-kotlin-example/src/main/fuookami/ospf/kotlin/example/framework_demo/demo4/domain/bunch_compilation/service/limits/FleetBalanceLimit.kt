@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private data class FleetBalanceShadowPriceKey(
    val airport: Airport,
    val aircraftMinorType: AircraftMinorType,
) : ShadowPriceKey(FleetBalanceShadowPriceKey::class) {
    override fun toString() = "Fleet Balance ($airport, $aircraftMinorType)"
}

class FleetBalanceLimit(
    private val fleetBalance: FleetBalance,
    private val coefficient: (Airport, AircraftMinorType) -> Flt64,
    override val name: String = "fleet_balance_limit"
) : CGPipeline {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((l, checkPoint) in fleetBalance.limits.withIndex()) {
            when (val result = model.addConstraint(
                relation = fleetBalance.slack[l] geq checkPoint.second.amount,
                name = "${name}_${l}",
                args = FleetBalanceShadowPriceKey(checkPoint.first.airport, checkPoint.first.aircraftMinorType)
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

        val poly = MutableLinearPolynomial()
        for ((l, checkPoint) in fleetBalance.limits.withIndex()) {
            poly += LinearMonomial(
                coefficient(checkPoint.first.airport, checkPoint.first.aircraftMinorType),
                fleetBalance.slack[l]
            )
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "fleet balance")
        ) {
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

    override fun extractor(): ShadowPriceExtractor? {
        return { map, args: ShadowPriceArguments ->
            when (args) {
                is TaskShadowPriceArguments -> {
                    if (args.prevTask is FlightTask && args.task == null) {
                        map[FleetBalanceShadowPriceKey(
                            airport = (args.prevTask!! as FlightTask).arr,
                            aircraftMinorType = (args.prevTask!! as FlightTask).aircraft!!.minorType
                        )]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    override fun refresh(
        shadowPriceMap: ShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup(this)) {
            val key = constraint.args as? FleetBalanceShadowPriceKey ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(key = key, price = price))
            }
        }

        return ok
    }
}











