package fuookami.ospf.kotlin.math.operator

import fuookami.ospf.kotlin.math.algebra.concept.PlusGroup
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.concept.resolveRealNumberConstants
import fuookami.ospf.kotlin.math.operator.Abs
import fuookami.ospf.kotlin.utils.math.operator.Order

class Precision<T>(
    precision: T
) where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> {
    private val precision = precision.abs()

    infix fun T.equal(other: T): Boolean {
        return (this - other).abs() <= precision
    }

    infix fun T.order(other: T): Order {
        return if (this eq other) {
            Order.Equal
        } else {
            val value = this.compareTo(other)
            if (value < 0) {
                Order.Less(value)
            } else {
                Order.Greater(value)
            }
        }
    }

    infix fun T.unequal(other: T): Boolean {
        return (this - other).abs() > precision
    }

    infix fun T.less(other: T): Boolean {
        return (other - this) >= precision
    }

    infix fun T.lessEqual(other: T): Boolean {
        return (this - other) <= precision
    }

    infix fun T.greater(other: T): Boolean {
        return (this - other) > precision
    }

    infix fun T.greaterEqual(other: T): Boolean {
        return (other - this) <= precision
    }
}

fun <T> withPrecision(
    constants: RealNumberConstants<T>,
    precision: T = constants.decimalPrecision
): Precision<T> where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> = Precision(precision)

inline fun <reified T> withPrecision(
    precision: T? = null
): Precision<T> where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> {
    if (precision != null) {
        return Precision(precision)
    }
    val constants = resolveRealNumberConstants<T>("Precision")
    return withPrecision(constants, constants.decimalPrecision)
}

