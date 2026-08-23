package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 约束每条限制线的线性密度到最大允许值。Constrains linear density per limit line to the maximum allowed value.
 *
 * @property aircraftModel 提供单位配置的飞机型号 / The aircraft model providing unit configuration
 * @property linearDensity 线性密度估算与限制 / The linear density estimation and limits
 * @property positions 货物位置列表 / The list of cargo positions
*/
class LinearDensityLimit(
    private val aircraftModel: AircraftModel,
    private val linearDensity: LinearDensity,
    private val positions: List<Position>,
    override val name: String = "linear_density_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (line in linearDensity.limitLines) {
            if (line.positions.none { it.status.available }) {
                continue
            }

            val poly = sum(line.positions.map { position ->
                val j = positions.indexOf(position)
                linearDensity.linearDensity[j].value
            })
            when (val result = model.addConstraint(
                relation = poly leq line.zone.maxLinearDensity.to(aircraftModel.linearDensityUnit)!!.value,
                name = "${name}_${line.zone.name}_${line.arm.value}"
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
