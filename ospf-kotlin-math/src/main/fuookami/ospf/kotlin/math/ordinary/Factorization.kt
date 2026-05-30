/**
 * 因式分解
 * Integer Factorization
 *
 * 提供整数质因数分解及相关功能。
 * 数学定义：将正整敌n 分解为质数的乘积，n = p1^e1 * p2^e2 * ... * pk^ek，
 * 其中 pi 为质数，ei 为对应质数的指数。
 * 边界情况：n <= 1 返回空列表，表示无质因数分解。
 * 因式分解使用筛法预先生成质数表，只遍历到 sqrt(n) 的质数进行试除。
 * 反因式分解（defactorize）将质因数分解结果还原为原整数。
 * 因数计算（divisors）返回所有能整除 n 的因数，包括 1 和n 本身。
 * 欧拉函数（eulerTotient）计算小二n 的正整数中与 n 互质的个数，
 * 公式：phi(n) = n * (1 - 1/p1) * (1 - 1/p2) * ...，其丌pi 丌n 的质因数。
 *
 * Provides integer prime factorization and related functionality.
 * Mathematical definition: decomposes positive integer n into product of primes,
 * n = p1^e1 * p2^e2 * ... * pk^ek, where pi are primes and ei are exponents.
 * Boundary case: n <= 1 returns empty list, indicating no prime factorization.
 * Factorization uses sieve method to pre-generate prime table, only testing primes up to sqrt(n).
 * Defactorize converts prime factorization result back to original integer.
 * Divisors returns all factors that divide n, including 1 and n itself.
 * Euler's totient function (eulerTotient) counts positive integers less than n coprime to n,
 * formula: phi(n) = n * (1 - 1/p1) * (1 - 1/p2) * ..., where pi are prime factors of n.
 */
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.operator.*

/** 计算整数的平方根上界，返回 UInt64 / Compute sqrt upper bound for integer, returning UInt64 */
private fun <I> computeSqrtLimit(num: I): UInt64 where I : Integer<I> {
    return (num.toFlt64().sqrt() as Flt64).floor().toUInt64() + UInt64.one
}

/** 将 UInt64 值转换为泛型整数类型 I / Convert UInt64 value to generic Integer type I */
private inline fun <reified I> uint64ToI(value: UInt64, constants: RealNumberConstants<I>): I where I : Integer<I> {
    return when (I::class) {
        UInt64::class -> value as I
        UInt32::class -> UInt32(value.value.toUInt()) as I
        UInt16::class -> UInt16(value.value.toUShort()) as I
        UInt8::class -> UInt8(value.value.toUByte()) as I
        UIntX::class -> UIntX(value.value.toLong()) as I
        Int64::class -> Int64(value.value.toLong()) as I
        Int32::class -> Int32(value.value.toInt()) as I
        Int16::class -> Int16(value.value.toShort()) as I
        Int8::class -> Int8(value.value.toByte()) as I
        IntX::class -> IntX(value.value.toLong()) as I
        else -> {
            // Fallback: increment from constants.one / 回退方案：从 constants.one 递增
            var result = constants.one
            val target = value.value
            var current = 1UL
            while (current < target) {
                result += constants.one
                current++
            }
            result
        }
    }
}

/** 使用预计算素数表进行质因数分解的核心实现 / Core factorization implementation using pre-computed primes */
private fun <I> factorizeWithPrimes(
    num: I,
    primes: List<I>,
    constants: RealNumberConstants<I>
): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    if (num <= constants.one) {
        return emptyList()
    }

    var n = num
    val factors = ArrayList<Pair<I, Int>>()

    for (prime in primes) {
        if (prime * prime > num) {
            break
        }

        var index = 0
        while (n % prime eq constants.zero) {
            index += 1
            n /= prime
        }
        if (index != 0) {
            factors.add(prime to index)
        }
    }

    if (n > constants.one) {
        factors.add(n to 1)
    }

    return factors
}

