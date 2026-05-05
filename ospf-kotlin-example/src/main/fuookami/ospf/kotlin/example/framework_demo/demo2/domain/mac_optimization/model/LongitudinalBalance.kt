package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class LongitudinalBalance(
    private val aircraftModel: AircraftModel,
    private val macRange: MACRange,
    private val torque: Torque
) {
    lateinit var slack: Map<MACRange.Type, QuantityLinearIntermediateSymbol>

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::slack.isInitialized) {
            slack = when (stowageMode) {
                StowageMode.FullLoad -> {
                    MACRange.Type.entries.associateWithNotNull { type ->
                        when (type) {
                            MACRange.Type.OPT, MACRange.Type.C -> {
                                null
                            }

                            else -> {
                                Quantity(
                                    LinearExpressionSymbol(name = "longitudinal_balance_slack_${type.name}"),
                                    aircraftModel.torqueUnit
                                )
                            }
                        }
                    }
                }

                StowageMode.Predistribution -> {
                    mapOf(
                        MACRange.Type.OPT to Quantity(
                            LinearExpressionSymbol(name = "longitudinal_balance_slack_opt"),
                            aircraftModel.torqueUnit
                        )
                    )
                }

                StowageMode.WeightRecommendation -> {
                    emptyMap()
                }
            }
        }
        slack.values.forEach {
            when (val result = model.add(it)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}










