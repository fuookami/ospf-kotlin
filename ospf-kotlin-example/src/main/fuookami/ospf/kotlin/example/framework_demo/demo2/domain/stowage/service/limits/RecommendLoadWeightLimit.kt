package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Enforces recommended load weight limits for positions requiring recommended weight validation.
 * 强制执行需要推荐重量验证的位置的推荐装载重量限制。
 *
 * @property positions 可用装载位置列表 / the list of available stowage positions
 * @property load 装载决策变量 / the load decision variables
*/
class RecommendLoadWeightLimit(
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "recommend_load_weight_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j, position) in positions.withIndex()) {
            if ((position.status.stowageNeeded || position.status.adjustmentNeeded)
                && position.status.recommendedWeightNeeded
            ) {
                val maxLoadWeight = position.mlw.mlw.value
                when (val result = model.addConstraint(
            relation = (load.z[j].value + maxLoadWeight * load.actualLoaded[j]) leq maxLoadWeight,
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
