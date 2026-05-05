package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import java.util.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class TotalWeightLimit(
    private val totalWeight: TotalWeight,
    override val name: String = "total_weight_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (phase in FlightPhase.entries) {
            when (val result = model.addConstraint(
            relation = LinearPolynomial(totalWeight.estimateTotalWeight[phase]!!.value) leq totalWeight.maxTotalWeight[phase]!!.value,
            name = "${name}_${phase.name.lowercase(Locale.getDefault())}"
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

        return ok
    }
}



