/**
 * 对整数进行质因数分解（内部实现）
 * Perform prime factorization of an integer (internal implementation)
 *
 * 使用筛法预先生成质数表，遍历到 sqrt(num) 的质数进行试除。
 * Uses sieve method to pre-generate prime table, testing primes up to sqrt(num).
 *
 * @param num 待分解的整数 / Integer to factorize
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 质因数分解结果列表，每个元素为 (质数, 指数) 对 / List of (prime, exponent) pairs
 */
fun <I> factorizeImpl(num: I, constants: RealNumberConstants<I>): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    // Compute sqrt(num) + 1 as the upper bound for prime candidates / 计算 sqrt(num) + 1 作为质数候选的上界
    val sqrtULong = (num.toFlt64().sqrt() as Flt64).floor().toUInt64().value + 1UL

    // Create sqrt limit as type I / 将 sqrt 上界转换为类型 I
    // Since sqrt(num) is much smaller than num, this iteration is acceptable / 因为 sqrt(num) 远小于 num，此迭代是可接受的
    var sqrtLimit = constants.one
    var count = sqrtULong - 1UL
    while (count > 0UL) {
        sqrtLimit += constants.one
        count--
    }

    // Get primes up to sqrt limit (O(sqrt(num)) iterations instead of O(num)) / 获取 sqrt 上界内的质数（O(sqrt(num)) 次迭代而非 O(num)）
    val primes = getPrimesImpl(sqrtLimit, constants)

    return factorizeWithPrimes(num, primes, constants)
}

/**
 * UInt64 专用优化质因数分解，直接使用缓存的素数表
 * Optimized factorization for UInt64 using cached prime table directly
 *
 * @param num 待分解的 UInt64 整数 / UInt64 integer to factorize
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 质因数分解结果列表，每个元素为 (质数, 指数) 对 / List of (prime, exponent) pairs
 */
fun factorizeImpl(num: UInt64, constants: RealNumberConstants<UInt64>): List<Pair<UInt64, Int>> {
    if (num <= UInt64.one) {
        return emptyList()
    }

    val sqrtLimit = computeSqrtLimit(num)
    val primes = getPrimesUpTo(sqrtLimit)

    return factorizeWithPrimes(num, primes, constants)
}

/**
 * 对整数进行质因数分解
 * Perform prime factorization of an integer
 *
 * @param num 待分解的整数 / Integer to factorize
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 质因数分解结果列表，每个元素为 (质数, 指数) 对 / List of (prime, exponent) pairs
 */
fun <I> factorize(
    num: I,
    constants: RealNumberConstants<I>
): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return factorizeImpl(num, constants)
}

/**
 * 对整数进行质因数分解（自动解析常量）
 * Perform prime factorization (auto-resolve constants)
 *
 * @param num 待分解的整数 / Integer to factorize
 * @return 质因数分解结果列表 / List of (prime, exponent) pairs
 */
