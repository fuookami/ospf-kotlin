@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*

class BunchCompilationContext : fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext<
    ShadowPriceArguments, FlightTaskBunch, FlightTask, Aircraft, FlightTaskAssignment
> {
    override lateinit var aggregation: Aggregation
    override lateinit var pipelineList: CGPipelineList

    override fun register(model: AbstractLinearMetaModelF64): Try {
        pipelineList = when (val result = PipelineListGenerator(aggregation)()) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }

    override fun <Map : ShadowPriceMap> selectFreeExecutors(
        fixedBunches: Set<FlightTaskBunch>,
        hiddenExecutors: Set<Aircraft>,
        shadowPriceMap: Map,
        model: AbstractLinearMetaModelF64
    ): Ret<Set<Aircraft>> {
        TODO("Not yet implemented")
    }
}










