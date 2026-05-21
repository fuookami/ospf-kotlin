package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.functional.sumOf

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> castDistanceValue(value: Any): V {
    // 安全不变量：距离运算在同一 V 数域中闭包，sqrt/pow 结果与输入域一致。
    // Safety invariant: distance operations are closed in the same V domain, so sqrt/pow results are V-compatible.
    return value as V
}

sealed interface Distance {
    operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V

    data object Euclidean : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            return castDistanceValue((lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).sqr() }).sqrt())
        }
    }

    data object Manhattan : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            return lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).abs() }
        }
    }

    class Minkowski(val p: Int) : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            val v = lhs[0]
            val sum = lhs.indices.sumOf(v.constants) { (lhs[it] - rhs[it]).abs().pow(p) }
            val exponentValue = run {
                var result = v.constants.zero
                for (i in 0 until p) { result += v.constants.one }
                result
            }
            return castDistanceValue(sum.pow(v.constants.one / exponentValue))
        }
    }

    data object Chebyshev : Distance {
        override operator fun <D : Dimension, V : FloatingNumber<V>> invoke(lhs: Point<D, V>, rhs: Point<D, V>): V {
            return (lhs.indices.maxOf { (lhs[it] - rhs[it]).abs() })
        }
    }
}
