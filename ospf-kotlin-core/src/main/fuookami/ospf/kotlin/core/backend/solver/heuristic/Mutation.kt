package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface Mutation<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRate: Flt64
    ): Solution
}

//interface MutationOperation {
//    operator fun invoke(
//        chromosome: Chromosome,
//        mutationRate: Flt64,
//        model: CallBackModelInterface
//    ): Chromosome?
//}
//
//class UniformMutationOperation(
//    val rng: Random
//) : MutationOperation {
//    override fun invoke(chromosome: Chromosome, mutationRate: Flt64, model: CallBackModelInterface): Chromosome? {
//        val newGene = chromosome.gene.toMutableList()
//        var flag = false
//        for (i in newGene.indices) {
//            if (Flt64(rng.nextDouble()) ls mutationRate) {
//                flag = true
//                newGene[i] = model.tokens.find(i)!!.random(rng)
//            }
//        }
//
//        return if (flag) {
//            Chromosome(
//                fitness = if (model.constraintSatisfied(newGene) != false) {
//                    model.objective(newGene)
//                } else {
//                    null
//                },
//                gene = newGene
//            )
//        } else {
//            null
//        }
//    }
//}
//
//class NonUniformMutationOperation(
//    val rng: Random
//) : MutationOperation {
//    override fun invoke(chromosome: Chromosome, mutationRate: Flt64, model: CallBackModelInterface): Chromosome? {
//        TODO("Not yet implemented")
//    }
//}
//
//class GaussianMutationOperation(
//    val rng: Random
//) : MutationOperation {
//    override fun invoke(chromosome: Chromosome, mutationRate: Flt64, model: CallBackModelInterface): Chromosome? {
//        TODO("Not yet implemented")
//    }
//}
