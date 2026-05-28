/**
 * 类型化值范囌
 * Typed Value Range
 *
 * 定义类型安全的值范围类，使用泛型参数静态编码区间开闭性，支持闭区间、开区间、半开半闭区间等类型，并提供算术运算和类型推导。
 * Defines type-safe value range class, using generic parameters to statically encode interval openness/closedness, supporting closed, open, and half-open interval types, with arithmetic operations and type inference.
 */
package fuookami.ospf.kotlin.math.algebra.value_range

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.Contains

/**
 * 区间类型标记接口
 * Interval Kind Marker Interface
 *
 * 用于标记区间的开闭性质，支持静态类型推导。
 * Used to mark interval openness/closedness, supporting static type inference.
 *
 * @property interval 对应皌Interval 枚举倌
 */
sealed interface IntervalKind {
    /**
     * 对应皌Interval 枚举倌
     * Corresponding Interval enum value
     */
    val interval: Interval
}

/**
 * 闭区间类型标讌
 * Closed Interval Kind Marker
 *
 * 表示闭区间类型，边界包含边界值。
 * Represents closed interval type, boundary includes boundary value.
 */
data object ClosedIntervalKind : IntervalKind {
    override val interval: Interval = Interval.Closed
}

/**
 * 开区间类型标记
 * Open Interval Kind Marker
 *
 * 表示开区间类型，边界不包含边界值。
 * Represents open interval type, boundary does not include boundary value.
 */
data object OpenIntervalKind : IntervalKind {
    override val interval: Interval = Interval.Open
}

/**
 * 运行时区间类型标讌
 * Runtime Interval Kind Marker
 *
 * 用于动态包装值范围，区间类型在运行时确定。
 * Used for dynamic wrapping of value ranges, interval type determined at runtime.
 *
 * @property interval 对应皌Interval 枚举倌
 */
data class RuntimeIntervalKind(
    override val interval: Interval
) : IntervalKind

/**
 * 动态类型化值范围类型别同
 * Dynamic Typed Value Range Type Alias
 *
 * 上下边界均为运行时确定类型。
 * Both upper and lower bounds are runtime-determined types.
 */
typealias DynamicTypedValueRange<T> = TypedValueRange<T, RuntimeIntervalKind, RuntimeIntervalKind>

/**
 * 闭区间类型化值范围类型别同
 * Closed Typed Value Range Type Alias
 *
 * 上下边界均为闭区间。
 * Both upper and lower bounds are closed intervals.
 */
typealias ClosedTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, ClosedIntervalKind>

/**
 * 开区间类型化值范围类型别同
 * Open Typed Value Range Type Alias
 *
 * 上下边界均为开区间。
 * Both upper and lower bounds are open intervals.
 */
typealias OpenTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, OpenIntervalKind>

/**
 * 左闭右开区间类型化值范围类型别同
 * Closed-Open Typed Value Range Type Alias
 *
 * 下边界为闭区间，上边界为开区间。
 * Lower bound is closed interval, upper bound is open interval.
 */
typealias ClosedOpenTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, OpenIntervalKind>

/**
 * 左开右闭区间类型化值范围类型别同
 * Open-Closed Typed Value Range Type Alias
 *
 * 下边界为开区间，上边界为闭区间。
 * Lower bound is open interval, upper bound is closed interval.
 */
typealias OpenClosedTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, ClosedIntervalKind>

/**
 * 类型化值范囌
 * Typed Value Range
 *
 * 使用泛型参数静态编码区间开闭性的值范围类，提供类型安全的区间运算。
 * Value range class with generic parameters statically encoding interval openness/closedness, providing type-safe interval operations.
 *
 * 类型推导规则，
 * - plus/minus/times/div: 先计箌ValueRange 运算结果，再依据结果上下界区间推富Closed/Open typed kind
 * - 仅当 ValueRange 返回 null 时回退丌null（如空区间、除零）
 * - RuntimeIntervalKind 仅用二dynamic 包装，不用于可静态推导的 typed API 返回倌
 *
 * Type inference rules:
 * - plus/minus/times/div: compute ValueRange result first, then infer Closed/Open from result bounds
 * - Fallback-to-null only when ValueRange returns null (e.g. empty interval, divide by zero)
 * - RuntimeIntervalKind is reserved for dynamic wrappers only, not for statically-inferable typed APIs
 *
 * @param T 数值类型，必须是实数和数域
 * @param LB 下边界区间类垌
 * @param UB 上边界区间类垌
 * @property valueRange 内部的值范囌
 * @property lowerKind 下边界区间类型标讌
 * @property upperKind 上边界区间类型标讌
 */
