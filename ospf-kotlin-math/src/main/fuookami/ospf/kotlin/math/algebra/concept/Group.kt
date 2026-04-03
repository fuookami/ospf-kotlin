package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Dec
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.utils.math.operator.Neg

interface Group<Self> : Monoid<Self>, Neg<Self>, Minus<Self, Self>, Dec<Self>

