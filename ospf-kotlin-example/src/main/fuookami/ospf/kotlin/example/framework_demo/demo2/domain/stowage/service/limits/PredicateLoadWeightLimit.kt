package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class PredicateLoadWeightLimit(
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "recommend_load_weight_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j, position) in positions.withIndex()) {
            if ((position.status.stowageNeeded || position.status.adjustmentNeeded)
                && position.status.predicateWeightNeeded
            ) {
                val maxLoadWeight = position.mlw.mlw.value
                val poly = MutableLinearPolynomial()
                poly += LinearMonomial(Flt64.one, load.y[j].value)
                poly += LinearMonomial(maxLoadWeight, load.actualLoaded[j])
                when (val result = model.addConstraint(
            relation = LinearPolynomial(poly.monomials, poly.constant) leq maxLoadWeight,
            name = "recommend_load_weight_limit_${position}",
                )) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        return Failed(result.error)
                    }

                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}




















