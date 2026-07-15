@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 强制每航班每舱位乘客数量不超过飞机容量的管线。Pipeline enforcing that passenger amounts per flight and class do not exceed aircraft capacity.
 *
 * @property flights List of flight tasks / 航班任务列表
 * @property amount Passenger amount component / 乘客数量组件
 * @property capacity Flight capacity / 航班容量
*/
class PassengerFlightCapacityConstraint(
    private val flights: List<FlightTask>,
    private val amount: PassengerAmount,
    private val capacity: FlightCapacity,
    override val name: String = "passenger_flight_capacity_constraint"
) : CGPipeline {

    /**
     * 向模型添加乘客航班容量约束。Adds passenger flight capacity constraints to the model.
     *
     * @param model The linear meta model to add constraints to / 要添加约束的线性元模型
     * @return Registration result / 注册结果
    */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (flight in flights) {
            for (cls in PassengerClass.entries) {
                when (val result = model.addConstraint(
                    amount.passengerAmount[flight, cls]!! leq capacity.passenger[flight, cls]!!,
                    name = "passenger_flight_capacity_constraint_${flight}_${cls}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}
