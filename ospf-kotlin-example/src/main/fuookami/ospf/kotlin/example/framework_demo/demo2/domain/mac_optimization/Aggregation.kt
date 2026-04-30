package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization


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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model.*

class Aggregation(
    internal val aircraftModel: AircraftModel,
    formula: Formula,
    totalWeight: TotalWeight,
    torque: Torque,
    internal val horizontalStabilizers: Map<HorizontalStabilizer.Key, HorizontalStabilizer>
) {
    val macRange = MACRange(
        aircraftModel = aircraftModel,
        formula = formula,
        totalWeight = totalWeight
    )

    val longitudinalBalance = LongitudinalBalance(
        aircraftModel = aircraftModel,
        macRange = macRange,
        torque = torque
    )

    val lateralBalance = if (aircraftModel.wideBody) {
        LateralBalance(
            aircraftModel = aircraftModel,
            torque = torque
        )
    } else {
        null
    }

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModelFlt64
    ): Try {
        when (val result = longitudinalBalance.register(stowageMode, model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (lateralBalance != null) {
            when (val result = lateralBalance.register(model)) {
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
        model: AbstractLinearMetaModelFlt64
    ): Try {
        TODO("not implemented yet")
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModelFlt64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModelFlt64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }
}













