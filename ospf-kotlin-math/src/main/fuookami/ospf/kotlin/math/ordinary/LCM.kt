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

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.operator.*

/**
 * 通过因式分解计算多个整数的最小公倍数（内部实现）
 * Compute LCM of multiple integers via factorization (internal implementation)
 *
 * 对每个数进行质因数分解，合并后取各质因数的最大指数。
 * Factorizes each number and merges results by taking the maximum exponent per prime.
 *
 * @param numbers 整数集合 / Collection of integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数，空集合返回 one，含零返回 zero / Least common multiple; one for empty, zero if any element is zero
 */
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
/**
 * 通过因式分解计算多个整数的最小公倍数
 * Compute LCM of multiple integers via factorization
 *
 * @param numbers 整数集合 / Collection of integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数 / Least common multiple
 */
fun <I> lcmByFactorization(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmImpl(numbers, constants)
}

/**
 * 通过因式分解计算多个整数的最小公倍数（自动解析常量）
 * Compute LCM via factorization (auto-resolve constants)
 *
 * @param numbers 整数集合 / Collection of integers
 * @return 最小公倍数 / Least common multiple
 */
inline fun <reified I> lcmByFactorization(numbers: Iterable<I>): Ret<I> where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("LCM").mapResolved { constants ->
        lcmByFactorization(
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 通过因式分解计算两个整数的最小公倍数
 * Compute LCM of two integers via factorization
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数 / Least common multiple
 */
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

/**
 * 通过因式分解计算两个整数的最小公倍数（自动解析常量）
 * Compute LCM of two integers via factorization (auto-resolve)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @return 最小公倍数 / Least common multiple
 */
inline fun <reified I> lcmByFactorization(x: I, y: I): Ret<I> where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("LCM").mapResolved { constants ->
        lcmByFactorization(
            x = x,
            y = y,
            constants = constants
        )
    }
}

/**
 * 通过因式分解计算多个整数的最小公倍数（可变参数）
 * Compute LCM via factorization (vararg)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数 / Least common multiple
 */
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

/**
 * 通过因式分解计算多个整数的最小公倍数（可变参数，自动解析常量）
 * Compute LCM via factorization (vararg, auto-resolve)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @return 最小公倍数 / Least common multiple
 */
inline fun <reified I> lcmByFactorization(x: I, y: I, z: I, vararg numbers: I): Ret<I> where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("LCM").mapResolved { constants ->
        lcmByFactorization(
            x = x,
            y = y,
            z = z,
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 使用 GCD 公式计算两个整数的最小公倍数
 * Compute LCM of two integers using GCD formula
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数 / Least common multiple
 */
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

/**
 * 计算两个整数的最小公倍数（自动解析常量）
 * Compute LCM of two integers (auto-resolve constants)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @return 最小公倍数 / Least common multiple
 */
inline fun <reified I> lcm(x: I, y: I): Ret<I> where I : Integer<I>, I : Rem<I, I>, I : Minus<I, I>, I : Div<I, I> {
    return resolveRealNumberConstantsSafe<I>("LCM").mapResolved { constants ->
        lcm(
            x = x,
            y = y,
            constants = constants
        )
    }
}
/**
 * 计算多个整数的最小公倍数
 * Compute LCM of multiple integers
 *
 * @param numbers 整数集合 / Collection of integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数 / Least common multiple
 */
fun <I> lcm(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcmImpl(numbers, constants)
}

/**
 * 计算多个整数的最小公倍数（自动解析常量）
 * Compute LCM of multiple integers (auto-resolve constants)
 *
 * @param numbers 整数集合 / Collection of integers
 * @return 最小公倍数 / Least common multiple
 */
inline fun <reified I> lcm(numbers: Iterable<I>): Ret<I> where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return resolveRealNumberConstantsSafe<I>("LCM").mapResolved { constants ->
        lcm(
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 计算多个整数的最小公倍数（可变参数）
 * Compute LCM of multiple integers (vararg)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最小公倍数 / Least common multiple
 */
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

/**
 * 计算多个整数的最小公倍数（可变参数，自动解析常量）
 * Compute LCM (vararg, auto-resolve constants)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @return 最小公倍数 / Least common multiple
 */
inline fun <reified I> lcm(x: I, y: I, z: I, vararg numbers: I): Ret<I> where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return resolveRealNumberConstantsSafe<I>("LCM").mapResolved { constants ->
        lcm(
            x = x,
            y = y,
            z = z,
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 计算两个浮点数的最小公倍数
 * Compute LCM of two floating-point numbers
 *
 * @param x 第一个浮点数 / First floating-point number
 * @param y 第二个浮点数 / Second floating-point number
 * @return 最小公倍数 / Least common multiple
 */
fun lcm(x: FltX, y: FltX): FltX {
    val px = x.abs()
    val py = y.abs()
    val thisGCD = gcd(px, py)
    return (px / thisGCD) * py
}

/**
 * 计算多个浮点数的最小公倍数
 * Compute LCM of multiple floating-point numbers
 *
 * @param numbers 浮点数集合 / Collection of floating-point numbers
 * @return 最小公倍数 / Least common multiple
 */
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

/**
 * 计算多个浮点数的最小公倍数（可变参数）
 * Compute LCM of multiple floating-point numbers (vararg)
 *
 * @param x 第一个浮点数 / First floating-point number
 * @param y 第二个浮点数 / Second floating-point number
 * @param z 第三个浮点数 / Third floating-point number
 * @param numbers 其余浮点数 / Remaining floating-point numbers
 * @return 最小公倍数 / Least common multiple
 */
fun <F : FloatingNumber<*>> lcm(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return lcm(listOf(x, y, z) + numbers.map { it.toFltX() })
}

/**
 * 计算两个有理数的最小公倍数
 * Compute LCM of two rational numbers
 *
 * @param x 第一个有理数 / First rational number
 * @param y 第二个有理数 / Second rational number
 * @return 最小公倍数 / Least common multiple
 */
fun lcm(x: RtnX, y: RtnX): RtnX {
    return lcm(listOf(x, y))
}

/**
 * 计算多个有理数的最小公倍数
 * Compute LCM of multiple rational numbers
 *
 * @param numbers 有理数集合 / Collection of rational numbers
 * @return 最小公倍数 / Least common multiple
 */
fun lcm(numbers: Iterable<RtnX>): RtnX {
    return RtnX(
        lcm(numbers.map { it.num }, IntX),
        gcd(numbers.map { it.den }, IntX)
    )
}

/**
 * 计算多个有理数的最小公倍数（可变参数）
 * Compute LCM of multiple rational numbers (vararg)
 *
 * @param x 第一个有理数 / First rational number
 * @param y 第二个有理数 / Second rational number
 * @param z 第三个有理数 / Third rational number
 * @param numbers 其余有理数 / Remaining rational numbers
 * @return 最小公倍数 / Least common multiple
 */
fun lcm(x: RtnX, y: RtnX, z: RtnX, vararg numbers: RtnX): RtnX {
    return lcm(listOf(x, y, z) + numbers.toList())
}
