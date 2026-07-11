/**
 * 比较运算符
 * Comparison Operators
 *
 * 定义带精度的比较运算符，包括 Equal、Unequal、Less、LessEqual、Greater、GreaterEqual，
 * 用于支持浮点数等需要容差的数值类型的精确比较。
 * Defines precision-based comparison operators, including Equal, Unequal, Less, LessEqual, Greater, GreaterEqual,
 * for precise comparison of numeric types that require tolerance, such as floating-point numbers.
*/
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.*

/**
 * 相等比较
 * Equality comparison
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class Equal<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Abs<U>, U : Ord<U> {
    companion object {
        /**
         * 从精度值创建相等比较器
         * Create equality comparator from precision
         *
         * @param precision 比较精度 / Comparison precision
         * @return 相等比较器实例 / Equality comparator instance
        */
        operator fun <T, U> invoke(precision: U): Equal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = Equal(precision)

        /**
         * 从常量提供者创建相等比较器
         * Create equality comparator from constants
         *
         * @param constants 数值常量提供器 / Real number constants provider
         * @return 相等比较器实例 / Equality comparator instance
        */
        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Equal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            Equal(constants.decimalPrecision)
    }

    /**
     * 执行相等比较
     * Perform equality comparison
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 是否相等 / Whether the values are equal
    */
    operator fun invoke(lhs: T, rhs: T) = when (precision) {
        null -> lhs.value() eq rhs.value()
        else -> abs((lhs - rhs).value()) <= precision
    }
}

/**
 * 不等比较
 * Inequality comparison
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class Unequal<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Abs<U>, U : Ord<U> {
    companion object {
        /**
         * 从精度值创建不等比较器
         * Create inequality comparator from precision
         *
         * @param precision 比较精度 / Comparison precision
         * @return 不等比较器实例 / Inequality comparator instance
        */
        operator fun <T, U> invoke(precision: U): Unequal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = Unequal(precision)

        /**
         * 从常量提供者创建不等比较器
         * Create inequality comparator from constants
         *
         * @param constants 数值常量提供器 / Real number constants provider
         * @return 不等比较器实例 / Inequality comparator instance
        */
        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Unequal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            Unequal(constants.decimalPrecision)
    }

    /**
     * 执行不等比较
     * Perform inequality comparison
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 是否不等 / Whether the values are unequal
    */
    operator fun invoke(lhs: T, rhs: T) = when (precision) {
        null -> lhs.value() neq rhs.value()
        else -> abs((lhs - rhs).value()) >= precision
    }
}

/**
 * 小于比较
 * Less-than comparison
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class Less<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Ord<U>, U : Neg<U> {
    companion object {
        /**
         * 从精度值创建小于比较器
         * Create less-than comparator from precision
         *
         * @param precision 比较精度 / Comparison precision
         * @return 小于比较器实例 / Less-than comparator instance
        */
        operator fun <T, U> invoke(precision: U): Less<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T>, U : Neg<U> =
            Less(precision)

        /**
         * 从常量提供者创建小于比较器
         * Create less-than comparator from constants
         *
         * @param constants 数值常量提供器 / Real number constants provider
         * @return 小于比较器实例 / Less-than comparator instance
        */
        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Less<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T>, U : Neg<U> =
            Less(constants.decimalPrecision)
    }

    /**
     * 执行小于比较
     * Perform less-than comparison
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 左值是否小于右值 / Whether lhs is less than rhs
    */
    operator fun invoke(lhs: T, rhs: T) = when (precision) {
        null -> lhs.value() < rhs.value()
        else -> (lhs - rhs).value() <= -precision
    }
}

/**
 * 小于等于比较
 * Less-than-or-equal comparison
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class LessEqual<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Ord<U> {
    companion object {
        /**
         * 从精度值创建小于等于比较器
         * Create less-than-or-equal comparator from precision
         *
         * @param precision 比较精度 / Comparison precision
         * @return 小于等于比较器实例 / Less-than-or-equal comparator instance
        */
        operator fun <T, U> invoke(precision: U): LessEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = LessEqual(precision)

        /**
         * 从常量提供者创建小于等于比较器
         * Create less-than-or-equal comparator from constants
         *
         * @param constants 数值常量提供器 / Real number constants provider
         * @return 小于等于比较器实例 / Less-than-or-equal comparator instance
        */
        operator fun <T, U> invoke(constants: RealNumberConstants<U>): LessEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            LessEqual(constants.decimalPrecision)
    }

    /**
     * 执行小于等于比较
     * Perform less-than-or-equal comparison
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 左值是否小于等于右值 / Whether lhs is less than or equal to rhs
    */
    operator fun invoke(lhs: T, rhs: T) = when (precision) {
        null -> lhs.value() <= rhs.value()
        else -> (lhs - rhs).value() <= precision
    }
}

