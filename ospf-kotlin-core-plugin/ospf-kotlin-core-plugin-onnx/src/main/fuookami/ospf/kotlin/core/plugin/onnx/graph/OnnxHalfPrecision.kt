/**
 * ONNX 半精度数值转换 / ONNX half-precision numeric conversions
*/
package fuookami.ospf.kotlin.core.plugin.onnx.graph

/**
 * IEEE 754 半精度（binary16）位模式转 `Double` / Convert an IEEE 754 half-precision (binary16) bit pattern into `Double`
 *
 * @param bits 16 位位模式 / 16-bit pattern
 * @return 对应数值 / decoded value
 */
internal fun halfBitsToDouble(bits: Int): Double {
    val sign = if (bits and 0x8000 != 0) -1.0 else 1.0
    val exponent = (bits shr 10) and 0x1F
    val mantissa = bits and 0x3FF
    return when (exponent) {
        0x00 -> sign * mantissa.toDouble() / 1024.0 * Math.pow(2.0, -14.0)
        0x1F -> if (mantissa == 0) {
            sign * Double.POSITIVE_INFINITY
        } else {
            Double.NaN
        }

        else -> sign * (1.0 + mantissa.toDouble() / 1024.0) * Math.pow(2.0, (exponent - 15).toDouble())
    }
}

/**
 * 脑浮点（bfloat16）位模式转 `Double` / Convert a bfloat16 bit pattern into `Double`
 *
 * bfloat16 与 float32 共享指数位，仅尾数截断到 7 位，因此左移 16 位即得 float32 位模式。 / bfloat16 shares the exponent layout of float32 with a truncated 7-bit mantissa, so shifting left
 * by 16 yields the float32 bit pattern.
 *
 * @param bits 16 位位模式 / 16-bit pattern
 * @return 对应数值 / decoded value
 */
internal fun bfloat16BitsToDouble(bits: Int): Double {
    val floatBits = bits shl 16
    return java.lang.Float.intBitsToFloat(floatBits).toDouble()
}

/**
 * `Double` 转半精度位模式 / Convert a `Double` into a half-precision bit pattern
 *
 * 用于把 `Double` 数值写回 FP16 输入张量。 / Used to write `Double` values back into FP16 input tensors.
 *
 * @param value 数值 / value
 * @return 16 位位模式 / 16-bit pattern
 */
internal fun doubleToHalfBits(value: Double): Int {
    if (value.isNaN()) {
        return 0x7E00
    }
    val sign = if (value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)) 0x8000 else 0
    val magnitude = Math.abs(value)
    if (magnitude == 0.0) {
        return sign
    }
    if (magnitude > 65504.0) {
        return sign or 0x7C00
    }
    if (magnitude < 6.103515625e-5) {
        // 次正规数：mantissa 按 2^-24 量化。 / Subnormal: quantize the mantissa against 2^-24.
        val scaled = Math.round(magnitude / 5.960464477539063e-8)
        return sign or scaled.toInt().coerceIn(0, 0x3FF)
    }
    val exponent = Math.floor(Math.log(magnitude) / Math.log(2.0)).toInt()
    var mantissa = Math.round((magnitude / Math.pow(2.0, exponent.toDouble()) - 1.0) * 1024.0).toInt()
    var biased = exponent + 15
    if (mantissa >= 1024) {
        mantissa = 0
        biased += 1
    }
    if (biased >= 0x1F) {
        return sign or 0x7C00
    }
    return sign or (biased shl 10) or (mantissa and 0x3FF)
}

/**
 * `Double` 转脑浮点位模式 / Convert a `Double` into a bfloat16 bit pattern
 *
 * @param value 数值 / value
 * @return 16 位位模式 / 16-bit pattern
 */
internal fun doubleToBFloat16Bits(value: Double): Int {
    val floatBits = java.lang.Float.floatToIntBits(value.toFloat())
    // 就近舍入到偶数，避免直接截断带来的系统性偏差。 / Round half to even instead of truncating, avoiding a systematic bias.
    val rounding = 0x7FFF + ((floatBits shr 16) and 1)
    return ((floatBits + rounding) ushr 16) and 0xFFFF
}
