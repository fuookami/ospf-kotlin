package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*

/**
 * 约束 CG 指数裕度（CLIM）在最大允许边界内。Constrains the CG index margin (CLIM) to be within the maximum allowed bounds.
 *
 * @property torque 提供CG指数裕度的力矩模型 / The torque model providing CG index margin
 * @property maxCLIM 最大CG指数裕度限制 / The maximum CG index margin limit
*/
class CLIMLimit(
    private val torque: Torque,
    private val maxCLIM: MaxCLIM,
    override val name: String = "max_clim_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.addConstraint(
            relation = (torque.clim.value - maxCLIM.maxCLIM.value) leq Flt64.zero,
            name = "${name}_ub"
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
            relation = (torque.clim.value + maxCLIM.maxCLIM.value) geq Flt64.zero,
            name = "${name}_lb"
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
