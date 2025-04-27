package fuookami.ospf.kotlin.utils.math.value_range

import java.util.*
import kotlin.reflect.full.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

open class ValueRangeSerializer<T>(
    private val valueSerializer: ValueWrapperSerializer<T>
) : KSerializer<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ValueRange<T>") {
        element<JsonElement>("lowerBound")
        element<JsonElement>("upperBound")
        element<String>("lowerInterval")
        element<String>("upperInterval")
    }

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

data object ValueRangeInt64Serializer : ValueRangeSerializer<Int64>(ValueWrapperSerializer<Int64>())
data object ValueRangeUInt64Serializer : ValueRangeSerializer<UInt64>(ValueWrapperSerializer<UInt64>())
data object ValueRangeFlt64Serializer : ValueRangeSerializer<Flt64>(ValueWrapperSerializer<Flt64>())

data class ValueRange<T>(
    val lowerBound: Bound<T>,
    val upperBound: Bound<T>,
    private val constants: RealNumberConstants<T>
) : Cloneable, Copyable<ValueRange<T>>, Eq<ValueRange<T>>,
    Plus<ValueRange<T>, ValueRange<T>>, Minus<ValueRange<T>, ValueRange<T>>,
    Times<ValueRange<T>, ValueRange<T>?>, Div<T, ValueRange<T>?>
        where T : RealNumber<T>, T : NumberField<T> {
    companion object {
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

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(): ValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            return ValueRange(
                Bound(ValueWrapper.NegativeInfinity(constants), Interval.Closed),
                Bound(ValueWrapper.Infinity(constants), Interval.Closed),
                constants
            )
        }

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            value: T
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(value, (T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

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

        @Suppress("UNCHECKED_CAST")
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
                constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            )
        }

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
            }
            val upperBound = when (val result = ValueWrapper(ub, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
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

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            lb: T,
            ub: Infinity,
            lbInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            val lowerBound = when (val result = ValueWrapper(lb, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
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

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            lb: NegativeInfinity,
            ub: T,
            ubInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            val upperBound = when (val result = ValueWrapper(ub, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
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

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> geq(
            lb: T,
            lbInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return geq(lb, lbInterval, (T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> gr(
            lb: T
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return geq(lb, Interval.Open, (T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

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
            }
            return invoke(
                lb = lowerBound,
                ub = ValueWrapper.Infinity(constants),
                lbInterval = lbInterval,
                ubInterval = Interval.Open,
                constants = constants
            )
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> leq(
            ub: T,
            lbInterval: Interval = Interval.Closed
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return leq(ub, lbInterval, (T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> ls(
            ub: T
        ): Ret<ValueRange<T>> where T : RealNumber<T>, T : NumberField<T> {
            return leq(ub, Interval.Open, (T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

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
            }
            return invoke(
                lb = ValueWrapper.NegativeInfinity(constants),
                ub = upperBound,
                lbInterval = Interval.Open,
                ubInterval = lbInterval,
                constants = constants
            )
        }

        @Suppress("UNCHECKED_CAST")
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
                constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            )
        }

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

    val mean by lazy {
        (lowerBound.value + upperBound.value) / constants.two
    }

    val diff by lazy {
        upperBound.value - lowerBound.value
    }

    val gap by lazy {
        try {
            diff / max(constants.decimalPrecision, abs(mean.unwrap()))
        } catch (e: Exception) {
            e.printStackTrace()
            constants.nan!!
        }
    }

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

    val fixedValue: T? by lazy {
        if (fixed) {
            lowerBound.value.unwrap()
        } else {
            null
        }
    }

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
        }
    }

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
        }
    }

    infix fun contains(value: T): Boolean {
        return if (lowerBound.value.isNegativeInfinity || upperBound.value.isInfinity) {
            true
        } else if (!lowerBound.value.isInfinityOrNegativeInfinity && !upperBound.value.isInfinityOrNegativeInfinity) {
            val lhs = lowerBound.interval.lowerBoundOperator<T>()(lowerBound.value.unwrap(), value)
            val rhs = upperBound.interval.upperBoundOperator<T>()(upperBound.value.unwrap(), value)
            lhs && rhs
        } else {
            false
        }
    }

    infix fun contains(valueRange: ValueRange<T>): Boolean {
        return if (lowerBound.value.isNegativeInfinity || upperBound.value.isInfinity) {
            true
        } else if (!lowerBound.value.isInfinityOrNegativeInfinity && !upperBound.value.isInfinityOrNegativeInfinity
            && !valueRange.lowerBound.value.isInfinityOrNegativeInfinity && !valueRange.upperBound.value.isInfinityOrNegativeInfinity
        ) {
            val lbInterval = lowerBound.interval intersect valueRange.lowerBound.interval
            val ubInterval = upperBound.interval intersect valueRange.upperBound.interval
            val lhs = lbInterval.lowerBoundOperator<T>()(lowerBound.value.unwrap(), valueRange.lowerBound.value.unwrap())
            val rhs = ubInterval.upperBoundOperator<T>()(upperBound.value.unwrap(), valueRange.upperBound.value.unwrap())
            lhs && rhs
        } else {
            false
        }
    }

    override fun copy(): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound.copy(),
            upperBound = upperBound.copy(),
            constants = constants
        )
    }

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

    operator fun plus(rhs: T): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound + rhs,
            upperBound = upperBound + rhs,
            constants = constants
        )
    }

    override fun plus(rhs: ValueRange<T>): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound + rhs.lowerBound,
            upperBound = upperBound + rhs.upperBound,
            constants = constants
        )
    }

    operator fun minus(rhs: T): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound - rhs,
            upperBound = upperBound - rhs,
            constants = constants
        )
    }

    override fun minus(rhs: ValueRange<T>): ValueRange<T> {
        return ValueRange(
            lowerBound = lowerBound - rhs.upperBound,
            upperBound = upperBound - rhs.lowerBound,
            constants = constants
        )
    }

    operator fun times(rhs: T): ValueRange<T>? {
        return if (rhs gr constants.zero) {
            try {
                ValueRange(lowerBound * rhs, upperBound * rhs, constants)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else if (rhs ls constants.zero) {
            try {
                ValueRange(upperBound * rhs, lowerBound * rhs, constants)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            when (val result = ValueRange(constants.zero, constants.zero, lowerBound.interval, upperBound.interval, constants)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    null
                }
            }
        }
    }

    override fun times(rhs: ValueRange<T>): ValueRange<T>? {
        val bounds = try {
            listOf(
                Bound(lowerBound.value * rhs.lowerBound.value, lowerBound.interval intersect rhs.lowerBound.interval),
                Bound(lowerBound.value * rhs.upperBound.value, lowerBound.interval intersect rhs.upperBound.interval),
                Bound(upperBound.value * rhs.lowerBound.value, upperBound.interval intersect rhs.lowerBound.interval),
                Bound(upperBound.value * rhs.upperBound.value, upperBound.interval intersect rhs.upperBound.interval)
            ).sortedWithThreeWayComparator { l, r -> l ord r }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return ValueRange(
            bounds.first(),
            bounds.last(),
            constants
        )
    }

    override fun div(rhs: T): ValueRange<T>? {
        return if (rhs eq constants.zero) {
            null
        } else {
            times(rhs.reciprocal())
        }
    }

    fun toFlt64() = ValueRange(
        lowerBound.toFlt64(),
        upperBound.toFlt64(),
        Flt64
    )

    @Suppress("UNCHECKED_CAST")
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

    override fun hashCode(): Int {
        var result = lowerBound.hashCode()
        result = 31 * result + upperBound.hashCode()
        result = 31 * result + constants.hashCode()
        return result
    }

    override fun toString(): String = "${lowerBound.interval.lowerSign}${lowerBound.value}, ${upperBound.value}${upperBound.interval.upperSign}"
}

operator fun <T> T.plus(valueRange: ValueRange<T>): ValueRange<T> where T : RealNumber<T>, T : NumberField<T> {
    return valueRange + this
}

operator fun <T> T.times(valueRange: ValueRange<T>): ValueRange<T>? where T : RealNumber<T>, T : NumberField<T> {
    return valueRange * this
}

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

@JvmName("negValueRangeFlt32")
operator fun ValueRange<Flt32>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    Flt32
)

@JvmName("negValueRangeFlt64")
operator fun ValueRange<Flt64>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    Flt64
)

@JvmName("negValueRangeFltX")
operator fun ValueRange<FltX>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    FltX
)

@JvmName("negValueRangeInt8")
operator fun ValueRange<Int8>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    Int8
)

@JvmName("negValueRangeInt16")
operator fun ValueRange<Int16>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    Int16
)

@JvmName("negValueRangeInt32")
operator fun ValueRange<Int32>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    Int32
)

@JvmName("negValueRangeInt64")
operator fun ValueRange<Int64>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    Int64
)

@JvmName("negValueRangeIntX")
operator fun ValueRange<IntX>.unaryMinus() = ValueRange(
    -upperBound,
    -lowerBound,
    IntX
)
