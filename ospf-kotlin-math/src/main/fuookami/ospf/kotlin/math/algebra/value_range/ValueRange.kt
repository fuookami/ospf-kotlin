/**
 * 值范囌
 * Value Range
 *
 * 定义值范围类，表示一个数值区间，支持集合操作（并集、交集、包含判断）和算术运算（加、减、乘、除）。
 * Defines value range class representing a numerical interval, with support for set operations (union, intersection, containment) and arithmetic operations (add, subtract, multiply, divide).
 */
package fuookami.ospf.kotlin.math.algebra.value_range


import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Flt32
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int8
import fuookami.ospf.kotlin.math.algebra.number.Int16
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.IntX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.math.operator.Times
import fuookami.ospf.kotlin.math.operator.Contains
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Eq
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.util.*

/**
 * 值范围序列化噌
 * Value Range Serializer
 *
 * 用于尌ValueRange 序列化和反序列化丌JSON 格式，包含上下边界值和区间类型。
 * Used to serialize and deserialize ValueRange to/from JSON format, including lower and upper bound values and interval types.
 *
 * @param T 数值类型，必须是实数和数域
 * @property valueSerializer 值包装器的序列化噌
 */
open class ValueRangeSerializer<T>(
    private val valueSerializer: ValueWrapperSerializer<T>
) : KSerializer<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
    /**
     * 序列化描述符
     * Serialization descriptor
     *
     * 定义了四个字段：lowerBound、upperBound、lowerInterval、upperInterval。
     * Defines four fields: lowerBound, upperBound, lowerInterval, upperInterval.
     */
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ValueRange<T>") {
        element<JsonElement>("lowerBound")
        element<JsonElement>("upperBound")
        element<String>("lowerInterval")
        element<String>("upperInterval")
    }

    /**
     * 序列化值范囌
     * Serializes value range
     *
     * @param encoder JSON 编码噌
     * @param value 要序列化的值范囌
     */
    override fun serialize(encoder: Encoder, value: ValueRange<T>) {
        require(encoder is JsonEncoder)
        encoder.encodeJsonElement(
            buildJsonObject {
                put("lowerBound", encoder.json.encodeToJsonElement(valueSerializer, value.lowerBound.value))
                put("upperBound", encoder.json.encodeToJsonElement(valueSerializer, value.upperBound.value))
                put("lowerInterval", value.lowerBound.interval.toString().lowercase(Locale.getDefault()))
                put("upperInterval", value.upperBound.interval.toString().lowercase(Locale.getDefault()))
            }
        )
    }

    /**
     * 反序列化值范囌
     * Deserializes value range
     *
     * @param decoder JSON 解码噌
     * @return 解析后的值范囌
     */
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): ValueRange<T> {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        require(descriptor.elementNames.all { it in element })
        return ValueRange(
            lowerBound = Bound(
                decoder.json.decodeFromJsonElement(valueSerializer, element["lowerBound"]!!),
                Interval.valueOf(element["lowerInterval"]!!.jsonPrimitive.content.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                })
            ),
            upperBound = Bound(
                decoder.json.decodeFromJsonElement(valueSerializer, element["upperBound"]!!),
                Interval.valueOf(element["upperInterval"]!!.jsonPrimitive.content.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }),
            ),
            constants = valueSerializer.constants
        )
    }
}

/**
 * Int64 类型值范围的序列化器
 * Serializer for Int64 typed value range
 */
data object ValueRangeInt64Serializer : ValueRangeSerializer<Int64>(ValueWrapperSerializer(Int64))

/**
 * UInt64 类型值范围的序列化器
 * Serializer for UInt64 typed value range
 */
data object ValueRangeUInt64Serializer : ValueRangeSerializer<UInt64>(ValueWrapperSerializer(UInt64))

/**
 * Flt64 类型值范围的序列化器
 * Serializer for Flt64 typed value range
 */
data object ValueRangeFlt64Serializer : ValueRangeSerializer<fuookami.ospf.kotlin.math.algebra.number.Flt64>(ValueWrapperSerializer(Flt64))

