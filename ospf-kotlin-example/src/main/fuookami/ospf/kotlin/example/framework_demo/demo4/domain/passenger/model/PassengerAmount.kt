@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 跟踪每个航班和舱位的乘客数量表达式，考虑取消和舱位/航班变更。Tracks passenger amount expressions per flight and class,
 * accounting for cancellations and class/flight changes.
 *
 * @property flights 航班任务列表 / List of flight tasks
 * @property passengers 按航班分组的乘客 / Passengers grouped by flight
 * @property cancel 乘客取消组件 / Passenger cancel component
 * @property change 乘客变更组件 / Passenger change component
*/
class PassengerAmount(
    private val flights: List<FlightTask>,
    private val passengers: Map<FlightTask, List<FlightPassenger>>,
    private val cancel: PassengerCancel,
    private val change: PassengerChange
) {
    lateinit var passengerAmount: Map<FlightTask, Map<PassengerClass, LinearIntermediateSymbol<Flt64>>>

    /**
     * 将乘客数量表达式注册到模型中。Registers passenger amount expressions with the model.
     *
     * @param model 要注册的线性元模型 / The linear meta model to register with
     * @return 注册结果 / Registration result
    */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::passengerAmount.isInitialized) {
            passengerAmount = flights.associateWith { flight ->
                PassengerClass.entries.associateWith { cls ->
                    var poly = LinearPolynomial()
                    for (passenger in (passengers[flight] ?: emptyList())) {
                        if (passenger.cls == cls) {
                            poly += passenger.amount.toFlt64()
                        }
                        poly -= Flt64.one * cancel.passengerCancel[passenger]
                        if (passenger.cls == cls) {
                            poly -= sum(change.passengerClassChange[passenger, _a])
                            poly -= sum(change.passengerFlightChange[passenger, _a, _a])
                        } else {
                            poly += Flt64.one * change.passengerClassChange[passenger, cls]!!
                        }
                    }
                    for (passenger in passengers.values.flatten()) {
                        val toFlights = change.toFlights[passenger.flight] ?: emptyList()
                        if (toFlights.contains(flight)) {
                            poly += Flt64.one * change.passengerFlightChange[passenger, flight, cls]!!
                        }
                    }
                    LinearExpressionSymbol(
                        poly,
                        name = "passenger_amount_${flight}_${cls}"
                    )
                }
            }
        }
        when (val result = model.add(passengerAmount)) {
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
