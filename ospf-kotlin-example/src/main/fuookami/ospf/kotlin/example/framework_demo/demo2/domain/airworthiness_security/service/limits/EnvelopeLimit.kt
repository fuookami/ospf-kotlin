package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*

class EnvelopeLimit(
    private val torque: Torque,
    private val envelopes: Map<FlightPhase, List<AbstractEnvelope>>,
    override val name: String = "envelope_limit"
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        for ((phase, thisEnvelopes) in envelopes) {
            val index = torque.index[phase]!!
            for (envelope in thisEnvelopes) {
                when (val result = model.addConstraint(
                    relation = index.value leq envelope.maxIndex.value,
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
                    relation = index.value geq envelope.minIndex.value,
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
















