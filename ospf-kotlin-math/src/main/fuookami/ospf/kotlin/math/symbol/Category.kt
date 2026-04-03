package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.operator.Order

sealed class Category {
    abstract val code: UInt64

    infix fun op(rhs: Category) = if (this.code < rhs.code) {
        rhs
    } else {
        this
    }
}

data object Linear : Category() {
    override val code = UInt64.one
}

data object Quadratic : Category() {
    override val code = UInt64.two
}

data object Standard : Category() {
    override val code = UInt64.three
}

data object Nonlinear : Category() {
    override val code = UInt64.ten
}

infix fun Category.ord(rhs: Category): Order {
    return this.code ord rhs.code
}

fun max(lhs: Category, rhs: Category): Category {
    return if (lhs.code > rhs.code) {
        lhs
    } else {
        rhs
    }
}

fun Collection<Category>.max(): Category {
    return this.maxBy { it.code }
}

fun Collection<Category>.maxOrNull(): Category? {
    return this.maxByOrNull { it.code }
}







