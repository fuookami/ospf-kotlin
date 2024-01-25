package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface PopulationCommunicationOperation {
    operator fun invoke(
        populations: List<Population>,
        model: CallBackModelInterface
    ): List<Population>
}

class RandomPopulationCommunicationOperation(
    val rng: Random
) : PopulationCommunicationOperation {
    override fun invoke(populations: List<Population>, model: CallBackModelInterface): List<Population> {
        TODO("Not yet implemented")
    }
}

class BetterToWorsePopulationCommunicationOperation(
    val rng: Random
) : PopulationCommunicationOperation {
    override fun invoke(populations: List<Population>, model: CallBackModelInterface): List<Population> {
        TODO("Not yet implemented")
    }
}

class MoreToLessPopulationCommunicationOperation(
    val rng: Random
) : PopulationCommunicationOperation {
    override fun invoke(populations: List<Population>, model: CallBackModelInterface): List<Population> {
        TODO("Not yet implemented")
    }
}
