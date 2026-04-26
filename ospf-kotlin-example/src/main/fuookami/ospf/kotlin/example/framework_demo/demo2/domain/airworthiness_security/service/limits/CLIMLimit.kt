package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*

class CLIMLimit(
    private val torque: Torque,
    private val maxCLIM: MaxCLIM,
    override val name: String = "max_clim_limit"
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        val upper = MutableLinearPolynomial()
        upper += LinearMonomial(Flt64.one, torque.clim.value)
        upper += LinearMonomial(-Flt64.one, maxCLIM.maxCLIM.value)
        when (val result = model.addConstraint(
            relation = LinearPolynomial(upper.monomials, upper.constant) leq Flt64.zero,
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

        val lower = MutableLinearPolynomial()
        lower += LinearMonomial(Flt64.one, torque.clim.value)
        lower += LinearMonomial(Flt64.one, maxCLIM.maxCLIM.value)
        when (val result = model.addConstraint(
            relation = LinearPolynomial(lower.monomials, lower.constant) geq Flt64.zero,
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


















