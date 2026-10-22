package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.operator.unit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.linear.SymmetricEigen
import fuookami.ospf.kotlin.core.solver.heuristic.operator.linear.SymmetricEigenResult

/** CMA 数值模式；显式协议不改变历史默认行为 / CMA numerical mode; explicit contracts preserve historical defaults. */
enum class CmaNumerics {
    /** 平台原生 Box-Muller / Platform-native Box-Muller. */
    Native,
    /** 极坐标高斯、fdlibm 与固定 Jacobi 顺序 / Polar Gaussian, fdlibm and fixed Jacobi order. */
    PolarFdlibmV1;

    internal fun ln(value: Double): Double = if (this == Native) kotlin.math.ln(value) else StrictMath.log(value)
    internal fun sqrt(value: Double): Double = if (this == Native) kotlin.math.sqrt(value) else StrictMath.sqrt(value)
    internal fun exp(value: Double): Double = if (this == Native) kotlin.math.exp(value) else StrictMath.exp(value)
    internal fun pow(base: Double, exponent: Double): Double = if (this == Native) base.pow(exponent) else StrictMath.pow(base, exponent)

    internal fun eigen(matrix: List<List<Double>>): SymmetricEigenResult = SymmetricEigen.decompose(matrix)

    internal fun gaussian(entropy: Generator<Flt64>): Double {
        if (this == Native) {
            val first = entropy.unit().coerceAtLeast(1.0e-12)
            val second = entropy.unit()
            return sqrt(-2.0 * ln(first)) * cos(2.0 * PI * second)
        }
        // 每次仅使用极坐标对的第一分量，不缓存第二分量，检查点无需隐藏采样状态。
        // Use only the first polar component without caching its partner, leaving no hidden checkpoint state.
        while (true) {
            val x = 2.0 * entropy.unit() - 1.0
            val y = 2.0 * entropy.unit() - 1.0
            val radius = x * x + y * y
            if (radius > 0.0 && radius < 1.0) {
                return x * sqrt((-2.0 * ln(radius)) / radius)
            }
        }
    }
}
