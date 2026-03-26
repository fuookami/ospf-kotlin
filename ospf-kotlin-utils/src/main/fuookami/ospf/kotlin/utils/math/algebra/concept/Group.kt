package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.operator.Dec
import fuookami.ospf.kotlin.utils.operator.Minus
import fuookami.ospf.kotlin.utils.operator.Neg

interface Group<Self> : Monoid<Self>, Neg<Self>, Minus<Self, Self>, Dec<Self>

