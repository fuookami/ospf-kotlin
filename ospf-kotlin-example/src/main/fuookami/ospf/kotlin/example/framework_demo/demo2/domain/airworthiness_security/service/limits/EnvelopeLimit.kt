package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*

/**
 * 约束 CG 指数在每个飞行阶段的包络线最小/最大边界内。Constrains the CG index to stay within the envelope min/max bounds for each flight phase.
 *
 * @property private val torque 参数。
 * @property private val envelopes 参数。
 * @property override val name 参数。
 */
class EnvelopeLimit(
    private val torque: Torque,
    private val envelopes: Map<FlightPhase, List<AbstractEnvelope>>,
    override val name: String = "envelope_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((phase, thisEnvelopes) in envelopes) {
            val index = torque.index[phase]!!
            for (envelope in thisEnvelopes) {
                when (val result = model.addConstraint(
                    relation = index.value leq envelope.maxIndex.value,
                    name = "${name}_${envelope.name}_ub"
                )) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        return Failed(result.error)
                    }

                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        return Fatal(result.errors)
                    }
                }

                when (val result = model.addConstraint(
                    relation = index.value geq envelope.minIndex.value,
                    name = "${name}_${envelope.name}_lb"
                )) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        return Failed(result.error)
                    }

                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}
