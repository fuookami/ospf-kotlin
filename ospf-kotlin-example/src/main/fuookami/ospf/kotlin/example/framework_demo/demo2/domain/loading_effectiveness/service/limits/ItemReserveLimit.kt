package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Minimizes the number of unloaded (reserved) items, encouraging more items to be loaded.
 * 最小化未装载（预留）的货物项数量，鼓励装载更多货物。
 *
 * @property items The list of cargo items. / 货物项列表
 * @property stowage The stowage assignment model. / 配载分配模型
 * @property coefficient Function computing the penalty coefficient for each item. / 计算每个货物项惩罚系数的函数
*/
class ItemReserveLimit(
    private val items: List<Item>,
    private val stowage: Stowage,
    private val coefficient: (Item) -> Flt64 = { Flt64.one },
    override val name: String = "item_reserve_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = MutableLinearPolynomial()
        for ((i, item) in items.withIndex()) {
            val c = coefficient(item)
            poly += c
            poly += LinearMonomial(-c, stowage.loaded[i])
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "item reserve"
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

