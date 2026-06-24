package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model.*

/**
 * 最小化按系数加权的实验纵向平衡松弛。Minimizes the experimental longitudinal balance slack weighted by a coefficient.
 *
 * @property aircraftModel 参数。
 * @property longitudinalBalance 参数。
 * @property coefficient 参数。
 */
class ExperimentalLongitudinalBalanceLimit(
    private val aircraftModel: AircraftModel,
    private val longitudinalBalance: ExperimentalLongitudinalBalance,
    private val coefficient: () -> Flt64,
    override val name: String = "experimental_longitudinal_balance_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            LinearMonomial(coefficient(), longitudinalBalance.longitudinalTorqueSlack.value),
            "experimental longitudinal balance"
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}
