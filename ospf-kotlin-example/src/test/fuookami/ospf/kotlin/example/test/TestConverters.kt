package fuookami.ospf.kotlin.example.test

import fuookami.ospf.kotlin.math.algebra.number.Flt64

import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * Example-test-shared Flt64 converter to avoid duplicated boilerplate.
 */
val flt64TestConverter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}
