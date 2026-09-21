/** Analysis capability declaration and dispatch helpers. / 分析能力声明与调度辅助。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel

/** 后端支持可以被调度的分析操作。 / Analysis operations whose backend support can be dispatched. */
enum class AnalysisCapability {
    Activity,
    FixedIntegerLpSensitivity,
    RhsPerturbation,
    AdaptivePerturbation,
    RemovalTest,
    ObjectiveTarget,
    AssumptionSolving,
    NativeUnsatCore,
    Conflict,
    Mus,
    ConstraintGroupAggregation,
    WarmStart,
    ExactCpLowering
}

/** Availability of an analysis operation. / 分析操作的可用级别。 */
enum class CapabilitySupport {
    /** Available with the declared implementation. / 有明确实现。 */
    Supported,

    /** Available only after runtime/model checks or a fallback. / 需要运行时或模型检查。 */
    Conditional,

    /** No usable implementation is declared. / 没有可用实现。 */
    Unsupported
}

/** 偏好完整名称的调用方使用的兼容别名。 / Compatibility alias for callers that prefer the longer name. */
typealias AnalysisCapabilitySupport = CapabilitySupport

/**
 * 用于分析调度、与求解器无关的能力矩阵。 / Solver-neutral capability matrix used by analysis dispatch.
 *
 * 基础标志映射 `SolverCapabilities`；当某阶段的支持取决于通用求解器描述符无法推断的细节时，
 * 分析能力映射允许后端适配器覆盖该阶段。 / The primitive flags mirror `SolverCapabilities`; the
 * analysis map allows a backend adapter to override a stage when its support depends on details
 * that cannot be inferred from the generic solver descriptor.
 *
 * @property modelTypes 支持的求解器模型类型 / Supported solver model types
 * @property nativeAssumptionSolving 是否原生支持 assumption 求解 / Whether assumption solving is native
 * @property nativeUnsatCore 是否原生支持 unsat core / Whether unsat-core extraction is native
 * @property fallbackSatisfactionOnly 是否支持仅满足性回退 / Whether satisfaction-only fallback is available
 * @property dual 是否支持对偶值 / Whether dual values are available
 * @property warmStart 是否支持 warm start / Whether warm starts are supported
 * @property exactCpLowering 是否支持精确 CP 降阶 / Whether exact CP lowering is supported
 * @property constraintProgrammingFeatures CP 特性支持级别 / CP feature support levels
 * @property analysisCapabilities 显式分析能力覆盖项 / Explicit analysis capability overrides
 */
