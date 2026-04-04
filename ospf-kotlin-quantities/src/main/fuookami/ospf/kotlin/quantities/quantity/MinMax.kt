package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.PartialOrd

fun <T : PartialOrd<T>> min(lhs: Quantity<T>, rhs: Quantity<T>): Quantity<T>? = when (lhs partialOrd rhs) {
    is Order.Less, is Order.Equal -> {
        lhs
    }

    is Order.Greater -> {
        rhs
    }

    null -> {
        null
    }
}

fun <T : PartialOrd<T>> max(lhs: Quantity<T>, rhs: Quantity<T>): Quantity<T>? = when (lhs partialOrd rhs) {
    is Order.Greater, is Order.Equal -> {
        lhs
    }

    is Order.Less -> {
        rhs
    }

    null -> {
        null
    }
}
