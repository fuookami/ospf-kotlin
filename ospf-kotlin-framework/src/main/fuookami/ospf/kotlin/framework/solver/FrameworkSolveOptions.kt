package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStatusCallBack
import fuookami.ospf.kotlin.core.solver.SolveOptions
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * Framework-level solve options shared by framework shortcut solvers.
 * framework 层统一求解选项，用于收敛各类快捷求解入口的分散参数�?
 */
data class FrameworkSolveOptions(
    val name: String? = null,
    val toLogModel: Boolean = false,
    val solutionAmount: UInt64? = null,
    val modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null,
    val registrationStatusCallBack: RegistrationStatusCallBack? = null,
    val solvingStatusCallBack: SolvingStatusCallBack? = null,
    val valueConversionPolicy: SolveValueConversionPolicy? = null,
    val bendersIterationLimit: UInt64? = null,
    val bendersStallIterationLimit: UInt64? = null
) {
    val effectiveValueConversionPolicy: SolveValueConversionPolicy
        get() = valueConversionPolicy ?: SolveValueConversionPolicy.Strict

    fun solveName(defaultName: String): String = name ?: defaultName

    fun toCoreSolveOptions(): SolveOptions {
        return SolveOptions(
            solutionAmount = solutionAmount,
            modelBuildingStatusCallBack = modelBuildingStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack,
            valueConversionPolicy = valueConversionPolicy
        )
    }

    class Builder {
        var name: String? = null
        var toLogModel: Boolean = false
        var solutionAmount: UInt64? = null
        var modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null
        var registrationStatusCallBack: RegistrationStatusCallBack? = null
        var solvingStatusCallBack: SolvingStatusCallBack? = null
        var valueConversionPolicy: SolveValueConversionPolicy? = null
        var bendersIterationLimit: UInt64? = null
        var bendersStallIterationLimit: UInt64? = null

        fun build(): FrameworkSolveOptions {
            return FrameworkSolveOptions(
                name = name,
                toLogModel = toLogModel,
                solutionAmount = solutionAmount,
                modelBuildingStatusCallBack = modelBuildingStatusCallBack,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                valueConversionPolicy = valueConversionPolicy,
                bendersIterationLimit = bendersIterationLimit,
                bendersStallIterationLimit = bendersStallIterationLimit
            )
        }
    }

    companion object {
        fun builder(): Builder = Builder()

        fun build(block: Builder.() -> Unit): FrameworkSolveOptions {
            return Builder().apply(block).build()
        }
    }
}
