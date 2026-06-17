/**
 * Julia 集
 * Julia Set
 *
 * Julia 集是复动力系统中与 Mandelbrot 集密切相关的分形集合。
 * 对于固定的复数 c，迭代 z_{n+1} = z_n^2 + c，若序列不发散则 z 属于 Julia 集。
 * 不同的 c 值产生不同形状的 Julia 集，常用于分形图形生成和复动力系统研究。
 *
 * The Julia set is a fractal set closely related to the Mandelbrot set in complex dynamics.
 * For a fixed complex c, iterate z_{n+1} = z_n^2 + c; if the sequence does not diverge, z belongs to the Julia set.
 * Different values of c produce different Julia set shapes, commonly used for fractal graphics generation and complex dynamics research.
 */
package fuookami.ospf.kotlin.math.fractal

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * Julia 集迭代函数
 * Julia Set iteration function
 *
 * 公式 / Formula:
 * x_{n+1} = x^2 - y^2 + a
 * y_{n+1} = 2*x*y + b
 *
 * @property c 复数参数 c = (a, b) / Complex parameter c = (a, b)
 */
class JuliaSet<V : FloatingNumber<V>>(
    val c: Point<Dim2, V>
) {
    companion object {
        operator fun invoke(real: Flt64, imag: Flt64): JuliaSet<Flt64> {
            return JuliaSet(point2(real, imag))
        }
    }

    operator fun invoke(z: Point<Dim2, V>): Point<Dim2, V> {
        val x = z[0]
        val y = z[1]
        val two = x.constants.two
        val real = x * x - y * y + c[0]
        val imag = two * x * y + c[1]
        return Point<Dim2, V>(listOf(real, imag), Dim2)
    }
}

/**
 * Julia 集序列生成器
 * Julia Set sequence generator
 */
data class JuliaSetGenerator(
    val juliaSet: JuliaSet<Flt64> = JuliaSet(Flt64(-0.7), Flt64(0.27015)),
    private var _z: Point<Dim2, Flt64> = point2()
) : Generator<Point<Dim2, Flt64>> {
    companion object {
        operator fun invoke(
            real: Flt64,
            imag: Flt64,
            z: Point<Dim2, Flt64> = point2()
        ): JuliaSetGenerator {
            return JuliaSetGenerator(JuliaSet(real, imag), z)
        }
    }

    val z by ::_z

    override fun invoke(): Point<Dim2, Flt64> {
        val z = _z.copy()
        _z = juliaSet(_z)
        return z
    }
}

/**
 * 多重 Julia 集
 * Multi Julia Set
 *
 * 多重 Julia 集是 Julia 集的推广，使用 z^n + c 替代 z^2 + c。
 * 通过改变指数 n，可以产生不同形状的分形图案。
 *
 * The Multi Julia set is a generalization of the Julia set, using z^n + c instead of z^2 + c.
 * By changing the exponent n, different fractal patterns can be produced.
 *
 * @property c 复数参数 c = (a, b) / Complex parameter c = (a, b)
 * @property n 指数参数 / Exponent parameter
 */
class MultiJuliaSet(
    val c: Point<Dim2, Flt64>,
    val n: Flt64 = Flt64(2.0)
) {
    companion object {
        operator fun invoke(real: Flt64, imag: Flt64, n: Flt64 = Flt64(2.0)): MultiJuliaSet {
            return MultiJuliaSet(point2(real, imag), n)
        }
    }

    operator fun invoke(z: Point<Dim2, Flt64>): Point<Dim2, Flt64> {
        val x = z[0]
        val y = z[1]
        val r2 = x * x + y * y
        val halfN = n / Flt64(2.0)
        val rN = r2.pow(halfN) as Flt64
        val theta = if (x eq Flt64.zero) {
            Flt64.pi / Flt64(2.0)
        } else {
            (y / x).atan() as Flt64
        }
        val nTheta = n * theta
        return point2(
            rN * (nTheta.cos() as Flt64) + c[0],
            rN * (nTheta.sin() as Flt64) + c[1]
        )
    }
}

/**
 * 多重 Julia 集序列生成器
 * Multi Julia Set sequence generator
 */
data class MultiJuliaSetGenerator(
    val multiJuliaSet: MultiJuliaSet = MultiJuliaSet(Flt64(-0.7), Flt64(0.27015)),
    private var _z: Point<Dim2, Flt64> = point2()
) : Generator<Point<Dim2, Flt64>> {
    companion object {
        operator fun invoke(
            real: Flt64,
            imag: Flt64,
            n: Flt64 = Flt64(2.0),
            z: Point<Dim2, Flt64> = point2()
        ): MultiJuliaSetGenerator {
            return MultiJuliaSetGenerator(MultiJuliaSet(real, imag, n), z)
        }
    }

    val z by ::_z

    override fun invoke(): Point<Dim2, Flt64> {
        val z = _z.copy()
        _z = multiJuliaSet(_z)
        return z
    }
}
