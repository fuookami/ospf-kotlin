@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.*

class PipelineListGenerator(
    private val aggregation: Aggregation
) {
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
