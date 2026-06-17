package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 定义具有最优点和边界点的 MAC（平均气动弦）范围用于平衡优化。Defines the MAC (Mean Aerodynamic Chord) range with optimal and boundary points for balance optimization.
 *
 * @property points 参数。
 */
class MACRange(
    val points: List<Point>
) {
    val optPoint = points.find { it.type == Type.OPT }!!
    val lhsPoints = points.filter { (it.index ls optPoint.index)!! }
    val rhsPoints = points.filter { (it.index gr optPoint.index)!! }

    enum class Type {
        OPT,
        A,
        B,
        C
    }

    data class Point(
        val type: Type,
        val mac: MAC,
        val balancedArm: Quantity<Flt64>,
        val torque: Quantity<Flt64>,
        val index: Quantity<Flt64>
    )

    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            formula: Formula,
            totalWeight: TotalWeight
        ): MACRange {
            TODO("not implemented yet")
        }
    }
}
