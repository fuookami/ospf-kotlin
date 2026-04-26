package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*

class HorizontalStabilizerLimit(
    private val horizontalStabilizers: Map<HorizontalStabilizer.Key, HorizontalStabilizer>,
    private val coefficient: (HorizontalStabilizer.Key) -> Flt64,
    override val name: String = "horizontal_stabilizer_limit"
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        for ((key, hs) in horizontalStabilizers) {
            when (val result = model.minimize(
                coefficient(key) * hs.warnSlack,
                "horizontal stabilizer $key"
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


















