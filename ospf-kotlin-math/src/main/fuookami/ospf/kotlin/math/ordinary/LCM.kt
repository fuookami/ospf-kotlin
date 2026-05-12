/**
 * 最小公倍数
 * Least Common Multiple (LCM)
 *
 * 提供计算整数、浮点数和有理数最小公倍数的功能。
 * 数学定义：lcm(a, b) 是能同时袌a 和b 整除的最小正整数。
 * 计算公式：lcm(a, b) = |a * b| / gcd(a, b)。
 * 对于 a = 0 戌b = 0，lcm(a, 0) = 0，lcm(0, b) = 0。
 * 支持两种计算方法：通过 GCD 直接计算，或通过因式分解计算。
 * 因式分解方法适用于需要精确分解的场景，将各数的质因数取最大指数后合并。
 * 对于浮点敌FltX，先通过乘以 10 的幂次将小数转换为整数，再计箌LCM。
 * 对于有理敌RtnX，lcm(a/b, c/d) = lcm(a, c) / gcd(b, d)。
 * 边界情况：空集合返回 one，任一数为零返囌zero，负数取绝对值后计算。
 *
 * Provides functionality for computing the least common multiple of integers,
 * floating-point numbers, and rational numbers.
 * Mathematical definition: lcm(a, b) is the smallest positive integer divisible by both a and b.
 * Computation formula: lcm(a, b) = |a * b| / gcd(a, b).
 * For a = 0 or b = 0: lcm(a, 0) = 0, lcm(0, b) = 0.
 * Supports two computation methods: direct calculation via GCD, or via factorization.
 * The factorization method is suitable for scenarios requiring exact decomposition,
 * merging prime factors by taking the maximum exponent across all numbers.
 * For FltX floating-point numbers, converts decimals to integers by multiplying powers of 10.
 * For RtnX rational numbers: lcm(a/b, c/d) = lcm(a, c) / gcd(b, d).
 * Boundary cases: empty collection returns one; any zero returns zero; negative values use absolute values.
 */
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.IntX
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Pow
import fuookami.ospf.kotlin.math.operator.Rem

fun <I> lcmImpl(numbers: Iterable<I>, constants: RealNumberConstants<I>): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    val factors = numbers.map { factorizeImpl(it, constants) }
    if (factors.isEmpty()) {
        return constants.one
    } else if (factors.any { it.isEmpty() }) {
        return constants.zero
    } else if (factors.size == 1) {
        return numbers.first()
    }

    val mergedFactors = factors
        .flatten()
        .groupBy { it.first }
        .mapValues { it.value.maxOf { (_, value) -> value } }
    return mergedFactors.entries.fold(constants.one) { lhs, (factor, index) ->
        lhs * factor.pow(index)
    }
}
fun <I> lcmByFactorization(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmImpl(numbers, constants)
}

inline fun <reified I> lcmByFactorization(numbers: Iterable<I>): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcmByFactorization(
    x: I,
    y: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        numbers = listOf(x, y),
        constants = constants
    )
}

inline fun <reified I> lcmByFactorization(x: I, y: I): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        x = x,
        y = y,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcmByFactorization(
    x: I,
    y: I,
    z: I,
    vararg numbers: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        numbers = listOf(x, y, z) + numbers.toList(),
        constants = constants
    )
}

inline fun <reified I> lcmByFactorization(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        x = x,
        y = y,
        z = z,
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcm(
    x: I,
    y: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I>, I : Div<I, I> {
    val px = x.abs()
    val py = y.abs()

    // 短路：任一为零则返回零
    if (px eq constants.zero || py eq constants.zero) {
        return constants.zero
    }

    val thisGCD = gcdModImpl(px, py)
    return (px / thisGCD) * py
}

inline fun <reified I> lcm(x: I, y: I): I where I : Integer<I>, I : Rem<I, I>, I : Minus<I, I>, I : Div<I, I> {
    return lcm(
        x = x,
        y = y,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}
fun <I> lcm(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcmImpl(numbers, constants)
}

inline fun <reified I> lcm(numbers: Iterable<I>): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcm(
    x: I,
    y: I,
    z: I,
    vararg numbers: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(
        numbers = listOf(x, y, z) + numbers.toList(),
        constants = constants
    )
}

inline fun <reified I> lcm(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(
        x = x,
        y = y,
        z = z,
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun lcm(x: FltX, y: FltX): FltX {
    val px = x.abs()
    val py = y.abs()
    val thisGCD = gcd(px, py)
    return (px / thisGCD) * py
}

fun lcm(numbers: Iterable<FltX>): FltX {
    var factor = 0
    var scaledNumbers = numbers.toList()
    val integerNumbers = ArrayList<IntX>()
    while (true) {
        integerNumbers.clear()
        for (num in scaledNumbers) {
            if (num.round() eq num) {
                integerNumbers.add(num.toIntX().abs())
            } else {
                factor += 1
                scaledNumbers = scaledNumbers.map { it * FltX.ten }
                break
            }
        }
        if (integerNumbers.size == scaledNumbers.size) {
            break
        }
    }
    return lcmImpl(integerNumbers.sortedDescending(), IntX).toFltX() / FltX.ten.pow(factor)
}

fun <F : FloatingNumber<*>> lcm(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return lcm(listOf(x, y, z) + numbers.map { it.toFltX() })
}

fun lcm(x: RtnX, y: RtnX): RtnX {
    return lcm(listOf(x, y))
}

fun lcm(numbers: Iterable<RtnX>): RtnX {
    return RtnX(
        lcm(numbers.map { it.num }),
        gcd(numbers.map { it.den })
    )
}

fun lcm(x: RtnX, y: RtnX, z: RtnX, vararg numbers: RtnX): RtnX {
    return lcm(listOf(x, y, z) + numbers.toList())
}
