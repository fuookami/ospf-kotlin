/**
 * 系数钳制辅助函数
 * Coefficient clamping helper
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 将 Flt64 系数钳制到精度允许范围内，防止溢出。
 * Clamp Flt64 coefficient to precision-allowed range to prevent overflow.
 *
 * @return 钳制后的系数值 / The clamped coefficient value
 */
internal fun Flt64.clampCoefficient(): Flt64 {
    val threshold = Flt64.decimalPrecision.reciprocal()
    return when {
        isInfinity() || this geq threshold -> threshold
        isNegativeInfinity() || this leq -threshold -> -threshold
        else -> this
    }
}
