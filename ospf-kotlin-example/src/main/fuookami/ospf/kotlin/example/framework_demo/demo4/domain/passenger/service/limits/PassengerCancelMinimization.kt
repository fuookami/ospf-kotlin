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
 * 最小化乘客取消加权和的管线。Pipeline minimizing the weighted sum of passenger cancellations.
 *
 * @property passengers List of flight-passenger associations / 航班乘客关联列表
 * @property cancel Passenger cancel component / 乘客取消组件
 * @property coefficient Weight coefficient per passenger / 每位乘客的权重系数
*/
class PassengerCancelMinimization(
    private val passengers: List<FlightPassenger>,
    private val cancel: PassengerCancel,
    private val coefficient: (FlightPassenger) -> Flt64 = { _ -> Flt64.one },
    override val name: String = "passenger_cancel_minimization"
) : CGPipeline {

    /**
     * 向模型添加乘客取消最小化目标。Adds the passenger cancel minimization objective to the model.
     *
     * @param model The linear meta model to add objective to / 要添加目标的线性元模型
     * @return Registration result / 注册结果
    */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = MutableLinearPolynomial()
        for (passenger in passengers) {
            poly += LinearMonomial(coefficient(passenger), cancel.passengerCancel[passenger])
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "passenger cancel"
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
