/**
 * 最大公约数
 * Greatest Common Divisor (GCD)
 *
 * 提供计算整数、浮点数和有理数最大公约数的功能。
 * 对于整数，使用欧几里得算法（辗转相除法）计算 GCD。
 * 数学定义：gcd(a, b) 是能同时整除 a 和b 的最大正整数。
 * 对于 a = 0 戌b = 0，gcd(a, 0) = |a|，gcd(0, b) = |b|，gcd(0, 0) = 0。
 * 扩展欧几里得算法同时求解 gcd(a, b) = ax + by 的整数解 x 和y（贝祖等式）。
 * 对于浮点敌FltX，先通过乘以 10 的幂次将小数转换为整数，再计箌GCD。
 * 对于有理敌RtnX，gcd(a/b, c/d) = gcd(a, c) / lcm(b, d)。
 * 边界情况：空集合返回 one，负数取绝对值后计算。
 *
 * Provides functionality for computing the greatest common divisor of integers,
 * floating-point numbers, and rational numbers.
 * For integers, uses the Euclidean algorithm (division-based method) to compute GCD.
 * Mathematical definition: gcd(a, b) is the largest positive integer that divides both a and b.
 * For a = 0 or b = 0: gcd(a, 0) = |a|, gcd(0, b) = |b|, gcd(0, 0) = 0.
 * Extended Euclidean algorithm also solves the Bezout identity: gcd(a, b) = ax + by.
 * For FltX floating-point numbers, converts decimals to integers by multiplying powers of 10.
 * For RtnX rational numbers: gcd(a/b, c/d) = gcd(a, c) / lcm(b, d).
 * Boundary cases: empty collection returns one; negative values are converted to absolute values.
*/
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 使用减法实现的 GCD 算法（内部实现）
 * GCD using subtraction-based algorithm (internal implementation)
 *
 * 通过反复相减将较大值减小，直到其中一个为零。
 * Repeatedly subtracts the smaller value until one becomes zero.
 *
 * @param x 第一个非负整数 / First non-negative integer
 * @param y 第二个非负整数 / Second non-negative integer
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcdImpl(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
    val zero = x.constants.zero
    assert(x >= zero)
    assert(y >= zero)

    if (x == zero) {
        return y
    }
    if (y == zero) {
        return x
    }

    var a = x
    var b = y
    while (b != zero) {
        if (a > b) {
            val temp = b
            b = a
            a = temp
        }
        b -= a
    }
    return a
}

/**
 * 使用取模实现的 GCD 算法（内部实现）
 * GCD using modulo-based algorithm (internal implementation)
 *
 * 使用欧几里得算法（辗转相除法），比减法版本效率更高。
 * Uses the Euclidean algorithm (division-based method), more efficient than the subtraction version.
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcdModImpl(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    val zero = x.constants.zero
    var a = x.abs()
    var b = y.abs()
    while (b != zero) {
        val r = a % b
        a = b
        b = r
    }
    return a
}

/**
 * 计算多个整数的 GCD（内部实现）
 * Compute GCD of multiple integers (internal implementation)
 *
 * 通过迭代对每对相邻元素调用取模 GCD 实现累积计算。
 * Computes by iteratively calling modulo GCD on each adjacent pair.
 *
 * @param numbers 整数集合，需已排序且取绝对值 / Collection of integers, must be sorted and absolute-valued
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最大公约数，空集合返回 one / Greatest common divisor; returns one for empty collection
*/
fun <I> gcdImpl(numbers: Iterable<I>, constants: RealNumberConstants<I>): I where I : Integer<I>, I : Rem<I, I> {
    val iter = numbers.iterator()
    if (!iter.hasNext()) return constants.one

    var acc = iter.next()
    while (iter.hasNext()) {
        acc = gcdModImpl(acc, iter.next())
    }
    return acc
}

