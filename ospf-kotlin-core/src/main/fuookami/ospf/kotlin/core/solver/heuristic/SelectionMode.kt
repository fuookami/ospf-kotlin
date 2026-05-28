/**
 * 选择模式接口与实现
 * Selection mode interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.minMaxWithPartialThreeWayComparatorOrNull
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.operator.pow
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 选择模式接口，定义如何从种群中选择个体数量。
 * Selection mode interface, defining how to select the number of individuals from the population.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
interface SelectionMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: Population<T, ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): UInt64
}

/**
 * 静态选择模式，使用密度范围下界作为选择数量。
 * Static selection mode, using density range lower bound as selection count.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
class StaticSelectionMode<ObjValue, V>() : SelectionMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: Population<T, ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): UInt64 {
        return population.densityRange.lowerBound.value.unwrap()
    }
}

/**
 * 自适应动态选择模式，根据适应度和密度动态调整选择数量。
 * Adaptive dynamic selection mode, dynamically adjusting selection count based on fitness and density.
 */
data object AdaptiveDynamicSelectionMode : SelectionMode<Flt64, Flt64> {
    override operator fun <T : Individual<Flt64, Flt64>> invoke(
        iteration: Iteration,
        population: Population<T, Flt64, Flt64>,
        model: AbstractCallBackModelInterface<*, Flt64, Flt64>
    ): UInt64 {
        val fitnessParameter = calculateFitnessParameter(population, model)
            ?: return population.densityRange.lowerBound.value.unwrap()
        val densityParameter = calculateDensityParameter(population)
        val density = (Flt64(population.individuals.size) * fitnessParameter * densityParameter).ceil().toUInt64()

        return if (density gr population.densityRange.upperBound.value.unwrap()) {
            population.densityRange.upperBound.value.unwrap()
        } else if (density ls population.densityRange.lowerBound.value.unwrap()) {
            density * (population.densityRange.upperBound.value.unwrap() / density + UInt64.one)
        } else {
            density / (density / population.densityRange.lowerBound.value.unwrap() + UInt64.one)
        }
    }

    private fun <T : Individual<Flt64, Flt64>> calculateFitnessParameter(
        population: Population<T, Flt64, Flt64>,
        model: AbstractCallBackModelInterface<*, Flt64, Flt64>
    ): Flt64? {
        val a = Flt64(0.1)
        val b = Flt64(0.6)
        val (minFitness, maxFitness) = population.individuals
            .map { it.fitness }
            .minMaxWithPartialThreeWayComparatorOrNull { lhs, rhs -> model.compareObjective(lhs, rhs) }
            ?: return null
        val x = abs(maxFitness - minFitness) / max(minFitness, maxFitness)
        return if (x < ((b + Flt64.three * a) / Flt64(4))) {
            (Flt64.three * b + a) / (Flt64(4) * b)
        } else if (x > ((Flt64.three * b + a) / Flt64(4))) {
            Flt64.one
        } else {
            val i = -(Flt64(16) * a * (a + b) + Flt64(24) * a - Flt64(24) * b) / (b * pow(a + b, 3))
            val j = a / b
            i * pow(x, 3) + j * x + Flt64(0.5) + (b + Flt64.three * a) / (Flt64(8) * b)
        }
    }

    private fun <T : Individual<Flt64, Flt64>> calculateDensityParameter(population: Population<T, Flt64, Flt64>): Flt64 {
        val a = population.densityRange.upperBound.value.unwrap().toFlt64()
        val b = population.densityRange.lowerBound.value.unwrap().toFlt64()
        return if (UInt64(population.density) < ((b + Flt64.three * a) / Flt64(4)).ceil().toUInt64()) {
            Flt64.one
        } else if (UInt64(population.density) > ((Flt64.three * b + a) / Flt64(4)).ceil().toUInt64()) {
            (b + Flt64.three * a) / (Flt64(4) * b)
        } else {
            val i = (Flt64(16) * a * (a + b) + Flt64(24) * a - Flt64(24) * b) / (b * pow(a + b, 3))
            val j = -(a / b)
            val x = (Flt64(population.density) + (a + b) / Flt64(4))
            (i * pow(x, 3) + j * x + Flt64(0.5) + (b + Flt64.three * a) / (Flt64(8) / b)) /
                    (Flt64(population.density) / Flt64(4))
        }
    }
}
