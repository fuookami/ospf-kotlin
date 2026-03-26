package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.operator.Inc

interface Monoid<Self> : Semigroup<Self>, Inc<Self>

