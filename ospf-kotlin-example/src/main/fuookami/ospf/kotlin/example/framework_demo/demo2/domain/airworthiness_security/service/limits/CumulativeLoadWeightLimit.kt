package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 约束沿机身每个检查点的累积载荷重量。Constrains cumulative load weight at each checkpoint along the fuselage.
 *
 * @property aircraftModel 提供单位配置的飞机型号 / The aircraft model providing unit configuration
 * @property maxCumulativeLoadWeight 各检查点的最大累积载荷重量限制 / The maximum cumulative load weight limits at checkpoints
 * @property positions 货物位置列表 / The list of cargo positions
 * @property load 载荷估算模型 / The load estimation model
*/
class CumulativeLoadWeightLimit(
    private val aircraftModel: AircraftModel,
    private val maxCumulativeLoadWeight: MaxCumulativeLoadWeight,
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "cumulative_load_weight_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (checkPoint in maxCumulativeLoadWeight.checkPoints) {
            if (checkPoint.parts.none { it.position.status.available }) {
                continue
            }

            val poly = sum(checkPoint.parts.map { part ->
                val j = positions.indexOf(part.position)
                part.weight * load.estimateLoadWeight[j].value
            })
            when (val result = model.addConstraint(
                relation = poly leq checkPoint.maxSum.to(aircraftModel.weightUnit)!!.value,
                name = "${name}_${checkPoint.zone.name}_${checkPoint.toArm.value}"
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
