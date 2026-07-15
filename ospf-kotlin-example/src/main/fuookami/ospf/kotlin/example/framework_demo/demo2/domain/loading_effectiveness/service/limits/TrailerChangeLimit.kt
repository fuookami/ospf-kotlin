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

/**
 * Minimizes trailer changes between adjacent positions for ordered trailer pairs.
 * 最小化有序拖车对在相邻位置之间的拖车更换。
 *
 * @property adjacentPositions The list of adjacent position pairs. / 相邻位置对列表
 * @property orderedTrailers The list of ordered trailer pairs. / 有序拖车对列表
 * @property loading The trailer loading model providing change symbols. / 提供更换符号的拖车装载模型
 * @property coefficient Function computing the penalty coefficient for each trailer-position pair. / 计算每个拖车-位置对惩罚系数的函数
*/
class TrailerChangeLimit(
    private val adjacentPositions: List<PositionPair>,
    private val orderedTrailers: List<Pair<Trailer, Trailer>>,
    private val loading: TrailerLoading,
    private val coefficient: (Pair<Position, Trailer>, Pair<Position, Trailer>) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "trailer_change_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            sum(orderedTrailers.flatMapIndexed { p1, (trailer1, trailer2) ->
                adjacentPositions.mapIndexed { p2, (position1, position2) ->
                    coefficient(position2 to trailer1, position1 to trailer2) * loading.trailerChange[p1, p2]
                }
            }),
            "trailer change"
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

