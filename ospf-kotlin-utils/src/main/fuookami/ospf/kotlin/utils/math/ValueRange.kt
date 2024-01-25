package fuookami.ospf.kotlin.utils.math

import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*

private data object IntervalTypeSerializer : KSerializer<IntervalType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntervalType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IntervalType) {
        encoder.encodeString(value.toString().lowercase(Locale.getDefault()))
    }

    override fun deserialize(decoder: Decoder): IntervalType {
        return IntervalType.valueOf(decoder.decodeString()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }
}

@Serializable(with = IntervalTypeSerializer::class)
enum class IntervalType {
    Open {
        override val lowerSign = "("
        override val upperSign = ")"
        override fun union(rhs: IntervalType) = rhs
        override fun intersect(rhs: IntervalType) = Open
    },
    Closed {
        override val lowerSign = "["
        override val upperSign = "]"
        override fun union(rhs: IntervalType) = Closed
        override fun intersect(rhs: IntervalType) = rhs
    };

    abstract val lowerSign: String
    abstract val upperSign: String
    abstract infix fun union(rhs: IntervalType): IntervalType
    abstract infix fun intersect(rhs: IntervalType): IntervalType
}

private typealias GlobalInfinity = Infinity
private typealias GlobalNegativeInfinity = NegativeInfinity

class ValueWrapperSerializer<T>(
    private val dataSerializer: RealNumberKSerializer<T>
) : KSerializer<ValueWrapper<T>> where T : RealNumber<T>, T : NumberField<T> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("ValueWrapper<T>", PolymorphicKind.SEALED) {
        element("Value", dataSerializer.descriptor)
        element("Infinity", PrimitiveSerialDescriptor("Infinity", PrimitiveKind.DOUBLE))
        element("NegativeInfinity", PrimitiveSerialDescriptor("NegativeInfinity", PrimitiveKind.DOUBLE))
    }

    val constants = dataSerializer.constants

    override fun serialize(encoder: Encoder, value: ValueWrapper<T>) {
        require(encoder is JsonEncoder)
        val element = when (value) {
            is ValueWrapper.Value -> encoder.json.encodeToJsonElement(dataSerializer, value.value)
            is ValueWrapper.Infinity -> encoder.json.encodeToJsonElement(Double.POSITIVE_INFINITY)
            is ValueWrapper.NegativeInfinity -> encoder.json.encodeToJsonElement(Double.NEGATIVE_INFINITY)
        }
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ValueWrapper<T> {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return when (element.jsonPrimitive.doubleOrNull) {
            Double.POSITIVE_INFINITY -> {
                ValueWrapper(Infinity, dataSerializer.constants)
            }

            Double.NEGATIVE_INFINITY -> {
                ValueWrapper(NegativeInfinity, dataSerializer.constants)
            }

            else -> {
                ValueWrapper(decoder.json.decodeFromJsonElement(dataSerializer, element), dataSerializer.constants)
            }
        }
    }
}

