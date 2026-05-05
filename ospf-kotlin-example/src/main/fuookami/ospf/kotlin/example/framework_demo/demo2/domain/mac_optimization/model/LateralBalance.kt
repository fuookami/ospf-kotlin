package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class LateralBalance(
    private val aircraftModel: AircraftModel,
    private val torque: Torque
) {
    lateinit var slack: QuantityLinearIntermediateSymbol

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::slack.isInitialized) {
            slack = Quantity(
                LinearExpressionSymbol(name = "lateral_balance_slack"),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(slack)) {
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






