package fuookami.ospf.kotlin.utils.operator

import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*

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

@Suppress("UNCHECKED_CAST")
inline fun <reified T> withPrecision(precision: T = (T::class.companionObjectInstance!! as RealNumberConstants<T>).decimalPrecision)
        where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> = Precision(precision)
