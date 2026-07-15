package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

/**
 * Total weight model managing estimated and actual total weight calculations
 * across different flight phases (takeoff, landing, zero-fuel).
 * 总重模型，管理不同飞行阶段（起飞、着陆、零油）的预估和实际总重计算。
 *
 * @property maxTotalWeight the maximum total weight per flight phase / 每个飞行阶段的最大总重
 * @property computedTotalWeight the computed total weight per flight phase, or empty if not yet computed / 每个飞行阶段的已计算总重，未计算时为空
*/
class TotalWeight(
    val maxTotalWeight: Map<FlightPhase, Quantity<Flt64>>,
    val computedTotalWeight: Map<FlightPhase, Quantity<Flt64>>,
    private val aircraftModel: AircraftModel,
    private val fuselage: Fuselage,
    private val fuel: Map<FlightPhase, FuelConstant>,
    private val payload: Payload
) {
    lateinit var estimateTotalWeight: Map<FlightPhase, QuantityLinearIntermediateSymbol<Flt64>>
    lateinit var actualTotalWeight: Map<FlightPhase, QuantityLinearIntermediateSymbol<Flt64>>

    /**
     * Registers total weight symbols into the model.
     * 将总重符号注册到模型中。
     *
     * @param model the linear meta-model to register into / 要注册到的线性元模型
     * @return success or failure / 成功或失败
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::estimateTotalWeight.isInitialized) {
            estimateTotalWeight = FlightPhase.entries.associateWith { phase ->
                val totalWeight = computedTotalWeight[phase]
                if (totalWeight != null) {
                    Quantity(
                        LinearExpressionSymbol(
                            LinearPolynomial(totalWeight.to(aircraftModel.weightUnit)!!.value),
                            name = "total_weight_${phase.name.lowercase()}"
                        ),
                        aircraftModel.weightUnit
                    )
                } else {
                    Quantity(
                        LinearExpressionSymbol(
                            totalWeight(phase, LinearPolynomial(payload.estimatePayload.to(aircraftModel.weightUnit)!!.value)),
                            name = "estimate_total_weight_${phase.name.lowercase()}",
                        ),
                        aircraftModel.weightUnit
                    )
                }
            }
        }
        estimateTotalWeight.values.forEach {
            when (val result = model.add(it)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::actualTotalWeight.isInitialized) {
            actualTotalWeight = FlightPhase.entries.associateWith { phase ->
                Quantity(
                    LinearExpressionSymbol(
                        totalWeight(phase, LinearPolynomial(payload.actualPayload.to(aircraftModel.weightUnit)!!.value)),
                        name = "actual_total_weight_${phase.name.lowercase()}",
                    ),
                    aircraftModel.weightUnit
                )
            }
        }
        actualTotalWeight.values.forEach {
            when (val result = model.add(it)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    /**
     * Computes the total weight polynomial for a given flight phase and payload.
     * 计算给定飞行阶段和业载的总重多项式。
     *
     * @param phase the flight phase / 飞行阶段
     * @param payload the payload polynomial / 业载多项式
     * @return the total weight polynomial / 总重多项式
    */
    private fun totalWeight(
        phase: FlightPhase,
        payload: LinearPolynomial<Flt64>
    ): LinearPolynomial<Flt64> {
        val poly = MutableLinearPolynomial()
        poly += payload
        poly += fuselage.dow.to(aircraftModel.weightUnit)!!.value
        poly += fuselage.liferaft?.weight?.let {
            it.to(aircraftModel.weightUnit)!!.value
        } ?: Flt64.zero
        when (phase) {
            FlightPhase.TakeOff, FlightPhase.Landing -> {
                poly += fuel[phase]!!.weight.to(aircraftModel.weightUnit)!!.value
            }

            FlightPhase.ZeroFuel -> {}
        }
        return LinearPolynomial(poly)
    }
}
