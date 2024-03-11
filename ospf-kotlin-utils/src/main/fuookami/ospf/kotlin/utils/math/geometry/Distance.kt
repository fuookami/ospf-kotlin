package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

sealed interface Distance {
    operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64

    data object Euclidean : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.sumOf { (lhs[it] - rhs[it]).sqr() }).sqrt()
        }
    }

    data object Manhattan : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return lhs.indices.sumOf { (lhs[it] - rhs[it]).abs() }
        }
    }

    class Minkowski(val p: Int) : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.sumOf { (lhs[it] - rhs[it]).pow(p) }).pow(Flt64(1.0 / p.toDouble())) as Flt64
        }
    }

    data object Chebyshev : Distance {
        override operator fun <D : Dimension> invoke(lhs: Point<D>, rhs: Point<D>): Flt64 {
            return (lhs.indices.maxOf { (lhs[it] - rhs[it]).abs() })
        }
    }
}
