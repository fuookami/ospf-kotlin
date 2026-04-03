package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.math.operator.Div
import fuookami.ospf.kotlin.utils.math.operator.IntDiv
import fuookami.ospf.kotlin.utils.math.operator.Reciprocal
import fuookami.ospf.kotlin.utils.math.operator.Rem

interface MultiplicativeGroup<Self> : MultiplicativeMonoid<Self>,
    Reciprocal<Self>,
    Div<Self, Self>,
    IntDiv<Self, Self>,
    Rem<Self, Self>

