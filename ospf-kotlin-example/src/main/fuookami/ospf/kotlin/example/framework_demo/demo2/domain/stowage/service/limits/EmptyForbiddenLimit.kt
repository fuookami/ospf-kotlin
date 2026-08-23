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
 * Enforces that positions marked as empty-forbidden must be loaded.
 * 强制执行标记为禁止空载的位置必须被装载。
 *
 * @property items 待装载的货物项目列表 / the list of cargo items to be stowed
 * @property positions 可用装载位置列表 / the list of available stowage positions
 * @property load 装载决策变量 / the load decision variables
*/
class EmptyForbiddenLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "empty_forbidden_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j, position) in positions.withIndex()) {
            if (position.status.available && position.type.contains(PositionTypeCode.EmptyForbidden)) {
                when (val result = model.addConstraint(
                    relation = load.estimateLoaded[j] eq true,
                    name = "${name}_${position}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                    return result
                }

                is Fatal -> {
                    return result
                }
                }
            }
        }

        return ok
    }
}
