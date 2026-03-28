package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.operator.PartialEq

interface Arithmetic<Self> : Copyable<Self>, PartialEq<Self> {
    val constants: ArithmeticConstants<Self>

    infix fun equiv(rhs: Self): Boolean
}

interface ArithmeticConstants<Self> {
    val zero: Self
    val one: Self
}

