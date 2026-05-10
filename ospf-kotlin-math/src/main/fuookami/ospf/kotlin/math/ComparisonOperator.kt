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

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.Abs
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.operator.Neg

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
        operator fun <T, U> invoke(precision: U): Equal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = Equal(precision)

        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Equal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            Equal(constants.decimalPrecision)
    }

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
        operator fun <T, U> invoke(precision: U): Unequal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = Unequal(precision)

        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Unequal<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            Unequal(constants.decimalPrecision)
    }

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
        operator fun <T, U> invoke(precision: U): Less<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T>, U : Neg<U> =
            Less(precision)

        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Less<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T>, U : Neg<U> =
            Less(constants.decimalPrecision)
    }

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
        operator fun <T, U> invoke(precision: U): LessEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = LessEqual(precision)

        operator fun <T, U> invoke(constants: RealNumberConstants<U>): LessEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            LessEqual(constants.decimalPrecision)
    }

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
        operator fun <T, U> invoke(precision: U): Greater<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> = Greater(precision)

        operator fun <T, U> invoke(constants: RealNumberConstants<U>): Greater<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T> =
            Greater(constants.decimalPrecision)
    }

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
        operator fun <T, U> invoke(precision: U): GreaterEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : FloatingNumber<U>, T : Minus<T, T> =
            GreaterEqual(precision)

        operator fun <T, U> invoke(constants: RealNumberConstants<U>): GreaterEqual<T, U>
                where T : Arithmetic<U>, T : Invariant<U>, U : RealNumber<U>, T : Minus<T, T>, U : Neg<U> =
            GreaterEqual(constants.decimalPrecision)
    }

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
    infix fun T.eq(rhs: T): Boolean {
        val op = Equal<T, U>(precision)
        return op(this, rhs)
    }

    infix fun T.neq(rhs: T): Boolean {
        val op = Unequal<T, U>(precision)
        return op(this, rhs)
    }

    infix fun T.ls(rhs: T): Boolean {
        val op = Less<T, U>(precision)
        return op(this, rhs)
    }

    infix fun T.leq(rhs: T): Boolean {
        val op = LessEqual<T, U>(precision)
        return op(this, rhs)
    }

    infix fun T.gr(rhs: T): Boolean {
        val op = Greater<T, U>(precision)
        return op(this, rhs)
    }

    infix fun T.geq(rhs: T): Boolean {
        val op = GreaterEqual<T, U>(precision)
        return op(this, rhs)
    }
}




