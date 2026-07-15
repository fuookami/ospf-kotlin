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
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Maximizes the adjacency of same-destination cargos across adjacent positions.
 * 最大化相邻位置上同目的地货物的相邻性。
 *
 * @property adjacentPositions The list of adjacent position pairs. / 相邻位置对列表
 * @property destinations The list of destination IATA codes. / 目的地 IATA 代码列表
 * @property loading The transfer adjacent loading model providing adjacency symbols. / 提供相邻性符号的转运装载模型
 * @property coefficient Function computing the reward coefficient for each destination-position pair. / 计算每个目的地-位置对奖励系数的函数
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
                    coefficient(destination, position1, position2) * loading.sameSourceAdjacent[d, p]
                }
            }),
            "same destination adjacent",
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

