/**
 * 分形逃逸时间 / Fractal escape time
*/
package fuookami.ospf.kotlin.math.fractal

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*

/**
 * 计算复数迭代 z_{n+1} = z_n^2 + c 的逃逸时间 / Compute the escape time of the complex iteration z_{n+1} = z_n^2 + c
 *
 * 逃逸时间指首次满足 |z| > bailoutRadius 的迭代次数；若在 [maxIterations] 内未逃逸，
 * 则认为该点属于分形集合，返回 [maxIterations]。 / Escape time is the iteration index at which |z| first exceeds bailoutRadius; if the point does not escape
 * within [maxIterations] it is considered part of the fractal set and [maxIterations] is returned.
 *
 * @param z 初始迭代点 / Initial iteration point
 * @param c 复数参数 / Complex parameter
 * @param maxIterations 最大迭代次数 / Maximum iteration count
 * @param bailoutRadius 逃逸半径 / Escape radius
 * @return 逃逸时间 / Escape time
*/
fun escapeTime(
    z: Point<Dim2, Flt64>,
    c: Point<Dim2, Flt64>,
    maxIterations: UInt64 = UInt64(100),
    bailoutRadius: Flt64 = Flt64(2.0)
): Ret<UInt64> {
    return when {
        maxIterations <= UInt64.zero -> Failed(
            ErrorCode.IllegalArgument,
            "maxIterations must be positive / 最大迭代次数必须为正"
        )

        bailoutRadius.value <= 0.0 -> Failed(
            ErrorCode.IllegalArgument,
            "bailoutRadius must be positive / 逃逸半径必须为正"
        )

        else -> {
            val bailoutSquared = bailoutRadius.value * bailoutRadius.value
            var x = z[0].value
            var y = z[1].value
            val cx = c[0].value
            val cy = c[1].value

            var iterations = UInt64.zero
            while (iterations < maxIterations) {
                val xSquared = x * x
                val ySquared = y * y
                if (xSquared + ySquared > bailoutSquared) {
                    break
                }
                val nextX = xSquared - ySquared + cx
                val nextY = 2.0 * x * y + cy
                x = nextX
                y = nextY
                iterations += UInt64.one
            }
            ok(iterations)
        }
    }
}
