package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.functional.sumOf

sealed interface Distance {
    operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V

    data object Euclidean : Distance {
        @Suppress("UNCHECKED_CAST")
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            return (lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).sqr() }).sqrt() as V
        }
    }

    data object Manhattan : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            return lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).abs() }
        }
    }

    class Minkowski(val p: Int) : Distance {
        @Suppress("UNCHECKED_CAST")
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            val sum = lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).abs().pow(p) }
            val pV = run {
                var result = v.constants.zero
                for (i in 0 until p) { result += v.constants.one }
                result
            }
            return sum.pow(v.constants.one / pV) as V
        }
    }

    data object Chebyshev : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            return (lhs.indices.maxOf { (lhs[it] - rhs[it]).abs() })
        }
    }
}
