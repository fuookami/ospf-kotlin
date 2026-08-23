package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * 约束总载荷不超过最大允许载荷。Constrains the total payload to not exceed the maximum allowed payload.
 *
 * @property payload 载荷估算与最大载荷限制 / The payload estimation and maximum payload limit
 * @property minPayload 最小总业载要求 / The minimum total payload requirement
*/
class PayloadLimit(
    private val payload: Payload,
    private val minPayload: Quantity<Flt64>,
    override val name: String = "payload_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.addConstraint(
            relation = payload.estimatePayload.value leq payload.maxPayload.value,
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

        when (val result = model.addConstraint(
            relation = payload.estimatePayload.value geq minPayload.value,
            name = "${name}_minimum"
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
