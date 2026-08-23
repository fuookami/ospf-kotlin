package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service.limits

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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Minimizes empty loading patterns between adjacent positions with configurable coefficients.
 * 使用可配置系数最小化相邻位置之间的空载模式。
 *
 * @property adjacentPositions 相邻位置对列表 / The list of adjacent position pairs
 * @property divideEmptyLoading 带有中间符号的空载分割模型 / The divide-empty-loading model with intermediate symbols
 * @property emptyBetweenCargoCoefficient 货物之间空位惩罚的系数函数 / Coefficient function for empty-between-cargo penalties
 * @property emptyCargoBetweenCargoCoefficient 货物之间空货惩罚的系数函数 / Coefficient function for empty-cargo-between-cargo penalties
 * @property emptyBetweenEmptyCargoCoefficient 空货之间空位惩罚的系数函数 / Coefficient function for empty-between-empty-cargo penalties
*/
class DivideEmptyLoadingLimit(
    private val adjacentPositions: List<PositionPair>,
    private val divideEmptyLoading: DivideEmptyLoading,
    private val emptyBetweenCargoCoefficient: (Position, Position) -> Flt64 = { _, _ -> Flt64.one },
    private val emptyCargoBetweenCargoCoefficient: (Position, Position) -> Flt64 = { _, _ -> Flt64.one },
    private val emptyBetweenEmptyCargoCoefficient: (Position, Position) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "divide_empty_loading_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            sum(adjacentPositions.mapIndexed { p, (position1, position2) ->
                emptyBetweenCargoCoefficient(position1, position2) * divideEmptyLoading.emptyBetweenCargo[p]
            }),
            "empty between cargo"
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = model.minimize(
            sum(adjacentPositions.mapIndexed { p, (position1, position2) ->
                emptyCargoBetweenCargoCoefficient(position1, position2) * divideEmptyLoading.emptyCargoBetweenCargo[p]
            }),
            "empty cargo between cargo"
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = model.minimize(
            sum(adjacentPositions.mapIndexed { p, (position1, position2) ->
                emptyBetweenEmptyCargoCoefficient(position1, position2) * divideEmptyLoading.emptyBetweenEmptyCargo[p]
            })
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

