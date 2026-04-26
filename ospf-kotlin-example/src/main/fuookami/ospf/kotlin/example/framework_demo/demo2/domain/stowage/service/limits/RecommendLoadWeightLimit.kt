package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class RecommendLoadWeightLimit(
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "recommend_load_weight_limit",
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        for ((j, position) in positions.withIndex()) {
            if ((position.status.stowageNeeded || position.status.adjustmentNeeded)
                && position.status.recommendedWeightNeeded
            ) {
                val maxLoadWeight = position.mlw.mlw.value
                val poly = MutableLinearPolynomial()
                poly += LinearMonomial(Flt64.one, load.z[j].value)
                poly += LinearMonomial(maxLoadWeight, load.actualLoaded[j])
                when (val result = model.addConstraint(
            relation = LinearPolynomial(poly.monomials, poly.constant) leq maxLoadWeight,
            name = "recommend_load_weight_limit_${position}",
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





















