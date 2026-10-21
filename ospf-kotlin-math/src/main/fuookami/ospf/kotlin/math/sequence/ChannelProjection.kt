/**
 * 通道投影 / Channel projection
 *
 * 混沌吸引子与分形迭代器都会产出多分量序列（标量、二维点、三维点、四维点等）。
 * [ChannelProjection] 把这些异构输出统一投影为单个标量通道，
 * 使上层的参数演化算子可以用同一套接口消费混沌与分形序列。 / Chaotic attractors and fractal iterators both produce multi-component sequences (scalar, 2D point, 3D point, 4D point, etc.).
 * [ChannelProjection] projects these heterogeneous outputs into a single scalar channel,
 * so upper-layer parameter-evolution operators can consume chaotic and fractal sequences through one interface.
*/
package fuookami.ospf.kotlin.math.sequence

import kotlin.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*

/**
 * 通道投影策略 / Channel projection strategy
*/
sealed interface ChannelProjection {

    /** 标量通道，直接取值 / Scalar channel, taking the value as is */
    data object Scalar : ChannelProjection

    /**
     * 分量通道，取指定分量下标 / Component channel, taking the component at the given index
     *
     * @property index 分量下标 / Component index
    */
    data class Component(val index: Int) : ChannelProjection

    /** 模长通道，取 L2 范数 / Magnitude channel, taking the L2 norm */
    data object Magnitude : ChannelProjection

    /** 相位通道，取复平面极角（需要至少两个分量）/ Phase channel, taking the polar angle in the complex plane (requires at least two components) */
    data object Phase : ChannelProjection
}

/**
 * 将标量投影为标量通道 / Project a scalar into a scalar channel
 *
 * @param value 标量值 / Scalar value
 * @return 投影结果 / Projection result
*/
fun ChannelProjection.project(value: Flt64): Ret<Flt64> {
    return when (this) {
        ChannelProjection.Scalar -> ok(value)
        ChannelProjection.Magnitude -> ok(Flt64(abs(value.value)))
        else -> Failed(
            ErrorCode.IllegalArgument,
            "projection requires a multi-component value / 该投影需要多分量输入"
        )
    }
}

/**
 * 将多分量值投影为标量通道 / Project a multi-component value into a scalar channel
 *
 * @param components 分量列表 / Component list
 * @return 投影结果 / Projection result
*/
fun ChannelProjection.project(components: List<Flt64>): Ret<Flt64> {
    if (components.isEmpty()) {
        return Failed(ErrorCode.IllegalArgument, "components must be non-empty / 分量列表不得为空")
    }

    return when (this) {
        ChannelProjection.Scalar -> if (components.size == 1) {
            ok(components[0])
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "scalar projection requires exactly one component / 标量投影要求分量个数恰为 1"
            )
        }

        is ChannelProjection.Component -> if (index in components.indices) {
            ok(components[index])
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "component index $index is out of range / 分量下标 $index 越界"
            )
        }

        ChannelProjection.Magnitude -> {
            var squared = 0.0
            for (component in components) {
                squared += component.value * component.value
            }
            ok(Flt64(sqrt(squared)))
        }

        ChannelProjection.Phase -> if (components.size >= 2) {
            ok(Flt64(atan2(components[1].value, components[0].value)))
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "phase projection requires at least two components / 相位投影至少需要两个分量"
            )
        }
    }
}

/**
 * 将几何点投影为标量通道 / Project a geometric point into a scalar channel
 *
 * @param D 维度类型 / Dimension type
 * @param point 几何点 / Geometric point
 * @return 投影结果 / Projection result
*/
fun <D : Dimension> ChannelProjection.project(point: Point<D, Flt64>): Ret<Flt64> {
    return project(point.position)
}