data class CapabilityMatrix(
    val modelTypes: Set<SolverModelType> = emptySet(),
    val nativeAssumptionSolving: Boolean = false,
    val nativeUnsatCore: Boolean = false,
    val fallbackSatisfactionOnly: Boolean = false,
    val dual: Boolean = false,
    val warmStart: Boolean = false,
    val exactCpLowering: Boolean = false,
    val constraintProgrammingFeatures: Map<ConstraintProgrammingFeature, ConstraintProgrammingSupportLevel> = emptyMap(),
    val analysisCapabilities: Map<AnalysisCapability, CapabilitySupport> = emptyMap()
) {
    /** Alias used by sensitivity adapters. / 灵敏度适配器使用的别名。 */
    val lpDual: Boolean
        get() = dual

    /** 返回操作声明或推断出的支持级别。 / Return the declared or inferred support for an operation.
     *
     * @param capability 要查询的分析操作 / Analysis operation to query
     * @return 操作支持级别 / Operation support level
     */
    fun support(capability: AnalysisCapability): CapabilitySupport {
        return analysisCapabilities[capability] ?: inferredSupport(capability)
    }

    /** 分析器完成检查后即可使用条件支持。 / Conditional support is usable after the analyzer performs its checks.
     *
     * @param capability 要检查的分析操作 / Analysis operation to check
     * @return 是否存在可用实现 / Whether a usable implementation exists
     */
    fun supports(capability: AnalysisCapability): Boolean {
        return support(capability) != CapabilitySupport.Unsupported
    }

    /** 声明的支持是否为原生实现而非框架回退。 / Whether the declared support is native rather than a framework fallback.
     *
     * @param capability 要检查的分析操作 / Analysis operation to check
     * @return 是否为原生支持 / Whether native support is declared
     */
    fun isNative(capability: AnalysisCapability): Boolean {
        return when (capability) {
            AnalysisCapability.AssumptionSolving -> nativeAssumptionSolving
            AnalysisCapability.NativeUnsatCore -> nativeUnsatCore
            AnalysisCapability.WarmStart -> warmStart
            AnalysisCapability.ExactCpLowering -> exactCpLowering
            else -> false
        }
    }

    /** 使用现有 CP 支持级别词汇查询 CP 特性支持。 / CP feature support with the existing CP support-level vocabulary.
     *
     * @param feature 要查询的 CP 特性 / CP feature to query
     * @return CP 特性支持级别 / CP feature support level
     */
    fun cpSupport(feature: ConstraintProgrammingFeature): ConstraintProgrammingSupportLevel {
        return constraintProgrammingFeatures[feature] ?: ConstraintProgrammingSupportLevel.Unsupported
    }

    /** CP 特性是否以原生或精确降阶级别可用。 / Whether a CP feature is available at native or exact-lowering level.
     *
     * @param feature 要检查的 CP 特性 / CP feature to check
     * @return CP 特性是否可用 / Whether the CP feature is available
     */
    fun supports(feature: ConstraintProgrammingFeature): Boolean {
        return cpSupport(feature) != ConstraintProgrammingSupportLevel.Unsupported
    }

    private fun inferredSupport(capability: AnalysisCapability): CapabilitySupport {
        return when (capability) {
            AnalysisCapability.Activity,
            AnalysisCapability.ConstraintGroupAggregation -> CapabilitySupport.Supported

            AnalysisCapability.FixedIntegerLpSensitivity -> when {
                dual && (SolverModelType.MIP in modelTypes || exactCpLowering) -> CapabilitySupport.Supported
                dual -> CapabilitySupport.Conditional
                else -> CapabilitySupport.Unsupported
            }

            AnalysisCapability.RhsPerturbation,
            AnalysisCapability.RemovalTest,
            AnalysisCapability.ObjectiveTarget -> if (modelTypes.isEmpty()) {
                CapabilitySupport.Conditional
            } else {
                CapabilitySupport.Supported
            }

            AnalysisCapability.AdaptivePerturbation -> when (support(AnalysisCapability.RhsPerturbation)) {
                CapabilitySupport.Supported -> CapabilitySupport.Supported
                CapabilitySupport.Conditional -> CapabilitySupport.Conditional
                CapabilitySupport.Unsupported -> CapabilitySupport.Unsupported
            }

            AnalysisCapability.AssumptionSolving -> when {
                nativeAssumptionSolving -> CapabilitySupport.Supported
                fallbackSatisfactionOnly -> CapabilitySupport.Conditional
                else -> CapabilitySupport.Unsupported
            }

            AnalysisCapability.NativeUnsatCore -> if (nativeUnsatCore) {
                CapabilitySupport.Supported
            } else {
                CapabilitySupport.Unsupported
            }

            AnalysisCapability.Conflict -> when {
                nativeUnsatCore -> CapabilitySupport.Supported
                fallbackSatisfactionOnly -> CapabilitySupport.Conditional
                else -> CapabilitySupport.Unsupported
            }

            AnalysisCapability.Mus -> when {
                support(AnalysisCapability.Conflict) == CapabilitySupport.Unsupported -> CapabilitySupport.Unsupported
                modelTypes.isEmpty() -> CapabilitySupport.Conditional
                else -> CapabilitySupport.Supported
            }

            AnalysisCapability.WarmStart -> if (warmStart) {
                CapabilitySupport.Supported
            } else {
                CapabilitySupport.Unsupported
            }

            AnalysisCapability.ExactCpLowering -> if (exactCpLowering) {
                CapabilitySupport.Supported
            } else {
                CapabilitySupport.Unsupported
            }
        }
    }

    companion object {
        /** 从现有求解器描述符构造矩阵。 / Build a matrix from the existing solver descriptor.
         *
         * @param descriptor 求解器描述符 / Solver descriptor
         * @return 能力矩阵 / Capability matrix
         */
        fun from(descriptor: SolverDescriptor): CapabilityMatrix {
            return from(descriptor.capabilities)
        }

        /** 从现有求解器能力记录构造矩阵。 / Build a matrix from the existing solver capability record.
         *
         * @param capabilities 求解器能力记录 / Solver capability record
         * @return 能力矩阵 / Capability matrix
         */
        fun from(capabilities: SolverCapabilities): CapabilityMatrix {
            val cpFeatures = capabilities.constraintProgrammingFeatures.toMap()
            // A satisfaction-only fallback may only be claimed when the backend declares a CP
            // satisfaction path. / 只有后端声明 CP 满足路径时，才允许声称 satisfaction-only 回退。
            // Deriving it from any model type made linear/MIP-only backends advertise an unusable
            // fallback. / 从任意模型类型推导会让仅支持线性/MIP 的后端声称无法兑现的回退。
            val cpSatisfaction = cpFeatures[ConstraintProgrammingFeature.BooleanLogic] != null &&
                cpFeatures[ConstraintProgrammingFeature.BooleanLogic] != ConstraintProgrammingSupportLevel.Unsupported
            return CapabilityMatrix(
                modelTypes = capabilities.modelTypes.toSet(),
                nativeAssumptionSolving = cpFeatures[ConstraintProgrammingFeature.Assumption] == ConstraintProgrammingSupportLevel.Native,
                nativeUnsatCore = cpFeatures[ConstraintProgrammingFeature.ConflictCore] == ConstraintProgrammingSupportLevel.Native,
                fallbackSatisfactionOnly = cpSatisfaction,
                dual = capabilities.dual,
                warmStart = capabilities.warmStart,
                exactCpLowering = cpFeatures.values.any { it == ConstraintProgrammingSupportLevel.ExactLowering },
                constraintProgrammingFeatures = cpFeatures
            )
        }
    }
}