/**
 * 计算两个整数的最大公约数
 * Compute greatest common divisor of two integers
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcd(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
    return gcdImpl(x, y)
}

/**
 * 计算多个整数的最大公约数
 * Compute greatest common divisor of multiple integers
 *
 * @param numbers 整数集合 / Collection of integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcd(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(
        numbers.map { it.abs() }.sortedDescending(),
        constants
    )
}

/**
 * 计算多个整数的最大公约数（自动解析常量）
 * Compute GCD of multiple integers (auto-resolve constants)
 *
 * @param numbers 整数集合 / Collection of integers
 * @return 最大公约数 / Greatest common divisor
*/
inline fun <reified I> gcd(numbers: Iterable<I>): Ret<I> where I : Integer<I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("GCD").mapResolved { constants ->
        gcd(
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 计算多个整数的最大公约数（可变参数）
 * Compute GCD of multiple integers (vararg)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcd(
    x: I,
    y: I,
    z: I,
    vararg numbers: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(
        numbers = listOf(x, y, z) + numbers.toList(),
        constants = constants
    )
}

/**
 * 计算多个整数的最大公约数（可变参数，自动解析常量）
 * Compute GCD of multiple integers (vararg, auto-resolve constants)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @return 最大公约数 / Greatest common divisor
*/
inline fun <reified I> gcd(x: I, y: I, z: I, vararg numbers: I): Ret<I> where I : Integer<I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("GCD").mapResolved { constants ->
        gcd(
            x = x,
            y = y,
            z = z,
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 使用取模算法计算两个整数的 GCD
 * Compute GCD of two integers using modulo algorithm
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcdMod(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcdModImpl(x, y)
}

/**
 * 使用取模算法计算多个整数的 GCD
 * Compute GCD of multiple integers using modulo algorithm
 *
 * @param numbers 整数集合 / Collection of integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcdMod(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(numbers, constants)
}

/**
 * 使用取模算法计算多个整数的 GCD（自动解析常量）
 * Compute GCD of multiple integers using modulo (auto-resolve constants)
 *
 * @param numbers 整数集合 / Collection of integers
 * @return 最大公约数 / Greatest common divisor
*/
inline fun <reified I> gcdMod(numbers: Iterable<I>): Ret<I> where I : Integer<I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("GCD").mapResolved { constants ->
        gcdMod(
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 使用取模算法计算多个整数的 GCD（可变参数）
 * Compute GCD of multiple integers using modulo (vararg)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 最大公约数 / Greatest common divisor
*/
fun <I> gcdMod(
    x: I,
    y: I,
    z: I,
    vararg numbers: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I> {
    return gcdMod(
        numbers = listOf(x, y, z) + numbers.toList(),
        constants = constants
    )
}

/**
 * 使用取模算法计算多个整数的 GCD（可变参数，自动解析常量）
 * Compute GCD using modulo (vararg, auto-resolve constants)
 *
 * @param x 第一个整数 / First integer
 * @param y 第二个整数 / Second integer
 * @param z 第三个整数 / Third integer
 * @param numbers 其余整数 / Remaining integers
 * @return 最大公约数 / Greatest common divisor
*/
inline fun <reified I> gcdMod(x: I, y: I, z: I, vararg numbers: I): Ret<I> where I : Integer<I>, I : Rem<I, I> {
    return resolveRealNumberConstantsSafe<I>("GCD").mapResolved { constants ->
        gcdMod(
            x = x,
            y = y,
            z = z,
            numbers = numbers,
            constants = constants
        )
    }
}

/**
 * 扩展欧几里得算法结果
 * Extended Euclidean algorithm result
 *
 * @property gcd 最大公约数 / Greatest common divisor
 * @property x 贝祖等式系数 x / Bezout identity coefficient x
 * @property y 贝祖等式系数 y / Bezout identity coefficient y
*/
data class ExtendedGcdResult<I>(
    val gcd: I,
    val x: I,
    val y: I
)

/**
 * 扩展欧几里得算法，求解 gcd(a, b) = ax + by
 * Extended Euclidean algorithm, solves gcd(a, b) = ax + by
 *
 * @param a 第一个整数 / First integer
 * @param b 第二个整数 / Second integer
 * @return 包含 GCD 和贝祖等式系数的结果 / Result containing GCD and Bezout identity coefficients
*/
fun <I> extendedGcd(
    a: I,
    b: I
): ExtendedGcdResult<I> where I : IntegerNumber<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    val zero = a.constants.zero
    val one = a.constants.one

    var oldR = a
    var r = b
    var oldS = one
    var s = zero
    var oldT = zero
    var t = one

    while (r != zero) {
        val q = oldR / r

        val nextR = oldR - q * r
        oldR = r
        r = nextR

        val nextS = oldS - q * s
        oldS = s
        s = nextS

        val nextT = oldT - q * t
        oldT = t
        t = nextT
    }

    return if (oldR < zero) {
        ExtendedGcdResult(-oldR, -oldS, -oldT)
    } else {
        ExtendedGcdResult(oldR, oldS, oldT)
    }
}

/**
 * 计算两个浮点数的最大公约数
 * Compute greatest common divisor of two floating-point numbers
 *
 * @param x 第一个浮点数 / First floating-point number
 * @param y 第二个浮点数 / Second floating-point number
 * @return 最大公约数 / Greatest common divisor
*/
fun gcd(x: FltX, y: FltX): FltX {
    return gcd(listOf(x, y))
}

/**
 * 计算多个浮点数的最大公约数
 * Compute greatest common divisor of multiple floating-point numbers
 *
 * @param numbers 浮点数集合 / Collection of floating-point numbers
 * @return 最大公约数 / Greatest common divisor
*/
fun gcd(numbers: Iterable<FltX>): FltX {
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
    val integerGCD = gcdImpl(
        integerNumbers.sortedDescending(),
        IntX
    ).toFltX()
    return integerGCD / FltX.ten.pow(factor)
}

/**
 * 计算多个浮点数的最大公约数（可变参数）
 * Compute GCD of multiple floating-point numbers (vararg)
 *
 * @param x 第一个浮点数 / First floating-point number
 * @param y 第二个浮点数 / Second floating-point number
 * @param z 第三个浮点数 / Third floating-point number
 * @param numbers 其余浮点数 / Remaining floating-point numbers
 * @return 最大公约数 / Greatest common divisor
*/
fun <F : FloatingNumber<*>> gcd(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return gcd(listOf(x, y, z) + numbers.map { it.toFltX() })
}

/**
 * 计算两个有理数的最大公约数
 * Compute greatest common divisor of two rational numbers
 *
 * @param x 第一个有理数 / First rational number
 * @param y 第二个有理数 / Second rational number
 * @return 最大公约数 / Greatest common divisor
*/
fun gcd(x: RtnX, y: RtnX): RtnX {
    return gcd(listOf(x, y))
}

/**
 * 计算多个有理数的最大公约数
 * Compute greatest common divisor of multiple rational numbers
 *
 * @param numbers 有理数集合 / Collection of rational numbers
 * @return 最大公约数 / Greatest common divisor
*/
fun gcd(numbers: Iterable<RtnX>): RtnX {
    return RtnX(
        gcd(numbers.map { it.num }, IntX),
        lcm(numbers.map { it.den }, IntX)
    )
}

/**
 * 计算多个有理数的最大公约数（可变参数）
 * Compute GCD of multiple rational numbers (vararg)
 *
 * @param x 第一个有理数 / First rational number
 * @param y 第二个有理数 / Second rational number
 * @param z 第三个有理数 / Third rational number
 * @param numbers 其余有理数 / Remaining rational numbers
 * @return 最大公约数 / Greatest common divisor
*/
fun gcd(x: RtnX, y: RtnX, z: RtnX, vararg numbers: RtnX): RtnX {
    return gcd(listOf(x, y, z) + numbers.toList())
}
