package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

/**
 *
 */
interface SelectionMode {
    /**
     * calculate population density
     *
     * @param population
     * @param model
     * @return
     */
    operator fun invoke(
        population: Population,
        model: CallBackModelInterface
    ): UInt64
}

data object StaticSelectionMode : SelectionMode {
    override operator fun invoke(
        population: Population,
        model: CallBackModelInterface
    ): UInt64 {
        return population.densityRange.lowerBound.value.unwrap()
    }
}

data object AdaptiveDynamicSelectionMode : SelectionMode {
    override operator fun invoke(
        population: Population,
        model: CallBackModelInterface
    ): UInt64 {
        val fitnessParameter = calculateFitnessParameter(population, model)
            ?: return population.densityRange.lowerBound.value.unwrap()
        val densityParameter = calculateDensityParameter(population)
        val density = (Flt64(population.chromosomes.size) * fitnessParameter * densityParameter).ceil().toUInt64()

        return if (density > population.densityRange.upperBound.value.unwrap()) {
            population.densityRange.upperBound.value.unwrap()
        } else if (density < population.densityRange.lowerBound.value.unwrap()) {
            density * (population.densityRange.upperBound.value.unwrap() / density + UInt64.one)
        } else {
            density / (density / population.densityRange.lowerBound.value.unwrap() + UInt64.one)
        }
    }

    private fun calculateFitnessParameter(
        population: Population,
        model: CallBackModelInterface
    ): Flt64? {
        val a = Flt64(0.1)
        val b = Flt64(0.6)
        val (minFitness, maxFitness) = population.chromosomes
            .mapNotNull { it.fitness }
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

    private fun calculateDensityParameter(population: Population): Flt64 {
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

interface SelectionOperation {
    operator fun invoke(
        population: Population,
        model: CallBackModelInterface
    ): List<Chromosome>
}

class RouletteSelectionOperation(
    val rng: Random
) : SelectionOperation {
    override fun invoke(
        population: Population,
        model: CallBackModelInterface
    ): List<Chromosome> {
        TODO("Not yet implemented")
    }
}

data object RankSelectionOperation : SelectionOperation {
    override fun invoke(population: Population, model: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}

class TournamentSelectionOperation(
    val rng: Random,
    val eliteAmount: UInt64,
    val groupMinAmount: UInt64
) : SelectionOperation {
    override fun invoke(population: Population, model: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}
