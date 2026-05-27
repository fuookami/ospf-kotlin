/**
 * 区间类型
 * Interval Type
 *
 * 定义区间的开闭性质，包括开区间（Open）和闭区间（Closed），用于表示值范围的边界是否包含边界值本身。
 * Defines the openness/closedness of intervals, including Open and Closed types, used to represent whether the boundaries of a value range include the boundary values themselves.
 */
package fuookami.ospf.kotlin.math.algebra.value_range

import java.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.PartialOrd

/**
 * 区间序列化器
 * Interval Serializer
 *
 * 用于尌Interval 枚举值序列化和反序列化为字符串格式。
 * Used to serialize and deserialize Interval enum values to/from string format.
 */
private data object IntervalSerializer : KSerializer<Interval> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntervalType", PrimitiveKind.STRING)

    /**
     * 序列匌Interval 枚举值为小写字符丌
     * Serializes Interval enum value to lowercase string
     *
     * @param encoder 编码噌
     * @param value 要序列化的区间倌
     */
    override fun serialize(encoder: Encoder, value: Interval) {
        encoder.encodeString(value.toString().lowercase(Locale.getDefault()))
    }

    /**
     * 反序列化字符串为 Interval 枚举倌
     * Deserializes string to Interval enum value
     *
     * @param decoder 解码噌
     * @return 解析后的 Interval 枚举倌
     */
    override fun deserialize(decoder: Decoder): Interval {
        return Interval.valueOf(
            decoder.decodeString()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }
}

/**
 * 区间类型枚举
 * Interval Type Enum
 *
 * 表示区间的开闭性质，
 * - Open：开区间，边界不包含边界值（妌(a, b) 表示 a < x < b，
 * - Closed：闭区间，边界包含边界值（妌[a, b] 表示 a <= x <= b，
 *
 * Represents the openness/closedness of an interval:
 * - Open: Open interval, boundary does not include boundary value (e.g., (a, b) means a < x < b)
 * - Closed: Closed interval, boundary includes boundary value (e.g., [a, b] means a <= x <= b)
 *
 * @property lowerSign 下边界符号表示（开区间丌"("，闭区间丌"["，
 * @property upperSign 上边界符号表示（开区间丌")"，闭区间丌"]"，
 */
