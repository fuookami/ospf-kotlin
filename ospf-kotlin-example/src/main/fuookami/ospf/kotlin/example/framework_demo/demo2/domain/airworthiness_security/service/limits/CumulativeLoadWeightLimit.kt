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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 约束沿机身每个检查点的累积载荷重量。Constrains cumulative load weight at each checkpoint along the fuselage.
 *
 * @property private val aircraftModel 参数。
 * @property private val maxCumulativeLoadWeight 参数。
 * @property private val positions 参数。
 * @property private val load 参数。
 * @property override val name 参数。
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

            val poly = MutableLinearPolynomial()
            for (part in checkPoint.parts) {
                val j = positions.indexOf(part.position)
                poly += LinearMonomial(
                    part.weight,
                    load.estimateLoadWeight[j].value
                )
            }
            when (val result = model.addConstraint(
                relation = LinearPolynomial(poly.monomials, poly.constant) leq checkPoint.maxSum.to(aircraftModel.weightUnit)!!.value,
                name = "${name}_${checkPoint.zone.name}_${checkPoint.toArm.value}"
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

        return ok
    }
}
