package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model.*

/**
 * 最小化宽体飞机按系数加权的横向平衡松弛。Minimizes lateral balance slack weighted by a coefficient for wide-body aircraft.
 *
 * @property aircraftModel 参数。
 * @property lateralBalance 参数。
 * @property coefficient 参数。
 */
class LateralBalanceLimit(
    private val aircraftModel: AircraftModel,
    private val lateralBalance: LateralBalance,
    private val coefficient: () -> Flt64,
    override val name: String = "longitudinal_balance_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = MutableLinearPolynomial()
        poly += LinearMonomial(
            coefficient(),
            lateralBalance.slack.value
        )
        when (val result = model.minimize(
            LinearPolynomial(poly.monomials, poly.constant),
            name = "longitudinal balance"
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
