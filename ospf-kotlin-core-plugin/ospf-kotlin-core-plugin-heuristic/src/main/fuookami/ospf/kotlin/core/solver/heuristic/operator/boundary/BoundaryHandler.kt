/**
 * 边界处理算子 / Boundary handling operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.boundary

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration

/**
 * 边界处理算子接口，决定越界值如何回到可行域。 / Boundary handler interface that decides how an out-of-range value returns to the feasible region.
*/
fun interface BoundaryHandler {

    /**
     * 处理越界值 / Handle an out-of-range value
     *
     * @param iteration 当前迭代 / Current iteration
     * @param index 变量下标 / Variable index
     * @param value 待处理值 / Value to handle
     * @param lowerBound 下界 / Lower bound
     * @param upperBound 上界 / Upper bound
     * @return 处理后的值 / Handled value
    */
    operator fun invoke(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        lowerBound: Flt64,
        upperBound: Flt64
    ): Flt64
}

/** 截断边界处理，直接把越界值压回边界 / Clamping boundary handler pressing out-of-range values back onto the bounds */
data object ClampBoundary : BoundaryHandler {
    override fun invoke(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        lowerBound: Flt64,
        upperBound: Flt64
    ): Flt64 {
        return Flt64(value.toDouble().coerceIn(lowerBound.toDouble(), upperBound.toDouble()))
    }
}

/** 反射边界处理，越界后按边界镜像折返 / Reflecting boundary handler mirroring out-of-range values back across the bounds */
data object ReflectBoundary : BoundaryHandler {
    override fun invoke(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        lowerBound: Flt64,
        upperBound: Flt64
    ): Flt64 {
        val lower = lowerBound.toDouble()
        val upper = upperBound.toDouble()
        val width = upper - lower
        if (width <= 0.0) {
            return lowerBound
        }

        var position = value.toDouble() - lower
        var periods = 0
        while ((position < 0.0 || position > width) && periods < 64) {
            position = if (position < 0.0) {
                -position
            } else {
                2.0 * width - position
            }
            ++periods
        }
        return Flt64((lower + position).coerceIn(lower, upper))
    }
}

/** 环绕边界处理，越界后按区间长度取模环绕 / Wrapping boundary handler wrapping out-of-range values modulo the interval width */
data object WrapBoundary : BoundaryHandler {
    override fun invoke(
        iteration: Iteration,
        index: Int,
        value: Flt64,
        lowerBound: Flt64,
        upperBound: Flt64
    ): Flt64 {
        val lower = lowerBound.toDouble()
        val upper = upperBound.toDouble()
        val width = upper - lower
        if (width <= 0.0) {
            return lowerBound
        }

        var position = (value.toDouble() - lower) % width
        if (position < 0.0) {
            position += width
        }
        return Flt64(lower + position)
    }
}
