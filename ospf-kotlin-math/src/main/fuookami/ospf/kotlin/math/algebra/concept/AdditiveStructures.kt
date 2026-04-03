package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.math.operator.Dec
import fuookami.ospf.kotlin.utils.math.operator.Inc
import fuookami.ospf.kotlin.utils.math.operator.Minus
import fuookami.ospf.kotlin.utils.math.operator.Neg
import fuookami.ospf.kotlin.utils.math.operator.Plus

interface PlusSemiGroup<Self> : Semigroup<Self>, Plus<Self, Self>, Inc<Self>

interface PlusGroup<Self> : AbelianGroup<Self>, PlusSemiGroup<Self>,
    Neg<Self>, Minus<Self, Self>, Dec<Self>

