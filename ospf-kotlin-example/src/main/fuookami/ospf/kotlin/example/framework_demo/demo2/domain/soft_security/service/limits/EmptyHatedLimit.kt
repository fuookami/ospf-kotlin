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
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Penalizes empty positions that are marked as empty-hated to discourage leaving them empty.
 * 对标记为空载厌恶的位置施加惩罚以避免留空。
 *
 * @property positions 装载位置列表 / The list of stowage positions
 * @property load 载荷分布数据 / The load distribution data
 * @property coefficient 每个位置的惩罚系数函数 / The penalty coefficient function per position
*/
class EmptyHatedLimit(
    private val positions: List<Position>,
    private val load: Load,
    private val coefficient: (Position) -> Flt64 = { Flt64.one },
    override val name: String = "empty_hated_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = sum(positions.withIndex().mapNotNull { (j, position) ->
            if (position.type.contains(PositionTypeCode.EmptyHated)) {
                val c = coefficient(position)
                (-c * load.full[j]) + c
            } else {
                null
            }
        })
        when (val result = model.minimize(
            LinearExpressionSymbol(poly),
            name = "empty hated"
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
