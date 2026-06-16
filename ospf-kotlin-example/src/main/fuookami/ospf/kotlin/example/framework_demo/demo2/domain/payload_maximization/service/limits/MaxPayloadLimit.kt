package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service.limits

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

import fuookami.ospf.kotlin.framework.model.*

/** Maximizes the estimated payload as an objective function with an optional reward coefficient. */
class MaxPayloadLimit(
    private val aircraftModel: AircraftModel,
    private val payload: Payload,
    private val coefficient: () -> Flt64 = { Flt64.one },
    override val name: String = "max_payload_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.maximize(
            LinearMonomial(coefficient(), payload.estimatePayload.value)
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
