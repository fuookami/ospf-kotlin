package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
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
 * @property torque 提供各飞行阶段CG指数的力矩模型 / The torque model providing CG index per flight phase
 * @property envelopes 各飞行阶段的包络线边界 / The envelope bounds per flight phase
*/
class EnvelopeLimit(
    private val torque: Torque,
    private val envelopes: Map<FlightPhase, List<AbstractEnvelope>>,
    override val name: String = "envelope_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((phase, thisEnvelopes) in envelopes) {
            val longitudinalTorque = torque.longitudinalTorque[phase]!!
            for (envelope in thisEnvelopes) {
                when (val result = model.addConstraint(
                    relation = longitudinalTorque.value leq envelope.maxIndex.value,
                    name = "${name}_${envelope.name}_ub"
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
                    relation = longitudinalTorque.value geq envelope.minIndex.value,
                    name = "${name}_${envelope.name}_lb"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}
