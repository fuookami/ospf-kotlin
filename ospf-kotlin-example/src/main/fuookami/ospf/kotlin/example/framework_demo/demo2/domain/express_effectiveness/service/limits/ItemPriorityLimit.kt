package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Maximizes the total priority score by matching items to positions based on absolute order.
 * 通过基于绝对顺序将项目匹配到位置来最大化总优先级分数。
 *
 * @property items The list of cargo items. / 货物项列表
 * @property positions The list of stowage positions. / 配载位置列表
 * @property unloading The absolute order model for priority coefficients. / 绝对顺序优先级系数模型
 * @property stowage The stowage assignment model. / 配载分配模型
 * @property coefficient Function computing the priority coefficient for each item. / 计算每个货物项优先级系数的函数
*/
class ItemPriorityLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val unloading: AbsoluteOrder,
    private val stowage: Stowage,
    private val coefficient: (Item) -> Flt64 = { Flt64.one },
    override val name: String = "item_priority_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.maximize(
            sum(items.flatMapIndexed { i, item ->
                positions.mapIndexed { j, position ->
                    coefficient(item) * unloading(item.cargo.priority, position) * stowage.stowage[i, j]
                }
            }),
            name = "item priority"
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}
