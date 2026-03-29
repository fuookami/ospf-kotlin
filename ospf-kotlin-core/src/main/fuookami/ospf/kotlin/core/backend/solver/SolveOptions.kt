package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.backend.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.core.frontend.model.status.ModelBuildingStatusCallBack
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64

data class SolveOptions(
    val solutionAmount: UInt64? = null,
    val modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null,
    val solvingStatusCallBack: SolvingStatusCallBack? = null,
    val valueConversionPolicy: SolveValueConversionPolicy? = null
) {
    val effectiveValueConversionPolicy: SolveValueConversionPolicy
        get() = valueConversionPolicy ?: SolveValueConversionPolicy.AllowRounding

    class Builder {
        var solutionAmount: UInt64? = null
        var modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null
        var solvingStatusCallBack: SolvingStatusCallBack? = null
        var valueConversionPolicy: SolveValueConversionPolicy? = null

        fun build(): SolveOptions {
            return SolveOptions(
                solutionAmount = solutionAmount,
                modelBuildingStatusCallBack = modelBuildingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                valueConversionPolicy = valueConversionPolicy
            )
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }

        fun build(block: Builder.() -> Unit): SolveOptions {
            return Builder().apply(block).build()
        }
    }
}
