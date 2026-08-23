package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNode

private val coordinateKeys = listOf("x", "y", "z")

/**
 * 读取 VRP 约定的坐标属性。 / Read coordinates using the VRP attribute convention.
 *
 * 通用网络节点不定义坐标语义；VRP 仅约定 `x`、`y`、`z` 属性作为坐标轴。 / / Generic network nodes have no coordinate semantics; VRP reserves `x`, `y`, and `z` for axes.
 */
internal fun <V : RealNumber<V>> NetworkNode<V>.vrpCoordinates(): List<Quantity<V>> {
    return vrpCoordinateAttributes().map { it.second }
}

/**
 * 读取带轴名称的 VRP 坐标快照。 / Read a VRP coordinate snapshot retaining axis names.
 */
internal fun <V : RealNumber<V>> NetworkNode<V>.vrpCoordinateAttributes(): List<Pair<String, Quantity<V>>> {
    return coordinateKeys.mapNotNull { key ->
        attributes[key]?.let { value -> key to value }
    }
}
