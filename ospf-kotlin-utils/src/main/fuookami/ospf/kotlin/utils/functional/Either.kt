/**
 * Either 类型
 *
 * Either type representing a value that can be either Left or Right.
 * Similar to Result but with two generic types for both branches.
 * Either 类型，表示可以是 Left 或 Right 的值。
 * 类似于 Result 但两个分支都有泛型类型。
 */
package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.concept.Movable
import fuookami.ospf.kotlin.utils.concept.copy
import fuookami.ospf.kotlin.utils.concept.move
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * Either 序列化器
 *
 * Custom serializer for Either type supporting JSON serialization.
 * Either 类型的自定义序列化器，支持 JSON 序列化。
 *
 * @param L Left 值的类型 / The type of Left value
 * @param R Right 值的类型 / The type of Right value
 * @param leftSerializer Left 值的序列化器 / The serializer for Left value
 * @param rightSerializer Right 值的序列化器 / The serializer for Right value
 */
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

/**
 * Either 密封类
 *
 * Sealed class representing a value that can be either Left or Right.
 * Used for error handling or branching logic where both outcomes carry values.
 * 密封类，表示可以是 Left 或 Right 的值。
 * 用于错误处理或分支逻辑，两种结果都携带值。
 *
 * @param L Left 值的类型 / The type of Left value
 * @param R Right 值的类型 / The type of Right value
 */
sealed class Either<L, R> {
    /**
     * Left 子类
     *
     * Represents the Left branch of Either, typically used for error or failure cases.
     * 表示 Either 的 Left 分支，通常用于错误或失败情况。
     *
     * @param value Left 分支携带的值 / The value carried by the Left branch
     */
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

    /**
     * Right 子类
     *
     * Represents the Right branch of Either, typically used for success or valid cases.
     * 表示 Either 的 Right 分支，通常用于成功或有效情况。
     *
     * @param value Right 分支携带的值 / The value carried by the Right branch
     */
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

    /**
     * 是否为 Left
     *
     * Returns true if this is a Left value.
     * 如果是 Left 值则返回 true。
     */
    val isLeft get() = this is Left

    /**
     * 是否为 Right
     *
     * Returns true if this is a Right value.
     * 如果是 Right 值则返回 true。
     */
    val isRight get() = this is Right

    /**
     * 获取 Left 值（如果存在）
     *
     * Returns the Left value if present, otherwise null.
     * 如果存在 Left 值则返回，否则返回 null。
     */
    val left: L?
        get() = when (this) {
            is Left -> {
                this.value
            }

            else -> {
                null
            }
        }

    /**
     * 获取 Right 值（如果存在）
     *
     * Returns the Right value if present, otherwise null.
     * 如果存在 Right 值则返回，否则返回 null。
     */
    val right: R?
        get() = when (this) {
            is Right -> {
                this.value
            }

            else -> {
                null
            }
        }

    /**
     * 如果是 Left 则执行提取器
     *
     * Creates a matcher that executes the extractor if this is a Left value.
     * 创建一个匹配器，如果是 Left 值则执行提取器。
     *
     * @param Ret 返回值类型 / The return type
     * @param extractor Left 值的提取函数 / The extraction function for Left value
     * @return Either 匹配器 / An Either matcher
     */
    fun <Ret> ifLeft(extractor: Extractor<Ret, L>) = EitherMatcher<L, R, Ret>(this).ifLeft(extractor)

    /**
     * 如果是 Right 则执行提取器
     *
     * Creates a matcher that executes the extractor if this is a Right value.
     * 创建一个匹配器，如果是 Right 值则执行提取器。
     *
     * @param Ret 返回值类型 / The return type
     * @param extractor Right 值的提取函数 / The extraction function for Right value
     * @return Either 匹配器 / An Either matcher
     */
    fun <Ret> ifRight(extractor: Extractor<Ret, R>) = EitherMatcher<L, R, Ret>(this).ifRight(extractor)

    /**
     * 映射 Left 值
     *
     * Maps the Left value using the extractor, preserving Right values unchanged.
     * 使用提取器映射 Left 值，保持 Right 不变。
     *
     * @param Ret 映射后的类型 / The mapped type
     * @param extractor Left 值的映射函数 / The mapping function for Left value
     * @return 映射后的 Either / The mapped Either
     */
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

    /**
     * 映射 Right 值
     *
     * Maps the Right value using the extractor, preserving Left values unchanged.
     * 使用提取器映射 Right 值，保持 Left 不变。
     *
     * @param Ret 映射后的类型 / The mapped type
     * @param extractor Right 值的映射函数 / The mapping function for Right value
     * @return 映射后的 Either / The mapped Either
     */
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

