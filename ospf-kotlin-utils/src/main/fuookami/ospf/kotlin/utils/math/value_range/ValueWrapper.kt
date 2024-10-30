package fuookami.ospf.kotlin.utils.math.value_range

import java.util.*
import kotlin.reflect.full.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

internal typealias GlobalInfinity = Infinity
internal typealias GlobalNegativeInfinity = NegativeInfinity

class ValueWrapperSerializer<T>(
    private val valueSerializer: KSerializer<T>,
    internal val constants: RealNumberConstants<T>
) : KSerializer<ValueWrapper<T>> where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T> invoke(): ValueWrapperSerializer<T> where T : RealNumber<T>, T : NumberField<T> {
            val serializer = T::class.serializer()
            val constants = (T::class.companionObjectInstance!! as RealNumberConstants<T>)
            return ValueWrapperSerializer(serializer, constants)
        }
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("ValueWrapper<T>", PolymorphicKind.SEALED) {
        element("Value", valueSerializer.descriptor)
        element("Infinity", PrimitiveSerialDescriptor("Infinity", PrimitiveKind.DOUBLE))
        element("NegativeInfinity", PrimitiveSerialDescriptor("NegativeInfinity", PrimitiveKind.DOUBLE))
    }

    override fun serialize(encoder: Encoder, value: ValueWrapper<T>) {
        require(encoder is JsonEncoder)
        val element = when (value) {
            is ValueWrapper.Value -> encoder.json.encodeToJsonElement(valueSerializer, value.value)
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
                ValueWrapper.Infinity(constants)
            }

            Double.NEGATIVE_INFINITY -> {
                ValueWrapper.NegativeInfinity(constants)
            }

            else -> {
                ValueWrapper.Value(decoder.json.decodeFromJsonElement(valueSerializer, element), constants)
            }
        }
    }
}

