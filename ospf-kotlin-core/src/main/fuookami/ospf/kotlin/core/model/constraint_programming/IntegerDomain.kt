/**
 * CP 整数值域。 / Integer domains for constraint programming.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import java.math.BigInteger
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * CP 的整数值域，只允许精确的有符号 64 位整数。直接构造 [Interval] 或 [Values] 时调用方应保证其不变量；模型建模代码应优先使用 / An exact signed 64-bit integer domain for CP.
 * [interval] 和 [values] 工厂，以便把非法输入转换成 [Ret] 失败。 / Callers constructing [Interval] or [Values] directly are expected to preserve their invariants; model-building code should prefer [interval] and [values] so invalid input is returned as a [Ret] failure.
 */
sealed interface IntegerDomain {
    /** 最小值 / Lower bound. */
    val lowerBound: Int64

    /** 最大值 / Upper bound. */
    val upperBound: Int64

    /** 是否是单点值域 / Whether the domain contains exactly one value. */
    val singleton: Boolean
        get() = lowerBound == upperBound

    /** 可表示的值数量；超过 [ULong] 范围时返回 null。 / Cardinality, or null if it exceeds ULong. */
    val cardinality: ULong?

    /**
     * 判断值是否属于值域。 / Check whether a value belongs to the domain.
     *
     * @param value 待检查的整数 / Integer to check
     * @return 值是否属于值域 / Whether the value belongs to the domain
     */
    operator fun contains(value: Int64): Boolean

    /**
     * 在规模受限时枚举值域。 / Enumerate the domain when its size is bounded.
     *
     * @param maximumAmount 最大枚举数量 / Maximum amount to enumerate
     * @return 有限枚举结果；超过门禁时返回 null / Values, or null when the limit is exceeded
     */
    fun enumerate(maximumAmount: Int = DEFAULT_ENUMERATION_LIMIT): List<Int64>? {
        val amount = cardinality ?: return null
        if (amount > maximumAmount.toULong()) {
            return null
        }
        return when (this) {
            is Interval -> {
                val result = ArrayList<Int64>(amount.toInt())
                var current = lowerBound.toLong()
                val end = upperBound.toLong()
                while (current <= end) {
                    result += Int64(current)
                    if (current == Long.MAX_VALUE) {
                        break
                    }
                    ++current
                }
                result
            }

            is Values -> values
        }
    }

    /**
     * 连续整数区间 / A contiguous integer interval.
     *
     * @property lowerBound 下界 / Lower bound
     * @property upperBound 上界 / Upper bound
     */
    data class Interval(
        override val lowerBound: Int64,
        override val upperBound: Int64
    ) : IntegerDomain {
        override val cardinality: ULong?
            get() = inclusiveCardinality(lowerBound.toLong(), upperBound.toLong())

        override fun contains(value: Int64): Boolean {
            return value >= lowerBound && value <= upperBound
        }
    }

    /**
     * 离散整数集合，工厂会排序并去重。 / A sparse integer set, sorted and de-duplicated by its factory.
     *
     * @property values 排序去重后的整数集合 / Sorted, de-duplicated integer values
     */
    data class Values(
        val values: List<Int64>
    ) : IntegerDomain {
        override val lowerBound: Int64
            get() = values.firstOrNull() ?: Int64.zero

        override val upperBound: Int64
            get() = values.lastOrNull() ?: Int64.zero

        override val cardinality: ULong?
            get() = values.size.toULong()

        override fun contains(value: Int64): Boolean {
            return values.contains(value)
        }
    }

