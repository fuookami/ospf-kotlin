package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 约束每个限制区内左右位置之间的不对称线性密度。Constrains unsymmetrical linear density between left and right positions within each limit zone.
 *
 * @property aircraftModel The aircraft model providing unit configuration / 提供单位配置的飞机型号
 * @property maxUnsymmetricalLinearDensity The maximum unsymmetrical linear density limits / 最大不对称线性密度限制
 * @property linearDensity The linear density estimation model / 线性密度估算模型
 * @property positions The list of cargo positions / 货物位置列表
*/
class UnsymmetricalLinearDensityLimit(
    private val aircraftModel: AircraftModel,
    private val maxUnsymmetricalLinearDensity: MaxUnsymmetricalLinearDensity,
    private val linearDensity: LinearDensity,
    private val positions: List<Position>,
    override val name: String = "unsymmetrical_linear_density_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (zone in maxUnsymmetricalLinearDensity.limitZones) {
            for (line in zone.lines) {
                if (line.positions.none { it.status.available }) {
                    continue
                }

                assert(line.positions.size == 2)
                for ((l, limit) in zone.limits.withIndex()) {
                    val poly = MutableLinearPolynomial()
                    if (limit.leftCoefficient != null) {
                        val j = positions.indexOf(line.positions.find { it.coordinate.onLeft })
                        poly += LinearMonomial(
                            limit.leftCoefficient,
                            linearDensity.linearDensity[j].value
                        )
                    }
                    if (limit.rightCoefficient != null) {
                        val j = positions.indexOf(line.positions.find { it.coordinate.onRight })
                        poly += LinearMonomial(
                            limit.rightCoefficient,
                            linearDensity.linearDensity[j].value
                        )
                    }
                    when (val result = model.addConstraint(
                        relation = LinearPolynomial(poly.monomials, poly.constant) leq limit.maxSum.to(aircraftModel.linearDensityUnit)!!.value,
                        name = "${name}_${line.zone.name}_${line.arm.value}_${l}"
                    )) {
                        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                            return Failed(result.error)
                        }

                        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                            return Fatal(result.errors)
                        }
                    }
                }
            }
        }

        return ok
    }
}
