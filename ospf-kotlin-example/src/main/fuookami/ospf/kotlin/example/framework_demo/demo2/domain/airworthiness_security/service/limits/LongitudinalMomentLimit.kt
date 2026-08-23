package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*

/**
 * 将每个飞行阶段的纵向力矩限制在给定区间。Constrains the longitudinal moment of every flight phase to a supplied interval.
 *
 * @property torque 已注册的纵向力矩模型 / The registered longitudinal torque model
 * @property min 区间下界 / The interval lower bound
 * @property max 区间上界 / The interval upper bound
 */
class LongitudinalMomentLimit(
    private val torque: Torque,
    private val min: Quantity<Flt64>,
    private val max: Quantity<Flt64>,
    override val name: String
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (phase in FlightPhase.entries) {
            val moment = torque.longitudinalTorque[phase]!!
            when (val result = model.addConstraint(
                relation = moment.value geq min.value,
                name = "${name}_${phase.name.lowercase()}_minimum"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = model.addConstraint(
                relation = moment.value leq max.value,
                name = "${name}_${phase.name.lowercase()}_maximum"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }
}
