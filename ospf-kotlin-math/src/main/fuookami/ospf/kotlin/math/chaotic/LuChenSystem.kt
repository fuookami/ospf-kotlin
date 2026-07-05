/**
 * Lu-Chen 系统
 * Lu-Chen System
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Lu-Chen 系统
 * Lu-Chen System
 *
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 * @property h 时间步长 / Time step size
 */
data class LuChenSystem<V : FloatingNumber<V>>(val a: V, val b: V, val c: V, val d: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = a * (y - x)
        val dy = x - x * z + c * y + d
        val dz = x * y - b * z
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(36.0),
            b: Flt64 = Flt64(20.0),
            c: Flt64 = Flt64(3.0),
            d: Flt64 = Flt64(1.0),
            h: Flt64 = Flt64(0.01)
        ): LuChenSystem<Flt64> = LuChenSystem(a, b, c, d, h)
    }
}

/**
 * Lu-Chen 系统生成器
 * Lu-Chen System generator
 *
 * @property attractor Lu-Chen 系统吸引子 / Lu-Chen system attractor
 * @property _x 当前状态点 / Current state point
 */
data class LuChenSystemGenerator(
    val attractor: LuChenSystem<Flt64> = LuChenSystem(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one),
        Random.nextFlt64(-Flt64.one, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
