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
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Maximizes the adjacency of same-destination cargos across adjacent positions.
 * 最大化相邻位置上同目的地货物的相邻性。
 *
 * @property adjacentPositions 相邻位置对列表 / The list of adjacent position pairs.
 * @property destinations 目的地 IATA 代码列表 / The list of destination IATA codes.
 * @property loading 提供相邻性符号的转运装载模型 / The transfer adjacent loading model providing adjacency symbols.
 * @property coefficient 计算每个目的地-位置对奖励系数的函数 / Function computing the reward coefficient for each destination-position pair.
*/
class SameDestinationAdjacent(
    private val adjacentPositions: List<PositionPair>,
    private val destinations: List<IATA>,
    private val loading: TransferAdjacentLoading,
    private val coefficient: (IATA, Position, Position) -> Flt64 = { _, _, _ -> Flt64.one },
    override val name: String = "same_destination_adjacent_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.maximize(
            sum(destinations.flatMapIndexed { d, destination ->
                adjacentPositions.mapIndexed { p, (position1, position2) ->
                    coefficient(destination, position1, position2) * loading.sameDestinationAdjacent[d, p]
                }
            }),
            "same destination adjacent",
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

