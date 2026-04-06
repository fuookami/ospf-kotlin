/**
 * 最大公约数
 * Greatest Common Divisor (GCD)
 *
 * 提供计算整数、浮点数和有理数最大公约数的功能。
 * 对于整数，使用欧几里得算法（辗转相除法）计算 GCD。
 * 数学定义：gcd(a, b) 是能同时整除 a 和 b 的最大正整数。
 * 对于 a = 0 或 b = 0，gcd(a, 0) = |a|，gcd(0, b) = |b|，gcd(0, 0) = 0。
 * 扩展欧几里得算法同时求解 gcd(a, b) = ax + by 的整数解 x 和 y（贝祖等式）。
 * 对于浮点数 FltX，先通过乘以 10 的幂次将小数转换为整数，再计算 GCD。
 * 对于有理数 RtnX，gcd(a/b, c/d) = gcd(a, c) / lcm(b, d)。
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

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.IntX
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Rem

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

fun <I> gcdImpl(numbers: Iterable<I>, constants: RealNumberConstants<I>): I where I : Integer<I>, I : Rem<I, I> {
    val iter = numbers.iterator()
    if (!iter.hasNext()) return constants.one

    var acc = iter.next()
    while (iter.hasNext()) {
        acc = gcdModImpl(acc, iter.next())
    }
    return acc
}

@Suppress("UNCHECKED_CAST")
fun <I> gcd(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
    return gcdImpl(x, y)
}

fun <I> gcd(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(
        numbers.map { it.abs() }.sortedDescending(),
        constants
    )
}

inline fun <reified I> gcd(numbers: Iterable<I>): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("GCD")
    )
}

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

inline fun <reified I> gcd(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(
        x = x,
        y = y,
        z = z,
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("GCD")
    )
}

fun <I> gcdMod(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcdModImpl(x, y)
}

fun <I> gcdMod(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(numbers, constants)
}

inline fun <reified I> gcdMod(numbers: Iterable<I>): I where I : Integer<I>, I : Rem<I, I> {
    return gcdMod(
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("GCD")
    )
}

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

inline fun <reified I> gcdMod(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcdMod(
        x = x,
        y = y,
        z = z,
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("GCD")
    )
}

data class ExtendedGcdResult<I>(
    val gcd: I,
    val x: I,
    val y: I
)

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

fun gcd(x: FltX, y: FltX): FltX {
    return gcd(listOf(x, y))
}

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

fun <F : FloatingNumber<*>> gcd(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return gcd(listOf(x, y, z) + numbers.map { it.toFltX() })
}

fun gcd(x: RtnX, y: RtnX): RtnX {
    return gcd(listOf(x, y))
}

fun gcd(numbers: Iterable<RtnX>): RtnX {
    return RtnX(
        gcd(numbers.map { it.num }),
        lcm(numbers.map { it.den })
    )
}

fun gcd(x: RtnX, y: RtnX, z: RtnX, vararg numbers: RtnX): RtnX {
    return gcd(listOf(x, y, z) + numbers.toList())
}
