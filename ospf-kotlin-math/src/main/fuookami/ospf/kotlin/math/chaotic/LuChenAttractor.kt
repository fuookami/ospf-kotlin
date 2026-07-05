/**
 * Lu-Chen 吸引子
 * Lu-Chen Attractor
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Lu-Chen 吸引子
 * Lu-Chen Attractor
 *
 * @property alpha 系统参数 alpha / System parameter alpha
 * @property beta 系统参数 beta / System parameter beta
 * @property zeta 系统参数 zeta / System parameter zeta
 * @property h 时间步长 / Time step size
 */
data class LuChenAttractor<V : FloatingNumber<V>>(val alpha: V, val beta: V, val zeta: V, val h: V) :
    Extractor<Point<Dim3, V>, Point<Dim3, V>> {
    override operator fun invoke(p: Point<Dim3, V>): Point<Dim3, V> {
        val x = p[0];
        val y = p[1];
        val z = p[2]
        val dx = -(alpha * beta) / (alpha + beta) * x - y * z + zeta
        val dy = alpha * y + x * z
        val dz = beta * z + x * y
        return Point<Dim3, V>(listOf(x + h * dx, y + h * dy, z + h * dz), Dim3)
    }

    /**
     * 工厂方法，使用默认的 Flt64 类型参数创建 Lu-Chen 吸引子实例
     * Factory method to create a Lu-Chen attractor instance with default Flt64 parameters
     *
     * @param alpha 系统参数 alpha / System parameter alpha
     * @param beta 系统参数 beta / System parameter beta
     * @param zeta 系统参数 zeta / System parameter zeta
     * @param h 时间步长 / Time step size
     */
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(-10.0),
            beta: Flt64 = Flt64(-4.0),
            zeta: Flt64 = Flt64(18.1),
            h: Flt64 = Flt64(0.01)
        ): LuChenAttractor<Flt64> = LuChenAttractor(alpha, beta, zeta, h)
    }
}

/**
 * Lu-Chen 吸引子生成器
 * Lu-Chen Attractor Generator
 *
 * @property attractor Lu-Chen 吸引子实例 / Lu-Chen attractor instance
 * @property _x 内部可变状态点，初始值为随机生成的三维点 / Internal mutable state point initialized with a random 3D point
 */
data class LuChenAttractorGenerator(
    val attractor: LuChenAttractor<Flt64> = LuChenAttractor(),
    private var _x: Point<Dim3, Flt64> = point3(
        Random.nextFlt64(
            Flt64.decimalPrecision,
            Flt64.one
        ),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one),
        Random.nextFlt64(Flt64.decimalPrecision, Flt64.one)
    )
) : Generator<Point<Dim3, Flt64>> {
    /** 当前状态点 / Current state point */
    val x by ::_x;
    override operator fun invoke(): Point<Dim3, Flt64> {
        val x = _x.copy(); _x = attractor(x); return x
    }
}
