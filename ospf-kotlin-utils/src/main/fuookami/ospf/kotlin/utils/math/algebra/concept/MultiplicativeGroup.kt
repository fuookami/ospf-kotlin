package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.operator.Div
import fuookami.ospf.kotlin.utils.operator.IntDiv
import fuookami.ospf.kotlin.utils.operator.Reciprocal
import fuookami.ospf.kotlin.utils.operator.Rem

interface MultiplicativeGroup<Self> : MultiplicativeMonoid<Self>,
    Reciprocal<Self>,
    Div<Self, Self>,
    IntDiv<Self, Self>,
    Rem<Self, Self>

