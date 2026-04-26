package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class LoadWeightLimit(
    private val positions: List<Position>,
    private val load: Load,
    private val maxLoadWeight: MaxLoadWeight,
    override val name: String = "load_weight_limit"
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        for ((j, position) in positions.withIndex()) {
            if (position.status.available) {
                when (val result = model.addConstraint(
            relation = load.estimateLoadWeight[j].value leq maxLoadWeight.maxLoadWeight[j].value,
            name = "${name}_${position}"
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




















