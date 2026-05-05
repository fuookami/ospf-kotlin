package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac


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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.MAC


class Aggregation(
    aircraftModel: AircraftModel,
    fuselage: Fuselage,
    fuel: Map<FlightPhase, FuelConstant>,
    formula: Formula,
    positions: List<Position>,
    load: Load,
    totalWeight: TotalWeight,
    horizontalStabilizers: HashMap<HorizontalStabilizer.Key, Pair<List<HorizontalStabilizer.Point>, HorizontalStabilizer.Limit>>
) {
    val torque = Torque(
        aircraftModel = aircraftModel,
        fuselage = fuselage,
        fuel = fuel,
        formula = formula,
        positions = positions,
        load = load
    )

    val mac = MAC(
        aircraftModel = aircraftModel,
        formula = formula,
        totalWeight = totalWeight,
        torque = torque
    )

    val horizontalStabilizers = horizontalStabilizers.mapValues {
        HorizontalStabilizer(
            aircraftModel = aircraftModel,
            key = it.key,
            points = it.value.first,
            limit = it.value.second,
            totalWeight = totalWeight,
            mac = mac
        )
    }

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = torque.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = mac.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        horizontalStabilizers.values.forEach {
            when (val result = it.register(stowageMode, model)) {
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

    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // MAC computation constraints go into the master problem.
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // MAC does not contribute to the sub problem.
        return ok
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}













