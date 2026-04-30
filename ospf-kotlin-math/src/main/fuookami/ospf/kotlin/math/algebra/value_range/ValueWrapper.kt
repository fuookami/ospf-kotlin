/**
 * 值包装器
 * Value Wrapper
 *
 * 定义值包装器类，用于处理值范围边界中的普通值、正无穷和负无穷，支持序列化、算术运算和比较操作。
 * Defines value wrapper class for handling normal values, positive infinity, and negative infinity in value range boundaries, with support for serialization, arithmetic operations, and comparison operations.
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
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.math.operator.Times
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.utils.functional.orderOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

/**
 * 正无穷标记对象
 * Positive Infinity Marker Object
 *
 * 用于在全局范围内标识正无穷值。
 * Used to identify positive infinity values globally.
 */
data object Infinity

/**
 * 负无穷标记对象
 * Negative Infinity Marker Object
 *
 * 用于在全局范围内标识负无穷值。
 * Used to identify negative infinity values globally.
 */
data object NegativeInfinity

/**
 * 全局正无穷类型别名
 * Global Positive Infinity Type Alias
 *
 * 内部使用的全局正无穷类型别名。
 * Internal global positive infinity type alias.
 */
internal typealias GlobalInfinity = Infinity

/**
 * 全局负无穷类型别名
 * Global Negative Infinity Type Alias
 *
 * 内部使用的全局负无穷类型别名。
 * Internal global negative infinity type alias.
 */
internal typealias GlobalNegativeInfinity = NegativeInfinity

/**
 * 值包装器序列化器
 * Value Wrapper Serializer
 *
 * 用于将 ValueWrapper 序列化和反序列化为 JSON 格式，支持普通值、正无穷和负无穷的表示。
 * Used to serialize and deserialize ValueWrapper to/from JSON format, supporting representation of normal values, positive infinity, and negative infinity.
 *
 * @param T 数值类型，必须是实数和数域
 * @property valueSerializer 基础值的序列化器
 * @property constants 数值常量对象
 */
class ValueWrapperSerializer<T>(
    private val valueSerializer: KSerializer<T>,
    internal val constants: RealNumberConstants<T>
) : KSerializer<ValueWrapper<T>> where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        /**
         * 创建值包装器序列化器的便捷方法
         * Convenience method to create value wrapper serializer
         *
         * @param constants 数值常量对象
         * @return 新的 ValueWrapperSerializer 实例
         */
        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T> invoke(
            constants: RealNumberConstants<T>
        ): ValueWrapperSerializer<T> where T : RealNumber<T>, T : NumberField<T> {
            return ValueWrapperSerializer(T::class.serializer(), constants)
        }

        /**
         * 创建值包装器序列化器的便捷方法（自动解析常量）
         * Convenience method to create value wrapper serializer (auto-resolves constants)
         *
         * @return 新的 ValueWrapperSerializer 实例
         */
        @Suppress("UNCHECKED_CAST")
        @OptIn(InternalSerializationApi::class)
        inline operator fun <reified T> invoke(): ValueWrapperSerializer<T> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(resolveRealNumberConstants<T>("ValueWrapper"))
        }
    }

    /**
     * 序列化描述符
     * Serialization descriptor
     *
     * 定义了三种可能的序列化形式：Value、Infinity 和 NegativeInfinity。
     * Defines three possible serialization forms: Value, Infinity, and NegativeInfinity.
     */
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("ValueWrapper<T>", PolymorphicKind.SEALED) {
        element("Value", valueSerializer.descriptor)
        element("Infinity", PrimitiveSerialDescriptor("Infinity", PrimitiveKind.DOUBLE))
        element("NegativeInfinity", PrimitiveSerialDescriptor("NegativeInfinity", PrimitiveKind.DOUBLE))
    }

    /**
     * 序列化值包装器
     * Serializes value wrapper
     *
     * 将值包装器转换为 JSON 元素：
     * - 普通值：序列化为对应数值
     * - 正无穷：序列化为 Double.POSITIVE_INFINITY
     * - 负无穷：序列化为 Double.NEGATIVE_INFINITY
     *
     * Converts value wrapper to JSON element:
     * - Normal value: serialized to corresponding number
     * - Positive infinity: serialized to Double.POSITIVE_INFINITY
     * - Negative infinity: serialized to Double.NEGATIVE_INFINITY
     *
     * @param encoder JSON 编码器
     * @param value 要序列化的值包装器
     */
    override fun serialize(encoder: Encoder, value: ValueWrapper<T>) {
        require(encoder is JsonEncoder)
        val element = when (value) {
            is ValueWrapper.Value -> encoder.json.encodeToJsonElement(valueSerializer, value.value)
            is ValueWrapper.Infinity -> encoder.json.encodeToJsonElement(Double.POSITIVE_INFINITY)
            is ValueWrapper.NegativeInfinity -> encoder.json.encodeToJsonElement(Double.NEGATIVE_INFINITY)
        }
        encoder.encodeJsonElement(element)
    }

    /**
     * 反序列化值包装器
     * Deserializes value wrapper
     *
     * 从 JSON 元素解析值包装器：
     * - Double.POSITIVE_INFINITY：解析为正无穷
     * - Double.NEGATIVE_INFINITY：解析为负无穷
     * - 其他数值：解析为普通值
     *
     * Parses value wrapper from JSON element:
     * - Double.POSITIVE_INFINITY: parsed as positive infinity
     * - Double.NEGATIVE_INFINITY: parsed as negative infinity
     * - Other numbers: parsed as normal values
     *
     * @param decoder JSON 解码器
     * @return 解析后的值包装器
     */
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

