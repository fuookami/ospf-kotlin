package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

class Aggregation(
    internal val aircraftModel: AircraftModel,
    formula: Formula,
    fuselage: Fuselage,
    fuel: Map<FlightPhase, FuelConstant>,
    val flight: Flight,
    val items: List<Item>,
    val positions: List<Position>,
    plannedPayload: Quantity<Flt64>,
    maxPayload: Quantity<Flt64>,
    computedPayload: Quantity<Flt64>?,
    maxTotalWeight: Map<FlightPhase, Quantity<Flt64>>,
    computedTotalWeight: Map<FlightPhase, Quantity<Flt64>>,
    val withMultiLoadingSchema: Boolean,
    val appointment: Appointment,
    val biologicalLimit: BiologicalLimit,
    internal val neighbours: HashMap<NeighbourType, List<Neighbour>>
) {
    val stowage = Stowage(
        items = items,
        positions = positions,
    )

    val load = Load(
        aircraftModel = aircraftModel,
        formula = formula,
        items = items,
        positions = positions,
        withMultiLoadingSchema = withMultiLoadingSchema,
        stowage = stowage
    )

    val payload = Payload(
        plannedPayload = plannedPayload,
        maxPayload = maxPayload,
        computedPayload = computedPayload,
        aircraftModel = aircraftModel,
        items = items,
        positions = positions,
        load = load
    )

    val totalWeight = TotalWeight(
        maxTotalWeight = maxTotalWeight,
        computedTotalWeight = computedTotalWeight,
        aircraftModel = aircraftModel,
        fuselage = fuselage,
        fuel = fuel,
        payload = payload,
    )

    val maxLoadWeight = MaxLoadWeight(
        aircraftModel = aircraftModel,
        fuselage = fuselage,
        items = items,
        positions = positions,
        totalWeight = totalWeight
    )

    val ballast = if (aircraftModel.ballastNeeded) {
        Ballast(
            aircraftModel = aircraftModel,
            positions = positions,
            minBallastWeight = null,
            load = load
        )
    } else {
        null
    }
    
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModelF64
    ): Try {
        when (val result = stowage.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = load.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = payload.register(stowageMode, model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = totalWeight.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }


        when (val result = maxLoadWeight.register(model)) {
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
        model: AbstractLinearMetaModelF64
    ): Try {
        TODO("not implemented yet")
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModelF64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModelF64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }
}













