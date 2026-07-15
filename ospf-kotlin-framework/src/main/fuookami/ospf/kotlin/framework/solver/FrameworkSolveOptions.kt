package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.solver.SolveOptions
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy

/**
 * Framework-level solve options shared by framework shortcut solvers.
 * framework 层统一求解选项，用于收敛各类快捷求解入口的分散参数。
 *
 * @property name 求解名称，可为 null / Solve name, nullable
 * @property toLogModel 是否输出模型日志 / Whether to log the model
 * @property solutionAmount 解数量，可为 null / Solution amount, nullable
 * @property modelBuildingStatusCallBack 模型构建状态回调，可为 null / Model building status callback, nullable
 * @property registrationStatusCallBack 注册状态回调，可为 null / Registration status callback, nullable
 * @property solvingStatusCallBack 求解状态回调，可为 null / Solving status callback, nullable
 * @property valueConversionPolicy 值转换策略，可为 null / Value conversion policy, nullable
 * @property bendersIterationLimit Benders 迭代次数限制，可为 null / Benders iteration limit, nullable
 * @property bendersStallIterationLimit Benders 停滞迭代次数限制，可为 null / Benders stall iteration limit, nullable
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

    /** 生效的值转换策略，默认为 Strict / Effective value conversion policy, defaults to Strict */
    val effectiveValueConversionPolicy: SolveValueConversionPolicy
        get() = valueConversionPolicy ?: SolveValueConversionPolicy.Strict

    /**
     * 获取求解名称（带回退默认值）
     * Get solve name with fallback default
     *
     * @param defaultName 默认名称 / Default name
     * @return 有效求解名称 / Effective solve name
    */
    fun solveName(defaultName: String): String = name ?: defaultName

    /**
     * 转换为核心层求解选项
     * Convert to core-level solve options
     *
     * @return 核心层求解选项 / Core-level solve options
    */
    fun toCoreSolveOptions(): SolveOptions {
        return SolveOptions(
            solutionAmount = solutionAmount,
            modelBuildingStatusCallBack = modelBuildingStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack,
            valueConversionPolicy = valueConversionPolicy
        )
    }

    /**
     * 构建器
     * Builder
    */
    class Builder {
        /** 求解名称 / Solve name */
        var name: String? = null

        /** 是否输出模型日志 / Whether to log the model */
        var toLogModel: Boolean = false

        /** 解数量 / Solution amount */
        var solutionAmount: UInt64? = null

        /** 模型构建状态回调 / Model building status callback */
        var modelBuildingStatusCallBack: ModelBuildingStatusCallBack? = null

        /** 注册状态回调 / Registration status callback */
        var registrationStatusCallBack: RegistrationStatusCallBack? = null

        /** 求解状态回调 / Solving status callback */
        var solvingStatusCallBack: SolvingStatusCallBack? = null

        /** 值转换策略 / Value conversion policy */
        var valueConversionPolicy: SolveValueConversionPolicy? = null

        /** Benders 迭代次数限制 / Benders iteration limit */
        var bendersIterationLimit: UInt64? = null

        /** Benders 停滞迭代次数限制 / Benders stall iteration limit */
        var bendersStallIterationLimit: UInt64? = null

        /**
         * 构建求解选项
         * Build solve options
         *
         * @return 框架求解选项 / Framework solve options
        */
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
        /**
         * 创建构建器
         * Create builder
         *
         * @return 新构建器实例 / New builder instance
        */
        fun builder(): Builder = Builder()

        /**
         * 通过构建器 DSL 构建求解选项
         * Build solve options via builder DSL
         *
         * @param block 构建器配置块 / Builder configuration block
         * @return 框架求解选项 / Framework solve options
        */
        fun build(block: Builder.() -> Unit): FrameworkSolveOptions {
            return Builder().apply(block).build()
        }
    }
}