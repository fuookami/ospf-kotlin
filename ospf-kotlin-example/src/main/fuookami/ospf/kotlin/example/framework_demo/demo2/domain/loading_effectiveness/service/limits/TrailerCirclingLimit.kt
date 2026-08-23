package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.error.*
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Minimizes trailer circling, where items from the same trailer are loaded in reverse order across adjacent positions.
 * 最小化拖车环绕，即同一拖车上的货物在相邻位置以相反顺序装载的情况。
 *
 * @property orderedItemsInTrailers 同一拖车内的有序货物项对列表 / The list of ordered item pairs within the same trailer.
 * @property adjacentPositions 相邻位置对列表 / The list of adjacent position pairs.
 * @property loading 提供环绕符号的拖车装载模型 / The trailer loading model providing circling symbols.
 * @property coefficient 计算每个货物项-位置对惩罚系数的函数 / Function computing the penalty coefficient for each item-position pair.
*/
class TrailerCirclingLimit(
    private val orderedItemsInTrailers: List<ItemPair>,
    private val adjacentPositions: List<PositionPair>,
    private val loading: TrailerLoading,
    private val coefficient: (Pair<Position, Item>, Pair<Position, Item>) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "trailer_circling_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val monomials = orderedItemsInTrailers.flatMapIndexed { p1, (item1, item2) ->
            adjacentPositions.mapIndexed { p2, (position1, position2) ->
                coefficient(position2 to item1, position1 to item2) * loading.trailerCircling[p1, p2]
            }
        }
        if (monomials.isEmpty()) {
            return ok
        }

        when (val result = model.minimize(
            sum(monomials),
            "trailer circling"
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}

