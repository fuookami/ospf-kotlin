package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*

/**
 * 用对称上下界约束横向力矩。Constrains lateral torque with symmetric upper and lower bounds.
 *
 * @property torque 已注册的横向力矩模型 / The registered lateral torque model
 * @property maxLateralImbalance 最大允许绝对横向力矩 / The maximum allowed absolute lateral torque
 */
class LateralImbalanceLimit(
    private val torque: Torque,
    private val maxLateralImbalance: Quantity<Flt64>,
    override val name: String = "lateral_imbalance_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val lateralTorque = torque.lateralTorque.value.toLinearPolynomial()
        when (val result = model.addConstraint(
            relation = lateralTorque leq maxLateralImbalance.value,
            name = "${name}_maximum"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = model.addConstraint(
            relation = -lateralTorque leq maxLateralImbalance.value,
            name = "${name}_minimum"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }
}
