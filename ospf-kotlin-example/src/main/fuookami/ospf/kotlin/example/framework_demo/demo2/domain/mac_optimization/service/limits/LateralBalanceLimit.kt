package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model.*

class LateralBalanceLimit(
    private val aircraftModel: AircraftModel,
    private val lateralBalance: LateralBalance,
    private val coefficient: () -> Flt64,
    override val name: String = "longitudinal_balance_limit"
) : Pipeline<AbstractLinearMetaModelFlt64> {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        val poly = MutableLinearPolynomial()
        poly += LinearMonomial(
            coefficient(),
            lateralBalance.slack.value
        )
        when (val result = model.minimize(
            LinearPolynomial(poly.monomials, poly.constant),
            name = "longitudinal balance"
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

















