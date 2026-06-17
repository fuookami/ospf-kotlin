package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits

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
 * 约束位置之间的载荷重量以均衡推荐重量分布。Constrains load weights between positions to equalize recommended weight distribution.
 *
 * @property private val aircraftModel 参数。
 * @property private val positions 参数。
 * @property private val load 参数。
 * @property override val name 参数。
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

                val rhs1 = MutableLinearPolynomial()
                rhs1 += LinearMonomial(Flt64.one, load.z[j2].value)
                rhs1 += LinearMonomial(position1.mlw.mlw.value, load.actualLoaded[j2])
                when (val result = model.addConstraint(
                    relation = load.z[j1].value leq LinearPolynomial(rhs1.monomials, rhs1.constant),
                    name = "${name}_${position1}_${position2}"
                )) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
                }

                val rhs2 = MutableLinearPolynomial()
                rhs2 += LinearMonomial(Flt64.one, load.z[j1].value)
                rhs2 += LinearMonomial(position2.mlw.mlw.value, load.actualLoaded[j1])
                when (val result = model.addConstraint(
                    relation = load.z[j2].value leq LinearPolynomial(rhs2.monomials, rhs2.constant),
                    name = "${name}_${position2}_${position1}"
                )) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
