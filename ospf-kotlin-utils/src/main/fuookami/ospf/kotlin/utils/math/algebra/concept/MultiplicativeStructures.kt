package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.operator.Div
import fuookami.ospf.kotlin.utils.operator.IntDiv
import fuookami.ospf.kotlin.utils.operator.Reciprocal
import fuookami.ospf.kotlin.utils.operator.Rem
import fuookami.ospf.kotlin.utils.operator.Times

interface TimesSemiGroup<Self> : MultiplicativeSemigroup<Self>, Times<Self, Self>

interface TimesGroup<Self> : MultiplicativeGroup<Self>, TimesSemiGroup<Self>,
    Reciprocal<Self>, Div<Self, Self>, IntDiv<Self, Self>, Rem<Self, Self>