    /**
     * 同时映射 Left 和 Right 值
     *
     * Maps both Left and Right values using their respective extractors.
     * 使用各自的提取器同时映射 Left 和 Right 值。
     *
     * @param Ret1 Left 映射后的类型 / The mapped type for Left
     * @param Ret2 Right 映射后的类型 / The mapped type for Right
     * @param extractor1 Left 值的映射函数 / The mapping function for Left value
     * @param extractor2 Right 值的映射函数 / The mapping function for Right value
     * @return 映射后的 Either / The mapped Either
     */
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

/**
 * Either 匹配器
 *
 * Matcher class for pattern matching on Either values.
 * Either 值模式匹配的匹配器类。
 *
 * @param L Left 值的类型 / The type of Left value
 * @param R Right 值的类型 / The type of Right value
 * @param Ret 返回值类型 / The return type
 * @param value 要匹配的 Either 值 / The Either value to match
 */
class EitherMatcher<L, R, Ret>(
    private val value: Either<L, R>
) {
    private lateinit var leftCallBack: (L) -> Ret
    private lateinit var rightCallBack: (R) -> Ret

    /**
     * 设置 Left 分支的回调
     *
     * Sets the callback for the Left branch.
     * 设置 Left 分支的回调函数。
     *
     * @param callBack Left 值的处理函数 / The handler function for Left value
     * @return 匹配器本身 / The matcher itself
     */
    fun ifLeft(callBack: (L) -> Ret): EitherMatcher<L, R, Ret> {
        leftCallBack = callBack
        return this
    }

    /**
     * 设置 Right 分支的回调
     *
     * Sets the callback for the Right branch.
     * 设置 Right 分支的回调函数。
     *
     * @param callBack Right 值的处理函数 / The handler function for Right value
     * @return 匹配器本身 / The matcher itself
     */
    fun ifRight(callBack: (R) -> Ret): EitherMatcher<L, R, Ret> {
        rightCallBack = callBack
        return this
    }

    /**
     * 执行匹配并返回结果
     *
     * Executes the matching and returns the result based on which branch is present.
     * 执行匹配并根据存在的分支返回结果。
     *
     * @return 匹配结果 / The matching result
     * @throws NullPointerException 如果未设置相应的回调 / If the corresponding callback is not set
     */
    @Throws(NullPointerException::class)
    operator fun invoke(): Ret = when (value) {
        is Either.Left -> {
            leftCallBack(value.value); }

        is Either.Right -> {
            rightCallBack(value.value); }
    }
}

/**
 * Either 模式匹配函数
 *
 * Pattern matching function for Either values with callbacks for both branches.
 * Either 值的模式匹配函数，为两个分支提供回调。
 *
 * @param L Left 值的类型 / The type of Left value
 * @param R Right 值的类型 / The type of Right value
 * @param Ret 返回值类型 / The return type
 * @param value 要匹配的 Either 值 / The Either value to match
 * @param leftCallBack Left 分支的回调 / The callback for Left branch
 * @param rightCallBack Right 分支的回调 / The callback for Right branch
 * @return 匹配结果 / The matching result
 * @throws NullPointerException 如果未设置相应的回调 / If the corresponding callback is not set
 */
@Throws(NullPointerException::class)
fun <L, R, Ret> match(
    value: Either<L, R>,
    leftCallBack: (L) -> Ret,
    rightCallBack: (R) -> Ret
): Ret {
    val matcher = value.ifLeft(leftCallBack).ifRight(rightCallBack)
    return matcher()
}

/**
 * 复制 Either 值
 *
 * Creates a copy of an Either value with Copyable contents.
 * 创建包含 Copyable 内容的 Either 值的副本。
 *
 * @param L Left 值的类型，必须实现 Copyable / The type of Left value, must implement Copyable
 * @param R Right 值的类型，必须实现 Copyable / The type of Right value, must implement Copyable
 * @param value 要复制的 Either 值 / The Either value to copy
 * @return 复制后的 Either / The copied Either
 */
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

/**
 * 移动 Either 值
 *
 * Creates a moved version of an Either value with Movable contents.
 * 创建包含 Movable 内容的 Either 值的移动版本。
 *
 * @param L Left 值的类型，必须实现 Movable / The type of Left value, must implement Movable
 * @param R Right 值的类型，必须实现 Movable / The type of Right value, must implement Movable
 * @param value 要移动的 Either 值 / The Either value to move
 * @return 移动后的 Either / The moved Either
 */
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
