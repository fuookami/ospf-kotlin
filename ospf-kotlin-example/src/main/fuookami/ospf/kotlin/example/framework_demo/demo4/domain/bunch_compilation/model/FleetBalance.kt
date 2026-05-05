@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class FleetBalance(
    aircrafts: List<Aircraft>,
    originBunches: List<FlightTaskBunch>,
    private val compilation: Compilation
) {
    data class CheckPoint(
        val airport: Airport,
        val aircraftMinorType: AircraftMinorType
    ) : ManualIndexed() {
        operator fun invoke(bunch: FlightTaskBunch): Boolean {
            return bunch.aircraft.minorType == aircraftMinorType && bunch.arr == airport
        }

        override fun hashCode(): Int {
            return airport.hashCode() xor aircraftMinorType.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CheckPoint

            if (airport != other.airport) return false
            if (aircraftMinorType != other.aircraftMinorType) return false

            return true
        }
    }

    data class Limit(
        val amount: UInt64,
        val aircrafts: List<Aircraft>
    )

    val limits: List<Pair<CheckPoint, Limit>> by lazy {
        ManualIndexed.flush<CheckPoint>()

        val limits = HashMap<CheckPoint, Pair<UInt64, MutableList<Aircraft>>>()
        for (aircraft in aircrafts) {
            val bunch = originBunches.find { it.aircraft == aircraft }
            val key = if (bunch != null && !bunch.empty) {
                CheckPoint(bunch.arr, aircraft.minorType)
            } else {
                CheckPoint(aircraft.usability.location, aircraft.minorType)
            }
            if (!limits.containsKey(key)) {
                key.setIndexed()
                limits[key] = UInt64.zero to mutableListOf()
            }
            limits[key] = (limits[key]!!.first + UInt64.one) to limits[key]!!.second
        }
        limits.entries.map { it.key to Limit(it.value.first, it.value.second) }
    }

    lateinit var fleet: LinearExpressionSymbols1<Flt64>
    lateinit var slack: LinearIntermediateSymbols1<Flt64>

    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (limits.isNotEmpty()) {
            if (!::fleet.isInitialized) {
                fleet = LinearExpressionSymbols1<Flt64>(
                    "fleet",
                    Shape1(limits.size)
                ) { l, _ ->
                    val limit = limits[l]
                    val poly = MutableLinearPolynomial()
                    for (aircraft in limit.second.aircrafts) {
                        poly += LinearMonomial(Flt64.one, compilation.z[aircraft])
                    }
                    LinearExpressionSymbol(
                        poly,
                        name = "fleet_$l"
                    )
                }
            }
            when (val result = model.add(fleet)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }

            if (!::slack.isInitialized) {
                slack = LinearIntermediateSymbols1<Flt64>(
                    "fleet_slack",
                    Shape1(limits.size)
                ) { l, _ ->
                    val poly = MutableLinearPolynomial()
                    poly += LinearMonomial(Flt64.one, fleet[l])
                    SlackFunction(
                        x = LinearPolynomial(poly.monomials, poly.constant),
                        threshold = limits[l].second.amount.toFlt64(),
                        withPositive = false,
                        name = "fleet_slack_$l"
                    )
                }
            }
            when (val result = model.add(slack)) {
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

    fun addColumns(
        iteration: UInt64,
        bunches: List<FlightTaskBunch>,
    ): Try {
        val xi = compilation.x[iteration.toInt()]

        for ((checkPoint, _) in limits) {
            val thisBunches = bunches.filter { checkPoint(it) }
            if (thisBunches.isNotEmpty()) {
                val thisFleet = fleet[checkPoint]
                thisFleet.flush()
                for (bunch in thisBunches) {
                    thisFleet.asMutable() += LinearMonomial(Flt64.one, xi[bunch])
                }
            }
        }

        return ok
    }
}












