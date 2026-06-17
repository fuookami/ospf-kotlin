package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 约束每个限制区的表面密度到最大允许值。Constrains surface density per limit zone to the maximum allowed value.
 *
 * @property private val surfaceDensity 参数。
 * @property private val positions 参数。
 * @property override val name 参数。
 */
class SurfaceDensityLimit(
    private val surfaceDensity: SurfaceDensity,
    private val positions: List<Position>,
    override val name: String = "surface_density_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (limitZone in surfaceDensity.limitsZones) {
            for ((j, position) in positions.withIndex()) {
                if (position.status.unavailable) {
                    continue
                }

                if (position.coordinate.withIntersectionWith(limitZone.frontArm, limitZone.backArm)) {
                    when (val result = model.addConstraint(
            relation = LinearPolynomial(surfaceDensity.surfaceDensity[j].value) leq limitZone.maxSurfaceDensity.value,
            name = "${name}_${limitZone.name}_${position}"
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
