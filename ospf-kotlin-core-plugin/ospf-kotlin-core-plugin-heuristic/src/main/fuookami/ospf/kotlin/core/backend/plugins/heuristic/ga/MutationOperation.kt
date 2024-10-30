package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import kotlin.random.Random

interface MutationMode {
    /**
     * calculate mutation rate
     *
     * @param population
     * @param model
     * @return
     */
    operator fun invoke(
        population: Population,
        model: CallBackModelInterface
    ): Flt64
}

data object StaticMutationMode : MutationMode {
    override fun invoke(population: Population, model: CallBackModelInterface): Flt64 {
        return population.mutationRange.upperBound.value.unwrap()
    }
}

data object AdaptiveDynamicMutationMode : MutationMode {
    override fun invoke(population: Population, model: CallBackModelInterface): Flt64 {
        val (minFitness, maxFitness) = population.chromosomes
            .mapNotNull { it.fitness }
            .minMaxWithPartialThreeWayComparatorOrNull { lhs, rhs -> model.compareObjective(lhs, rhs) }
            ?: return population.mutationRange.upperBound.value.unwrap()
        val x = abs(maxFitness - minFitness) / max(minFitness, maxFitness)

        return if (x ls Flt64.decimalPrecision) {
            population.mutationRange.upperBound.value.unwrap()
        } else {
            return min(
                population.mutationRange.lowerBound.value.unwrap() * x,
                population.mutationRange.upperBound.value.unwrap()
            )
        }
    }
}

interface MutationOperation {
    operator fun invoke(
        chromosome: Chromosome,
        mutationRate: Flt64,
        model: CallBackModelInterface
    ): Chromosome?
}

class UniformMutationOperation(
    val rng: Random
) : MutationOperation {
    override fun invoke(chromosome: Chromosome, mutationRate: Flt64, model: CallBackModelInterface): Chromosome? {
        val newGene = chromosome.gene.toMutableList()
        var flag = false
        for (i in newGene.indices) {
            if (Flt64(rng.nextDouble()) ls mutationRate) {
                flag = true
                newGene[i] = model.tokens.find(i)!!.random(rng)
            }
        }

        return if (flag) {
            Chromosome(
                fitness = if (model.constraintSatisfied(newGene) != false) {
                    model.objective(newGene)
                } else {
                    null
                },
                gene = newGene
            )
        } else {
            null
        }
    }
}

class NonUniformMutationOperation(
    val rng: Random
) : MutationOperation {
    override fun invoke(chromosome: Chromosome, mutationRate: Flt64, model: CallBackModelInterface): Chromosome? {
        TODO("Not yet implemented")
    }
}

class GaussianMutationOperation(
    val rng: Random
) : MutationOperation {
    override fun invoke(chromosome: Chromosome, mutationRate: Flt64, model: CallBackModelInterface): Chromosome? {
        TODO("Not yet implemented")
    }
}
