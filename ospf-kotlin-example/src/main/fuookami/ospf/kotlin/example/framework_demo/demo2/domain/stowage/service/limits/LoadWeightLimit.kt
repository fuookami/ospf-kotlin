package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Enforces the maximum load weight constraint for each position.
 * 强制执行每个位置的最大装载重量约束。
 *
 * @property positions the list of available stowage positions / 可用装载位置列表
 * @property load the load decision variables / 装载决策变量
 * @property maxLoadWeight the maximum load weight limits per position / 每个位置的最大装载重量限制
 */
class LoadWeightLimit(
    private val positions: List<Position>,
    private val load: Load,
    private val maxLoadWeight: MaxLoadWeight,
    override val name: String = "load_weight_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j, position) in positions.withIndex()) {
            if (position.status.available) {
                when (val result = model.addConstraint(
            relation = load.estimateLoadWeight[j].value leq maxLoadWeight.maxLoadWeight[j].value,
            name = "${name}_${position}"
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
