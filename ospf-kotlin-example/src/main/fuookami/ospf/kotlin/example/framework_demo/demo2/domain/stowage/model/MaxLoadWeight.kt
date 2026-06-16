package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.multiarray.*

import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.quantities.quantity.*

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*

class MaxLoadWeight(
    private val aircraftModel: AircraftModel,
    private val fuselage: Fuselage,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val totalWeight: TotalWeight
) {
    lateinit var maxLoadWeight: QuantityLinearIntermediateSymbols1<Flt64>

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::maxLoadWeight.isInitialized) {
            maxLoadWeight = QuantityLinearIntermediateSymbols1<Flt64>("max_load_weight", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val symbol: LinearIntermediateSymbol<Flt64> = if (position.mlw.segments.isNotEmpty()) {
                    val zfw = totalWeight.computedTotalWeight[FlightPhase.ZeroFuel]
                    if (zfw != null) {
                        LinearExpressionSymbol(
                            LinearPolynomial(position.mlw(zfw).to(aircraftModel.weightUnit)!!.value),
                            name = "max_load_weight_${position}"
                        )
                    } else {
                        LinearExpressionSymbol(
                            LinearPolynomial(position.mlw.mlw.to(aircraftModel.weightUnit)!!.value),
                            name = "max_load_weight_${position}",
                        )
                    }
                } else {
                    LinearExpressionSymbol(
                        LinearPolynomial(position.mlw.mlw.to(aircraftModel.weightUnit)!!.value),
                        name = "max_load_weight_${position}"
                    )
                }
                Quantity(
                    symbol,
                    aircraftModel.weightUnit
                )
            }
        }
        when (val result = model.add(maxLoadWeight)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
        }

        return ok
    }
}