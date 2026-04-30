package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class EmptyForbiddenLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val load: Load,
    override val name: String = "empty_forbidden_limit"
) : Pipeline<AbstractLinearMetaModelFlt64> {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        for ((j, position) in positions.withIndex()) {
            if (position.status.available && position.type.contains(PositionTypeCode.EmptyForbidden)) {
                when (val result = model.addConstraint(
                    relation = load.estimateLoaded[j] eq true,
                    name = "${name}_${position}"
                )) {
                    is Ok<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }

                is Fatal<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }
                }
            }
        }

        return ok
    }
}


















