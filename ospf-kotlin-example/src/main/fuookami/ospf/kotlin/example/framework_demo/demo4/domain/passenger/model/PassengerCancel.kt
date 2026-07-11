@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/**
 * 跟踪列生成公式的乘客取消变量。Tracks passenger cancellation variables for the column generation formulation.
 *
 * @property passengers List of flight-passenger associations / 航班乘客关联列表
*/
class PassengerCancel(
    private val passengers: List<FlightPassenger>
) {
    lateinit var passengerCancel: UIntVariable1

    /**
     * 将取消变量注册到模型中。Registers cancellation variables with the model.
     *
     * @param model The linear meta model to register with / 要注册的线性元模型
     * @return Registration result / 注册结果
    */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::passengerCancel.isInitialized) {
            passengerCancel = UIntVariable1(
                "passenger_cancel",
                Shape1(passengers.size)
            )
            for (passenger in passengers) {
                passengerCancel[passenger].range.leq(passenger.amount)
            }
        }
        when (val result = model.add(passengerCancel)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}
