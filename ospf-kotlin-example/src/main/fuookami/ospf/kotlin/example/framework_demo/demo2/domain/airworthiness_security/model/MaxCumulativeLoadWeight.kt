package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class MaxCumulativeLoadWeight(
    val limitZones: List<LimitZone>,
) {
    enum class Direction {
        FWD,
        AFT
    }

    data class Part(
        val position: Position,
        val weight: Flt64
    )

    data class CheckPoint(
        val zone: LimitZone,
        val toArm: Quantity<Flt64>,
        val maxSum: Quantity<Flt64>,
        val parts: List<Part>
    )
    data class Point(
        val toArm: Quantity<Flt64>,
        val maxSum: Quantity<Flt64>,
    )

    data class LimitZone(
        val direction: Direction,
        val name: String,
        val fromArm: Quantity<Flt64>,
        val points: List<Point>,
        val checkpoints: List<CheckPoint>
    ) {
        companion object {
            operator fun invoke(
                direction: Direction,
                fromArm: Quantity<Flt64>,
                points: List<Point>,
                positions: List<Position>
            ): LimitZone {
                TODO("not implemented yet")
            }
        }
    }

    val checkPoints by lazy {
        limitZones.flatMap { it.checkpoints }
    }
}
