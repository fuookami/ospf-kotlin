package fuookami.ospf.kotlin.core.test

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 测试侧通用 Flt64 converter，避免重复样板定义。
 * Shared Flt64 converter for tests to avoid duplicated boilerplate.
 */
val flt64TestConverter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}
