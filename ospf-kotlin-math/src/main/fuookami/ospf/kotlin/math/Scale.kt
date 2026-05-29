/**
 * 缩放操作
 * Scale Operations
 *
 * 定义 Scale 数据类用于表示科学计数缩放因子，支持 SI 单位前缀 (atto, femto, pico, nano, micro, milli, centi, deci, deca, hecto, kilo, mega, giga, tera, peta, exa)，以及乘法和除法运算。
 * Defines Scale data class for representing scientific scaling factors, supporting SI unit prefixes (atto, femto, pico, nano, micro, milli, centi, deci, deca, hecto, kilo, mega, giga, tera, peta, exa), with multiplication and division operations.
 */
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.algebra.number.*

private typealias ScaleBase = Either<FltX, RtnX>

/**
 * 科学计数缩放因子
 * Scientific scaling factor
 *
 * @property scales 缩放因子列表 / List of scaling factors
 */
data class Scale(
    val scales: List<Pair<ScaleBase, FltX>> = emptyList()
) {
    companion object {
        /** 阿托 (10^-18) / Atto (10^-18) */
        val atto = Scale(10, -18)
        /** 飞托 (10^-15) / Femto (10^-15) */
        val femto = Scale(10, -15)
        /** 皮可 (10^-12) / Pico (10^-12) */
        val pico = Scale(10, -12)
        /** 纳诺 (10^-9) / Nano (10^-9) */
        val nano = Scale(10, -9)
        /** 微 (10^-6) / Micro (10^-6) */
        val micro = Scale(10, -6)
        /** 毫 (10^-3) / Milli (10^-3) */
        val milli = Scale(10, -3)
        /** 厘 (10^-2) / Centi (10^-2) */
        val centi = Scale(10, -2)
        /** 分 (10^-1) / Deci (10^-1) */
        val deci = Scale(10, -1)
        /** 十 (10^1) / Deca (10^1) */
        val deca = Scale(10, 1)
        /** 百 (10^2) / Hecto (10^2) */
        val hecto = Scale(10, 2)
        /** 千 (10^3) / Kilo (10^3) */
        val kilo = Scale(10, 3)
        /** 兆 (10^6) / Mega (10^6) */
        val mega = Scale(10, 6)
        /** 吉 (10^9) / Giga (10^9) */
        val giga = Scale(10, 9)
        /** 太 (10^12) / Tera (10^12) */
        val tera = Scale(10, 12)
        /** 拍 (10^15) / Peta (10^15) */
        val peta = Scale(10, 15)
        /** 艾 (10^18) / Exa (10^18) */
        val exa = Scale(10, 18)

        /**
         * 从 FltX 底数和 FltX 指数创建缩放因子
         * Create scale from FltX base and FltX exponent
         *
         * @param base 底数 / The base value
         * @param index 指数 / The exponent value
         * @return 缩放因子 / The scale factor
         */
        operator fun invoke(base: FltX, index: FltX): Scale {
            val base: ScaleBase = Either.Left(base)
            return Scale(listOf(base to index))
        }

        /**
         * 从 FltX 底数和 Int 指数创建缩放因子
         * Create scale from FltX base and Int exponent
         *
         * @param base 底数 / The base value
         * @param index 指数（默认 1） / The exponent value (default 1)
         * @return 缩放因子 / The scale factor
         */
        operator fun invoke(base: FltX, index: Int = 1): Scale {
            return Scale(base, FltX(index.toLong()))
        }

        /**
         * 从 Double 底数和 Int 指数创建缩放因子
         * Create scale from Double base and Int exponent
         *
         * @param base 底数 / The base value
         * @param index 指数（默认 1） / The exponent value (default 1)
         * @return 缩放因子 / The scale factor
         */
        operator fun invoke(base: Double, index: Int = 1): Scale {
            return Scale(FltX(base), index)
        }

        /**
         * 从 Int 底数和 Int 指数创建缩放因子
         * Create scale from Int base and Int exponent
         *
         * @param base 底数 / The base value
         * @param index 指数（默认 1） / The exponent value (default 1)
         * @return 缩放因子 / The scale factor
         */
        operator fun invoke(base: Int, index: Int = 1): Scale {
            return Scale(FltX(base.toLong()), index)
        }

        /**
         * 从 RtnX 底数和 FltX 指数创建缩放因子
         * Create scale from RtnX base and FltX exponent
         *
         * @param base 底数 / The base value
         * @param index 指数 / The exponent value
         * @return 缩放因子 / The scale factor
         */
        operator fun invoke(base: RtnX, index: FltX): Scale {
            val base: ScaleBase = Either.Right(base)
            return Scale(listOf(base to index))
        }

        /**
         * 从 RtnX 底数和 Int 指数创建缩放因子
         * Create scale from RtnX base and Int exponent
         *
         * @param base 底数 / The base value
         * @param index 指数（默认 1） / The exponent value (default 1)
         * @return 缩放因子 / The scale factor
         */
        operator fun invoke(base: RtnX, index: Int = 1): Scale {
            val base: ScaleBase = Either.Right(base)
            return Scale(listOf(base to FltX(index.toLong())))
        }

        /** 按底数值升序排列缩放因子列表 / Sort scale factor list by base value ascending */
        private fun List<Pair<ScaleBase, FltX>>.sort(): List<Pair<ScaleBase, FltX>> {
            return sortedBy {
                when (val base = it.first) {
                    is Either.Left -> base.value
                    is Either.Right -> base.value.toFltX()
                }
            }
        }

        /** 过滤指数为零的项并排序 / Filter out zero-exponent entries and sort */
        private fun List<Pair<ScaleBase, FltX>>.tidy(): List<Pair<ScaleBase, FltX>> {
            return this
                .filter { it.second neq FltX.zero }
                .sort()
        }
    }

    val value by lazy {
        scales.fold(FltX.one) { acc, scale ->
            acc * when (val base = scale.first) {
                is Either.Left -> base.value.pow(scale.second)
                is Either.Right -> base.value.pow(scale.second)
            }
        }.stripTrailingZeros()
    }

    /**
     * 判断两个底数是否匹配
     * Check whether two bases match
     *
     * @param other 另一个底数 / The other base
     * @return 类型和值均相等返回 true / True if type and value are both equal
     */
    private fun ScaleBase.matches(other: ScaleBase): Boolean {
        return when (this) {
            is Either.Left -> when (other) {
                is Either.Left -> this.value eq other.value
                is Either.Right -> false
            }

            is Either.Right -> when (other) {
                is Either.Left -> false
                is Either.Right -> this.value eq other.value
            }
        }
    }

    /**
     * 生成底数的缓存键
     * Generate cache key for the base
     *
     * @return 用于 HashMap 查找的字符串键 / String key for HashMap lookup
     */
    private fun ScaleBase.cacheKey(): String {
        return when (this) {
            is Either.Left -> "L:${value.stripTrailingZeros().toPlainString()}"
            is Either.Right -> "R:$value"
        }
    }

    /**
     * 更新单个底数的指数
     * Update exponent of a single base
     *
     * @param base 要更新的底数 / The base to update
     * @param delta 指数增量 / Exponent delta
     * @return 更新后的新缩放因子 / New scale after update
     */
    private fun updateSingleBase(base: ScaleBase, delta: FltX): Scale {
        val index = scales.indexOfFirst { it.first.matches(base) }
        val newScales = if (index == -1) {
            scales + listOf(base to delta)
        } else {
            scales.mapIndexed { i, pair ->
                if (i == index) {
                    pair.first to (pair.second + delta)
                } else {
                    pair
                }
            }
        }
        return Scale(newScales.tidy())
    }

    /**
     * 与另一个缩放因子合并（乘法或除法）
     * Merge with another scale factor (multiplication or division)
     *
     * @param other 另一个缩放因子 / The other scale factor
     * @param subtract true 表示除法（减指数），false 表示乘法（加指数） / True for division (subtract exponent), false for multiplication (add exponent)
     * @return 合并后的新缩放因子 / New merged scale factor
     */
    private fun mergeWithScale(other: Scale, subtract: Boolean): Scale {
        if (other.scales.isEmpty()) {
            return this
        }

        val merged = scales.toMutableList()
        val indexBuckets = HashMap<String, MutableList<Int>>(merged.size + other.scales.size)

        fun indexBase(index: Int) {
            val key = merged[index].first.cacheKey()
            indexBuckets.getOrPut(key) { mutableListOf() }.add(index)
        }

        fun findExistingIndex(base: ScaleBase): Int {
            val key = base.cacheKey()
            indexBuckets[key]?.forEach { idx ->
                if (merged[idx].first.matches(base)) {
                    return idx
                }
            }

            for (idx in merged.indices) {
                if (merged[idx].first.matches(base)) {
                    return idx
                }
            }

            return -1
        }

        for (i in merged.indices) {
            indexBase(i)
        }

        for ((base, index) in other.scales) {
            val delta = if (subtract) -index else index
            val existingIndex = findExistingIndex(base)
            if (existingIndex == -1) {
                merged.add(base to delta)
                indexBase(merged.lastIndex)
            } else {
                val existing = merged[existingIndex]
                merged[existingIndex] = existing.first to (existing.second + delta)
            }
        }

        return Scale(merged.tidy())
    }

    /**
     * 乘以 FltX
     * Multiply by FltX
     *
     * @param other 乘数 / The multiplier
     * @return 新缩放因子 / New scale factor
     */
    operator fun times(other: FltX): Scale {
        if (other eq FltX.zero) {
            return Scale(FltX.zero, FltX.one)
        }
        val base: ScaleBase = Either.Left(other)
        return updateSingleBase(base, FltX.one)
    }

    /**
     * 乘以 RtnX
     * Multiply by RtnX
     *
     * @param other 乘数 / The multiplier
     * @return 新缩放因子 / New scale factor
     */
    operator fun times(other: RtnX): Scale {
        if (other eq RtnX.zero) {
            return Scale(FltX.zero, FltX.one)
        }
        val base: ScaleBase = Either.Right(other)
        return updateSingleBase(base, FltX.one)
    }

    /**
     * 除以 FltX
     * Divide by FltX
     *
     * @param other 除数 / The divisor
     * @return 新缩放因子 / New scale factor
     * @throws ArithmeticException 除以零时抛出 / If dividing by zero
     */
    operator fun div(other: FltX): Scale {
        if (other eq FltX.zero) {
            throw ArithmeticException("Cannot divide by zero")
        }
        val base: ScaleBase = Either.Left(other)
        return updateSingleBase(base, -FltX.one)
    }

    /**
     * 除以 RtnX
     * Divide by RtnX
     *
     * @param other 除数 / The divisor
     * @return 新缩放因子 / New scale factor
     * @throws ArithmeticException 除以零时抛出 / If dividing by zero
     */
    operator fun div(other: RtnX): Scale {
        if (other eq RtnX.zero) {
            throw ArithmeticException("Cannot divide by zero")
        }
        val base: ScaleBase = Either.Right(other)
        return updateSingleBase(base, -FltX.one)
    }

    /**
     * 乘以另一个缩放因子
     * Multiply by another scale
     *
     * @param other 另一个缩放因子 / The other scale factor
     * @return 合并后的新缩放因子 / New merged scale factor
     */
    operator fun times(other: Scale): Scale {
        return mergeWithScale(other, subtract = false)
    }

    /**
     * 除以另一个缩放因子
     * Divide by another scale
     *
     * @param other 另一个缩放因子 / The other scale factor
     * @return 合并后的新缩放因子 / New merged scale factor
     */
    operator fun div(other: Scale): Scale {
        return mergeWithScale(other, subtract = true)
    }
}

/**
 * 获取缩放因子的值（Java 互操作）
 * Get scale value (Java interop)
 *
 * @param scale 缩放因子 / The scale factor
 * @return 缩放因子的浮点值 / The floating-point value of the scale factor
 */
fun getValue(scale: Scale): FltX {
    return scale.value
}
