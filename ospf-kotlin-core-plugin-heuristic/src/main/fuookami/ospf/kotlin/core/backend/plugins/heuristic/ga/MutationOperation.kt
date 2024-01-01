package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

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

data object StaticMutationMode: MutationMode {
    override fun invoke(population: Population, model: CallBackModelInterface): Flt64 {
        TODO("Not yet implemented")
    }
}

data object AdaptiveDynamicMutationMode: MutationMode {
    override fun invoke(population: Population, model: CallBackModelInterface): Flt64 {
        TODO("Not yet implemented")
    }
}

interface MutationOperation {
    operator fun invoke(
        chromosome: Chromosome,
        mutationRage: Flt64,
        model: CallBackModelInterface
    ): Chromosome?
}

data object UniformMutationOperation: MutationOperation {
    override fun invoke(chromosome: Chromosome, mutationRage: Flt64, model: CallBackModelInterface): Chromosome? {
        TODO("Not yet implemented")
    }
}

data object NonUniformMutationOperation: MutationOperation {
    override fun invoke(chromosome: Chromosome, mutationRage: Flt64, model: CallBackModelInterface): Chromosome? {
        TODO("Not yet implemented")
    }
}

data object GaussianMutationOperation: MutationOperation {
    override fun invoke(chromosome: Chromosome, mutationRage: Flt64, model: CallBackModelInterface): Chromosome? {
        TODO("Not yet implemented")
    }
}
