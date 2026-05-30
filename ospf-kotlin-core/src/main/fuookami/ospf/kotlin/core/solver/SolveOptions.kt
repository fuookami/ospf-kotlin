/**
 * 求解选项配置
 * Solve options configuration
 */
package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack

/**
 * 求解选项，封装求解过程中的可选参数。
 * Solve options, encapsulating optional parameters for the solving process.
 *
 * @property solutionAmount 期望的解数量 / Desired number of solutions
 * @property modelBuildingStatusCallBack 模型构建状态回调 / Model building status callback
 * @property solvingStatusCallBack 求解状态回调 / Solving status callback
 * @property valueConversionPolicy 值转换策略 / Value conversion policy
 */
data class SolveOptions(
    val solutionAmount: UInt64? = null,
    val modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null,
    val solvingStatusCallBack: SolvingStatusCallBack? = null,
    val valueConversionPolicy: SolveValueConversionPolicy? = null
) {
    /** 有效的值转换策略 / Effective value conversion policy */
    val effectiveValueConversionPolicy: SolveValueConversionPolicy
        get() = valueConversionPolicy ?: SolveValueConversionPolicy.AllowRounding

    /** 求解选项构建器，通过链式调用逐步配置各项参数。 / Solve options builder that configures parameters step by step via chaining. */
    class Builder {
        var solutionAmount: UInt64? = null
        var modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null
        var solvingStatusCallBack: SolvingStatusCallBack? = null
        var valueConversionPolicy: SolveValueConversionPolicy? = null

        /**
         * 构建求解选项。
         * Build solve options.
         *
         * @return 求解选项实例 / Solve options instance
         */
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
        /**
         * 创建求解选项构建器。
         * Create a solve options builder.
         *
         * @return 构建器实例 / Builder instance
         */
        fun builder(): Builder {
            return Builder()
        }

        /**
         * 通过 DSL 块构建求解选项。
         * Build solve options via a DSL block.
         *
         * @param block 构建器配置块 / Builder configuration block
         * @return 求解选项实例 / Solve options instance
         */
        fun build(block: Builder.() -> Unit): SolveOptions {
            return Builder().apply(block).build()
        }
    }
}