/**
 * Return CP features used by the active original model that are explicitly declared unsupported.
 *
 * An empty feature declaration means that the solver did not publish a CP feature map (for
 * example, the fake solver used by protocol tests); it must not be treated as a declaration that
 * every feature is unsupported. / 返回活动原始模型中被显式声明为不支持的 CP 特性。空 map 表示
 * 后端没有发布 CP 特性声明，不能误判为所有特性都不支持。
 */
internal fun CapabilityMatrix.unsupportedDeclaredCpFeatures(
    snapshot: ConstraintProgrammingModelSnapshot,
    activeSources: Set<DiagnosticSource>? = null
): Set<ConstraintProgrammingFeature> {
    if (constraintProgrammingFeatures.isEmpty()) {
        return emptySet()
    }
    val declaredUnsupported = constraintProgrammingFeatures
        .filterValues { it == ConstraintProgrammingSupportLevel.Unsupported }
        .keys
    if (declaredUnsupported.isEmpty()) {
        return emptySet()
    }

    val used = LinkedHashSet<ConstraintProgrammingFeature>()
    snapshot.variables.forEach { variable ->
        when (variable.domain) {
            IntegerDomain.boolean -> used += ConstraintProgrammingFeature.BooleanLogic
            is IntegerDomain.Values -> used += ConstraintProgrammingFeature.SparseDomain
            is IntegerDomain.Interval -> {}
        }
    }
    snapshot.intervals.forEach { interval ->
        used += ConstraintProgrammingFeature.Interval
        if (interval.presence != null) {
            used += ConstraintProgrammingFeature.OptionalInterval
        }
    }
    snapshot.constraints.forEach { entry ->
        if (activeSources == null || DiagnosticSource.Constraint(entry.id) in activeSources) {
            collectCpFeatures(entry.constraint, used)
        }
    }
    return used.filterTo(linkedSetOf()) { it in declaredUnsupported }
}

private fun collectCpFeatures(
    constraint: ConstraintProgrammingConstraint,
    used: MutableSet<ConstraintProgrammingFeature>
) {
    when (constraint) {
        is ConstraintProgrammingConstraint.IntegerComparison -> {}
        is ConstraintProgrammingConstraint.BoolAnd,
        is ConstraintProgrammingConstraint.BoolOr,
        is ConstraintProgrammingConstraint.BoolXor,
        is ConstraintProgrammingConstraint.Literal -> used += ConstraintProgrammingFeature.BooleanLogic
        is ConstraintProgrammingConstraint.Implication,
        is ConstraintProgrammingConstraint.Reified -> used += ConstraintProgrammingFeature.Reification
        is ConstraintProgrammingConstraint.AllDifferent -> used += ConstraintProgrammingFeature.AllDifferent
        is ConstraintProgrammingConstraint.Element -> used += ConstraintProgrammingFeature.Element
        is ConstraintProgrammingConstraint.AllowedAssignments,
        is ConstraintProgrammingConstraint.ForbiddenAssignments -> used += ConstraintProgrammingFeature.Table
        is NoOverlap -> used += ConstraintProgrammingFeature.NoOverlap
        is Cumulative -> used += ConstraintProgrammingFeature.Cumulative
        is ConstraintProgrammingConstraint.Circuit -> used += ConstraintProgrammingFeature.Circuit
        is ConstraintProgrammingConstraint.Automaton -> used += ConstraintProgrammingFeature.Automaton
        is ConstraintProgrammingConstraint.Reservoir -> used += ConstraintProgrammingFeature.Reservoir
    }
}
