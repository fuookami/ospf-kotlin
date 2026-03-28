package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.operator.Dec
import fuookami.ospf.kotlin.utils.operator.Inc
import fuookami.ospf.kotlin.utils.operator.Minus
import fuookami.ospf.kotlin.utils.operator.Neg
import fuookami.ospf.kotlin.utils.operator.Plus

interface PlusSemiGroup<Self> : Semigroup<Self>, Plus<Self, Self>, Inc<Self>

interface PlusGroup<Self> : AbelianGroup<Self>, PlusSemiGroup<Self>,
    Neg<Self>, Minus<Self, Self>, Dec<Self>

