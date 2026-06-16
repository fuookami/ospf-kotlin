package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

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
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*

/** Computes the Mean Aerodynamic Chord (MAC) percentage as a linear intermediate symbol. */
class MAC(
    private val aircraftModel: AircraftModel,
    private val formula: Formula,
    private val totalWeight: TotalWeight,
    private val torque: Torque
) {
    lateinit var mac: LinearIntermediateSymbol<Flt64>

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::mac.isInitialized) {
            val tow = totalWeight.computedTotalWeight[FlightPhase.TakeOff]
            mac = if (tow != null) {
                val index = torque.index[FlightPhase.TakeOff]!!
                LinearExpressionSymbol(
                    formula.mac(
                        Quantity(
                            LinearPolynomial(
                                monomials = listOf(LinearMonomial(Flt64.one, index.value)),
                                constant = Flt64.zero
                            ),
                            index.unit
                        ),
                        tow
                    ),
                    name = "mac"
                )
            } else {
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = "mac"
                )
            }
        }
        when (val result = model.add(mac)) {
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
