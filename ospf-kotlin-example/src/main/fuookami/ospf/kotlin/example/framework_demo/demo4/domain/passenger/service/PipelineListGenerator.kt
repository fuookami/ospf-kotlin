@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 包含容量、取消和变更约束的乘客管线列表生成器。Generator for the passenger pipeline list including capacity, cancel, and change constraints.
 *
 * @property private val aggregation 参数。
 */
class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    /**
     * Creates and returns the pipeline list with all passenger constraints and objectives.
 *
     * @return 返回结果。
     */
    operator fun invoke(): Ret<CGPipelineList> {
        val pipelines = ArrayList<CGPipeline>()

        pipelines.add(
            PassengerFlightCapacityConstraint(
                flights = aggregation.flights,
                amount = aggregation.amount,
                capacity = aggregation.capacity
            )
        )

        pipelines.add(
            PassengerRouteCancelConstraint(
                passengers = aggregation.passengers,
                cancel = aggregation.cancel
            )
        )

        pipelines.add(
            PassengerFlightChangeConstraint(
                timeWindow = aggregation.timeWindow,
                passengers = aggregation.passengers,
                time = aggregation.time,
                change = aggregation.change
            )
        )

        pipelines.add(
            PassengerCancelMinimization(
                passengers = aggregation.passengers,
                cancel = aggregation.cancel,
                coefficient = { _ ->
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            PassengerClassChangeMinimization(
                passengers = aggregation.passengers,
                change = aggregation.change,
                coefficient = { _, _ ->
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            PassengerFlightChangeMinimization(
                passengers = aggregation.passengers,
                change = aggregation.change,
                coefficient = { _, _ , _->
                    TODO("not implemented yet")
                }
            )
        )

        return Ok(pipelines)
    }
}