sealed class ValueWrapper<T>(
    val constants: RealNumberConstants<T>
) : Cloneable, Copyable<ValueWrapper<T>>, Ord<ValueWrapper<T>>, Eq<ValueWrapper<T>>,
    Plus<ValueWrapper<T>, ValueWrapper<T>>, Minus<ValueWrapper<T>, ValueWrapper<T>>,
    Times<ValueWrapper<T>, ValueWrapper<T>>, Div<ValueWrapper<T>, ValueWrapper<T>>
        where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            value: T
        ): Ret<ValueWrapper<T>> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(value, (T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

        operator fun <T> invoke(
            value: T,
            constants: RealNumberConstants<T>
        ): Ret<ValueWrapper<T>> where T : RealNumber<T>, T : NumberField<T> {
            return when (value) {
                constants.infinity -> Ok(Infinity(constants))
                constants.negativeInfinity -> Ok(NegativeInfinity(constants))
                constants.nan -> Failed(ErrorCode.IllegalArgument, "Illegal argument NaN for value range!!!")
                else -> Ok(Value(value, constants))
            }
        }

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            _inf: GlobalInfinity
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return Infinity((T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

        operator fun <T> invoke(
            _inf: GlobalInfinity,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return Infinity(constants)
        }

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            _negInf: GlobalNegativeInfinity
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return NegativeInfinity((T::class.companionObjectInstance!! as RealNumberConstants<T>))
        }

        operator fun <T> invoke(
            _negInf: GlobalNegativeInfinity,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return NegativeInfinity(constants)
        }
    }

    val isInfinity get() = this is Infinity
    val isNegativeInfinity get() = this is NegativeInfinity
    val isInfinityOrNegativeInfinity by lazy { isInfinity || isNegativeInfinity }

    abstract operator fun plus(rhs: T): ValueWrapper<T>
    abstract operator fun minus(rhs: T): ValueWrapper<T>
    abstract operator fun times(rhs: T): ValueWrapper<T>
    abstract operator fun div(rhs: T): ValueWrapper<T>

    abstract fun toFlt64(): Flt64

    fun unwrap(): T {
        return when (this) {
            is Value<T> -> {
                this.value
            }

            is Infinity<T> -> {
                constants.infinity!!
            }

            is NegativeInfinity<T> -> {
                constants.negativeInfinity!!
            }
        }
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

    infix fun eq(rhs: T): Boolean {
        return when (rhs) {
            constants.infinity -> this is Infinity
            constants.negativeInfinity -> this is NegativeInfinity
            constants.nan -> throw IllegalArgumentException("Illegal argument NaN for value range!!!")
            else -> (this as? Value<T>)?.value?.eq(rhs) == true
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

        override fun plus(rhs: T): ValueWrapper<T> = ValueWrapper(value + rhs, constants).value!!
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Value(value + rhs.value, constants)
            is Infinity -> Infinity(constants)
            is NegativeInfinity -> NegativeInfinity(constants)
        }

        override fun minus(rhs: T): ValueWrapper<T> = ValueWrapper(value - rhs, constants).value!!
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Value(value - rhs.value, constants)
            is Infinity -> NegativeInfinity(constants)
            is NegativeInfinity -> Infinity(constants)
        }

        override fun times(rhs: T): ValueWrapper<T> = ValueWrapper(value * rhs, constants).value!!
        override fun times(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Value(value * rhs.value, constants)
            is Infinity -> if (value < constants.zero) {
                NegativeInfinity(constants)
            } else if (value > constants.zero) {
                Infinity(constants)
            } else {
                Value(constants.zero, constants)
            }

            is NegativeInfinity -> if (value < constants.zero) {
                Infinity(constants)
            } else if (value > constants.zero) {
                NegativeInfinity(constants)
            } else {
                Value(constants.zero, constants)
            }
        }

        override fun div(rhs: T): ValueWrapper<T> = ValueWrapper(value / rhs, constants).value!!
        override fun div(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Value(value / rhs.value, constants)
            is Infinity -> if (value < constants.zero) {
                Value(-constants.epsilon, constants)
            } else if (value > constants.zero) {
                Value(constants.epsilon, constants)
            } else {
                Value(constants.zero, constants)
            }

            is NegativeInfinity -> if (value < constants.zero) {
                Value(constants.epsilon, constants)
            } else if (value > constants.zero) {
                Value(-constants.epsilon, constants)
            } else {
                Value(constants.zero, constants)
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
            else -> Infinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Infinity(constants)
            is Infinity -> Infinity(constants)
            is NegativeInfinity -> throw IllegalArgumentException("Invalid plus between inf and -inf!!!")
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid minus between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid minus between inf and inf!!!")
            else -> Infinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Infinity(constants)
            is Infinity -> throw IllegalArgumentException("Invalid minus between inf and inf!!!")
            is NegativeInfinity -> Infinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid times between inf and nan!!!")
            rhs.constants.negativeInfinity -> NegativeInfinity(constants)
            rhs.constants.infinity -> Infinity(constants)
            rhs.constants.zero -> Value(constants.zero, constants)
            else -> if (rhs < rhs.constants.zero) {
                NegativeInfinity(constants)
            } else {
                Infinity(constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                NegativeInfinity(constants)
            } else if (rhs.value > rhs.constants.zero) {
                Infinity(constants)
            } else {
                Value(constants.zero, constants)
            }

            is Infinity -> Infinity(constants)
            is NegativeInfinity -> NegativeInfinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid div between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid div between inf and inf!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid div between inf and -inf!!!")
            rhs.constants.zero -> throw IllegalArgumentException("Invalid div between inf and 0!!!")
            else -> if (rhs < rhs.constants.zero) {
                NegativeInfinity(constants)
            } else {
                Infinity(constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                NegativeInfinity(constants)
            } else if (rhs.value > rhs.constants.zero) {
                Infinity(constants)
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
            else -> NegativeInfinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> NegativeInfinity(constants)
            is Infinity -> throw IllegalArgumentException("Invalid plus between -inf and inf!!!")
            is NegativeInfinity -> NegativeInfinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid minus between -inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid minus between -inf and -inf!!!")
            else -> NegativeInfinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> NegativeInfinity(constants)
            is Infinity -> NegativeInfinity(constants)
            is NegativeInfinity -> throw IllegalArgumentException("Invalid minus between -inf and -inf!!!")
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid times between -inf and nan!!!")
            rhs.constants.negativeInfinity -> Infinity(constants)
            rhs.constants.infinity -> NegativeInfinity(constants)
            rhs.constants.zero -> Value(constants.zero, constants)
            else -> if (rhs < rhs.constants.zero) {
                Infinity(constants)
            } else {
                NegativeInfinity(constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun times(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                Infinity(constants)
            } else if (rhs.value > rhs.constants.zero) {
                NegativeInfinity(constants)
            } else {
                Value(constants.zero, constants)
            }

            is Infinity -> NegativeInfinity(constants)
            is NegativeInfinity -> Infinity(constants)
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid div between -inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid div between -inf and -inf!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid div between -inf and inf!!!")
            rhs.constants.zero -> throw IllegalArgumentException("Invalid div between -inf and 0!!!")
            else -> if (rhs < rhs.constants.zero) {
                Infinity(constants)
            } else {
                NegativeInfinity(constants)
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun div(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> if (rhs.value < rhs.constants.zero) {
                Infinity(constants)
            } else if (rhs.value > rhs.constants.zero) {
                NegativeInfinity(constants)
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

@JvmName("negValueWrapperFlt32")
operator fun ValueWrapper<Flt32>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperFlt64")
operator fun ValueWrapper<Flt64>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperFltX")
operator fun ValueWrapper<FltX>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperInt8")
operator fun ValueWrapper<Int8>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperInt16")
operator fun ValueWrapper<Int16>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperInt32")
operator fun ValueWrapper<Int32>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperInt64")
operator fun ValueWrapper<Int64>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

@JvmName("negValueWrapperIntX")
operator fun ValueWrapper<IntX>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}
