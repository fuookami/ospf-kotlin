@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 强制取消沿乘客路线传播的管线。Pipeline enforcing that cancellation propagates along passenger routes.
 *
 * @property private val passengers 参数。
 * @property private val cancel 参数。
 */
class PassengerRouteCancelConstraint(
    private val passengers: List<FlightPassenger>,
    private val cancel: PassengerCancel,
    override val name: String = "passenger_route_cancel_constraint"
) : CGPipeline {
    /**
     * Adds route cancel constraints ensuring previous legs cancel before subsequent ones.
 *
     * @param model 参数。
     * @return 返回结果。
     */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (passenger in passengers) {
            val prev = passenger.prev
            if (prev != null) {
                when (val result = model.addConstraint(
                    cancel.passengerCancel[prev] leq cancel.passengerCancel[passenger],
                    name = "passenger_route_cancel_constraint_${passenger}"
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
