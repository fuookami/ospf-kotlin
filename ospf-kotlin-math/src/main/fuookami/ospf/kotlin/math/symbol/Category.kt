/**
 * 表达式类型分类
 * Expression Category
 *
 * 定义符号表达式按次数的分类体系，包括线性、二次、标准和非线性。
 * 用于在编译时和运行时区分表达式的类型，支持优化和类型检查。
 * Defines the classification hierarchy for symbolic expressions by degree,
 * including Linear, Quadratic, Standard, and Nonlinear.
 * Used to distinguish expression types at compile-time and runtime,
 * supporting optimization and type checking.
 */
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Order

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







