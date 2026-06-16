package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.quantities.quantity.*

/** Types of fuel tanks available on an aircraft. */
enum class FuelTankType {
    Main,
    Center,
    OuterMain,          // B747 Main Tank 1 & 4
    InnerMain,          // B747 Main Tank 2 & 3
    Reserve             // B747 Reserve Tank
}

/** Interpolated balanced arm lookup table for a fuel tank across different volumes and flight phases. */
data class FuelTankBalancedArm(
    val points: List<Point>
) {
    data class Point(
        val volume: Quantity<Flt64>,
        val takeOffBalancedArm: Quantity<Flt64>,
        val landingBalancedArm: Quantity<Flt64>?
    ) {
        operator fun get(phase: FlightPhase): Quantity<Flt64> {
            return when (phase) {
                FlightPhase.TakeOff, FlightPhase.ZeroFuel -> takeOffBalancedArm
                FlightPhase.Landing -> landingBalancedArm ?: takeOffBalancedArm
            }
        }
    }

    operator fun invoke(phase: FlightPhase, volume: Quantity<Flt64>): Quantity<Flt64> {
        assert(points.size >= 2)
        return if (volume.partialOrd(points.first().volume) is Order.Less) {
            points.first()[phase]
        } else if (volume.partialOrd(points.last().volume) is Order.Greater) {
            points.last()[phase]
        } else {
            val i = (1 until points.size).first {
                points[it - 1].volume.partialOrd(volume) is Order.Less
                    && volume.partialOrd(points[it].volume) is Order.Less
            }
            val dy = points[i][phase] - points[i - 1][phase]
            val dx = points[i].volume - points[i - 1].volume
            (dy / dx) * (volume - points[i - 1].volume)
        }
    }
}

/** A fuel tank with its type, capacity, and balanced arm lookup data. */
data class FuelTank(
    val type: FuelTankType,
    val name: String,
    val maxVolume: Quantity<Flt64>,
    val balancedArm: FuelTankBalancedArm
)

/** A snapshot view of a fuel tank at a specific volume with computed weight and balanced arm. */
data class FuelTankView(
    val tank: FuelTank,
    val volume: Quantity<Flt64>,
    val weight: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>
) {
    val type by tank::type
    val name by tank::name
}

/** Precomputed fuel constants (density, weight, index) for a given flight phase. */
data class FuelConstant(
    val density: Quantity<Flt64>,
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
) {
    companion object {
        operator fun invoke(
            density: Quantity<Flt64>,
            weight: Quantity<Flt64>,
            tanks: List<FuelTankView>,
            aircraftModel: AircraftModel,
            formula: Formula
        ): FuelConstant {
            var index = Quantity(Flt64.zero, aircraftModel.torqueUnit)
            for (tank in tanks) {
                index += formula.index(tank.weight, tank.balancedArm)
            }
            return FuelConstant(
                density = density,
                weight = weight,
                index = index
            )
        }
    }
}
