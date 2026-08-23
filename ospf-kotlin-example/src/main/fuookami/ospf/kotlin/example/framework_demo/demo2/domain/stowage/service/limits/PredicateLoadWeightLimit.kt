package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.error.*
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
 * Enforces predicate-based load weight limits for positions requiring predicate weight validation.
 * 强制执行需要谓词重量验证的位置的基于谓词的装载重量限制。
 *
 * @property positions 可用装载位置列表 / the list of available stowage positions
 * @property load 装载决策变量 / the load decision variables
*/
class PredicateLoadWeightLimit(
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "predicate_load_weight_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j, position) in positions.withIndex()) {
            if (position.status.predicateWeightNeeded) {
                val predicateLoadWeight = position.plw ?: return Failed(Err(
                    ErrorCode.IllegalArgument,
                    "舱位 ${position.spaceName} 缺少谓词装载重量规格 / " +
                        "Position ${position.spaceName} is missing predicate load weight specification."
                ))
                val maxLoadWeight = predicateLoadWeight.max.value
                when (val result = model.addConstraint(
                    relation = load.y[j].value leq maxLoadWeight,
                    name = "${name}_${position}",
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
