/**
 * 物理量取值域枚举
 * Quantity value domain enumeration
 *
 * 定义物理量的取值域（连续或离散），并提供乘法、除法和幂次合成规则。
 * Defines quantity value domains (continuous or discrete) and provides multiplication, division, and power composition rules.
*/
package fuookami.ospf.kotlin.quantities.dimension

/**
 * 物理量取值域，用于表达该量应按连续值还是离散值处理。
 * Quantity value domain, used to mark whether a quantity should be treated as continuous or discrete.
*/
enum class QuantityDomain {
    /** 连续量 / Continuous quantity */
    Continuous,

    /** 离散量 / Discrete quantity */
    Discrete
}

/**
 * 取值域乘法合成规则：只有离散量与离散量相乘时，结果仍保持离散。
 * Value-domain multiplication rule: only discrete times discrete keeps the result discrete.
 *
 * @param other 另一个取值域 / The other value domain
 * @return 合成后的取值域 / The resulting value domain
*/
operator fun QuantityDomain.times(other: QuantityDomain): QuantityDomain {
    return if (this == QuantityDomain.Discrete && other == QuantityDomain.Discrete) {
        QuantityDomain.Discrete
    } else {
        QuantityDomain.Continuous
    }
}

/**
 * 取值域除法合成规则：比率和倒数都按连续量处理。
 * Value-domain division rule: ratios and reciprocals are treated as continuous.
 *
 * @param other 另一个取值域 / The other value domain
 * @return 合成后的取值域（始终为连续） / The resulting value domain (always continuous)
*/
operator fun QuantityDomain.div(other: QuantityDomain): QuantityDomain {
    return QuantityDomain.Continuous
}

/**
 * 取值域幂次合成规则：正整数幂等价于连续乘法，零次幂、负幂按连续量处理。
 * Value-domain power rule: positive integer powers compose by multiplication; zero and negative powers are continuous.
 *
 * @param index 幂次指数 / The power exponent
 * @return 合成后的取值域 / The resulting value domain
*/
fun QuantityDomain.pow(index: Int): QuantityDomain {
    return when {
        index <= 0 -> QuantityDomain.Continuous
        index == 1 -> this
        else -> (1 until index).fold(this) { acc, _ -> acc * this }
    }
}
