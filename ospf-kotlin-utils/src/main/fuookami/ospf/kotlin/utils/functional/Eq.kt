/**
 * 相等性接口
 *
 * Interfaces for defining custom equality comparison.
 * Similar to Haskell's Eq typeclass for type-safe equality operations.
 * 定义自定义相等性比较的接口。
 * 类似于 Haskell 的 Eq 类型类，用于类型安全的相等性操作。
 *
 * Key interfaces:
 * - [PartialEq]: Partial equality with nullable result (for values that may not be comparable)
 * - [Eq]: Total equality with definite result
 *
 * 主要接口：
 * - [PartialEq]: 部分相等，结果可空（用于可能不可比较的值）
 * - [Eq]: 完全相等，结果确定
*/
package fuookami.ospf.kotlin.utils.functional

/**
 * 部分相等接口
 *
 * Interface for partial equality comparison. Returns null if values cannot be compared.
 * 部分相等性比较接口。如果值无法比较则返回 null。
 *
 * Similar to Haskell's Eq typeclass with partial comparison support.
 * 类似于 Haskell 的 Eq 类型类，支持部分比较。
 *
 * @param Self 实现此接口的类型 / The type implementing this interface
*/
interface PartialEq<in Self> {

    /**
     * 部分相等比较
     *
     * Compares this value with another for equality.
     * Returns null if the values cannot be meaningfully compared.
     * 比较此值与另一个值是否相等。如果值无法有意义地比较则返回 null。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 相等则返回 true，不相等则返回 false，无法比较则返回 null / true if equal, false if not equal, null if not comparable
    */
    infix fun partialEq(rhs: Self): Boolean?
}

/**
 * 完全相等接口
 *
 * Interface for total equality comparison.
 * Extends [PartialEq] with guaranteed non-null results.
 * 完全相等性比较接口。
 * 扩展 [PartialEq] 并保证结果非空。
 *
 * @param Self 实现此接口的类型 / The type implementing this interface
*/
interface Eq<in Self> : PartialEq<Self> {

    /**
     * 相等比较
     *
     * Compares this value with another for equality.
     * 比较此值与另一个值是否相等。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 相等则返回 true，否则返回 false / true if equal, false otherwise
    */
    infix fun eq(rhs: Self): Boolean {
        return (this partialEq rhs)!!
    }

    /**
     * 不相等比较
     *
     * Compares this value with another for inequality.
     * 比较此值与另一个值是否不相等。
     *
     * @param rhs 要比较的值 / The value to compare with
     * @return 不相等则返回 true，否则返回 false / true if not equal, false otherwise
    */
    infix fun neq(rhs: Self): Boolean {
        return !(this eq rhs)
    }
}

/**
 * 非空值与可空值的部分相等比较
 *
 * Compares a non-null value with a nullable value for partial equality.
 * 非空值与可空值的部分相等比较。
 *
 * @param T 实现 PartialEq 的类型 / The type implementing PartialEq
 * @param rhs 要比较的可空值 / The nullable value to compare with
 * @return 如果 rhs 为 null 则返回 false，否则返回部分相等结果 / false if rhs is null, otherwise the partial equality result
*/
@JvmName("notNullPartialEqNullable")
infix fun <T : PartialEq<T>> T.partialEq(rhs: T?): Boolean? {
    return if (rhs == null) {
        false
    } else {
        this.partialEq(rhs)
    }
}

/**
 * 可空值与非空值的部分相等比较
 *
 * Compares a nullable value with a non-null value for partial equality.
 * 可空值与非空值的部分相等比较。
 *
 * @param T 实现 PartialEq 的类型 / The type implementing PartialEq
 * @param rhs 要比较的非空值 / The non-null value to compare with
 * @return 如果此值为 null 则返回 false，否则返回部分相等结果 / false if this is null, otherwise the partial equality result
*/
@JvmName("nullablePartialEqNotNull")
infix fun <T : PartialEq<T>> T?.partialEq(rhs: T): Boolean? {
    return if (this == null) {
        false
    } else {
        this.partialEq(rhs)
    }
}

