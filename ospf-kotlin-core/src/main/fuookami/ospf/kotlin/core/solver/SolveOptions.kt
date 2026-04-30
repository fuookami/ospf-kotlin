package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStatusCallBack
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class SolveOptions(
    val solutionAmount: UInt64? = null,
    val modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null,
    val solvingStatusCallBack: SolvingStatusCallBack? = null,
    val valueConversionPolicy: SolveValueConversionPolicy? = null
) {
    val effectiveValueConversionPolicy: SolveValueConversionPolicy
        get() = valueConversionPolicy ?: SolveValueConversionPolicy.Strict

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
