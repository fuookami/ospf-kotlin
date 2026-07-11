/**
 * 排序接口
 *
 * Interfaces for defining ordering and comparison operations.
 * Similar to Haskell's Ord typeclass for type-safe ordering.
 * 定义排序和比较操作的接口。
 * 类似于 Haskell 的 Ord 类型类，用于类型安全的排序。
 *
 * Key types:
 * - [Order]: Represents comparison result (Less, Equal, Greater)
 * - [PartialOrd]: Partial ordering with nullable result
 * - [Ord]: Total ordering with definite result
 *
 * 主要类型：
 * - [Order]: 表示比较结果（Less、Equal、Greater）
 * - [PartialOrd]: 部分排序，结果可空
 * - [Ord]: 完全排序，结果确定
*/
package fuookami.ospf.kotlin.utils.functional

/**
 * 排序结果密封接口
 *
 * Sealed interface representing the result of a three-way comparison.
 * 表示三路比较结果的密封接口。
 *
 * Can be Less, Equal, or Greater with an associated integer value.
 * 可以是 Less、Equal 或 Greater，并关联一个整数值。
*/
sealed interface Order {

    /**
     * 比较结果的整数值
     *
     * Integer value representing the comparison result.
     * Negative for Less, zero for Equal, positive for Greater.
     * 表示比较结果的整数值。负数表示 Less，零表示 Equal，正数表示 Greater。
    */
    val value: Int

    /**
     * 取反操作
     *
     * Negates the order (Less becomes Greater and vice versa).
     * 取反排序结果（Less 变为 Greater，反之亦然）。
    */
    operator fun unaryMinus(): Order

    /**
     * 如果相等则执行函数
     *
     * Executes the given function if this is Equal, returning its result.
     * Used for chained comparisons.
     * 如果是 Equal 则执行给定函数并返回其结果。用于链式比较。
     *
     * @param f 只有在 Equal 时才执行的函数 / The function to execute only if Equal
     * @return 当前 Order 或函数的结果 / The current Order or the function result
    */
    fun ifEqual(f: () -> Order): Order = this

    /**
     * 小于结果
     *
     * Represents a "less than" comparison result.
     * 表示"小于"比较结果。
     *
     * @param value 负整数值 / Negative integer value
    */
    data class Less(override val value: Int = -1) : Order {
        init {
            assert(value < 0)
        }

        override operator fun unaryMinus(): Order {
            return Greater(-value)
        }
    }

    /**
     * 相等结果
     *
     * Represents an "equal" comparison result.
     * 表示"相等"比较结果。
    */
    data object Equal : Order {
        override val value = 0

        override operator fun unaryMinus(): Order {
            return Equal
        }

        override fun ifEqual(f: () -> Order) = f()
    }

    /**
     * 大于结果
     *
     * Represents a "greater than" comparison result.
     * 表示"大于"比较结果。
     *
     * @param value 正整数值 / Positive integer value
    */
    data class Greater(override val value: Int = 1) : Order {
        init {
            assert(value > 0)
        }

        override operator fun unaryMinus(): Order {
            return Less(-value)
        }
    }
}

/**
 * 从整数值创建 Order
 *
 * Creates an Order from an integer value.
 * 从整数值创建 Order。
 *
 * @param value 比较值，负数为 Less，零为 Equal，正数为 Greater / Comparison value, negative for Less, zero for Equal, positive for Greater
 * @return 对应的 Order 值 / The corresponding Order value
*/
fun orderOf(value: Int): Order {
    return if (value < 0) {
        Order.Less(value)
    } else if (value > 0) {
        Order.Greater(value)
    } else {
        Order.Equal
    }
}

/**
 * 计算两个 Comparable 值之间的顺序
 *
 * Computes the order between two Comparable values.
 * 计算两个 Comparable 值之间的顺序。
 *
 * @param T 可比较的类型 / The Comparable type
 * @param lhs 左侧值 / The left-hand side value
 * @param rhs 右侧值 / The right-hand side value
 * @return 比较结果的 Order / The Order comparison result
*/
fun <T : Comparable<T>> orderBetween(lhs: T, rhs: T): Order {
    return orderOf(lhs.compareTo(rhs))
}

/**
 * Comparable 类型的排序比较
 *
 * Computes the order between two Comparable values using infix notation.
 * 使用中缀表示法计算两个 Comparable 值之间的顺序。
 *
 * @param T 可比较的类型 / The Comparable type
 * @param rhs 右侧值 / The right-hand side value
 * @return 比较结果的 Order / The Order comparison result
*/
@JvmName("comparableOrd")
infix fun <T : Comparable<T>> T.ord(rhs: T): Order {
    return orderOf(this.compareTo(rhs))
}

/**
 * 可空 Comparable 类型的排序比较
 *
 * Computes the order between two nullable Comparable values.
 * Null is considered less than any non-null value.
 * 计算两个可空 Comparable 值之间的顺序。null 被认为小于任何非空值。
 *
 * @param T 可比较的类型 / The Comparable type
 * @param rhs 右侧可空值 / The nullable right-hand side value
 * @return 比较结果的 Order / The Order comparison result
*/
@JvmName("comparableNullableOrd")
infix fun <T : Comparable<T>> T?.ord(rhs: T?): Order {
    return if (this == null && rhs != null) {
        Order.Less()
    } else if (this != null && rhs == null) {
        Order.Greater()
    } else if (this != null && rhs != null) {
        this ord rhs
    } else {
        Order.Equal
    }
}

