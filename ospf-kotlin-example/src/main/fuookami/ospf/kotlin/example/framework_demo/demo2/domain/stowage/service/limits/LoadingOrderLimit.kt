package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Enforces loading order constraints between adjacent non-bulk positions.
 * 强制执行相邻非散货位置之间的装载顺序约束。
 *
 * @property positions 可用装载位置列表 / the list of available stowage positions
 * @property neighbours 相邻位置对列表 / the list of adjacent position pairs
 * @property load 装载决策变量 / the load decision variables
*/
class LoadingOrderLimit(
    private val positions: List<Position>,
    private val neighbours: List<Neighbour>,
    private val load: Load,
    override val name: String = "loading_order_limit"
): Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (neighbour in neighbours) {
            val j1 = positions.indexOfFirst { it.base == neighbour.pair.first }
            val position1 = positions[j1]
            val j2 = positions.indexOfFirst { it.base == neighbour.pair.second }
            val position2 = positions[j2]

            if (position1.status.unavailable
                || position2.status.unavailable
                || position1.location.bulk
                || position2.location.bulk
            ) {
                continue
            }

            when (val result = model.addConstraint(
                load.actualLoaded[j1] geq load.actualLoaded[j2],
                name = "${name}_${position1}_${position2}"
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

        return ok
    }
}
