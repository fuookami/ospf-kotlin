package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.math.operator.Minus

interface TotallyOrdered<in Self> : Ord<Self>

interface VectorSpace<Self, Scalar> : Plus<Self, Self>, Minus<Self, Self> {
    fun scale(rhs: Scalar): Self
}

interface NormedSpace<Self, Scalar> : VectorSpace<Self, Scalar> {
    val norm: Scalar
    val unit: Self
}

interface InnerProductSpace<Self, Scalar> : NormedSpace<Self, Scalar> {
    infix fun dot(rhs: Self): Scalar
}
