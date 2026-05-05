package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model.*

class Aggregation(
    internal val aircraftModel: AircraftModel,
    flight: Flight,
    items: List<Item>,
    positions: List<Position>,
    stowage: Stowage,
    load: Load,
    payload: Payload
) {
    val redundancy = Redundancy(
        aircraftModel = aircraftModel,
        flight = flight,
        items = items,
        positions = positions,
        stowage = stowage,
        load = load,
        payload = payload
    )

    val experimentalLongitudinalBalance = ExperimentalLongitudinalBalance(
        aircraftModel = aircraftModel,
        positions = positions,
        load = load,
        payload = payload,
        redundancy = redundancy
    )

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = redundancy.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = experimentalLongitudinalBalance.register(model)) {
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

    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Redundancy constraints go into the master problem.
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // Redundancy does not contribute to the sub problem.
        return ok
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}













