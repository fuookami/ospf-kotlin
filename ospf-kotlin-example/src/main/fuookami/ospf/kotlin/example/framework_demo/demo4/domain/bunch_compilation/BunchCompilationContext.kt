@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 管理聚合和管线注册的批次编译上下文。Context for bunch compilation managing aggregation and pipeline registration. */
class BunchCompilationContext : fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext<
    ShadowPriceArguments, FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment
> {
    override lateinit var aggregation: Aggregation
    override lateinit var pipelineList: CGPipelineList

    /**
     * Registers the pipeline list and delegates to the parent registration.
 *
     * @param model 参数。
     * @return 返回结果。
     */
    override fun register(model: AbstractLinearMetaModel<Flt64>): Try {
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

    /** 为分支定价算法基于影子价格选择空闲执行器。Selects free executors based on shadow prices for the branch-and-price algorithm. */
    override fun <Map : ShadowPriceMap> selectFreeExecutors(
        fixedBunches: Set<FlightTaskBunch>,
        hiddenExecutors: Set<Aircraft>,
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<Aircraft>> {
        TODO("Not yet implemented")
    }
}