/**
 * 值包装器
 * Value Wrapper
 *
 * 密封类，用于包装值范围边界中的值，支持三种情况：
 * - Value：普通数值
 * - Infinity：正无穷
 * - NegativeInfinity：负无穷
 *
 * Sealed class for wrapping values in value range boundaries, supporting three cases:
 * - Value: normal number
 * - Infinity: positive infinity
 * - NegativeInfinity: negative infinity
 *
 * @param T 数值类型，必须是实数和数域
 * @property constants 数值常量对象
 */
sealed class ValueWrapper<T>(
    val constants: RealNumberConstants<T>
) : Cloneable, Copyable<ValueWrapper<T>>, Ord<ValueWrapper<T>>, Eq<ValueWrapper<T>>,
    Plus<ValueWrapper<T>, ValueWrapper<T>>, Minus<ValueWrapper<T>, ValueWrapper<T>>,
    Times<ValueWrapper<T>, ValueWrapper<T>>, Div<ValueWrapper<T>, ValueWrapper<T>>
        where T : RealNumber<T>, T : NumberField<T> {
    companion object {
        /**
         * 从数值创建值包装器（自动解析常量）
         * Creates value wrapper from number (auto-resolves constants)
         *
         * @param value 要包装的数值
         * @return 创建结果（成功返回值包装器，失败返回错误）
         */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            value: T
        ): Ret<ValueWrapper<T>> where T : RealNumber<T>, T : NumberField<T> {
            return invoke(value, resolveRealNumberConstants<T>("ValueWrapper"))
        }

        /**
         * 从数值创建值包装器
         * Creates value wrapper from number
         *
         * 根据输入值自动判断类型：
         * - 正无穷：返回 Infinity
         * - 负无穷：返回 NegativeInfinity
         * - NaN：返回错误
         * - 其他：返回 Value
         *
         * Automatically determines type based on input value:
         * - Positive infinity: returns Infinity
         * - Negative infinity: returns NegativeInfinity
         * - NaN: returns error
         * - Others: returns Value
         *
         * @param value 要包装的数值
         * @param constants 数值常量对象
         * @return 创建结果（成功返回值包装器，失败返回错误）
         */
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

        /**
         * 创建正无穷值包装器（自动解析常量）
         * Creates positive infinity value wrapper (auto-resolves constants)
         *
         * @return 正无穷值包装器
         */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(
            _inf: GlobalInfinity
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return Infinity(resolveRealNumberConstants<T>("ValueWrapper"))
        }

        /**
         * 创建正无穷值包装器
         * Creates positive infinity value wrapper
         *
         * @param _inf 正无穷标记
         * @param constants 数值常量对象
         * @return 正无穷值包装器
         */
        operator fun <T> invoke(
            _inf: GlobalInfinity,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return Infinity(constants)
        }

        /**
         * 创建负无穷值包装器（自动解析常量）
         * Creates negative infinity value wrapper (auto-resolves constants)
         *
         * @return 负无穷值包装器
         */
        @Suppress("UNCHECKED_AS")
        inline operator fun <reified T> invoke(
            _negInf: GlobalNegativeInfinity
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return NegativeInfinity(resolveRealNumberConstants<T>("ValueWrapper"))
        }

        /**
         * 创建负无穷值包装器
         * Creates negative infinity value wrapper
         *
         * @param _negInf 负无穷标记
         * @param constants 数值常量对象
         * @return 负无穷值包装器
         */
        operator fun <T> invoke(
            _negInf: GlobalNegativeInfinity,
            constants: RealNumberConstants<T>
        ): ValueWrapper<T> where T : RealNumber<T>, T : NumberField<T> {
            return NegativeInfinity(constants)
        }
    }

    /**
     * 是否为正无穷
     * Whether is positive infinity
     */
    val isInfinity get() = this is Infinity

    /**
     * 是否为负无穷
     * Whether is negative infinity
     */
    val isNegativeInfinity get() = this is NegativeInfinity

    /**
     * 是否为无穷（正无穷或负无穷）
     * Whether is infinity (positive or negative infinity)
     */
    val isInfinityOrNegativeInfinity by lazy { isInfinity || isNegativeInfinity }

    /**
     * 与数值相加
     * Adds with a number
     *
     * @param rhs 要添加的数值
     * @return 新的值包装器
     */
    abstract operator fun plus(rhs: T): ValueWrapper<T>

    /**
     * 与数值相减
     * Subtracts with a number
     *
     * @param rhs 要减去的数值
     * @return 新的值包装器
     */
    abstract operator fun minus(rhs: T): ValueWrapper<T>

    /**
     * 与数值相乘
     * Multiplies with a number
     *
     * @param rhs 要乘的数值
     * @return 新的值包装器
     */
    abstract operator fun times(rhs: T): ValueWrapper<T>

    /**
     * 与数值相除
     * Divides with a number
     *
     * @param rhs 要除的数值
     * @return 新的值包装器
     */
    abstract operator fun div(rhs: T): ValueWrapper<T>

    /**
     * 转换为 Flt64 类型
     * Converts to Flt64 type
     *
     * @return Flt64 类型的数值
     */
    abstract fun toFlt64(): Flt64

    /**
     * 解包获取实际数值
     * Unwraps to get actual number
     *
     * 如果是无穷值，返回对应的数值常量（可能为 null）。
     * If it's an infinity value, returns corresponding number constant (may be null).
     *
     * @return 实际数值
     */
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

    /**
     * 解包获取实际数值（可空）
     * Unwraps to get actual number (nullable)
     *
     * 如果是无穷值且常量为 null，返回 null。
     * If it's an infinity value and constant is null, returns null.
     *
     * @return 实际数值，或 null
     */
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

    /**
     * 判断是否等于指定数值
     * Determines if equals specified number
     *
     * @param rhs 要比较的数值
     * @return 是否相等
     */
    infix fun eq(rhs: T): Boolean {
        return when (rhs) {
            constants.infinity -> this is Infinity
            constants.negativeInfinity -> this is NegativeInfinity
            constants.nan -> throw IllegalArgumentException("Illegal argument NaN for value range!!!")
            else -> (this as? Value<T>)?.value?.eq(rhs) == true
        }
    }

    /**
     * 普通值包装器
     * Normal Value Wrapper
     *
     * 包装一个普通的数值，不能是无穷或 NaN。
     * Wraps a normal number, cannot be infinity or NaN.
     *
     * @param T 数值类型
     * @property value 包装的数值
     */
    class Value<T>(val value: T, constants: RealNumberConstants<T>) :
        ValueWrapper<T>(constants) where T : RealNumber<T>, T : NumberField<T> {
        init {
            assert(value != constants.infinity)
            assert(value != constants.negativeInfinity)
            assert(value != constants.nan)
        }

        /**
         * 复制值包装器
         * Copies value wrapper
         *
         * @return 新的值包装器副本
         */
        override fun copy() = Value(value.copy(), constants)

        /**
         * 克隆值包装器
         * Clones value wrapper
         *
         * @return 克隆的值包装器
         */
        public override fun clone() = copy()

        /**
         * 部分相等比较
         * Partial equality comparison
         *
         * @param rhs 另一个值包装器
         * @return 是否相等
         */
        override fun partialEq(rhs: ValueWrapper<T>): Boolean = when (rhs) {
            is Value -> value.eq(rhs.value)
            else -> false
        }

        /**
         * 部分序比较
         * Partial order comparison
         *
         * 普通值小于正无穷，大于负无穷。
         * Normal values are less than positive infinity, greater than negative infinity.
         *
         * @param rhs 另一个值包装器
         * @return 比较结果
         */
        override fun partialOrd(rhs: ValueWrapper<T>) = when (rhs) {
            is Value -> value.ord(rhs.value)
            is Infinity -> orderOf(-1)
            is NegativeInfinity -> orderOf(1)
        }

        /**
         * 与数值相加
         * Adds with a number
         *
         * @param rhs 要添加的数值
         * @return 新的值包装器
         */
        override fun plus(rhs: T): ValueWrapper<T> = ValueWrapper(value + rhs, constants).value!!

        /**
         * 与值包装器相加
         * Adds with a value wrapper
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         */
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Value(value + rhs.value, constants)
            is Infinity -> Infinity(constants)
            is NegativeInfinity -> NegativeInfinity(constants)
        }

        /**
         * 与数值相减
         * Subtracts with a number
         *
         * @param rhs 要减去的数值
         * @return 新的值包装器
         */
        override fun minus(rhs: T): ValueWrapper<T> = ValueWrapper(value - rhs, constants).value!!

        /**
         * 与值包装器相减
         * Subtracts with a value wrapper
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         */
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Value(value - rhs.value, constants)
            is Infinity -> NegativeInfinity(constants)
            is NegativeInfinity -> Infinity(constants)
        }

        /**
         * 与数值相乘
         * Multiplies with a number
         *
         * @param rhs 要乘的数值
         * @return 新的值包装器
         */
        override fun times(rhs: T): ValueWrapper<T> = ValueWrapper(value * rhs, constants).value!!

        /**
         * 与值包装器相乘
         * Multiplies with a value wrapper
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         */
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

        /**
         * 与数值相除
         * Divides with a number
         *
         * @param rhs 要除的数值
         * @return 新的值包装器
         */
        override fun div(rhs: T): ValueWrapper<T> = ValueWrapper(value / rhs, constants).value!!

        /**
         * 与值包装器相除
         * Divides with a value wrapper
         *
         * 除以无穷大时，结果趋近于零（使用 epsilon 表示）。
         * When dividing by infinity, result approaches zero (represented using epsilon).
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         */
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

        /**
         * 获取字符串表示
         * Gets string representation
         *
         * @return 数值的字符串形式
         */
        override fun toString() = "$value"

        /**
         * 转换为 Flt64 类型
         * Converts to Flt64 type
         *
         * @return Flt64 类型的数值
         */
        override fun toFlt64() = value.toFlt64()
    }

    /**
     * 正无穷值包装器
     * Positive Infinity Value Wrapper
     *
     * 表示正无穷大，大于任何普通值。
     * Represents positive infinity, greater than any normal value.
     *
     * @param T 数值类型
     */
    class Infinity<T>(constants: RealNumberConstants<T>) :
        ValueWrapper<T>(constants) where T : RealNumber<T>, T : NumberField<T> {
        /**
         * 复制值包装器
         * Copies value wrapper
         *
         * @return 新的正无穷值包装器副本
         */
        override fun copy() = Infinity(constants)

        /**
         * 克隆值包装器
         * Clones value wrapper
         *
         * @return 克隆的正无穷值包装器
         */
        public override fun clone() = copy()

        /**
         * 部分相等比较
         * Partial equality comparison
         *
         * 只有另一个正无穷才相等。
         * Only equals another positive infinity.
         *
         * @param rhs 另一个值包装器
         * @return 是否相等
         */
        override fun partialEq(rhs: ValueWrapper<T>): Boolean = rhs is Infinity

        /**
         * 部分序比较
         * Partial order comparison
         *
         * 正无穷大于任何其他值。
         * Positive infinity is greater than any other value.
         *
         * @param rhs 另一个值包装器
         * @return 比较结果
         */
        override fun partialOrd(rhs: ValueWrapper<T>) = when (rhs) {
            is Infinity -> orderOf(0)
            else -> orderOf(1)
        }

        /**
         * 与数值相加
         * Adds with a number
         *
         * 正无穷加上 NaN 或负无穷会抛出异常。
         * Adding NaN or negative infinity to positive infinity throws exception.
         *
         * @param rhs 要添加的数值
         * @return 正无穷值包装器
         * @throws IllegalArgumentException 当加上 NaN 或负无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid plus between inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid plus between inf and -inf!!!")
            else -> Infinity(constants)
        }

        /**
         * 与值包装器相加
         * Adds with a value wrapper
         *
         * 正无穷加上负无穷会抛出异常。
         * Adding negative infinity to positive infinity throws exception.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当加上负无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Infinity(constants)
            is Infinity -> Infinity(constants)
            is NegativeInfinity -> throw IllegalArgumentException("Invalid plus between inf and -inf!!!")
        }

        /**
         * 与数值相减
         * Subtracts with a number
         *
         * 正无穷减去 NaN 或正无穷会抛出异常。
         * Subtracting NaN or positive infinity from positive infinity throws exception.
         *
         * @param rhs 要减去的数值
         * @return 正无穷值包装器
         * @throws IllegalArgumentException 当减去 NaN 或正无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid minus between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid minus between inf and inf!!!")
            else -> Infinity(constants)
        }

        /**
         * 与值包装器相减
         * Subtracts with a value wrapper
         *
         * 正无穷减去正无穷会抛出异常。
         * Subtracting positive infinity from positive infinity throws exception.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当减去正无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> Infinity(constants)
            is Infinity -> throw IllegalArgumentException("Invalid minus between inf and inf!!!")
            is NegativeInfinity -> Infinity(constants)
        }

        /**
         * 与数值相乘
         * Multiplies with a number
         *
         * 正无穷乘以零返回零，乘以负数返回负无穷。
         * Positive infinity multiplied by zero returns zero, multiplied by negative number returns negative infinity.
         *
         * @param rhs 要乘的数值
         * @return 新的值包装器
         * @throws IllegalArgumentException 当乘以 NaN 时
         */
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

        /**
         * 与值包装器相乘
         * Multiplies with a value wrapper
         *
         * 正无穷乘以零返回零，乘以负值返回负无穷。
         * Positive infinity multiplied by zero returns zero, multiplied by negative value returns negative infinity.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当乘以 NaN 时
         */
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

        /**
         * 与数值相除
         * Divides with a number
         *
         * 正无穷除以无穷或零会抛出异常，除以负数返回负无穷。
         * Dividing positive infinity by infinity or zero throws exception, by negative number returns negative infinity.
         *
         * @param rhs 要除的数值
         * @return 新的值包装器
         * @throws IllegalArgumentException 当除以 NaN、无穷或零时
         */
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

        /**
         * 与值包装器相除
         * Divides with a value wrapper
         *
         * 正无穷除以无穷会抛出异常，除以零也会抛出异常。
         * Dividing positive infinity by infinity throws exception, dividing by zero also throws exception.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当除以无穷或零时
         */
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

        /**
         * 获取字符串表示
         * Gets string representation
         *
         * @return "inf"
         */
        override fun toString() = "inf"

        /**
         * 转换为 Flt64 类型
         * Converts to Flt64 type
         *
         * @return Flt64 类型的正无穷
         */
        override fun toFlt64() = Flt64.infinity
    }

    /**
     * 负无穷值包装器
     * Negative Infinity Value Wrapper
     *
     * 表示负无穷大，小于任何普通值。
     * Represents negative infinity, less than any normal value.
     *
     * @param T 数值类型
     */
    class NegativeInfinity<T>(constants: RealNumberConstants<T>) :
        ValueWrapper<T>(constants) where T : RealNumber<T>, T : NumberField<T> {
        /**
         * 复制值包装器
         * Copies value wrapper
         *
         * @return 新的负无穷值包装器副本
         */
        override fun copy() = NegativeInfinity(constants)

        /**
         * 克隆值包装器
         * Clones value wrapper
         *
         * @return 克隆的负无穷值包装器
         */
        public override fun clone() = copy()

        /**
         * 部分相等比较
         * Partial equality comparison
         *
         * 只有另一个负无穷才相等。
         * Only equals another negative infinity.
         *
         * @param rhs 另一个值包装器
         * @return 是否相等
         */
        override fun partialEq(rhs: ValueWrapper<T>): Boolean = rhs is NegativeInfinity

        /**
         * 部分序比较
         * Partial order comparison
         *
         * 负无穷小于任何其他值。
         * Negative infinity is less than any other value.
         *
         * @param rhs 另一个值包装器
         * @return 比较结果
         */
        override fun partialOrd(rhs: ValueWrapper<T>) = when (rhs) {
            is NegativeInfinity -> orderOf(0)
            else -> orderOf(-1)
        }

        /**
         * 与数值相加
         * Adds with a number
         *
         * 负无穷加上 NaN 或正无穷会抛出异常。
         * Adding NaN or positive infinity to negative infinity throws exception.
         *
         * @param rhs 要添加的数值
         * @return 负无穷值包装器
         * @throws IllegalArgumentException 当加上 NaN 或正无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid plus between inf and nan!!!")
            rhs.constants.infinity -> throw IllegalArgumentException("Invalid plus between -inf and inf!!!")
            else -> NegativeInfinity(constants)
        }

        /**
         * 与值包装器相加
         * Adds with a value wrapper
         *
         * 负无穷加上正无穷会抛出异常。
         * Adding positive infinity to negative infinity throws exception.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当加上正无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun plus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> NegativeInfinity(constants)
            is Infinity -> throw IllegalArgumentException("Invalid plus between -inf and inf!!!")
            is NegativeInfinity -> NegativeInfinity(constants)
        }

        /**
         * 与数值相减
         * Subtracts with a number
         *
         * 负无穷减去 NaN 或负无穷会抛出异常。
         * Subtracting NaN or negative infinity from negative infinity throws exception.
         *
         * @param rhs 要减去的数值
         * @return 负无穷值包装器
         * @throws IllegalArgumentException 当减去 NaN 或负无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: T): ValueWrapper<T> = when (rhs) {
            rhs.constants.nan -> throw IllegalArgumentException("Invalid minus between -inf and nan!!!")
            rhs.constants.negativeInfinity -> throw IllegalArgumentException("Invalid minus between -inf and -inf!!!")
            else -> NegativeInfinity(constants)
        }

        /**
         * 与值包装器相减
         * Subtracts with a value wrapper
         *
         * 负无穷减去负无穷会抛出异常。
         * Subtracting negative infinity from negative infinity throws exception.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当减去负无穷时
         */
        @Throws(IllegalArgumentException::class)
        override fun minus(rhs: ValueWrapper<T>): ValueWrapper<T> = when (rhs) {
            is Value -> NegativeInfinity(constants)
            is Infinity -> NegativeInfinity(constants)
            is NegativeInfinity -> throw IllegalArgumentException("Invalid minus between -inf and -inf!!!")
        }

        /**
         * 与数值相乘
         * Multiplies with a number
         *
         * 负无穷乘以零返回零，乘以负数返回正无穷。
         * Negative infinity multiplied by zero returns zero, multiplied by negative number returns positive infinity.
         *
         * @param rhs 要乘的数值
         * @return 新的值包装器
         * @throws IllegalArgumentException 当乘以 NaN 时
         */
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

        /**
         * 与值包装器相乘
         * Multiplies with a value wrapper
         *
         * 负无穷乘以零返回零，乘以负值返回正无穷。
         * Negative infinity multiplied by zero returns zero, multiplied by negative value returns positive infinity.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当乘以 NaN 时
         */
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

        /**
         * 与数值相除
         * Divides with a number
         *
         * 负无穷除以无穷或零会抛出异常，除以负数返回正无穷。
         * Dividing negative infinity by infinity or zero throws exception, by negative number returns positive infinity.
         *
         * @param rhs 要除的数值
         * @return 新的值包装器
         * @throws IllegalArgumentException 当除以 NaN、无穷或零时
         */
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

        /**
         * 与值包装器相除
         * Divides with a value wrapper
         *
         * 负无穷除以无穷会抛出异常，除以零也会抛出异常。
         * Dividing negative infinity by infinity throws exception, dividing by zero also throws exception.
         *
         * @param rhs 另一个值包装器
         * @return 新的值包装器
         * @throws IllegalArgumentException 当除以无穷或零时
         */
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

        /**
         * 获取字符串表示
         * Gets string representation
         *
         * @return "-inf"
         */
        override fun toString() = "-inf"

        /**
         * 转换为 Flt64 类型
         * Converts to Flt64 type
         *
         * @return Flt64 类型的负无穷
         */
        override fun toFlt64() = Flt64.negativeInfinity
    }
}