@Serializable(with = IntervalSerializer::class)
enum class Interval {
    /**
     * 开区间
     * Open Interval
     *
     * 边界不包含边界值，例如 (a, b) 表示所有满趌a < x < b 的值。
     * Boundary does not include boundary value, e.g., (a, b) represents all values satisfying a < x < b.
     */
    Open {
        override val lowerSign = "("
        override val upperSign = ")"

        /**
         * 开区间与另一区间的并集运箌
         * Union operation with another interval
         *
         * @param rhs 另一个区闌
         * @return 并集结果的区间类型（开区间优先，
         */
        override fun union(rhs: Interval) = rhs

        /**
         * 开区间与另一区间的交集运箌
         * Intersection operation with another interval
         *
         * @param rhs 另一个区闌
         * @return 交集结果的区间类型（总是返回开区间，
         */
        override fun intersect(rhs: Interval) = Open

        /**
         * 判断当前区间是否在另一区间外部（更宽松，
         * Determines if current interval is outer than another interval (more relaxed)
         *
         * @param rhs 另一个区闌
         * @return 当前区间是否在另一区间外部
         */
        override fun outer(rhs: Interval) = false

        /**
         * 获取下边界的比较操作函数
         * Gets the comparison operation function for lower bound
         *
         * 开区间下边界使用严格小于比较（<）。
         * Open interval lower bound uses strict less-than comparison (<).
         *
         * @return 用于判断值是否在下边界内的比较函敌
         */
        override fun <T : PartialOrd<T>> lowerBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs -> lhs partialOrd rhs is Order.Less }
        }

        /**
         * 获取上边界的比较操作函数
         * Gets the comparison operation function for upper bound
         *
         * 开区间上边界使用严格大于比较（>）。
         * Open interval upper bound uses strict greater-than comparison (>).
         *
         * @return 用于判断值是否在上边界内的比较函敌
         */
        override fun <T : PartialOrd<T>> upperBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs -> lhs partialOrd rhs is Order.Greater }
        }
    },

    /**
     * 闭区闌
     * Closed Interval
     *
     * 边界包含边界值，例如 [a, b] 表示所有满趌a <= x <= b 的值。
     * Boundary includes boundary value, e.g., [a, b] represents all values satisfying a <= x <= b.
     */
    Closed {
        override val lowerSign = "["
        override val upperSign = "]"

        /**
         * 闭区间与另一区间的并集运箌
         * Union operation with another interval
         *
         * @param rhs 另一个区闌
         * @return 并集结果的区间类型（闭区间优先）
         */
        override fun union(rhs: Interval) = Closed

        /**
         * 闭区间与另一区间的交集运箌
         * Intersection operation with another interval
         *
         * @param rhs 另一个区闌
         * @return 交集结果的区间类型（返回另一区间的类型）
         */
        override fun intersect(rhs: Interval) = rhs

        /**
         * 判断当前区间是否在另一区间外部（更宽松，
         * Determines if current interval is outer than another interval (more relaxed)
         *
         * @param rhs 另一个区闌
         * @return 当前区间是否在另一区间外部（闭区间比开区间更宽松）
         */
        override fun outer(rhs: Interval) = rhs == Open

        /**
         * 获取下边界的比较操作函数
         * Gets the comparison operation function for lower bound
         *
         * 闭区间下边界使用小于或等于比较（<=）。
         * Closed interval lower bound uses less-than-or-equal comparison (<=).
         *
         * @return 用于判断值是否在下边界内的比较函敌
         */
        override fun <T : PartialOrd<T>> lowerBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs ->
                when (lhs partialOrd rhs) {
                    is Order.Less, Order.Equal -> true

                    else -> false
                }
            }
        }

        /**
         * 获取上边界的比较操作函数
         * Gets the comparison operation function for upper bound
         *
         * 闭区间上边界使用大于或等于比较（>=）。
         * Closed interval upper bound uses greater-than-or-equal comparison (>=).
         *
         * @return 用于判断值是否在上边界内的比较函敌
         */
        override fun <T : PartialOrd<T>> upperBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs ->
                when (lhs partialOrd rhs) {
                    is Order.Greater, Order.Equal -> true

                    else -> false
                }
            }
        }
    };

    /**
     * 下边界符号表礌
     * Lower bound symbol representation
     */
    abstract val lowerSign: String

    /**
     * 上边界符号表礌
     * Upper bound symbol representation
     */
    abstract val upperSign: String

    /**
     * 计算与另一区间的并集类垌
     * Computes union type with another interval
     *
     * @param rhs 另一个区闌
     * @return 并集结果的区间类垌
     */
    abstract infix fun union(rhs: Interval): Interval

    /**
     * 计算与另一区间的交集类垌
     * Computes intersection type with another interval
     *
     * @param rhs 另一个区闌
     * @return 交集结果的区间类垌
     */
    abstract infix fun intersect(rhs: Interval): Interval

    /**
     * 判断当前区间是否在另一区间外部（边界更宽松，
     * Determines if current interval is outer than another interval (boundary is more relaxed)
     *
     * 当两个边界的值相等时，用于判断哪个区间在外部（包含范围更广）。
     * Used to determine which interval is outer (has broader coverage) when two boundary values are equal.
     *
     * @param rhs 另一个区闌
     * @return 当前区间是否在另一区间外部
     */
    abstract infix fun outer(rhs: Interval): Boolean

    /**
     * 获取下边界的比较操作函数
     * Gets the comparison operation function for lower bound
     *
     * 根据区间类型返回相应的比较操作：
     * - 开区间：严格小于（<，
     * - 闭区间：小于或等于（<=，
     *
     * Returns corresponding comparison operation based on interval type:
     * - Open: strict less-than (<)
     * - Closed: less-than-or-equal (<=)
     *
     * @return 用于判断值是否满足下边界条件的比较函敌
     */
    abstract fun <T : PartialOrd<T>> lowerBoundOperator(): (T, T) -> Boolean

    /**
     * 获取上边界的比较操作函数
     * Gets the comparison operation function for upper bound
     *
     * 根据区间类型返回相应的比较操作：
     * - 开区间：严格大于（>，
     * - 闭区间：大于或等于（>=，
     *
     * Returns corresponding comparison operation based on interval type:
     * - Open: strict greater-than (>)
     * - Closed: greater-than-or-equal (>=)
     *
     * @return 用于判断值是否满足上边界条件的比较函敌
     */
    abstract fun <T : PartialOrd<T>> upperBoundOperator(): (T, T) -> Boolean
}
