/**
 * 多样性算子 / Diversity operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.diversity

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.fractal.correlationDimension

/**
 * 多样性度量算子接口，用于监测种群是否塌缩。 / Diversity metric interface used to monitor population collapse.
*/
interface DiversityMetric {

    /**
     * 计算种群多样性 / Compute population diversity
     *
     * @param V 值类型 / Value type
     * @param solutions 解集合 / Solution set
     * @param converter 值转换器 / Value converter
     * @return 多样性度量值 / Diversity measure
    */
    operator fun <V> invoke(
        solutions: List<Solution<V>>,
        converter: IntoValue<V>
    ): Flt64 where V : RealNumber<V>, V : NumberField<V>
}

/** 平均成对欧氏距离 / Mean pairwise Euclidean distance */
data object MeanPairwiseDistance : DiversityMetric {
    override operator fun <V> invoke(
        solutions: List<Solution<V>>,
        converter: IntoValue<V>
    ): Flt64 where V : RealNumber<V>, V : NumberField<V> {
        if (solutions.size < 2) {
            return Flt64.zero
        }

        val dimension = solutions.first().size
        var accumulated = 0.0
        var pairs = 0L
        for (i in solutions.indices) {
            for (j in i + 1 until solutions.size) {
                var squared = 0.0
                for (k in 0 until dimension) {
                    val difference = converter.fromValue(solutions[i][k]).toDouble() -
                        converter.fromValue(solutions[j][k]).toDouble()
                    squared += difference * difference
                }
                accumulated += sqrt(squared)
                ++pairs
            }
        }
        return if (pairs == 0L) {
            Flt64.zero
        } else {
            Flt64(accumulated / pairs.toDouble())
        }
    }
}

/** 用相关维数（分形维数）度量种群多样性，可直接驱动自适应步长 / Measures population diversity by the correlation dimension (a fractal dimension), directly usable to drive adaptive step sizes */
data object FractalDimensionDiversity : DiversityMetric {
    override operator fun <V> invoke(
        solutions: List<Solution<V>>,
        converter: IntoValue<V>
    ): Flt64 where V : RealNumber<V>, V : NumberField<V> {
        val converted = solutions.map { solution ->
            solution.map { converter.fromValue(it) }
        }
        return when (val result = correlationDimension(converted)) {
            is Ok -> result.value
            else -> Flt64.zero
        }
    }
}
