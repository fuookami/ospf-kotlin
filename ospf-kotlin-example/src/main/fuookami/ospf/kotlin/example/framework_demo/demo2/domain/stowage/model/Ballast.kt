package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

class Ballast(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    val ballastPositions: List<Position>,
    val minBallastWeight: Quantity<Flt64>?,
    val adviceBallastWeight: Quantity<Flt64>?,
    val load: Load
) {
    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            positions: List<Position>,
            minBallastWeight: Quantity<Flt64>?,
            load: Load
        ): Ballast {
            TODO("not implemented yet")
        }
    }

    lateinit var ballastWeight: QuantityLinearIntermediateSymbol
    lateinit var adaptiveMinBallastWeight: QuantityLinearIntermediateSymbol

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::ballastWeight.isInitialized) {
            val poly = MutableLinearPolynomial()
            for (position in ballastPositions) {
                val j = positions.indexOf(position)
                poly += LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
            }
            ballastWeight = Quantity(
                LinearExpressionSymbol(
                    poly,
                    name = "ballast_weight"
                ),
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(ballastWeight)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        if (stowageMode.withSoftSecurity) {
            if (!::adaptiveMinBallastWeight.isInitialized) {
                adaptiveMinBallastWeight = if (minBallastWeight != null) {
                    Quantity(
                        LinearExpressionSymbol(
                            minBallastWeight.to(aircraftModel.weightUnit)!!.value,
                            name = "min_ballast_weight"
                        ),
                        aircraftModel.weightUnit
                    )
                } else {
                    val poly = MutableLinearPolynomial()
                    // todo
                    Quantity(
                        LinearExpressionSymbol(
                            poly,
                            name = "min_ballast_weight"
                        ),
                        aircraftModel.weightUnit
                    )
                }
            }
            when (val result = model.add(adaptiveMinBallastWeight)) {
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
}