class TypedValueRange<T, LB : IntervalKind, UB : IntervalKind> private constructor(
    private val valueRange: ValueRange<T>,
    val lowerKind: LB,
    val upperKind: UB
) : Contains<T> where T : RealNumber<T>, T : NumberField<T> {
    /**
     * 根据 Interval 枚举值获取对应的 IntervalKind
     * Gets corresponding IntervalKind from Interval enum value
     *
     * @param interval Interval 枚举倌
     * @return 对应皌IntervalKind
     */
    private fun kindOf(interval: Interval): IntervalKind {
        return when (interval) {
            Interval.Closed -> ClosedIntervalKind
            Interval.Open -> OpenIntervalKind
        }
    }

    /**
     * 将值范围转换为相同类型标记的类型化值范囌
     * Converts value range to typed value range with same kind markers
     *
     * @param range 要转换的值范囌
     * @return 相同类型标记的类型化值范囌
     */
    private fun toSameKindRange(range: ValueRange<T>): TypedValueRange<T, LB, UB> {
        return TypedValueRange.fromDynamic(
            range = range,
            lowerKind = lowerKind,
            upperKind = upperKind
        ).value!!
    }

    /**
     * 将值范围转换为最静态类型标记的类型化值范囌
     * Converts value range to typed value range with most static kind markers
     *
     * 根据值范围的边界区间类型自动推导最静态的类型标记。
     * Automatically infers most static kind markers from value range's boundary interval types.
     *
     * @param range 要转换的值范囌
     * @return 最静态类型标记的类型化值范围，戌null
     */
    private fun toMostStaticKindRange(range: ValueRange<T>): TypedValueRange<T, *, *>? {
        val inferredLower = kindOf(range.lowerBound.interval)
        val inferredUpper = kindOf(range.upperBound.interval)
        return toKindRangeOrNull(
            range = range,
            lowerKind = inferredLower,
            upperKind = inferredUpper
        )
    }

    /**
     * 将值范围转换为指定类型标记的类型化值范囌
     * Converts value range to typed value range with specified kind markers
     *
     * @param NLB 新的下边界区间类垌
     * @param NUB 新的上边界区间类垌
     * @param range 要转换的值范囌
     * @param lowerKind 下边界区间类型标讌
     * @param upperKind 上边界区间类型标讌
     * @return 指定类型标记的类型化值范围，戌null
     */
    private fun <NLB : IntervalKind, NUB : IntervalKind> toKindRangeOrNull(
        range: ValueRange<T>,
        lowerKind: NLB,
        upperKind: NUB
    ): TypedValueRange<T, NLB, NUB>? {
        return when (val result = TypedValueRange.fromDynamic(
            range = range,
            lowerKind = lowerKind,
            upperKind = upperKind
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                null
            }

            is Fatal -> {
                null
            }
        }
    }

    /**
     * 判断数值是否为正数
     * Determines if number is positive
     *
     * @param value 要判断的数倌
     * @return 是否为正敌
     */
    private fun isPositive(value: T): Boolean {
        val zero = value - value
        return value > zero
    }

    /**
     * 判断数值是否为负数
     * Determines if number is negative
     *
     * @param value 要判断的数倌
     * @return 是否为负敌
     */
    private fun isNegative(value: T): Boolean {
        val zero = value - value
        return value < zero
    }

    companion object {
        /**
         * 将值范围转换为动态类型化值范囌
         * Converts value range to dynamic typed value range
         *
         * @param T 数值类垌
         * @param range 要转换的值范囌
         * @return 动态类型化值范囌
         */
        private fun <T> toDynamicRange(
            range: ValueRange<T>
        ): DynamicTypedValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
            return TypedValueRange(
                valueRange = range.copy(),
                lowerKind = RuntimeIntervalKind(range.lowerBound.interval),
                upperKind = RuntimeIntervalKind(range.upperBound.interval)
            )
        }

        /**
         * 从值范围创建类型化值范围（动态验证）
         * Creates typed value range from value range (dynamic validation)
         *
         * 验证值范围的边界区间类型是否与指定的类型标记匹配。
         * Validates whether value range's boundary interval types match specified kind markers.
         *
         * @param T 数值类垌
         * @param LB 下边界区间类垌
         * @param UB 上边界区间类垌
         * @param range 要转换的值范囌
         * @param lowerKind 下边界区间类型标讌
         * @param upperKind 上边界区间类型标讌
         * @return 创建结果
         */
        fun <T, LB : IntervalKind, UB : IntervalKind> fromDynamic(
            range: ValueRange<T>,
            lowerKind: LB,
            upperKind: UB
        ): Ret<TypedValueRange<T, LB, UB>> where T : RealNumber<T>, T : NumberField<T> {
            return if (range.lowerBound.interval == lowerKind.interval && range.upperBound.interval == upperKind.interval) {
                Ok(
                    TypedValueRange(
                        valueRange = range.copy(),
                        lowerKind = lowerKind,
                        upperKind = upperKind
                    )
                )
            } else {
                Failed(
                    ErrorCode.IllegalArgument,
                    "TypedValueRange interval mismatch: expected lower=${lowerKind.interval}, upper=${upperKind.interval}, actual lower=${range.lowerBound.interval}, upper=${range.upperBound.interval}."
                )
            }
        }

        /**
         * 从数值创建类型化值范囌
         * Creates typed value range from numbers
         *
         * @param T 数值类垌
         * @param LB 下边界区间类垌
         * @param UB 上边界区间类垌
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param lowerKind 下边界区间类型标讌
         * @param upperKind 上边界区间类型标讌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T, LB : IntervalKind, UB : IntervalKind> fromValues(
            lb: T,
            ub: T,
            lowerKind: LB,
            upperKind: UB,
            constants: RealNumberConstants<T>
        ): Ret<TypedValueRange<T, LB, UB>> where T : RealNumber<T>, T : NumberField<T> {
            return when (val result = ValueRange(lb, ub, lowerKind.interval, upperKind.interval, constants)) {
                is Ok -> {
                    fromDynamic(result.value, lowerKind, upperKind)
                }

                is Failed -> {
                    Failed(result.error)
                }

                is Fatal -> {
                    Fatal(result.errors)
                }
            }
        }

        /**
         * 从值包装器创建类型化值范囌
         * Creates typed value range from value wrappers
         *
         * @param T 数值类垌
         * @param LB 下边界区间类垌
         * @param UB 上边界区间类垌
         * @param lb 下边界值包装器
         * @param ub 上边界值包装器
         * @param lowerKind 下边界区间类型标讌
         * @param upperKind 上边界区间类型标讌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T, LB : IntervalKind, UB : IntervalKind> fromBounds(
            lb: ValueWrapper<T>,
            ub: ValueWrapper<T>,
            lowerKind: LB,
            upperKind: UB,
            constants: RealNumberConstants<T>
        ): Ret<TypedValueRange<T, LB, UB>> where T : RealNumber<T>, T : NumberField<T> {
            return when (val result = ValueRange(lb, ub, lowerKind.interval, upperKind.interval, constants)) {
                is Ok -> {
                    fromDynamic(result.value, lowerKind, upperKind)
                }

                is Failed -> {
                    Failed(result.error)
                }

                is Fatal -> {
                    Fatal(result.errors)
                }
            }
        }

        /**
         * 创建闭区间类型化值范囌
         * Creates closed typed value range
         *
         * @param T 数值类垌
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> closed(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<ClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, ClosedIntervalKind, ClosedIntervalKind, constants)
        }

        /**
         * 创建开区间类型化值范囌
         * Creates open typed value range
         *
         * @param T 数值类垌
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> open(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<OpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, OpenIntervalKind, OpenIntervalKind, constants)
        }

        /**
         * 创建左闭右开区间类型化值范囌
         * Creates closed-open typed value range
         *
         * @param T 数值类垌
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> closedOpen(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<ClosedOpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, ClosedIntervalKind, OpenIntervalKind, constants)
        }

        /**
         * 创建左开右闭区间类型化值范囌
         * Creates open-closed typed value range
         *
         * @param T 数值类垌
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> openClosed(
            lb: T,
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<OpenClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return fromValues(lb, ub, OpenIntervalKind, ClosedIntervalKind, constants)
        }

        /**
         * 创建闭区间类型化值范围（自动解析常量，
         * Creates closed typed value range (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @return 创建结果
         */
        inline fun <reified T> closed(
            lb: T,
            ub: T
        ): Ret<ClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return closed(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.closed"))
        }

        /**
         * 创建开区间类型化值范围（自动解析常量，
         * Creates open typed value range (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @return 创建结果
         */
        inline fun <reified T> open(
            lb: T,
            ub: T
        ): Ret<OpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return open(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.open"))
        }

        /**
         * 创建左闭右开区间类型化值范围（自动解析常量，
         * Creates closed-open typed value range (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @return 创建结果
         */
        inline fun <reified T> closedOpen(
            lb: T,
            ub: T
        ): Ret<ClosedOpenTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return closedOpen(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.closedOpen"))
        }

        /**
         * 创建左开右闭区间类型化值范围（自动解析常量，
         * Creates open-closed typed value range (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @return 创建结果
         */
        inline fun <reified T> openClosed(
            lb: T,
            ub: T
        ): Ret<OpenClosedTypedValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return openClosed(lb, ub, resolveRealNumberConstants<T>("TypedValueRange.openClosed"))
        }
    }

    /**
     * 下边界倌
     * Lower bound value
     */
    val lowerBound: ValueWrapper<T> get() = valueRange.lowerBound.value

    /**
     * 上边界倌
     * Upper bound value
     */
    val upperBound: ValueWrapper<T> get() = valueRange.upperBound.value

    /**
     * 下边界区间类垌
     * Lower bound interval type
     */
    val lowerInterval: Interval get() = valueRange.lowerBound.interval

    /**
     * 上边界区间类垌
     * Upper bound interval type
     */
    val upperInterval: Interval get() = valueRange.upperBound.interval

    /**
     * 是否为固定值（单点区间，
     * Whether is a fixed value (single-point interval)
     */
    val fixed: Boolean get() = valueRange.fixed

    /**
     * 固定倌
     * Fixed value
     */
    val fixedValue: T? get() = valueRange.fixedValue

    /**
     * 转换为动态值范囌
     * Converts to dynamic value range
     *
     * @return 动态值范围副朌
     */
    fun toDynamic(): ValueRange<T> = valueRange.copy()

    /**
     * 判断值是否在范围册
     * Determines if value is within range
     *
     * @param value 要判断的倌
     * @return 是否在范围内
     */
    override infix operator fun contains(value: T): Boolean {
        return valueRange.contains(value)
    }

    /**
     * 判断另一类型化值范围是否完全包含在本范围内
     * Determines if another typed value range is fully contained in this range
     *
     * @param rhs 要判断的类型化值范囌
     * @return 是否完全包含
     */
    infix operator fun contains(rhs: TypedValueRange<T, *, *>): Boolean {
        return valueRange.contains(rhs.valueRange)
    }

    /**
     * 计算与另一类型化值范围的并集
     * Computes union with another typed value range
     *
     * 返回动态类型化值范围，因为并集的边界类型可能改变。
     * Returns dynamic typed value range, as union's boundary types may change.
     *
     * @param rhs 另一个类型化值范囌
     * @return 动态类型化值范围，戌null（不相交时）
     */
    infix fun union(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange union rhs.valueRange)?.let { toDynamicRange(it) }
    }

    /**
     * 计算与同类型类型化值范围的并集（保持类型）
     * Computes union with same-type typed value range (preserves type)
     *
     * 仅当两个类型化值范围具有相同类型标记时使用。
     * Only used when two typed value ranges have same kind markers.
     *
     * @param rhs 另一个同类型的类型化值范囌
     * @return 相同类型标记的类型化值范围，戌null
     */
    infix fun unionTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return (valueRange union rhs.valueRange)?.let { toSameKindRange(it) }
    }

    /**
     * 计算与另一类型化值范围的交集
     * Computes intersection with another typed value range
     *
     * 返回动态类型化值范围，因为交集的边界类型可能改变。
     * Returns dynamic typed value range, as intersection's boundary types may change.
     *
     * @param rhs 另一个类型化值范囌
     * @return 动态类型化值范围，戌null（不相交时）
     */
    infix fun intersect(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange intersect rhs.valueRange)?.let { toDynamicRange(it) }
    }

    /**
     * 计算与同类型类型化值范围的交集（保持类型）
     * Computes intersection with same-type typed value range (preserves type)
     *
     * 仅当两个类型化值范围具有相同类型标记时使用。
     * Only used when two typed value ranges have same kind markers.
     *
     * @param rhs 另一个同类型的类型化值范囌
     * @return 相同类型标记的类型化值范围，戌null
     */
    infix fun intersectTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return (valueRange intersect rhs.valueRange)?.let { toSameKindRange(it) }
    }

    /**
     * 类型化值范围与数值相加（保持类型，
     * Adds a number to typed value range (preserves type)
     *
     * @param rhs 要添加的数倌
     * @return 相同类型标记的类型化值范囌
     */
    fun plusTyped(rhs: T): TypedValueRange<T, LB, UB> {
        return toSameKindRange(valueRange + rhs)
    }

    /**
     * 类型化值范围与数值相劌
     * Adds a number to typed value range
     *
     * @param rhs 要添加的数倌
     * @return 相同类型标记的类型化值范囌
     */
    operator fun plus(rhs: T): TypedValueRange<T, LB, UB> {
        return plusTyped(rhs)
    }

    /**
     * 类型化值范围与另一类型化值范围相劌
     * Adds typed value range with another typed value range
     *
     * 返回动态类型化值范围，因为加法结果的边界类型可能改变。
     * Returns dynamic typed value range, as addition result's boundary types may change.
     *
     * @param rhs 另一个类型化值范囌
     * @return 动态类型化值范囌
     */
    operator fun plus(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange + rhs.valueRange)
    }

    /**
     * 类型化值范围与同类型类型化值范围相加（保持类型，
     * Adds typed value range with same-type typed value range (preserves type)
     *
     * @param rhs 另一个同类型的类型化值范囌
     * @return 相同类型标记的类型化值范围，戌null
     */
    fun plusTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return toKindRangeOrNull(valueRange + rhs.valueRange, lowerKind, upperKind)
    }

    /**
     * 类型化值范围与另一类型化值范围相加（跨类型推导）
     * Adds typed value range with another typed value range (cross-type inference)
     *
     * 根据结果边界类型自动推导最静态的类型标记。
     * Automatically infers most static kind markers from result boundary types.
     *
     * @param rhs 另一个类型化值范囌
     * @return 最静态类型标记的类型化值范围，戌null
     */
    fun plusTypedAcrossKinds(rhs: TypedValueRange<T, *, *>): TypedValueRange<T, *, *>? {
        return toMostStaticKindRange(valueRange + rhs.valueRange)
    }

    /**
     * 类型化值范围与数值相减（保持类型，
     * Subtracts a number from typed value range (preserves type)
     *
     * @param rhs 要减去的数倌
     * @return 相同类型标记的类型化值范囌
     */
    fun minusTyped(rhs: T): TypedValueRange<T, LB, UB> {
        return toSameKindRange(valueRange - rhs)
    }

    /**
     * 类型化值范围与数值相凌
     * Subtracts a number from typed value range
     *
     * @param rhs 要减去的数倌
     * @return 相同类型标记的类型化值范囌
     */
    operator fun minus(rhs: T): TypedValueRange<T, LB, UB> {
        return minusTyped(rhs)
    }

    /**
     * 类型化值范围与另一类型化值范围相凌
     * Subtracts typed value range with another typed value range
     *
     * 返回动态类型化值范围，因为减法结果的边界类型可能改变。
     * Returns dynamic typed value range, as subtraction result's boundary types may change.
     *
     * @param rhs 另一个类型化值范囌
     * @return 动态类型化值范囌
     */
    operator fun minus(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T> {
        return toDynamicRange(valueRange - rhs.valueRange)
    }

    /**
     * 类型化值范围与同类型类型化值范围相减（保持类型，
     * Subtracts typed value range with same-type typed value range (preserves type)
     *
     * @param rhs 另一个同类型的类型化值范囌
     * @return 相同类型标记的类型化值范围，戌null
     */
    fun minusTyped(rhs: TypedValueRange<T, LB, UB>): TypedValueRange<T, LB, UB>? {
        return toKindRangeOrNull(valueRange - rhs.valueRange, lowerKind, upperKind)
    }

    /**
     * 类型化值范围与另一类型化值范围相减（跨类型推导）
     * Subtracts typed value range with another typed value range (cross-type inference)
     *
     * 根据结果边界类型自动推导最静态的类型标记。
     * Automatically infers most static kind markers from result boundary types.
     *
     * @param rhs 另一个类型化值范囌
     * @return 最静态类型标记的类型化值范围，戌null
     */
    fun minusTypedAcrossKinds(rhs: TypedValueRange<T, *, *>): TypedValueRange<T, *, *>? {
        return toMostStaticKindRange(valueRange - rhs.valueRange)
    }

    /**
     * 类型化值范围与数值相乌
     * Multiplies typed value range by a number
     *
     * 返回动态类型化值范围，因为乘法可能改变区间方向。
     * Returns dynamic typed value range, as multiplication may change interval direction.
     *
     * @param rhs 要乘的数倌
     * @return 动态类型化值范围，戌null
     */
    operator fun times(rhs: T): DynamicTypedValueRange<T>? {
        return (valueRange * rhs)?.let { toDynamicRange(it) }
    }

    /**
     * 类型化值范围与正数相乘（保持类型）
     * Multiplies typed value range by positive number (preserves type)
     *
     * 仅当乘数为正数时使用，区间方向不变。
     * Only used when multiplier is positive, interval direction unchanged.
     *
     * @param rhs 要乘的正敌
     * @return 相同类型标记的类型化值范围，戌null
     */
    fun timesPositive(rhs: T): TypedValueRange<T, LB, UB>? {
        if (!isPositive(rhs)) {
            return null
        }
        return (valueRange * rhs)?.let { toKindRangeOrNull(it, lowerKind, upperKind) }
    }

    /**
     * 类型化值范围与负数相乘（类型翻转）
     * Multiplies typed value range by negative number (type flipped)
     *
     * 仅当乘数为负数时使用，区间方向翻转（上下边界类型互换）。
     * Only used when multiplier is negative, interval direction reversed (upper and lower bound types swapped).
     *
     * @param rhs 要乘的负敌
     * @return 类型翻转的类型化值范围，戌null
     */
    fun timesNegative(rhs: T): TypedValueRange<T, UB, LB>? {
        if (!isNegative(rhs)) {
            return null
        }
        return (valueRange * rhs)?.let { toKindRangeOrNull(it, upperKind, lowerKind) }
    }

    /**
     * 类型化值范围与数值相乘（跨类型推导）
     * Multiplies typed value range by a number (cross-type inference)
     *
     * 根据乘数符号和结果边界类型自动推导最静态的类型标记。
     * Automatically infers most static kind markers from multiplier sign and result boundary types.
     *
     * @param rhs 要乘的数倌
     * @return 最静态类型标记的类型化值范围，戌null
     */
    fun timesTyped(rhs: T): TypedValueRange<T, *, *>? {
        val scaled = valueRange * rhs ?: return null
        return toMostStaticKindRange(scaled)
    }

    /**
     * 类型化值范围与另一类型化值范围相乌
     * Multiplies typed value range with another typed value range
     *
     * 返回动态类型化值范围，因为乘法结果的边界类型可能改变。
     * Returns dynamic typed value range, as multiplication result's boundary types may change.
     *
     * @param rhs 另一个类型化值范囌
     * @return 动态类型化值范围，戌null
     */
    operator fun times(rhs: TypedValueRange<T, *, *>): DynamicTypedValueRange<T>? {
        return (valueRange * rhs.valueRange)?.let { toDynamicRange(it) }
    }

    /**
     * 类型化值范围与另一类型化值范围相乘（跨类型推导）
     * Multiplies typed value range with another typed value range (cross-type inference)
     *
     * 根据结果边界类型自动推导最静态的类型标记。
     * Automatically infers most static kind markers from result boundary types.
     *
     * @param rhs 另一个类型化值范囌
     * @return 最静态类型标记的类型化值范围，戌null
     */
    fun timesTypedAcrossKinds(rhs: TypedValueRange<T, *, *>): TypedValueRange<T, *, *>? {
        val scaled = valueRange * rhs.valueRange ?: return null
        return toMostStaticKindRange(scaled)
    }

    /**
     * 类型化值范围除以数倌
     * Divides typed value range by a number
     *
     * 返回动态类型化值范围，因为除法可能改变区间方向。
     * Returns dynamic typed value range, as division may change interval direction.
     *
     * @param rhs 要除的数倌
     * @return 动态类型化值范围，戌null
     */
    operator fun div(rhs: T): DynamicTypedValueRange<T>? {
        return (valueRange / rhs)?.let { toDynamicRange(it) }
    }

    /**
     * 类型化值范围除以正数（保持类型，
     * Divides typed value range by positive number (preserves type)
     *
     * 仅当除数为正数时使用，区间方向不变。
     * Only used when divisor is positive, interval direction unchanged.
     *
     * @param rhs 要除的正敌
     * @return 相同类型标记的类型化值范围，戌null
     */
    fun divPositive(rhs: T): TypedValueRange<T, LB, UB>? {
        if (!isPositive(rhs)) {
            return null
        }
        return (valueRange / rhs)?.let { toKindRangeOrNull(it, lowerKind, upperKind) }
    }

    /**
     * 类型化值范围除以负数（类型翻转，
     * Divides typed value range by negative number (type flipped)
     *
     * 仅当除数为负数时使用，区间方向翻转（上下边界类型互换）。
     * Only used when divisor is negative, interval direction reversed (upper and lower bound types swapped).
     *
     * @param rhs 要除的负敌
     * @return 类型翻转的类型化值范围，戌null
     */
    fun divNegative(rhs: T): TypedValueRange<T, UB, LB>? {
        if (!isNegative(rhs)) {
            return null
        }
        return (valueRange / rhs)?.let { toKindRangeOrNull(it, upperKind, lowerKind) }
    }

    /**
     * 类型化值范围除以数值（跨类型推导）
     * Divides typed value range by a number (cross-type inference)
     *
     * 根据除数符号和结果边界类型自动推导最静态的类型标记。
     * Automatically infers most static kind markers from divisor sign and result boundary types.
     *
     * @param rhs 要除的数倌
     * @return 最静态类型标记的类型化值范围，戌null
     */
    fun divTyped(rhs: T): TypedValueRange<T, *, *>? {
        val scaled = valueRange / rhs ?: return null
        return toMostStaticKindRange(scaled)
    }

    /**
     * 相等判断
     * Equality judgment
     *
     * @param other 要比较的对象
     * @return 是否相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TypedValueRange<*, *, *>) {
            return false
        }
        return valueRange == other.valueRange && lowerKind == other.lowerKind && upperKind == other.upperKind
    }

    /**
     * 计算哈希倌
     * Computes hash value
     *
     * @return 哈希倌
     */
    override fun hashCode(): Int {
        var result = valueRange.hashCode()
        result = 31 * result + lowerKind.hashCode()
        result = 31 * result + upperKind.hashCode()
        return result
    }

    /**
     * 获取字符串表礌
     * Gets string representation
     *
     * @return 类型化值范围的字符串形弌
     */
    override fun toString(): String {
        return "TypedValueRange(lower=$lowerBound, upper=$upperBound, lowerInterval=$lowerInterval, upperInterval=$upperInterval)"
    }
}

/**
 * 将值范围转换为动态类型化值范囌
 * Converts value range to dynamic typed value range
 *
 * @param T 数值类垌
 * @return 动态类型化值范囌
 */
fun <T> ValueRange<T>.toDynamicTypedValueRange(): DynamicTypedValueRange<T>
        where T : RealNumber<T>, T : NumberField<T> {
    return TypedValueRange.fromDynamic(
        range = this,
        lowerKind = RuntimeIntervalKind(lowerBound.interval),
        upperKind = RuntimeIntervalKind(upperBound.interval)
    ).value!!
}
