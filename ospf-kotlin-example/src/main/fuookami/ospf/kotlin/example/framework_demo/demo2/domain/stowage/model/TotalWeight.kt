package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

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













