package fuookami.ospf.kotlin.utils.functional

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import fuookami.ospf.kotlin.utils.concept.*

data class EitherSerializer<L, R>(
    val leftSerializer: KSerializer<L>,
    val rightSerializer: KSerializer<R>,
) : KSerializer<Either<L, R>> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor("Either", JsonElement::class.serializer().descriptor)

    @OptIn(InternalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Either<L, R> {
        decoder as? JsonDecoder ?: throw IllegalStateException(
            "This serializer can be used only with Json format." +
                    "Expected Decoder to be JsonDecoder, got ${this::class}"
        )
        val json = Json {
            ignoreUnknownKeys = true
        }
        val element = decoder.decodeSerializableValue(JsonElement::class.serializer())
        return try {
            val leftValue = json.decodeFromJsonElement(leftSerializer, element)
            Either.Left(leftValue)
        } catch (e: Exception) {
            e.printStackTrace()
            val rightValue = json.decodeFromJsonElement(rightSerializer, element)
            Either.Right(rightValue)
        }
    }

    override fun serialize(encoder: Encoder, value: Either<L, R>) {
        when (value) {
            is Either.Left -> {
                encoder.encodeSerializableValue(leftSerializer, value.value)
            }

            is Either.Right -> {
                encoder.encodeSerializableValue(rightSerializer, value.value)
            }
        }
    }
}

sealed class Either<L, R> {
    data class Left<L, R>(val value: L) : Either<L, R>() {
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Left<*, *>) return false

            if (value != other.value) return false

            return true
        }

        override fun toString() = "$value"
    }

    data class Right<L, R>(val value: R) : Either<L, R>() {
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Right<*, *>) return false

            if (value != other.value) return false

            return true
        }

        override fun toString() = "$value"
    }

    val isLeft get() = this is Left
    val isRight get() = this is Right

    val left: L?
        get() = when (this) {
            is Left -> {
                this.value
            }

            else -> {
                null
            }
        }

    val right: R?
        get() = when (this) {
            is Right -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> ifLeft(extractor: Extractor<Ret, L>) = EitherMatcher<L, R, Ret>(this).ifLeft(extractor)
    fun <Ret> ifRight(extractor: Extractor<Ret, R>) = EitherMatcher<L, R, Ret>(this).ifRight(extractor)

    @JvmName("mapLeft")
    fun <Ret> map(extractor: Extractor<Ret, L>): Either<Ret, R> {
        return when (this) {
            is Left -> {
                Left(extractor(this.value))
            }

            is Right -> {
                Right(this.value)
            }
        }
    }

    @JvmName("mapRight")
    fun <Ret> map(extractor: Extractor<Ret, R>): Either<L, Ret> {
        return when (this) {
            is Left -> {
                Left(this.value)
            }

            is Right -> {
                Right(extractor(this.value))
            }
        }
    }

    fun <Ret1, Ret2> map(extractor1: Extractor<Ret1, L>, extractor2: Extractor<Ret2, R>): Either<Ret1, Ret2> {
        return when (this) {
            is Left -> {
                Left(extractor1(this.value))
            }

            is Right -> {
                Right(extractor2(this.value))
            }
        }
    }
}

class EitherMatcher<L, R, Ret>(
    private val value: Either<L, R>
) {
    private lateinit var leftCallBack: (L) -> Ret
    private lateinit var rightCallBack: (R) -> Ret

    fun ifLeft(callBack: (L) -> Ret): EitherMatcher<L, R, Ret> {
        leftCallBack = callBack
        return this
    }

    fun ifRight(callBack: (R) -> Ret): EitherMatcher<L, R, Ret> {
        rightCallBack = callBack
        return this
    }

    @Throws(NullPointerException::class)
    operator fun invoke(): Ret = when (value) {
        is Either.Left -> {
            leftCallBack(value.value); }

        is Either.Right -> {
            rightCallBack(value.value); }
    }
}

@Throws(NullPointerException::class)
fun <L, R, Ret> match(
    value: Either<L, R>,
    leftCallBack: (L) -> Ret,
    rightCallBack: (R) -> Ret
): Ret {
    val matcher = value.ifLeft(leftCallBack).ifRight(rightCallBack)
    return matcher()
}

fun <L : Copyable<L>, R : Copyable<R>> Either<L, R>.copy(
    value: Either<L, R>
): Either<L, R> {
    return when (value) {
        is Either.Left -> {
            Either.Left(copy(value.value))
        }

        is Either.Right -> {
            Either.Right(copy(value.value))
        }
    }
}

fun <L : Movable<L>, R : Movable<R>> Either<L, R>.move(
    value: Either<L, R>
): Either<L, R> {
    return when (value) {
        is Either.Left -> {
            Either.Left(move(value.value))
        }

        is Either.Right -> {
            Either.Right(move(value.value))
        }
    }
}
