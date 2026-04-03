package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Inc

interface Monoid<Self> : Semigroup<Self>, Inc<Self>