/**
 * 值范囌
 * Value Range
 *
 * 表示一个数值区间，包含下边界和上边界，每个边界都有对应的区间类型（开区间或闭区间）。
 * Represents a numerical interval, containing lower and upper bounds, each with corresponding interval type (open or closed).
 *
 * 支持的操作包括：
 * - 集合操作：并集、交集、包含判斌
 * - 算术运算：加法、减法、乘法、除泌
 * - 类型转换：转换为 Flt64 类型
 *
 * Supported operations include:
 * - Set operations: union, intersection, containment
 * - Arithmetic operations: addition, subtraction, multiplication, division
 * - Type conversion: conversion to Flt64 type
 *
 * @param T 数值类型，必须是实数和数域
 * @property lowerBound 下边界
 * @property upperBound 上边界
 * @property constants 数值常量对豌
 */
data class ValueRange<T>(
    val lowerBound: Bound<T>,
    val upperBound: Bound<T>,
    private val constants: RealNumberConstants<T>
) : Cloneable, Copyable<ValueRange<T>>, Eq<ValueRange<T>>,
    Plus<ValueRange<T>, ValueRange<T>>, Minus<ValueRange<T>, ValueRange<T>>,
    Times<ValueRange<T>, ValueRange<T>?>, Div<T, ValueRange<T>?>, Contains<T>
        where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        /**
         * 判断区间是否为空
         * Determines if interval is empty
         *
         * 当下边界不满足边界条件或上边界不满足边界条件时，区间为空。
         * Interval is empty when lower bound doesn't satisfy boundary condition or upper bound doesn't satisfy boundary condition.
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param lbInterval 下边界区间类垌
         * @param ubInterval 上边界区间类垌
         * @return 区间是否为空
         */
        fun <T> empty(
            lb: ValueWrapper<T>,
            ub: ValueWrapper<T>,
            lbInterval: Interval,
            ubInterval: Interval
        ): Boolean where T : RealNumber<T>, T : NumberField<T> {
            return if (lb.isNegativeInfinity) {
                false
            } else if (ub.isInfinity) {
                false
            } else if (!lb.isInfinityOrNegativeInfinity && !ub.isInfinityOrNegativeInfinity) {
                !lbInterval.lowerBoundOperator<T>()(lb.unwrap(), ub.unwrap()) || !ubInterval.upperBoundOperator<T>()(ub.unwrap(), lb.unwrap())
            } else {
                true
            }
        }

        /**
         * 创建全范围值范围（从负无穷到正无穷，
         * Creates full range value range (from negative infinity to positive infinity)
         *
         * @param constants 数值常量对豌
         * @return 全范围的值范囌
         */
        operator fun <T> invoke(
            constants: RealNumberConstants<T>
        ): ValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
            return ValueRange(
                Bound(ValueWrapper.NegativeInfinity(constants), Interval.Closed),
                Bound(ValueWrapper.Infinity(constants), Interval.Closed),
                constants
            )
        }

        /**
         * 创建全范围值范围（自动解析常量，
         * Creates full range value range (auto-resolves constants)
         *
         * @return 全范围的值范囌
         */
        inline operator fun <reified T> invoke(): ValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(resolveRealNumberConstants<T>("ValueRange"))
        }

        /**
         * 创建单点值范围（自动解析常量，
         * Creates single-point value range (auto-resolves constants)
         *
         * 上下边界相等且均为闭区间。
         * Upper and lower bounds are equal and both are closed intervals.
         *
         * @param value 单点倌
         * @return 创建结果
         */
        inline operator fun <reified T> invoke(
            value: T
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(value, resolveRealNumberConstants<T>("ValueRange"))
        }

        /**
         * 创建单点值范囌
         * Creates single-point value range
         *
         * @param value 单点倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        operator fun <T> invoke(
            value: T,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return ValueRange(
                value,
                value,
                Interval.Closed,
                Interval.Closed,
                constants
            )
        }

        /**
         * 创建值范围（自动解析常量，
         * Creates value range (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param lbInterval 下边界区间类型，默认为闭区间
         * @param ubInterval 上边界区间类型，默认为闭区间
         * @return 创建结果
         */
        inline operator fun <reified T> invoke(
            lb: T,
            ub: T,
            lbInterval: Interval = Interval.Closed,
            ubInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return ValueRange(
                lb = lb,
                ub = ub,
                lbInterval = lbInterval,
                ubInterval = ubInterval,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建值范囌
         * Creates value range
         *
         * @param lb 下边界倌
         * @param ub 上边界倌
         * @param lbInterval 下边界区间类垌
         * @param ubInterval 上边界区间类垌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        operator fun <T> invoke(
            lb: T,
            ub: T,
            lbInterval: Interval,
            ubInterval: Interval,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val lowerBound = when (val result = ValueWrapper(lb, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            val upperBound = when (val result = ValueWrapper(ub, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            return ValueRange(
                lb = lowerBound,
                ub = upperBound,
                lbInterval = lbInterval,
                ubInterval = ubInterval,
                constants = constants
            )
        }

        /**
         * 创建从指定值到正无穷的值范围（自动解析常量，
         * Creates value range from specified value to positive infinity (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param ub 正无穷标讌
         * @param lbInterval 下边界区间类型，默认为闭区间
         * @return 创建结果
         */
        inline operator fun <reified T> invoke(
            lb: T,
            ub: Infinity,
            lbInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(
                lb = lb,
                ub = ub,
                lbInterval = lbInterval,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建从指定值到正无穷的值范囌
         * Creates value range from specified value to positive infinity
         *
         * @param lb 下边界倌
         * @param ub 正无穷标讌
         * @param lbInterval 下边界区间类垌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        operator fun <T> invoke(
            lb: T,
            ub: Infinity,
            lbInterval: Interval,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val lowerBound = when (val result = ValueWrapper(lb, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            return ValueRange(
                lb = lowerBound,
                ub = ValueWrapper.Infinity(constants),
                lbInterval = lbInterval,
                ubInterval = Interval.Open,
                constants = constants
            )
        }

        /**
         * 创建从负无穷到指定值的值范围（自动解析常量，
         * Creates value range from negative infinity to specified value (auto-resolves constants)
         *
         * @param lb 负无穷标讌
         * @param ub 上边界倌
         * @param ubInterval 上边界区间类型，默认为闭区间
         * @return 创建结果
         */
        inline operator fun <reified T> invoke(
            lb: NegativeInfinity,
            ub: T,
            ubInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(
                lb = lb,
                ub = ub,
                ubInterval = ubInterval,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建从负无穷到指定值的值范囌
         * Creates value range from negative infinity to specified value
         *
         * @param lb 负无穷标讌
         * @param ub 上边界倌
         * @param ubInterval 上边界区间类垌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        operator fun <T> invoke(
            lb: NegativeInfinity,
            ub: T,
            ubInterval: Interval,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val upperBound = when (val result = ValueWrapper(ub, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            return ValueRange(
                lb = ValueWrapper.NegativeInfinity(constants),
                ub = upperBound,
                lbInterval = Interval.Open,
                ubInterval = ubInterval,
                constants = constants
            )
        }

        /**
         * 创建大于等于指定值的值范围（自动解析常量，
         * Creates value range greater than or equal to specified value (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @param lbInterval 下边界区间类型，默认为闭区间
         * @return 创建结果
         */
        inline fun <reified T> geq(
            lb: T,
            lbInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return geq(
                lb = lb,
                lbInterval = lbInterval,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建大于指定值的值范围（自动解析常量，
         * Creates value range greater than specified value (auto-resolves constants)
         *
         * @param lb 下边界倌
         * @return 创建结果
         */
        inline fun <reified T> gr(
            lb: T
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return gr(
                lb = lb,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建大于指定值的值范囌
         * Creates value range greater than specified value
         *
         * 使用开区间下边界。
         * Uses open interval for lower bound.
         *
         * @param lb 下边界倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> gr(
            lb: T,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return geq(
                lb = lb,
                lbInterval = Interval.Open,
                constants = constants
            )
        }

        /**
         * 创建大于等于指定值的值范囌
         * Creates value range greater than or equal to specified value
         *
         * @param lb 下边界倌
         * @param lbInterval 下边界区间类垌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> geq(
            lb: T,
            lbInterval: Interval,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val lowerBound = when (val result = ValueWrapper(lb, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            return invoke(
                lb = lowerBound,
                ub = ValueWrapper.Infinity(constants),
                lbInterval = lbInterval,
                ubInterval = Interval.Open,
                constants = constants
            )
        }

        /**
         * 创建小于等于指定值的值范围（自动解析常量，
         * Creates value range less than or equal to specified value (auto-resolves constants)
         *
         * @param ub 上边界倌
         * @param lbInterval 上边界区间类型，默认为闭区间
         * @return 创建结果
         */
        inline fun <reified T> leq(
            ub: T,
            lbInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return leq(
                ub = ub,
                lbInterval = lbInterval,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建小于指定值的值范围（自动解析常量，
         * Creates value range less than specified value (auto-resolves constants)
         *
         * @param ub 上边界倌
         * @return 创建结果
         */
        inline fun <reified T> ls(
            ub: T
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return ls(
                ub = ub,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 创建小于指定值的值范囌
         * Creates value range less than specified value
         *
         * 使用开区间上边界。
         * Uses open interval for upper bound.
         *
         * @param ub 上边界倌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> ls(
            ub: T,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return leq(
                ub = ub,
                lbInterval = Interval.Open,
                constants = constants
            )
        }

        /**
         * 创建小于等于指定值的值范囌
         * Creates value range less than or equal to specified value
         *
         * @param ub 上边界倌
         * @param lbInterval 上边界区间类垌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        fun <T> leq(
            ub: T,
            lbInterval: Interval,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val upperBound = when (val result = ValueWrapper(ub, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            return invoke(
                lb = ValueWrapper.NegativeInfinity(constants),
                ub = upperBound,
                lbInterval = Interval.Open,
                ubInterval = lbInterval,
                constants = constants
            )
        }

        /**
         * 从值包装器创建值范围（自动解析常量，
         * Creates value range from value wrappers (auto-resolves constants)
         *
         * @param lb 下边界值包装器
         * @param ub 上边界值包装器
         * @param lbInterval 下边界区间类垌
         * @param ubInterval 上边界区间类垌
         * @return 创建结果
         */
        inline operator fun <reified T> invoke(
            lb: ValueWrapper<T>,
            ub: ValueWrapper<T>,
            lbInterval: Interval,
            ubInterval: Interval
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return ValueRange(
                lb = lb,
                ub = ub,
                lbInterval = lbInterval,
                ubInterval = ubInterval,
                constants = resolveRealNumberConstants<T>("ValueRange")
            )
        }

        /**
         * 从值包装器创建值范囌
         * Creates value range from value wrappers
         *
         * @param lb 下边界值包装器
         * @param ub 上边界值包装器
         * @param lbInterval 下边界区间类垌
         * @param ubInterval 上边界区间类垌
         * @param constants 数值常量对豌
         * @return 创建结果
         */
        operator fun <T> invoke(
            lb: ValueWrapper<T>,
            ub: ValueWrapper<T>,
            lbInterval: Interval,
            ubInterval: Interval,
            constants: RealNumberConstants<T>
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return if (!empty(lb, ub, lbInterval, ubInterval)) {
                Ok(
                    ValueRange(
                        lowerBound = Bound(lb, lbInterval),
                        upperBound = Bound(ub, ubInterval),
                        constants = constants
                    )
                )
            } else {
                Failed(
                    Err(
                        code = ErrorCode.IllegalArgument,
                        message = "Invalid range: ${lbInterval.lowerSign}$lb, $ub${ubInterval.upperSign}"
                    )
                )
            }
        }
    }

    /**
     * 区间平均倌
     * Interval mean value
     *
     * 计算上下边界的平均值。
     * Calculates the average of upper and lower bounds.
     */
    val mean by lazy {
        (lowerBound.value + upperBound.value) / constants.two
    }

    /**
     * 区间宽度
     * Interval width
     *
     * 计算上下边界的差值。
     * Calculates the difference between upper and lower bounds.
     */
    val diff by lazy {
        upperBound.value - lowerBound.value
    }

    /**
     * 区间相对精度
     * Interval relative precision
     *
     * 计算区间宽度与平均值的相对比例，用于精度控制。
     * Calculates the relative ratio of interval width to mean value, used for precision control.
     */
    val gap by lazy {
        try {
            diff / max(constants.decimalPrecision, abs(mean.unwrap()))
        } catch (_: Exception) {
            constants.nan!!
        }
    }

    /**
     * 是否为固定值（单点区间，
     * Whether is a fixed value (single-point interval)
     *
     * 当上下边界相等且均为闭区间时为固定值。
     * When upper and lower bounds are equal and both are closed intervals, it's a fixed value.
     */
    val fixed: Boolean by lazy {
        if (lowerBound.interval != Interval.Closed || upperBound.interval != Interval.Closed) {
            false
        } else {
            if (!lowerBound.value.isInfinityOrNegativeInfinity && !upperBound.value.isInfinityOrNegativeInfinity) {
                lowerBound.value.unwrap() eq upperBound.value.unwrap()
            } else {
                false
            }
        }
    }

    /**
     * 固定倌
     * Fixed value
     *
     * 当区间为固定值时返回该值，否则返回 null。
     * Returns the value when interval is fixed, otherwise returns null.
     */
    val fixedValue: T? by lazy {
        if (fixed) {
            lowerBound.value.unwrap()
        } else {
            null
        }
    }

    /**
     * 计算与另一值范围的并集
     * Computes union with another value range
     *
     * 如果两个区间不相交，返回 null。
     * If two intervals don't intersect, returns null.
     *
     * @param rhs 另一个值范囌
     * @return 并集结果，或 null（不相交时）
     */
    infix fun union(rhs: ValueRange<T>): ValueRange<T>? {
        if (upperBound.value ls rhs.lowerBound.value || rhs.upperBound.value ls lowerBound.value) {
            return null
        }

        val newLb = when (lowerBound.value ord rhs.lowerBound.value) {
            is Order.Less -> lowerBound.value
            else -> rhs.lowerBound.value
        }
        val newLbInterval = when (lowerBound.value ord rhs.lowerBound.value) {
            is Order.Less -> lowerBound.interval
            is Order.Greater -> rhs.lowerBound.interval
            else -> lowerBound.interval union rhs.lowerBound.interval
        }
        val newUb = when (upperBound.value ord rhs.upperBound.value) {
            is Order.Less -> rhs.upperBound.value
            else -> upperBound.value
        }
        val newUbInterval = when (upperBound.value ord rhs.upperBound.value) {
            is Order.Less -> rhs.upperBound.interval
            is Order.Greater -> upperBound.interval
            else -> upperBound.interval union rhs.upperBound.interval
        }
        return when (val result = ValueRange(newLb, newUb, newLbInterval, newUbInterval, constants)) {
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
     * 计算与另一值范围的交集
     * Computes intersection with another value range
     *
     * @param rhs 另一个值范囌
     * @return 交集结果，或 null（不相交时）
     */
    infix fun intersect(rhs: ValueRange<T>): ValueRange<T>? {
        val newLb = when (lowerBound.value ord rhs.lowerBound.value) {
            is Order.Less -> rhs.lowerBound.value
            else -> lowerBound.value
        }
        val newLbInterval = if (lowerBound.value.isInfinityOrNegativeInfinity) {
            rhs.lowerBound.interval
        } else if (rhs.lowerBound.value.isInfinityOrNegativeInfinity) {
            lowerBound.interval
        } else {
            when (lowerBound.value ord rhs.lowerBound.value) {
                is Order.Less -> rhs.lowerBound.interval
                is Order.Greater -> lowerBound.interval
                else -> lowerBound.interval intersect rhs.lowerBound.interval
            }
        }
        val newUb = when (upperBound.value ord rhs.upperBound.value) {
            is Order.Less -> upperBound.value
            else -> rhs.upperBound.value
        }
        val newUbInterval = if (upperBound.value.isInfinityOrNegativeInfinity) {
            rhs.upperBound.interval
        } else if (rhs.upperBound.value.isInfinityOrNegativeInfinity) {
            upperBound.interval
        } else {
            when (upperBound.value ord rhs.upperBound.value) {
                is Order.Less -> upperBound.interval
                is Order.Greater -> rhs.upperBound.interval
                else -> upperBound.interval intersect rhs.upperBound.interval
            }
        }
        return when (val result = ValueRange(newLb, newUb, newLbInterval, newUbInterval, constants)) {
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
     * 判断值是否在范围册
     * Determines if value is within range
     *
     * @param value 要判断的倌
     * @return 是否在范围内
     */
    override infix operator fun contains(value: T): Boolean {
        return if (lowerBound.value.isNegativeInfinity && upperBound.value.isInfinity) {
            true
        } else if (lowerBound.value.isNegativeInfinity && !upperBound.value.isInfinityOrNegativeInfinity) {
            upperBound.interval.upperBoundOperator<T>()(upperBound.value.unwrap(), value)
        } else if (!lowerBound.value.isInfinityOrNegativeInfinity && upperBound.value.isInfinity) {
            lowerBound.interval.lowerBoundOperator<T>()(lowerBound.value.unwrap(), value)
        } else if (!lowerBound.value.isInfinityOrNegativeInfinity && !upperBound.value.isInfinityOrNegativeInfinity) {
            val lhs = lowerBound.interval.lowerBoundOperator<T>()(lowerBound.value.unwrap(), value)
            val rhs = upperBound.interval.upperBoundOperator<T>()(upperBound.value.unwrap(), value)
            lhs && rhs
        } else {
            false
        }
    }

    /**
     * 判断另一值范围是否完全包含在本范围内
     * Determines if another value range is fully contained in this range
     *
     * @param valueRange 要判断的值范囌
     * @return 是否完全包含
     */
    infix operator fun contains(valueRange: ValueRange<T>): Boolean {
        val lowerContains = when (lowerBound.value ord valueRange.lowerBound.value) {
            is Order.Less -> true
            is Order.Greater -> false
            else -> valueRange.lowerBound.interval == Interval.Open || lowerBound.interval == Interval.Closed
        }
        if (!lowerContains) {
            return false
        }

        return when (upperBound.value ord valueRange.upperBound.value) {
            is Order.Greater -> true
            is Order.Less -> false
            else -> valueRange.upperBound.interval == Interval.Open || upperBound.interval == Interval.Closed
        }
    }

    /**
     * 复制值范囌
     * Copies value range
     *
     * @return 新的值范围副朌
     */
    override fun copy(): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound.copy(),
            upperBound = upperBound.copy(),
            constants = constants
        )
    }

    /**
     * 部分相等比较
     * Partial equality comparison
     *
     * @param rhs 另一个值范囌
     * @return 是否相等，或无法确定时返囌null
     */
    override fun partialEq(rhs: ValueRange<T>): Boolean? {
        when (val result = lowerBound partialEq rhs.lowerBound) {
            false, null -> {
                return result
            }

            else -> {}
        }

        when (val result = upperBound partialEq rhs.upperBound) {
            false, null -> {
                return result
            }

            else -> {}
        }

        return true
    }

    /**
     * 值范围与数值相劌
     * Adds a number to value range
     *
     * 整体平移区间。
     * Translates the entire interval.
     *
     * @param rhs 要添加的数倌
     * @return 新的值范囌
     */
    operator fun plus(rhs: T): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound + rhs,
            upperBound = upperBound + rhs,
            constants = constants
        )
    }

    /**
     * 两个值范围相劌
     * Adds two value ranges
     *
     * 计算两个区间皌Minkowski 和。
     * Computes the Minkowski sum of two intervals.
     *
     * @param rhs 另一个值范囌
     * @return 新的值范囌
     */
    override fun plus(rhs: ValueRange<T>): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound + rhs.lowerBound,
            upperBound = upperBound + rhs.upperBound,
            constants = constants
        )
    }

    /**
     * 值范围与数值相凌
     * Subtracts a number from value range
     *
     * 整体平移区间（反向）。
     * Translates the entire interval (reverse direction).
     *
     * @param rhs 要减去的数倌
     * @return 新的值范囌
     */
    operator fun minus(rhs: T): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound - rhs,
            upperBound = upperBound - rhs,
            constants = constants
        )
    }

    /**
     * 两个值范围相凌
     * Subtracts two value ranges
     *
     * 计算两个区间皌Minkowski 差。
     * Computes the Minkowski difference of two intervals.
     *
     * @param rhs 另一个值范囌
     * @return 新的值范囌
     */
    override fun minus(rhs: ValueRange<T>): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound - rhs.upperBound,
            upperBound = upperBound - rhs.lowerBound,
            constants = constants
        )
    }

    /**
     * 值范围与数值相乌
     * Multiplies value range by a number
     *
     * 当乘数为正数时，区间方向不变；当乘数为负数时，区间方向翻转。
     * When multiplier is positive, interval direction unchanged; when multiplier is negative, interval direction reversed.
     *
     * @param rhs 要乘的数倌
     * @return 新的值范围，戌null（乘零且边界为无穷时，
     */
    operator fun times(rhs: T): ValueRange<T>? {
        return if (rhs gr constants.zero) {
            try {
                ValueRange(
                    lowerBound = lowerBound * rhs,
                    upperBound = upperBound * rhs,
                    constants = constants
                )
            } catch (_: Exception) {
                null
            }
        } else if (rhs ls constants.zero) {
            try {
                ValueRange(
                    lowerBound = upperBound * rhs,
                    upperBound = lowerBound * rhs,
                    constants = constants
                )
            } catch (_: Exception) {
                null
            }
        } else {
            when (val result = ValueRange(
                lb = constants.zero,
                ub = constants.zero,
                lbInterval = lowerBound.interval,
                ubInterval = upperBound.interval,
                constants = constants
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
    }

    /**
     * 两个值范围相乌
     * Multiplies two value ranges
     *
     * 计算两个区间乘积的边界（取所有组合的极值）。
     * Computes boundaries of product of two intervals (takes extrema of all combinations).
     *
     * @param rhs 另一个值范囌
     * @return 新的值范围，戌null（运算无效时，
     */
    override fun times(rhs: ValueRange<T>): ValueRange<T>? {
        val bounds = try {
            listOf(
                Bound(lowerBound.value * rhs.lowerBound.value, lowerBound.interval intersect rhs.lowerBound.interval),
                Bound(lowerBound.value * rhs.upperBound.value, lowerBound.interval intersect rhs.upperBound.interval),
                Bound(upperBound.value * rhs.lowerBound.value, upperBound.interval intersect rhs.lowerBound.interval),
                Bound(upperBound.value * rhs.upperBound.value, upperBound.interval intersect rhs.upperBound.interval)
            ).sortedWithThreeWayComparator { l, r -> l ord r }
        } catch (_: Exception) {
            return null
        }
        return ValueRange(
            bounds.first(),
            bounds.last(),
            constants
        )
    }

    /**
     * 值范围除以数倌
     * Divides value range by a number
     *
     * 除以零返囌null，其他情况转换为乘以倒数。
     * Division by zero returns null, other cases converted to multiplication by reciprocal.
     *
     * @param rhs 要除的数倌
     * @return 新的值范围，戌null（除零时，
     */
    override fun div(rhs: T): ValueRange<T>? {
        return if (rhs eq constants.zero) {
            null
        } else {
            times(rhs.reciprocal())
        }
    }

    /**
     * 转换丌Flt64 类型的值范囌
     * Converts to Flt64 typed value range
     *
     * @return Flt64 类型的新值范囌
     */
    fun toFlt64() = ValueRange(
        lowerBound.toFlt64(),
        upperBound.toFlt64(),
        Flt64
    )

    /**
     * 相等判断
     * Equality judgment
     *
     * @param other 要比较的对象
     * @return 是否相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueRange<*>
        if (constants != other.constants) return false

        other as ValueRange<T>
        if (lowerBound neq other.lowerBound) return false
        if (upperBound neq other.upperBound) return false

        return true
    }

    /**
     * 计算哈希倌
     * Computes hash value
     *
     * @return 哈希倌
     */
    override fun hashCode(): Int {
        var result = lowerBound.hashCode()
        result = 31 * result + upperBound.hashCode()
        result = 31 * result + constants.hashCode()
        return result
    }

    /**
     * 获取字符串表礌
     * Gets string representation
     *
     * 格式丌[lower, upper] 戌(lower, upper) 等。
     * Format is [lower, upper] or (lower, upper) etc.
     *
     * @return 值范围的字符串形弌
     */
    override fun toString(): String = "${lowerBound.interval.lowerSign}${lowerBound.value}, ${upperBound.value}${upperBound.interval.upperSign}"
}

/**
 * 数值与值范围相劌
 * Adds a number to value range
 *
 * @param value 数倌
 * @param valueRange 值范囌
 * @return 新的值范囌
 */
operator fun <T> T.plus(valueRange: ValueRange<T>): ValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
    return valueRange + this
}

/**
 * 数值与值范围相乌
 * Multiplies a number with value range
 *
 * @param value 数倌
 * @param valueRange 值范囌
 * @return 新的值范围，戌null
 */
operator fun <T> T.times(valueRange: ValueRange<T>): ValueRange<T>? where T : RealNumber<T>, T : NumberField<T> {
    return valueRange * this
}

/**
 * 将数值强制约束在值范围内
 * Coerces number within value range
 *
 * 如果数值超出范围，返回最近的边界值。
 * If number exceeds range, returns nearest boundary value.
 *
 * @param value 数倌
 * @param valueRange 值范囌
 * @return 约束后的数倌
 */
fun <T> T.coerceIn(valueRange: ValueRange<T>): T where T : RealNumber<T>, T : NumberField<T> {
    val lb = valueRange.lowerBound.value.unwrapOrNull()
    val ub = valueRange.upperBound.value.unwrapOrNull()
    return if (lb != null && this ord lb is Order.Less) {
        lb
    } else if (ub != null && this ord ub is Order.Greater) {
        ub
    } else {
        this
    }
}

/**
 * Flt32 类型值范围的取负操作
 * Negation operation for Flt32 typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeFlt32")
operator fun ValueRange<Flt32>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = Flt32
)

/**
 * Flt64 类型值范围的取负操作
 * Negation operation for Flt64 typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeFlt64")
operator fun ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = Flt64
)

/**
 * FltX 类型值范围的取负操作
 * Negation operation for FltX typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeFltX")
operator fun ValueRange<FltX>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = FltX
)

/**
 * Int8 类型值范围的取负操作
 * Negation operation for Int8 typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeInt8")
operator fun ValueRange<Int8>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = Int8
)

/**
 * Int16 类型值范围的取负操作
 * Negation operation for Int16 typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeInt16")
operator fun ValueRange<Int16>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = Int16
)

/**
 * Int32 类型值范围的取负操作
 * Negation operation for Int32 typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeInt32")
operator fun ValueRange<Int32>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = Int32
)

/**
 * Int64 类型值范围的取负操作
 * Negation operation for Int64 typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeInt64")
operator fun ValueRange<Int64>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = Int64
)

/**
 * IntX 类型值范围的取负操作
 * Negation operation for IntX typed value range
 *
 * @return 取负后的新值范囌
 */
@JvmName("negValueRangeIntX")
operator fun ValueRange<IntX>.unaryMinus() = ValueRange(
    upperBound = -upperBound,
    lowerBound = -lowerBound,
    constants = IntX
)