inline fun <reified I> factorize(num: I): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return factorize(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

/** 将质因数分解结果还原为原整数（内部实现） / Convert factorization result back to original integer (internal) */
fun <I> defactorizeImpl(
    factors: Iterable<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I> {
    var value = constants.one
    for ((factor, index) in factors) {
        if (index < 0) {
            throw IllegalArgumentException("Negative factor index is not supported: $index.")
        }
        if (index == 0) {
            continue
        }
        value *= factor.pow(index)
    }
    return value
}

/**
 * 将质因数分解结果还原为原整数
 * Convert factorization result back to original integer
 *
 * @param factors 质因数分解结果 / Factorization result
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 还原后的整数 / Restored integer
 */
fun <I> defactorize(
    factors: Iterable<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I> {
    return defactorizeImpl(factors, constants)
}

/**
 * 将质因数分解结果还原为原整数（自动解析常量）
 * Convert factorization result back to integer (auto-resolve)
 *
 * @param factors 质因数分解结果 / Factorization result
 * @return 还原后的整数 / Restored integer
 */
inline fun <reified I> defactorize(
    factors: Iterable<Pair<I, Int>>
): I where I : Integer<I>, I : Pow<I> {
    return defactorize(
        factors = factors,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

/** 根据质因数分解结果计算所有因数（内部实现） / Compute all divisors from factorization result (internal) */
fun <I> divisorsImpl(
    factors: List<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Pow<I> {
    var values = listOf(constants.one)
    for ((factor, index) in factors) {
        if (index <= 0) {
            continue
        }
        val nextValues = ArrayList<I>(values.size * (index + 1))
        var factorPower = constants.one
        for (k in 0..index) {
            for (value in values) {
                nextValues.add(value * factorPower)
            }
            factorPower *= factor
        }
        values = nextValues
    }
    return values.sorted()
}

/** 计算整数的所有因数（内部实现） / Compute all divisors of an integer (internal) */
fun <I> divisorsImpl(
    num: I,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisorsImpl(factorizeImpl(num, constants), constants)
}

/**
 * 根据质因数分解结果计算所有因数
 * Compute all divisors from factorization result
 *
 * @param factors 质因数分解结果 / Factorization result
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 所有因数列表 / List of all divisors
 */
fun <I> divisors(
    factors: List<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Pow<I> {
    return divisorsImpl(factors, constants)
}

/**
 * 根据质因数分解结果计算所有因数（自动解析常量）
 * Compute all divisors from factorization (auto-resolve)
 *
 * @param factors 质因数分解结果 / Factorization result
 * @return 所有因数列表 / List of all divisors
 */
inline fun <reified I> divisors(
    factors: List<Pair<I, Int>>
): List<I> where I : Integer<I>, I : Pow<I> {
    return divisors(
        factors = factors,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

/**
 * 计算整数的所有因数
 * Compute all divisors of an integer
 *
 * @param num 目标整数 / Target integer
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 所有因数列表 / List of all divisors
 */
fun <I> divisors(
    num: I,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisorsImpl(num, constants)
}

/**
 * 计算整数的所有因数（自动解析常量）
 * Compute all divisors (auto-resolve constants)
 *
 * @param num 目标整数 / Target integer
 * @return 所有因数列表 / List of all divisors
 */
inline fun <reified I> divisors(
    num: I
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisors(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

/**
 * 根据质因数分解结果计算因数个数
 * Compute divisor count from factorization result
 *
 * @param factors 质因数分解结果 / Factorization result
 * @return 因数个数 / Number of divisors
 */
fun <I> divisorCount(factors: Iterable<Pair<I, Int>>): Int where I : Integer<I> {
    var count = 1
    for ((_, index) in factors) {
        if (index > 0) {
            count *= (index + 1)
        }
    }
    return count
}

/**
 * 计算整数的因数个数
 * Compute divisor count of an integer
 *
 * @param num 目标整数 / Target integer
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 因数个数 / Number of divisors
 */
fun <I> divisorCount(
    num: I,
    constants: RealNumberConstants<I>
): Int where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return divisorCount(factorizeImpl(num, constants))
}

/**
 * 计算整数的因数个数（自动解析常量）
 * Compute divisor count (auto-resolve constants)
 *
 * @param num 目标整数 / Target integer
 * @return 因数个数 / Number of divisors
 */
inline fun <reified I> divisorCount(
    num: I
): Int where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return divisorCount(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

/** 计算欧拉函数值（内部实现） / Compute Euler's totient function value (internal) */
fun <I> eulerTotientImpl(
    num: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    if (num == constants.zero || num == constants.one) {
        return num
    }
    var value = num
    val one = constants.one
    for ((prime, _) in factorizeImpl(num, constants)) {
        value = (value / prime) * (prime - one)
    }
    return value
}

/**
 * 计算欧拉函数值 phi(n)，即小于 n 且与 n 互质的正整数个数
 * Compute Euler's totient phi(n)
 *
 * @param num 目标整数 / Target integer
 * @param constants 数值常量提供器 / Real number constants provider
 * @return 欧拉函数值 / Euler's totient value
 */
fun <I> eulerTotient(
    num: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    return eulerTotientImpl(num, constants)
}

/**
 * 计算欧拉函数值（自动解析常量）
 * Compute Euler's totient (auto-resolve constants)
 *
 * @param num 目标整数 / Target integer
 * @return 欧拉函数值 / Euler's totient value
 */
inline fun <reified I> eulerTotient(
    num: I
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    return eulerTotient(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}