/**
 * 大于比较
 * Greater-than comparison
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class Greater<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Ord<U> {
    companion object {
        /**
         * 从精度值创建大于比较器
         * Create greater-than comparator from precision
         *
         * @param precision 比较精度 / Comparison precision
         * @return 大于比较器实例 / Greater-than comparator instance
        */
        operator fun <T, U> invoke(precision: U): Greater<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = Greater(precision)

        /**
         * 从常量提供者创建大于比较器
         * Create greater-than comparator from constants
         *
         * @param constants 数值常量提供器 / Real number constants provider
         * @return 大于比较器实例 / Greater-than comparator instance
        */
        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Greater<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            Greater(constants.decimalPrecision)
    }

    /**
     * 执行大于比较
     * Perform greater-than comparison
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 左值是否大于右值 / Whether lhs is greater than rhs
    */
    operator fun invoke(lhs: T, rhs: T) = when (precision) {
        null -> lhs.value() > rhs.value()
        else -> (lhs - rhs).value() >= precision
    }
}

/**
 * 大于等于比较
 * Greater-than-or-equal comparison
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class GreaterEqual<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Ord<U>, U : Neg<U> {
    companion object {
        /**
         * 从精度值创建大于等于比较器
         * Create greater-than-or-equal comparator from precision
         *
         * @param precision 比较精度 / Comparison precision
         * @return 大于等于比较器实例 / Greater-than-or-equal comparator instance
        */
        operator fun <T, U> invoke(precision: U): GreaterEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> =
            GreaterEqual(precision)

        /**
         * 从常量提供者创建大于等于比较器
         * Create greater-than-or-equal comparator from constants
         *
         * @param constants 数值常量提供器 / Real number constants provider
         * @return 大于等于比较器实例 / Greater-than-or-equal comparator instance
        */
        operator fun <T, U> invoke(constants: RealNumberConstants<U>): GreaterEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T>, U : Neg<U> =
            GreaterEqual(constants.decimalPrecision)
    }

    /**
     * 执行大于等于比较
     * Perform greater-than-or-equal comparison
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 左值是否大于等于右值 / Whether lhs is greater than or equal to rhs
    */
    operator fun invoke(lhs: T, rhs: T) = when (precision) {
        null -> lhs.value() >= rhs.value()
        else -> (lhs - rhs).value() >= -precision
    }
}

/**
 * 比较运算符集合
 * Comparison operator collection
 *
 * 提供统一的带精度比较运算符接口。
 * Provides unified precision-based comparison operator interface.
 *
 * @param T 值类型
 * @param U 数值类型
 * @property precision 比较精度
*/
data class ComparisonOperator<T, U>(
    val precision: U?
) where T : Arithmetic<U>, T : Invariant<U>, T : Minus<T, T>, U : Abs<U>, U : Ord<U>, U : Neg<U> {

    /**
     * 相等比较
     * Equality comparison
     *
     * @param rhs 右操作数 / Right operand
     * @return 是否相等 / Whether the values are equal
    */
    infix fun T.eq(rhs: T): Boolean {
        val op = Equal<T, U>(precision)
        return op(this, rhs)
    }

    /**
     * 不等比较
     * Inequality comparison
     *
     * @param rhs 右操作数 / Right operand
     * @return 是否不等 / Whether the values are unequal
    */
    infix fun T.neq(rhs: T): Boolean {
        val op = Unequal<T, U>(precision)
        return op(this, rhs)
    }

    /**
     * 小于比较
     * Less-than comparison
     *
     * @param rhs 右操作数 / Right operand
     * @return 左值是否小于右值 / Whether lhs is less than rhs
    */
    infix fun T.ls(rhs: T): Boolean {
        val op = Less<T, U>(precision)
        return op(this, rhs)
    }

    /**
     * 小于等于比较
     * Less-than-or-equal comparison
     *
     * @param rhs 右操作数 / Right operand
     * @return 左值是否小于等于右值 / Whether lhs is less than or equal to rhs
    */
    infix fun T.leq(rhs: T): Boolean {
        val op = LessEqual<T, U>(precision)
        return op(this, rhs)
    }

    /**
     * 大于比较
     * Greater-than comparison
     *
     * @param rhs 右操作数 / Right operand
     * @return 左值是否大于右值 / Whether lhs is greater than rhs
    */
    infix fun T.gr(rhs: T): Boolean {
        val op = Greater<T, U>(precision)
        return op(this, rhs)
    }

    /**
     * 大于等于比较
     * Greater-than-or-equal comparison
     *
     * @param rhs 右操作数 / Right operand
     * @return 左值是否大于等于右值 / Whether lhs is greater than or equal to rhs
    */
    infix fun T.geq(rhs: T): Boolean {
        val op = GreaterEqual<T, U>(precision)
        return op(this, rhs)
    }
}
