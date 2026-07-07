/**
 * Lotka-Volterra 系统（捕食者-猎物模型）
 * Lotka-Volterra System (Predator-Prey Model)
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Lotka-Volterra 系统（捕食者-猎物模型）
 * Lotka-Volterra System (Predator-Prey Model)
 *
 * @property a 猎物自然增长率 / Prey natural growth rate
 * @property b 捕食率 / Predation rate
 * @property c 捕食者死亡率 / Predator death rate
 * @property d 捕食者增长效率 / Predator growth efficiency
 * @property h 时间步长 / Time step size
 */
data class LotkaVolterraSystem<V : FloatingNumber<V>>(val a: V, val b: V, val c: V, val d: V, val h: V) :
    Extractor<Point<Dim2, V>, Point<Dim2, V>> {
    override operator fun invoke(p: Point<Dim2, V>): Point<Dim2, V> {
        val x = p[0];
        val y = p[1]
        val dx = a * x - b * x * y
        val dy = d * x * y - c * y
        return Point<Dim2, V>(listOf(x + h * dx, y + h * dy), Dim2)
    }

    companion object {
        operator fun invoke(
            a: Flt64 = Flt64(1.0),
            b: Flt64 = Flt64(0.1),
            c: Flt64 = Flt64(1.0),
            d: Flt64 = Flt64(0.1),
            h: Flt64 = Flt64(0.01)
        ): LotkaVolterraSystem<Flt64> = LotkaVolterraSystem(a, b, c, d, h)
    }
}

/**
 * Lotka-Volterra system generator that iteratively produces predator-prey dynamics sequences.
 * Lotka-Volterra 系统生成器，通过迭代产生捕食者-猎物动力学序列。
 *
 * @property attractor the Lotka-Volterra system instance / Lotka-Volterra 系统实例
 * @property _x the internal state variable for iteration / 迭代用的内部状态变量
 */
data class LotkaVolterraSystemGenerator(
    val attractor: LotkaVolterraSystem<Flt64> = LotkaVolterraSystem(),
    private var _x: Point<Dim2, Flt64> = point2(
        Random.nextFlt64(
            Flt64(0.1),
            Flt64.one
        ), Random.nextFlt64(Flt64(0.1), Flt64.one)
    )
) : Generator<Point<Dim2, Flt64>> {
    val x by ::_x;
    override operator fun invoke(): Point<Dim2, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
