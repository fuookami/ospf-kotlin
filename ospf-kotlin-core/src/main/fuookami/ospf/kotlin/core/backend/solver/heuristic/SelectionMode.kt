package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface SelectionMode<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: Population<T, V>,
        model: AbstractCallBackModelInterface<*, V>
    ): UInt64
}

class StaticSelectionMode<V>() : SelectionMode<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: Population<T, V>,
        model: AbstractCallBackModelInterface<*, V>
    ): UInt64 {
        return population.densityRange.lowerBound.value.unwrap()
    }
}

data object AdaptiveDynamicSelectionMode : SelectionMode<Flt64> {
    override operator fun <T : Individual<Flt64>> invoke(
        iteration: Iteration,
        population: Population<T, Flt64>,
        model: AbstractCallBackModelInterface<*, Flt64>
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

    private fun <T : Individual<Flt64>> calculateFitnessParameter(
        population: Population<T, Flt64>,
        model: AbstractCallBackModelInterface<*, Flt64>
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
            val i = -(Flt64(16) * a * (a + b) + Flt64(24) * a - Flt64(24) * b) / (b * pow(a + b, 3, Flt64))
            val j = a / b
            i * pow(x, 3, Flt64) + j * x + Flt64(0.5) + (b + Flt64.three * a) / (Flt64(8) * b)
        }
    }

    private fun <T : Individual<Flt64>> calculateDensityParameter(population: Population<T, Flt64>): Flt64 {
        val a = population.densityRange.upperBound.value.unwrap().toFlt64()
        val b = population.densityRange.lowerBound.value.unwrap().toFlt64()
        return if (UInt64(population.density) < ((b + Flt64.three * a) / Flt64(4)).ceil().toUInt64()) {
            Flt64.one
        } else if (UInt64(population.density) > ((Flt64.three * b + a) / Flt64(4)).ceil().toUInt64()) {
            (b + Flt64.three * a) / (Flt64(4) * b)
        } else {
            val i = (Flt64(16) * a * (a + b) + Flt64(24) * a - Flt64(24) * b) / (b * pow(a + b, 3, Flt64))
            val j = -(a / b)
            val x = (Flt64(population.density) + (a + b) / Flt64(4))
            (i * pow(x, 3, Flt64) + j * x + Flt64(0.5) + (b + Flt64.three * a) / (Flt64(8) / b)) /
                    (Flt64(population.density) / Flt64(4))
        }
    }
}
