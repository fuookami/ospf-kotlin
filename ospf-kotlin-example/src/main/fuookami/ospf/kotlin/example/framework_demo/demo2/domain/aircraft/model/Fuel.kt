package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

enum class FuelTankType {
    Main,
    Center,
    OuterMain,          // B747 Main Tank 1 & 4
    InnerMain,          // B747 Main Tank 2 & 3
    Reserve             // B747 Reserve Tank
}

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
            // 超过采样点的下限，取最小值
            points.first()[phase]
        } else if (volume.partialOrd(points.last().volume) is Order.Greater) {
            // 超过采样点的上限，取最大值
            points.last()[phase]
        } else {
            // 否则，使用线性插值计算
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

data class FuelTank(
    val type: FuelTankType,
    val name: String,
    val maxVolume: Quantity<Flt64>,
    val balancedArm: FuelTankBalancedArm
)

data class FuelTankView(
    val tank: FuelTank,
    val volume: Quantity<Flt64>,
    val weight: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>
) {
    val type by tank::type
    val name by tank::name
}

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