/**
 * 部分排序接口
 *
 * Interface for partial ordering comparison. Returns null if values cannot be compared.
 * 部分排序比较接口。如果值无法比较则返回 null。
 *
 * @param Self 实现此接口的类型 / The type implementing this interface
*/
interface PartialOrd<in Self> : PartialEq<Self> {
    override fun partialEq(rhs: Self): Boolean? {
        return partialOrd(rhs)?.let { it is Order.Equal }
    }

    /**
     * 部分排序比较
     *
     * Compares `this` value with [rhs] for partial ordering.
     * Returns [Order] if the values are comparable, or `null` if they are not.
     *
     * 将当前值与 [rhs] 进行部分排序比较。
     * 如果两个值可比较则返回 [Order]，否则返回 `null`。
     *
     * @param rhs 另一个待比较的值 / The other value to compare against
     * @return 比较结果，不可比较时为 null / The ordering result, or null if not comparable
    */
    infix fun partialOrd(rhs: Self): Order?
}

/**
 * 完全排序接口
 *
 * Interface for total ordering comparison. Extends [PartialOrd], [Eq], and [Comparable].
 * 完全排序比较接口。扩展 [PartialOrd]、[Eq] 和 [Comparable]。
 *
 * @param Self 实现此接口的类型 / The type implementing this interface
*/
interface Ord<in Self> : PartialOrd<Self>, Eq<Self>, Comparable<Self> {

    /**
     * 计算排序结果
     *
     * Computes the order between this value and another.
     * 计算此值与另一个值之间的顺序。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 比较结果的 Order / The Order comparison result
    */
    infix fun ord(rhs: Self): Order {
        return (this partialOrd rhs)!!
    }

    /**
     * 实现 Comparable 接口的比较方法
     *
     * Implementation of Comparable interface using ord.
     * 使用 ord 实现 Comparable 接口。
    */
    override operator fun compareTo(other: Self): Int {
        return (this ord other).value
    }

    /**
     * 小于比较
     *
     * Checks if this value is less than another.
     * 检查此值是否小于另一个值。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 如果小于则为 true / true if this value is less than the other
    */
    infix fun ls(rhs: Self): Boolean {
        return this < rhs
    }

    /**
     * 小于等于比较
     *
     * Checks if this value is less than or equal to another.
     * 检查此值是否小于等于另一个值。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 如果小于等于则为 true / true if this value is less than or equal to the other
    */
    infix fun leq(rhs: Self): Boolean {
        return this <= rhs
    }

    /**
     * 大于比较
     *
     * Checks if this value is greater than another.
     * 检查此值是否大于另一个值。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 如果大于则为 true / true if this value is greater than the other
    */
    infix fun gr(rhs: Self): Boolean {
        return this > rhs
    }

    /**
     * 大于等于比较
     *
     * Checks if this value is greater than or equal to another.
     * 检查此值是否大于等于另一个值。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 如果大于等于则为 true / true if this value is greater than or equal to the other
    */
    infix fun geq(rhs: Self): Boolean {
        return this >= rhs
    }
}

/**
 * 两个可空 PartialOrd 值之间的部分排序比较
 *
 * Computes the partial order between two nullable PartialOrd values.
 * Null is considered less than any non-null value.
 * 计算两个可空 PartialOrd 值之间的部分顺序。null 被认为小于任何非空值。
 *
 * @param T 实现 PartialOrd 的类型 / The type implementing PartialOrd
 * @param rhs 右侧可空值 / The nullable right-hand side value
 * @return 比较结果的 Order，或 null / The Order comparison result, or null
*/
infix fun <T : PartialOrd<T>> T?.partialOrd(rhs: T?): Order? {
    return if (this == null && rhs != null) {
        Order.Less()
    } else if (this != null && rhs == null) {
        Order.Greater()
    } else if (this != null && rhs != null) {
        this partialOrd rhs
    } else {
        Order.Equal
    }
}

/**
 * 两个可空 Ord 值之间的排序比较
 *
 * Computes the order between two nullable Ord values.
 * Null is considered less than any non-null value.
 * 计算两个可空 Ord 值之间的顺序。null 被认为小于任何非空值。
 *
 * @param T 实现 Ord 的类型 / The type implementing Ord
 * @param rhs 右侧可空值 / The nullable right-hand side value
 * @return 比较结果的 Order / The Order comparison result
*/
infix fun <T : Ord<T>> T?.ord(rhs: T?): Order {
    return if (this == null && rhs != null) {
        Order.Less()
    } else if (this != null && rhs == null) {
        Order.Greater()
    } else if (this != null && rhs != null) {
        this ord rhs
    } else {
        Order.Equal
    }
}

/**
 * 将值限制在指定范围内
 *
 * Coerces this value within the specified range bounds.
 * 将此值限制在指定范围边界内。
 *
 * @param T 实现 Ord 的类型 / The type implementing Ord
 * @param lb 可空的下界 / The nullable lower bound
 * @param ub 可空的上界 / The nullable upper bound
 * @return 限制后的值 / The coerced value
*/
fun <T : Ord<T>> T.coerceIn(lb: T?, ub: T?): T {
    return if (lb != null && this ord lb is Order.Less) {
        lb
    } else if (ub != null && this ord ub is Order.Greater) {
        ub
    } else {
        this
    }
}
