package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.math.operator.Div
import fuookami.ospf.kotlin.utils.math.operator.IntDiv
import fuookami.ospf.kotlin.utils.math.operator.Reciprocal
import fuookami.ospf.kotlin.utils.math.operator.Rem
import fuookami.ospf.kotlin.utils.math.operator.Times

interface TimesSemiGroup<Self> : MultiplicativeSemigroup<Self>, Times<Self, Self>

interface TimesGroup<Self> : MultiplicativeGroup<Self>, TimesSemiGroup<Self>,
    Reciprocal<Self>, Div<Self, Self>, IntDiv<Self, Self>, Rem<Self, Self>

