package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

class BunchCostMinimization<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val compilation: BunchCompilation<*, T, E, A>,
    override val name: String = "bunch_cost_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        when (val result = model.minimize(
            compilation.bunchCost,
            "bunch cost"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
