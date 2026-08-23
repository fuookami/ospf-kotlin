@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 最小化乘客航班变更加权和的管线。Pipeline minimizing the weighted sum of passenger flight changes.
 *
 * @property passengers 航班乘客关联列表 / List of flight-passenger associations
 * @property change 乘客变更组件 / Passenger change component
 * @property coefficient 每对航班和舱位的权重系数 / Weight coefficient per flight pair and class
*/
class PassengerFlightChangeMinimization(
    private val passengers: List<FlightPassenger>,
    private val change: PassengerChange,
    private val coefficient: (FlightTask, FlightTask, PassengerClass) -> Flt64 = { _, _, _ -> Flt64.one },
    override val name: String = "passenger_flight_change_minimization"
) : CGPipeline {

    /**
     * 向模型添加乘客航班变更最小化目标。Adds the passenger flight change minimization objective to the model.
     *
     * @param model 要添加目标的线性元模型 / The linear meta model to add objective to
     * @return 注册结果 / Registration result
    */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = sum(passengers.flatMap { passer ->
            (change.toFlights[passer.flight] ?: emptyList()).flatMap { toFlight ->
                PassengerClass.entries.map { cls ->
                    coefficient(passer.flight, toFlight, cls) * change.passengerFlightChange[passer, toFlight, cls]!!
                }
            }
        })
        when (val result = model.minimize(
            LinearExpressionSymbol(poly),
            name = "passenger flight change"
        )) {
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
