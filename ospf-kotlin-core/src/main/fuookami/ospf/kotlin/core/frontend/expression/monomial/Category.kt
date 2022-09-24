package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.utils.math.UInt64

sealed class Category {
    abstract val code: UInt64

    infix fun op(rhs: Category) = if (this.code < rhs.code) {
        rhs
    } else {
        this
    }
}

object Linear : Category() {
    override val code = UInt64.one
}

object Quadratic : Category() {
    override val code = UInt64.two
}

object Standard : Category() {
    override val code = UInt64.three
}

object Nonlinear : Category() {
    override val code = UInt64.ten
}
