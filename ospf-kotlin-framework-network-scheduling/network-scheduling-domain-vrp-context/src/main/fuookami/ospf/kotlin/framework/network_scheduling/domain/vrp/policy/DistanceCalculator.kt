package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.FloatingImpl
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 距离计算扩展点。 / Distance-calculation extension point. */
fun interface DistanceCalculator<V : RealNumber<V>> {
    /**
     * 计算两节点距离。 / Calculate distance between two nodes.
     *
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @return 距离或计算失败 / Distance or calculation failure
     */
    fun distance(from: NetworkNode<V>, to: NetworkNode<V>): Ret<Quantity<V>>
}

/** 完整精度欧氏距离策略。 / Full-precision Euclidean distance policy. */
open class EuclideanDistanceCalculator<V : FloatingImpl<V>>(
    private val distanceUnit: PhysicalUnit
) : DistanceCalculator<V> {
    override fun distance(from: NetworkNode<V>, to: NetworkNode<V>): Ret<Quantity<V>> {
        val fromCoordinates = from.vrpCoordinates()
        val toCoordinates = to.vrpCoordinates()
        val fromAxes = from.vrpCoordinateAttributes().map { it.first }
        val toAxes = to.vrpCoordinateAttributes().map { it.first }
        if (fromAxes.isEmpty() || fromAxes != toAxes) {
            return networkSchedulingFailure(
                "计算距离失败：节点坐标轴必须相同且非空 / " +
                        "Failed to calculate distance: node coordinate axes must be equal and non-empty"
            )
        }
        var squared = fromCoordinates.first().value.constants.zero
        for (index in fromCoordinates.indices) {
            val lhs = fromCoordinates[index].convertTo(distanceUnit)
                ?: return incompatibleCoordinate(from, distanceUnit)
            val rhs = toCoordinates[index].convertTo(distanceUnit)
                ?: return incompatibleCoordinate(to, distanceUnit)
            val difference = lhs.value - rhs.value
            squared += difference * difference
        }
        val distance = squared.sqrt()
            ?: return networkSchedulingFailure(
                "计算距离失败：平方根计算失败 / Failed to calculate distance: square-root calculation failed"
            )
        return ok(Quantity(distance, distanceUnit))
    }

    private fun incompatibleCoordinate(node: NetworkNode<V>, unit: PhysicalUnit): Ret<Quantity<V>> {
        return networkSchedulingFailure(
            "计算距离失败：节点 ${node.id.value} 的坐标无法转换到 ${unit.symbol} / " +
                    "Failed to calculate distance: coordinates of node ${node.id.value} cannot be converted to ${unit.symbol}"
        )
    }
}

/** Solomon 一位小数距离策略。 / Solomon one-decimal distance policy. */
class SolomonDistancePolicy<V : FloatingImpl<V>>(
    private val distanceUnit: PhysicalUnit
) : DistanceCalculator<V> {
    private val delegate = EuclideanDistanceCalculator<V>(distanceUnit)

    override fun distance(from: NetworkNode<V>, to: NetworkNode<V>): Ret<Quantity<V>> {
        val distance = delegate.distance(from, to).value
            ?: return networkSchedulingFailure(
                "计算 Solomon 距离失败：欧氏距离计算失败 / Failed to calculate Solomon distance: Euclidean calculation failed"
            )
        return ok(Quantity(distance.value.roundTo(1), distanceUnit))
    }
}
