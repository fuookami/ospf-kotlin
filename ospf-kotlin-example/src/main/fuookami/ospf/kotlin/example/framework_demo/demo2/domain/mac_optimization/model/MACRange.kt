package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.MAC

/**
 * Defines the MAC (Mean Aerodynamic Chord) range with optimal and boundary points for balance optimization.
 * 定义具有最优点和边界点的 MAC（平均气动弦）范围用于平衡优化。
 *
 * @property points The list of MAC range boundary and optimal points / MAC 范围边界点和最优点列表
 * @property optPoint 最优 MAC 点 / The optimal MAC point
 * @property lhsPoints 最优点左侧的点（较低索引） / Points to the left of the optimal point (lower index)
 * @property rhsPoints 最优点右侧的点（较高索引） / Points to the right of the optimal point (higher index)
*/
class MACRange(
    val points: List<Point>
) {
    val optPoint = points.find { it.type == Type.OPT }!!
    val lhsPoints = points.filter { (it.index ls optPoint.index)!! }
    val rhsPoints = points.filter { (it.index gr optPoint.index)!! }

    /**
     * MAC range point type classification.
     * MAC 范围点类型分类。
    */
    enum class Type {
        OPT,
        A,
        B,
        C
    }

    /**
     * A point on the MAC range curve with associated balance data.
     * MAC 范围曲线上具有关联平衡数据的点。
     *
     * @property type 此范围点的类型 / The type of this range point
     * @property mac 此点的 MAC 值 / The MAC value at this point
     * @property balancedArm 此点的平衡力臂 / The balanced arm at this point
     * @property torque 此点的扭矩 / The torque at this point
     * @property index 此点的指数 / The index at this point
    */
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
            val referenceWeight = totalWeight.computedTotalWeight[FlightPhase.TakeOff]
                ?: totalWeight.maxTotalWeight[FlightPhase.TakeOff]
                ?: Quantity(Flt64.one, aircraftModel.weightUnit)
            val macValues = listOf(
                Type.A to MAC(Flt64(-1.0)),
                Type.OPT to MAC(Flt64.zero),
                Type.B to MAC(Flt64.one),
                Type.C to MAC(Flt64.two)
            )
            val points = macValues.map { (type, mac) ->
                val balancedArm = formula.balancedArm(mac)
                val torque = (aircraftModel.gravity(referenceWeight) * balancedArm)!!
                    .to(aircraftModel.torqueUnit)!!
                val index = ((torque / formula.forceDistanceCoefficient)!!
                    + formula.doiCorrection.to(aircraftModel.torqueUnit)!!)!!
                Point(
                    type = type,
                    mac = mac,
                    balancedArm = balancedArm,
                    torque = torque,
                    index = index
                )
            }
            return MACRange(points)
        }
    }
}
