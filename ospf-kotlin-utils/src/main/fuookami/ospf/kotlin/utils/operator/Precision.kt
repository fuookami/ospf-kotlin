package fuookami.ospf.kotlin.utils.operator

import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class Precision<T>(
    precision: T
) where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> {
    private val precision = precision.abs()

    infix fun T.eq(other: T): Boolean {
        return (this - other).abs() <= precision
    }

    infix fun T.ord(other: T): Order {
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

    infix fun T.neq(other: T): Boolean {
        return (this - other).abs() > precision
    }

    infix fun T.ls(other: T): Boolean {
        return (other - this) >= precision
    }

    infix fun T.leq(other: T): Boolean {
        return (this - other) <= precision
    }

    infix fun T.gr(other: T): Boolean {
        return (this - other) > precision
    }

    infix fun T.geq(other: T): Boolean {
        return (other - this) <= precision
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> withPrecision(precision: T = (T::class.companionObjectInstance!! as RealNumberConstants<T>).decimalPrecision)
        where T : RealNumber<T>, T : PlusGroup<T>, T : Abs<T> = Precision(precision)
