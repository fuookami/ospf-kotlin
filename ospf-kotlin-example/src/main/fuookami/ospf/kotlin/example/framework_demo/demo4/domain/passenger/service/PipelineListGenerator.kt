@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.Parameter
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 包含容量、取消和变更约束的乘客管线列表生成器。Generator for the passenger pipeline list including capacity, cancel, and change constraints.
 *
 * @property aggregation 乘客域聚合。Passenger domain aggregation.
 * @property parameter 列生成主模型系数参数。Column generation master model coefficient parameters.
 */
class PipelineListGenerator(
    private val aggregation: Aggregation,
    private val parameter: Parameter = Parameter()
) {
    /**
     * 创建并返回包含所有乘客约束和目标的管线列表。Creates and returns the pipeline list with all passenger constraints and objectives.
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
                    // Source: Parameter.passengerCancelCoeff — ported from fsra-proof passengerCancel.
                    parameter.passengerCancelCoeff
                }
            )
        )

        pipelines.add(
            PassengerClassChangeMinimization(
                passengers = aggregation.passengers,
                change = aggregation.change,
                coefficient = { _, _ ->
                    // Source: Parameter.passengerClassChangeCoeff — ported from fsra-proof passengerClassChangeBase.
                    parameter.passengerClassChangeCoeff
                }
            )
        )

        pipelines.add(
            PassengerFlightChangeMinimization(
                passengers = aggregation.passengers,
                change = aggregation.change,
                coefficient = { _, _, _ ->
                    // Source: Parameter.passengerFlightChangeCoeff — default 1.0 (uniform weight for flight change).
                    parameter.passengerFlightChangeCoeff
                }
            )
        )

        return Ok(pipelines)
    }
}
