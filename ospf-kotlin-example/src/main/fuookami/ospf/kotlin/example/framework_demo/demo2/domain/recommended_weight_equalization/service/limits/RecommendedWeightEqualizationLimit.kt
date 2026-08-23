package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
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
 * Constrains load weights between positions to equalize recommended weight distribution.
 * 约束位置之间的载荷重量以均衡推荐重量分布。
 *
 * @property aircraftModel 飞机模型引用 / The aircraft model reference
 * @property positions 装载位置列表 / The list of stowage positions
 * @property load 载荷分布数据 / The load distribution data
*/
class RecommendedWeightEqualizationLimit(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "recommended_weight_equalization_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j1, position1) in positions.withIndex()) {
            if (!position1.status.recommendedWeightNeeded) {
                continue
            }

            for ((j2, position2) in positions.withIndex()) {
                if (j2 <= j1 || !position2.status.recommendedWeightNeeded) {
                    continue
                }

                when (val result = model.addConstraint(
                    relation = load.z[j1].value leq (
                        load.z[j2].value + position1.mlw.mlw.value * load.actualLoaded[j2]
                    ),
                    name = "${name}_${position1}_${position2}"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                when (val result = model.addConstraint(
                    relation = load.z[j2].value leq (
                        load.z[j1].value + position2.mlw.mlw.value * load.actualLoaded[j1]
                    ),
                    name = "${name}_${position2}_${position1}"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