@Serializable(with = ValueWrapperSerializer::class)
sealed class ValueWrapper<T>(
    internal val constants: RealNumberConstants<T>
) : Cloneable, Copyable<ValueWrapper<T>>, Ord<ValueWrapper<T>>, Eq<ValueWrapper<T>>,
    Plus<ValueWrapper<T>, ValueWrapper<T>>, Minus<ValueWrapper<T>, ValueWrapper<T>>,
    Times<ValueWrapper<T>, ValueWrapper<T>>, Div<ValueWrapper<T>, ValueWrapper<T>>
        where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        @Throws(IllegalArgumentException::class)
        operator fun <T> invoke(
            value: T,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return when (value) {
                constants.infinity -> Infinity(constants)
                constants.negativeInfinity -> NegativeInfinity(constants)
                constants.nan -> throw IllegalArgumentException("Illegal argument NaN for value range!!!")
                else -> Value(value, constants)
            }
        }

        operator fun <T> invoke(
            _inf: GlobalInfinity,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> = Infinity(constants)

        operator fun <T> invoke(
            _negInf: GlobalNegativeInfinity,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> = NegativeInfinity(constants)
    }

    val isInfinity get() = this is Infinity
    val isNegativeInfinity get() = this is NegativeInfinity
    val isInfinityOrNegativeInfinity by lazy { isInfinity && isNegativeInfinity }

    abstract operator fun plus(rhs: T): ValueWrapper<T>
    abstract operator fun minus(rhs: T): ValueWrapper<T>
    abstract operator fun times(rhs: T): ValueWrapper<T>
    abstract operator fun div(rhs: T): ValueWrapper<T>

    abstract fun toFlt64(): Flt64

    fun unwrap(): T {
        return (this as Value<T>).value
    }

    fun unwrapOrNull(): T? {
        return when (this) {
            is Value<T> -> {
                this.value
            }

            is Infinity<T> -> {
                constants.infinity
            }

            is NegativeInfinity<T> -> {
                constants.negativeInfinity
            }
        }
    }

    class Value<T>(val value: T, constants: RealNumberConstants<T>) :
        ValueWrapper<T>(constants) where T : RealNumber<T>, T : NumberField<T> {
        init {
            assert(value != constants.infinity)
            assert(value != constants.negativeInfinity)
            assert(value != constants.nan)
        }

        override fun copy() = Value(value.copy(), constants)
        public override fun clone() = copy()

        override fun partialEq(rhs: ValueWrapper<T>): Boolean = when (rhs) {
            is Value -> value.eq(rhs.value)
            else -> false
        }

        override fun partialOrd(rhs: ValueWrapper<T>) = when (rhs) {
            is Value -> value.ord(rhs.value)
            is Infinity -> orderOf(-1)
            is NegativeInfinity -> orderOf(1)
        }

        override fun plus(rhs: T): ValueWrapper<T> = ValueWrapper(value + rhs, constants)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(value + rhs.value, constants)
            is Infinity -> ValueWrapper(Infinity, constants)
            is NegativeInfinity -> ValueWrapper(NegativeInfinity, constants)
        }

        override fun minus(rhs: T): ValueWrapper<T> = ValueWrapper(value - rhs, constants)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(value - rhs.value, constants)
            is Infinity -> ValueWrapper(NegativeInfinity, constants)
            is NegativeInfinity -> ValueWrapper(Infinity, constants)
        }

        override fun times(rhs: T): ValueWrapper<T> = ValueWrapper(value * rhs, constants)
        override fun times(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(value * rhs.value, constants)
            is Infinity -> if (value < constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else if (value > constants.zero) {
                ValueWrapper(Infinity, constants)
            } else {
                ValueWrapper(constants.zero, constants)
            }

            is NegativeInfinity -> if (value < constants.zero) {
                ValueWrapper(Infinity, constants)
            } else if (value > constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else {
                ValueWrapper(constants.zero, constants)
            }
        }

        override fun div(rhs: T): ValueWrapper<T> = ValueWrapper(value / rhs, constants)
        override fun div(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(value / rhs.value, constants)
            is Infinity -> if (value < constants.zero) {
                ValueWrapper(-constants.epsilon, constants)
            } else if (value > constants.zero) {
                ValueWrapper(constants.epsilon, constants)
            } else {
                ValueWrapper(constants.zero, constants)
            }

            is NegativeInfinity -> if (value < constants.zero) {
                ValueWrapper(constants.epsilon, constants)
            } else if (value > constants.zero) {
                ValueWrapper(-constants.epsilon, constants)
            } else {
                ValueWrapper(constants.zero, constants)
            }
        }

        override fun toString() = "$value"
        override fun toFlt64() = value.toFlt64()
    }

    class Infinity<T>(constants: RealNumberConstants<T>) :
        ValueWrapper<T>(constants) where T : RealNumber<T>, T : NumberField<T> {
        override fun copy() = Infinity(constants)
        public override fun clone() = copy()

        override fun partialEq(rhs: ValueWrapper<T>): Boolean = rhs is Infinity
        override fun partialOrd(rhs: ValueWrapper<T>) = when (rhs) {
            is Infinity -> orderOf(0)
            else -> orderOf(1)
        }

        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid plus between inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid plus between inf and -inf!!!")
            else -> ValueWrapper(Infinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(Infinity, constants)
            is Infinity -> ValueWrapper(Infinity, constants)
            is NegativeInfinity -> throw IllegalArgumentException("Invalid plus between inf and -inf!!!")
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid minus between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid minus between inf and inf!!!")
            else -> ValueWrapper(Infinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(Infinity, constants)
            is Infinity -> throw IllegalArgumentException("Invalid minus between inf and inf!!!")
            is NegativeInfinity -> ValueWrapper(Infinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid times between inf and nan!!!")
            rhs.constants.negativeInfinity -> ValueWrapper(NegativeInfinity, constants)
            rhs.constants.infinity -> ValueWrapper(Infinity, constants)
            rhs.constants.zero -> ValueWrapper(constants.zero, constants)
            else -> if (rhs < rhs.constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else {
                ValueWrapper(Infinity, constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else if (rhs.value > rhs.constants.zero) {
                ValueWrapper(Infinity, constants)
            } else {
                ValueWrapper(constants.zero, constants)
            }

            is Infinity -> ValueWrapper(Infinity, constants)
            is NegativeInfinity -> ValueWrapper(NegativeInfinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid div between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid div between inf and inf!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid div between inf and -inf!!!")
            rhs.constants.zero -> throw IllegalArgumentException("Invalid div between inf and 0!!!")
            else -> if (rhs < rhs.constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else {
                ValueWrapper(Infinity, constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else if (rhs.value > rhs.constants.zero) {
                ValueWrapper(Infinity, constants)
            } else {
                throw IllegalArgumentException("Invalid div between inf and 0!!!")
            }

            is Infinity -> throw IllegalArgumentException("Invalid div between inf and inf!!!")
            is NegativeInfinity -> throw IllegalArgumentException("Invalid div between inf and -inf!!!")
        }

        override fun toString() = "inf"
        override fun toFlt64() = Flt64.infinity
    }

    class NegativeInfinity<T>(constants: RealNumberConstants<T>) :
        ValueWrapper<T>(constants) where T : RealNumber<T>, T : NumberField<T> {
        override fun copy() = Infinity(constants)
        public override fun clone() = copy()

        override fun partialEq(rhs: ValueWrapper<T>): Boolean = rhs is NegativeInfinity
        override fun partialOrd(rhs: ValueWrapper<T>) = when (rhs) {
            is NegativeInfinity -> orderOf(0)
            else -> orderOf(-1)
        }

        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid plus between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid plus between -inf and inf!!!")
            else -> ValueWrapper(NegativeInfinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(NegativeInfinity, constants)
            is Infinity -> throw IllegalArgumentException("Invalid plus between -inf and inf!!!")
            is NegativeInfinity -> ValueWrapper(NegativeInfinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid minus between -inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid minus between -inf and -inf!!!")
            else -> ValueWrapper(NegativeInfinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> ValueWrapper(NegativeInfinity, constants)
            is Infinity -> ValueWrapper(NegativeInfinity, constants)
            is NegativeInfinity -> throw IllegalArgumentException("Invalid minus between -inf and -inf!!!")
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid times between -inf and nan!!!")
            rhs.constants.negativeInfinity -> ValueWrapper(Infinity, constants)
            rhs.constants.infinity -> ValueWrapper(NegativeInfinity, constants)
            rhs.constants.zero -> ValueWrapper(constants.zero, constants)
            else -> if (rhs < rhs.constants.zero) {
                ValueWrapper(Infinity, constants)
            } else {
                ValueWrapper(NegativeInfinity, constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                ValueWrapper(Infinity, constants)
            } else if (rhs.value > rhs.constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else {
                ValueWrapper(constants.zero, constants)
            }

            is Infinity -> ValueWrapper(NegativeInfinity, constants)
            is NegativeInfinity -> ValueWrapper(Infinity, constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid div between -inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid div between -inf and -inf!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid div between -inf and inf!!!")
            rhs.constants.zero -> throw IllegalArgumentException("Invalid div between -inf and 0!!!")
            else -> if (rhs < rhs.constants.zero) {
                ValueWrapper(Infinity, constants)
            } else {
                ValueWrapper(NegativeInfinity, constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                ValueWrapper(Infinity, constants)
            } else if (rhs.value > rhs.constants.zero) {
                ValueWrapper(NegativeInfinity, constants)
            } else {
                throw IllegalArgumentException("Invalid div between -inf and 0!!!")
            }

            is Infinity -> throw IllegalArgumentException("Invalid div between -inf and inf!!!")
            is NegativeInfinity -> throw IllegalArgumentException("Invalid div between -inf and -inf!!!")
        }

        override fun toString() = "-inf"
        override fun toFlt64() = Flt64.negativeInfinity
    }
}

class ValueRangeSerializer<T>(
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
                put("lowerBound", encoder.json.encodeToJsonElement(valueSerializer, value.lowerBound))
                put("upperBound", encoder.json.encodeToJsonElement(valueSerializer, value.upperBound))
                put("lowerInterval", value.lowerInterval.toString().lowercase(Locale.getDefault()))
                put("upperInterval", value.upperInterval.toString().lowercase(Locale.getDefault()))
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
            decoder.json.decodeFromJsonElement(valueSerializer, element["lowerBound"]!!),
            decoder.json.decodeFromJsonElement(valueSerializer, element["upperBound"]!!),
            IntervalType.valueOf(element["lowerInterval"]!!.jsonPrimitive.content.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }),
            IntervalType.valueOf(element["upperInterval"]!!.jsonPrimitive.content.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }),
            valueSerializer.constants
        )
    }
}

@Serializable(with = ValueRangeSerializer::class)
data class ValueRange<T>(
    private val _lowerBound: ValueWrapper<T>,
    private val _upperBound: ValueWrapper<T>,
    val lowerInterval: IntervalType,
    val upperInterval: IntervalType,
    val constants: RealNumberConstants<T>
) : Cloneable, Copyable<ValueRange<T>>,
    Plus<ValueRange<T>, ValueRange<T>>, Minus<ValueRange<T>, ValueRange<T>>,
    Times<T, ValueRange<T>>, Div<T, ValueRange<T>>, Eq<ValueRange<T>>
        where T : RealNumber<T>, T : NumberField<T> {
    constructor(
        lowerBound: T,
        upperBound: T,
        lowerInterval: IntervalType,
        upperInterval: IntervalType,
        constants: RealNumberConstants<T>
    ) : this(
        ValueWrapper(lowerBound, constants),
        ValueWrapper(upperBound, constants),
        lowerInterval,
        upperInterval,
        constants
    )

    constructor(
        lowerBound: T,
        upperBound: T,
        constants: RealNumberConstants<T>
    ) : this(
        ValueWrapper(lowerBound, constants),
        ValueWrapper(upperBound, constants),
        IntervalType.Closed,
        IntervalType.Closed,
        constants
    )

    constructor(
        _inf: GlobalNegativeInfinity,
        upperBound: T,
        lowerInterval: IntervalType,
        upperInterval: IntervalType,
        constants: RealNumberConstants<T>
    ) : this(
        ValueWrapper(_inf, constants),
        ValueWrapper(upperBound, constants),
        lowerInterval,
        upperInterval,
        constants
    )

    constructor(
        _inf: GlobalNegativeInfinity,
        upperBound: T,
        constants: RealNumberConstants<T>
    ) : this(
        ValueWrapper(_inf, constants),
        ValueWrapper(upperBound, constants),
        IntervalType.Closed,
        IntervalType.Closed,
        constants
    )

    constructor(
        lowerBound: T,
        _inf: GlobalInfinity,
        lowerInterval: IntervalType,
        upperInterval: IntervalType,
        constants: RealNumberConstants<T>
    ) : this(
        ValueWrapper(lowerBound, constants),
        ValueWrapper(_inf, constants),
        lowerInterval,
        upperInterval,
        constants
    )

    constructor(
        lowerBound: T,
        _inf: GlobalInfinity,
        constants: RealNumberConstants<T>
    ) : this(
        ValueWrapper(lowerBound, constants),
        ValueWrapper(_inf, constants),
        IntervalType.Closed,
        IntervalType.Closed,
        constants
    )

    override fun copy() = ValueRange(
        _lowerBound,
        _upperBound,
        lowerInterval,
        upperInterval,
        constants
    )

    public override fun clone() = copy()

    val lowerBound: ValueWrapper<T>
        get() {
            @Throws(IllegalArgumentException::class)
            if (empty) {
                throw IllegalArgumentException("Illegal argument of value range: ${lowerInterval.lowerSign}${_lowerBound}, ${_upperBound}${upperInterval.upperSign}!!!")
            }
            return _lowerBound
        }
    val upperBound: ValueWrapper<T>
        get() {
            @Throws(IllegalArgumentException::class)
            if (empty) {
                throw IllegalArgumentException("Illegal argument of value range: ${lowerInterval.lowerSign}${_lowerBound}, ${_upperBound}${upperInterval.upperSign}!!!")
            }
            return _upperBound
        }

    val fixed by lazy {
        lowerInterval == IntervalType.Closed
                && upperInterval == IntervalType.Closed
                && !lowerBound.isInfinityOrNegativeInfinity
                && !upperBound.isInfinityOrNegativeInfinity
                && lowerBound eq upperBound
    }

    val empty by lazy {
        if (lowerInterval == IntervalType.Closed && upperInterval == IntervalType.Closed) {
            _lowerBound gr _upperBound
        } else {
            _lowerBound geq _upperBound
        }
    }

    val fixedValue: T? by lazy {
        if (fixed) {
            (lowerBound as ValueWrapper.Value).value
        } else {
            null
        }
    }

    operator fun contains(value: T): Boolean {
        val wrapper = ValueWrapper(value, constants)
        return when (lowerInterval) {
            IntervalType.Open -> lowerBound ls wrapper
            IntervalType.Closed -> lowerBound leq wrapper
        } && when (upperInterval) {
            IntervalType.Open -> upperBound gr wrapper
            IntervalType.Closed -> upperBound geq wrapper
        }
    }

    operator fun contains(value: ValueRange<T>): Boolean {
        return intersect(value) eq value
    }

    override fun plus(rhs: ValueRange<T>) = ValueRange(
        lowerBound + rhs.lowerBound,
        upperBound + rhs.upperBound,
        lowerInterval intersect rhs.lowerInterval,
        upperInterval intersect rhs.upperInterval,
        constants
    )

    operator fun plus(rhs: T) = ValueRange(
        lowerBound + rhs,
        upperBound + rhs,
        lowerInterval,
        upperInterval,
        constants
    )

    override fun minus(rhs: ValueRange<T>) = ValueRange(
        lowerBound - rhs.upperBound,
        upperBound - rhs.lowerBound,
        lowerInterval intersect rhs.lowerInterval,
        upperInterval intersect rhs.upperInterval,
        constants
    )

    operator fun minus(rhs: T) = ValueRange(
        lowerBound - rhs,
        upperBound - rhs,
        lowerInterval,
        upperInterval,
        constants
    )

    override fun times(rhs: T) = when {
        rhs < constants.zero -> ValueRange(
            upperBound * rhs,
            lowerBound * rhs,
            lowerInterval,
            upperInterval,
            constants
        )

        else -> ValueRange(
            lowerBound * rhs,
            upperBound * rhs,
            lowerInterval,
            upperInterval,
            constants
        )
    }

    override fun div(rhs: T) = when {
        rhs < constants.zero -> ValueRange(
            upperBound / rhs,
            lowerBound / rhs,
            lowerInterval,
            upperInterval,
            constants
        )

        else -> ValueRange(
            lowerBound / rhs,
            upperBound / rhs,
            lowerInterval,
            upperInterval,
            constants
        )
    }

    override fun partialEq(rhs: ValueRange<T>): Boolean {
        return lowerBound eq rhs.lowerBound
                && upperBound eq rhs.upperBound
                && lowerInterval == rhs.lowerInterval
                && upperInterval == rhs.upperInterval
    }

    infix fun intersect(rhs: ValueRange<T>) = ValueRange(
        max(lowerBound, rhs.lowerBound),
        min(upperBound, rhs.upperBound),
        lowerInterval intersect rhs.lowerInterval,
        upperInterval intersect rhs.upperInterval,
        constants
    )

    fun toFlt64() = ValueRange(
        lowerBound.toFlt64(),
        upperBound.toFlt64(),
        lowerInterval,
        upperInterval,
        Flt64
    )

    override fun toString() = "${lowerInterval.lowerSign}$lowerBound, $upperBound${upperInterval.upperSign}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueRange<*>

        if (_lowerBound != other._lowerBound) return false
        if (_upperBound != other._upperBound) return false
        if (lowerInterval != other.lowerInterval) return false
        if (upperInterval != other.upperInterval) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _lowerBound.hashCode()
        result = 31 * result + _upperBound.hashCode()
        result = 31 * result + lowerInterval.hashCode()
        result = 31 * result + upperInterval.hashCode()
        return result
    }
}

operator fun <T> T.times(rhs: ValueRange<T>) where T : RealNumber<T>, T : NumberField<T> = rhs * this
