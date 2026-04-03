package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

interface Dimension {
    val size: Int
    val indices get() = 0 until size
}

data object Dim1 : Dimension {
    override val size = 1
}

data object Dim2 : Dimension {
    override val size = 2
}

data object Dim3 : Dimension {
    override val size = 3
}




