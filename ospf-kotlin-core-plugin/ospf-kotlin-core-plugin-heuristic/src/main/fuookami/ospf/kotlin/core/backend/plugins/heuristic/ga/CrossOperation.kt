package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface CrossMode {
    operator fun invoke(
        population: Population,
        modelInterface: CallBackModelInterface
    ): UInt64
}

data object OneParentCrossMode : CrossMode {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
        TODO("Not yet implemented")
    }
}

data object TwoParentCrossMode : CrossMode {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
        TODO("Not yet implemented")
    }
}

data object MultiParentCrossMode : CrossMode {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
        TODO("Not yet implemented")
    }
}

data object AdaptiveMultiParentCrossMode : CrossMode {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
        TODO("Not yet implemented")
    }
}

interface CrossOperation {
    operator fun invoke(
        population: Population,
        modelInterface: CallBackModelInterface
    ): List<Chromosome>
}

data object OnePointCrossOperation : CrossOperation {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}

data object TwoPointCrossOperation : CrossOperation {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}

data object MultiPointCrossOperation : CrossOperation {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}

data object UniformCrossOperation : CrossOperation {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}

data object CycleCrossOperation : CrossOperation {
    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
        TODO("Not yet implemented")
    }
}
