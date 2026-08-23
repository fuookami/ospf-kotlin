package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import java.util.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
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
 * 约束每个飞行阶段的总飞机重量不超过最大允许值。Constrains the total aircraft weight for each flight phase to not exceed the maximum allowed.
 *
 * @property totalWeight 各飞行阶段的总重量估算与限制 / The total weight estimation and limits per flight phase
*/
class TotalWeightLimit(
    private val totalWeight: TotalWeight,
    override val name: String = "total_weight_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (phase in FlightPhase.entries) {
            val estimateTotalWeight = totalWeight.estimateTotalWeight[phase]
                ?: return Failed(
                    ErrorCode.ApplicationFailed,
                    "总重表达式缺失：${phase.name} / Total weight expression is missing: ${phase.name}"
                )
            val maximumTotalWeight = totalWeight.maxTotalWeight[phase] ?: continue

            when (val result = model.addConstraint(
                relation = LinearPolynomial(estimateTotalWeight.value) leq maximumTotalWeight.value,
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
