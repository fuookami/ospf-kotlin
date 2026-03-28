package fuookami.ospf.kotlin.utils.math

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.operator.Order
import fuookami.ospf.kotlin.utils.operator.TolerancedEq
import fuookami.ospf.kotlin.utils.operator.TolerancedOrd

fun <T> defaultTolerancedEq(): TolerancedEq<T>
        where T : RealNumber<T>, T : NumberField<T> {
    return TolerancedEq { lhs, rhs, tolerance ->
        val diff = if (lhs >= rhs) {
            lhs - rhs
        } else {
            rhs - lhs
        }
        diff.abs() <= tolerance
    }
}

fun <T> defaultTolerancedOrd(): TolerancedOrd<T>
        where T : RealNumber<T>, T : NumberField<T> {
    return TolerancedOrd { lhs, rhs, tolerance ->
        val diff = if (lhs >= rhs) {
            lhs - rhs
        } else {
            rhs - lhs
        }
        if (diff.abs() <= tolerance) {
            Order.Equal
        } else if (lhs < rhs) {
            Order.Less()
        } else {
            Order.Greater()
        }
    }
}