    companion object {
        /** 默认布尔值域 `{0, 1}`。 / The default Boolean domain `{0, 1}`. */
        val boolean: Values = Values(listOf(Int64.zero, Int64.one))

        /**
         * 创建连续整数区间。 / Create a contiguous integer interval.
         *
         * @param lowerBound 下界 / Lower bound
         * @param upperBound 上界 / Upper bound
         * @return 值域或非法参数错误 / The domain or an illegal-argument error
         */
        operator fun invoke(lowerBound: Int64, upperBound: Int64): Ret<Interval> {
            return interval(lowerBound, upperBound)
        }

        /**
         * 创建连续整数区间。 / Create a contiguous integer interval.
         *
         * @param lowerBound 下界 / Lower bound
         * @param upperBound 上界 / Upper bound
         * @return 值域或非法参数错误 / The domain or an illegal-argument error
         */
        fun interval(lowerBound: Int64, upperBound: Int64): Ret<Interval> {
            if (lowerBound > upperBound) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "整数值域下界不得大于上界：$lowerBound > $upperBound / " +
                        "Integer domain lower bound must not exceed upper bound: $lowerBound > $upperBound"
                )
            }
            return ok(Interval(lowerBound, upperBound))
        }

        /**
         * 使用 Long 创建区间的便捷入口。 / Long convenience overload for an interval.
         *
         * @param lowerBound 下界 / Lower bound
         * @param upperBound 上界 / Upper bound
         * @return 值域或非法参数错误 / The domain or an illegal-argument error
         */
        @JvmName("intervalLong")
        fun interval(lowerBound: Long, upperBound: Long): Ret<Interval> {
            return interval(Int64(lowerBound), Int64(upperBound))
        }

        /**
         * 使用 Int 创建区间的便捷入口。 / Int convenience overload for an interval.
         *
         * @param lowerBound 下界 / Lower bound
         * @param upperBound 上界 / Upper bound
         * @return 值域或非法参数错误 / The domain or an illegal-argument error
         */
        @JvmName("intervalInt")
        fun interval(lowerBound: Int, upperBound: Int): Ret<Interval> {
            return interval(Int64(lowerBound.toLong()), Int64(upperBound.toLong()))
        }

        /**
         * 创建离散整数集合；输入会排序、去重并复制。 / Create a sparse integer set; input is sorted, de-duplicated, and copied.
         * 支持 [Int64] 以及可无损转换的 Kotlin 整数类型。 / Supports [Int64] and losslessly convertible Kotlin integer types.
         * @param values 离散值 / Sparse values
         * @return 值域或非法参数错误 / The domain or an illegal-argument error
         */
        fun values(values: Iterable<*>): Ret<Values> {
            val converted = ArrayList<Int64>()
            for (value in values) {
                val integer = when (value) {
                    is Int64 -> value
                    is Long -> Int64(value)
                    is Int -> Int64(value.toLong())
                    is Short -> Int64(value.toLong())
                    is Byte -> Int64(value.toLong())
                    is UInt64 -> {
                        if (value.toULong() > Long.MAX_VALUE.toULong()) {
                            return Failed(
                                ErrorCode.IllegalArgument,
                                "无符号值超出 CP Int64 范围：$value / " +
                                    "Unsigned value exceeds the CP Int64 range: $value"
                            )
                        }
                        Int64(value.toULong().toLong())
                    }

                    is ULong -> {
                        if (value > Long.MAX_VALUE.toULong()) {
                            return Failed(
                                ErrorCode.IllegalArgument,
                                "无符号值超出 CP Int64 范围：$value / " +
                                    "Unsigned value exceeds the CP Int64 range: $value"
                            )
                        }
                        Int64(value.toLong())
                    }

                    is UInt -> Int64(value.toLong())
                    is UShort -> Int64(value.toLong())
                    is UByte -> Int64(value.toLong())
                    else -> {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "值域包含非整数值：$value / Integer domain contains a non-integer value: $value"
                        )
                    }
                }
                converted += integer
            }

            if (converted.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "整数离散值域不能为空 / Integer sparse domain must not be empty"
                )
            }

            return ok(
                Values(
                    converted
                        .distinct()
                        .sortedBy { it.toLong() }
                )
            )
        }

        /**
         * 创建稀疏值域的语义别名。 / Semantic alias for creating a sparse domain.
         *
         * @param values 离散值 / Sparse values
         * @return 值域或非法参数错误 / The domain or an illegal-argument error
         */
        fun sparse(values: Iterable<*>): Ret<Values> {
            return values(values)
        }

        /**
         * 创建单点值域。 / Create a singleton domain.
         *
         * @param value 单点值 / Singleton value
         * @return 单点值域 / Singleton domain
         */
        fun singleton(value: Int64): Values {
            return Values(listOf(value))
        }

        /**
         * 创建单点值域的 Long 便捷入口。 / Long convenience overload for a singleton domain.
         *
         * @param value 单点值 / Singleton value
         * @return 单点值域 / Singleton domain
         */
        @JvmName("singletonLong")
        fun singleton(value: Long): Values {
            return singleton(Int64(value))
        }

        private const val DEFAULT_ENUMERATION_LIMIT = 10_000
    }
}

/** Sparse-domain semantic alias. / 稀疏值域语义别名。 */
typealias SparseIntegerDomain = IntegerDomain.Values

private fun inclusiveCardinality(lowerBound: Long, upperBound: Long): ULong? {
    if (lowerBound > upperBound) {
        return null
    }
    val cardinality = BigInteger.valueOf(upperBound)
        .subtract(BigInteger.valueOf(lowerBound))
        .add(BigInteger.ONE)
    return cardinality.toString().toULongOrNull()
}