/**
 * Flt32 类型值包装器的取负操作
 * Negation operation for Flt32 typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperFlt32")
operator fun ValueWrapper<Flt32>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * Flt64 类型值包装器的取负操作
 * Negation operation for Flt64 typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperFlt64")
operator fun ValueWrapper<F64>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * FltX 类型值包装器的取负操作
 * Negation operation for FltX typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperFltX")
operator fun ValueWrapper<FltX>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * Int8 类型值包装器的取负操作
 * Negation operation for Int8 typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperInt8")
operator fun ValueWrapper<Int8>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * Int16 类型值包装器的取负操作
 * Negation operation for Int16 typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperInt16")
operator fun ValueWrapper<Int16>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * Int32 类型值包装器的取负操作
 * Negation operation for Int32 typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperInt32")
operator fun ValueWrapper<Int32>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * Int64 类型值包装器的取负操作
 * Negation operation for Int64 typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperInt64")
operator fun ValueWrapper<Int64>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}

/**
 * IntX 类型值包装器的取负操作
 * Negation operation for IntX typed value wrapper
 *
 * @return 取负后的新值包装器
 */
@JvmName("negValueWrapperIntX")
operator fun ValueWrapper<IntX>.unaryMinus() = when (this) {
    is ValueWrapper.Value -> ValueWrapper.Value(-value, constants)
    is ValueWrapper.Infinity -> ValueWrapper.NegativeInfinity(constants)
    is ValueWrapper.NegativeInfinity -> ValueWrapper.Infinity(constants)
}