/**
 * 两个可空值之间的部分相等比较
 *
 * Compares two nullable values for partial equality.
 * 两个可空值之间的部分相等比较。
 *
 * @param T 实现 PartialEq 的类型 / The type implementing PartialEq
 * @param rhs 要比较的可空值 / The nullable value to compare with
 * @return 两个值都为 null 则返回 true，一个为 null 则返回 false，否则返回部分相等结果 / true if both are null, false if one is null, otherwise the partial equality result
*/
@JvmName("nullablePartialEqNullable")
infix fun <T : PartialEq<T>> T?.partialEq(rhs: T?): Boolean? {
    return if (this == null && rhs == null) {
        true
    } else if (this != null && rhs != null) {
        this partialEq rhs
    } else {
        false
    }
}

/**
 * 非空值与可空值的完全相等比较
 *
 * Compares a non-null value with a nullable value for total equality.
 * 非空值与可空值的完全相等比较。
 *
 * @param T 实现 Eq 的类型 / The type implementing Eq
 * @param rhs 要比较的可空值 / The nullable value to compare with
 * @return 如果 rhs 为 null 则返回 false，否则返回相等结果 / false if rhs is null, otherwise the equality result
*/
@JvmName("notNullEqNullable")
infix fun <T : Eq<T>> T.eq(rhs: T?): Boolean {
    return if (rhs == null) {
        false
    } else {
        this.eq(rhs)
    }
}

/**
 * 可空值与非空值的完全相等比较
 *
 * Compares a nullable value with a non-null value for total equality.
 * 可空值与非空值的完全相等比较。
 *
 * @param T 实现 Eq 的类型 / The type implementing Eq
 * @param rhs 要比较的非空值 / The non-null value to compare with
 * @return 如果此值为 null 则返回 false，否则返回相等结果 / false if this is null, otherwise the equality result
*/
@JvmName("nullableEqNotNull")
infix fun <T : Eq<T>> T?.eq(rhs: T): Boolean {
    return this?.eq(rhs) ?: false
}

/**
 * 两个可空值之间的完全相等比较
 *
 * Compares two nullable values for total equality.
 * 两个可空值之间的完全相等比较。
 *
 * @param T 实现 Eq 的类型 / The type implementing Eq
 * @param rhs 要比较的可空值 / The nullable value to compare with
 * @return 两个值都为 null 则返回 true，一个为 null 则返回 false，否则返回相等结果 / true if both are null, false if one is null, otherwise the equality result
*/
@JvmName("nullableEqNullable")
infix fun <T : Eq<T>> T?.eq(rhs: T?): Boolean {
    return if (this == null && rhs == null) {
        true
    } else if (this != null && rhs != null) {
        this eq rhs
    } else {
        false
    }
}

/**
 * 可空值与非空值的不相等比较
 *
 * Compares a nullable value with a non-null value for inequality.
 * 可空值与非空值的不相等比较。
 *
 * @param T 实现 Eq 的类型 / The type implementing Eq
 * @param rhs 要比较的非空值 / The non-null value to compare with
 * @return 如果此值为 null 则返回 true，否则返回不相等结果 / true if this is null, otherwise the inequality result
*/
@JvmName("nullableNeqNotNull")
infix fun <T : Eq<T>> T?.neq(rhs: T): Boolean {
    return this?.neq(rhs) ?: true
}

/**
 * 非空值与可空值的不相等比较
 *
 * Compares a non-null value with a nullable value for inequality.
 * 非空值与可空值的不相等比较。
 *
 * @param T 实现 Eq 的类型 / The type implementing Eq
 * @param rhs 要比较的可空值 / The nullable value to compare with
 * @return 如果 rhs 为 null 则返回 true，否则返回不相等结果 / true if rhs is null, otherwise the inequality result
*/
@JvmName("notNullNeqNullable")
infix fun <T : Eq<T>> T.neq(rhs: T?): Boolean {
    return if (rhs == null) {
        true
    } else {
        this.neq(rhs)
    }
}

/**
 * 两个可空值之间的不相等比较
 *
 * Compares two nullable values for inequality.
 * 两个可空值之间的不相等比较。
 *
 * @param T 实现 Eq 的类型 / The type implementing Eq
 * @param rhs 要比较的可空值 / The nullable value to compare with
 * @return 两个值都为 null 则返回 false，一个为 null 则返回 true，否则返回不相等结果 / false if both are null, true if one is null, otherwise the inequality result
*/
@JvmName("nullableNeqNullable")
infix fun <T : Eq<T>> T?.neq(rhs: T?): Boolean {
    return if (this == null && rhs == null) {
        false
    } else if (this != null && rhs != null) {
        this neq rhs
    } else {
        true
    }
}
