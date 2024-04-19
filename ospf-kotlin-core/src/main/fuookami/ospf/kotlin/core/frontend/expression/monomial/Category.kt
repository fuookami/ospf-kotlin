package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.utils.math.*

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
