package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Maximizes the estimated payload as an objective function with an optional reward coefficient.
 * 将估计载荷作为目标函数最大化（带可选奖励系数）。
 *
 * @property aircraftModel 飞机模型引用 / The aircraft model reference
 * @property payload 待最大化的载荷 / The payload to be maximized
 * @property coefficient 目标函数的奖励系数函数 / The reward coefficient function for the objective
*/
class MaxPayloadLimit(
    private val aircraftModel: AircraftModel,
    private val payload: Payload,
    private val coefficient: () -> Flt64 = { Flt64.one },
    override val name: String = "max_payload_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val objectiveCoefficient = coefficient()
        val payloadPolynomial = payload.estimatePayload.value.polynomial
        val objective = LinearPolynomial(
            monomials = payloadPolynomial.monomials.map {
                LinearMonomial(objectiveCoefficient * it.coefficient, it.symbol)
            },
            constant = objectiveCoefficient * payloadPolynomial.constant
        )
        when (val result = model.maximize(
            objective,
            name = name
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
