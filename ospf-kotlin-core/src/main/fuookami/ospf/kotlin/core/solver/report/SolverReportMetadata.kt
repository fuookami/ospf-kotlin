/** Common report metadata builders. / 统一报告元数据构造器。 */
package fuookami.ospf.kotlin.core.solver.report

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.config.SCIPSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Build a redacted snapshot for common and backend-specific solver parameters. /
 * 构造包含通用及 backend 参数的脱敏配置快照。
 *
 * @param config 求解器配置 / Solver configuration
 * @return 脱敏配置快照 / Redacted configuration snapshot
 */
fun SolverConfig.configurationSnapshot(): BackendConfigurationSnapshot {
    val parameters = buildList {
        add(BackendParameter("time", BackendParameterValue.Text(time.toString())))
        add(BackendParameter("threadNum", BackendParameterValue.Integer(threadNum.toLong())))
        add(BackendParameter("gap", BackendParameterValue.Decimal(gap.toString())))
        notImprovementTime?.let { add(BackendParameter("notImprovementTime", BackendParameterValue.Text(it.toString()))) }
        add(BackendParameter("improveThreshold", BackendParameterValue.Decimal(improveThreshold.toString())))
        interruptibleTime?.let { add(BackendParameter("interruptibleTime", BackendParameterValue.Text(it.toString()))) }
        interruptibleGap?.let { add(BackendParameter("interruptibleGap", BackendParameterValue.Decimal(it.toString()))) }
        backendConfiguration?.parameters()?.forEach { parameter ->
            add(parameter.copy(name = "backend.${parameter.name}"))
        }
    }.sortedBy { it.name }
    return BackendConfigurationSnapshot(type = backendConfiguration?.type ?: "solver", parameters = parameters)
}

/**
 * Build auditable runtime provenance for a backend solve. /
 * 为 backend 求解构造可审计运行来源。
 *
 * @param descriptor backend 描述符 / Backend descriptor
 * @param config 生效配置 / Effective configuration
 * @param nativeVersion 原生库版本 / Native library version
 * @return 求解来源 / Solve provenance
 */
fun solverProvenance(
    descriptor: SolverDescriptor,
    config: SolverConfig,
    nativeVersion: String? = descriptor.backendVersion,
    ignoredParameters: Map<String, String> = emptyMap()
): SolverProvenance {
    val effective = linkedMapOf<String, String>()
    effective["time"] = config.time.toString()
    effective["threadNum"] = config.threadNum.toString()
    effective["gap"] = config.gap.toString()
    config.notImprovementTime?.let { effective["notImprovementTime"] = it.toString() }
    effective["improveThreshold"] = config.improveThreshold.toString()
    config.interruptibleTime?.let { effective["interruptibleTime"] = it.toString() }
    config.interruptibleGap?.let { effective["interruptibleGap"] = it.toString() }
    config.backendConfiguration?.parameters()?.forEach { parameter ->
        effective["backend.${parameter.name}"] = parameter.redactedValue()
    }
    return SolverProvenance(
        descriptor = descriptor,
        nativeVersion = nativeVersion,
        effectiveParameters = effective.toSortedMap(),
        ignoredParameters = ignoredParameters.toSortedMap(),
        configuration = config.configurationSnapshot(),
        threadCount = config.threadNum.toInt(),
        randomSeed = (config.backendConfiguration as? SCIPSolverConfig)?.randomSeed,
        deterministic = (config.backendConfiguration as? SCIPSolverConfig)?.deterministic
    )
}

/**
 * Build model, configuration and solver fingerprints for a linear solve. /
 * 构造线性求解的模型、配置和求解器指纹。
 *
 * @param model 线性模型 / Linear model
 * @param config 求解器配置 / Solver configuration
 * @param descriptor backend 描述符 / Backend descriptor
 * @return 求解指纹集合 / Solve fingerprint set
 */
fun linearSolveFingerprints(
    model: LinearTriadModelView,
    config: SolverConfig,
    descriptor: SolverDescriptor
): SolveFingerprints {
    return SolveFingerprints(
        model = model.toNormalizedMathematicalModel().fingerprint(),
        configuration = config.configurationSnapshot().fingerprint(),
        solver = solverFingerprint(descriptor)
    )
}

/**
 * Build model, configuration and solver fingerprints for a quadratic solve. /
 * 构造二次求解的模型、配置和求解器指纹。
 *
 * @param model 二次模型 / Quadratic model
 * @param config 求解器配置 / Solver configuration
 * @param descriptor backend 描述符 / Backend descriptor
 * @return 求解指纹集合 / Solve fingerprint set
 */
fun quadraticSolveFingerprints(
    model: QuadraticTetradModelView,
    config: SolverConfig,
    descriptor: SolverDescriptor
): SolveFingerprints {
    return SolveFingerprints(
        model = model.toNormalizedMathematicalModel().fingerprint(),
        configuration = config.configurationSnapshot().fingerprint(),
        solver = solverFingerprint(descriptor)
    )
}

/**
 * Build a deterministic fingerprint for a solver descriptor. /
 * 为求解器描述符构造确定性指纹。
 *
 * @param descriptor backend 描述符 / Backend descriptor
 * @return 求解器指纹 / Solver fingerprint
 */
fun solverFingerprint(descriptor: SolverDescriptor): SolverFingerprint {
    val capabilities = descriptor.capabilities
    val modelTypes = capabilities.modelTypes.map { it.name }.sorted().joinToString(",")
    val constraintProgrammingFeatures = capabilities.constraintProgrammingFeatures
        .entries
        .sortedBy { it.key.name }
        .joinToString(",") { (feature, support) -> "${feature.name}=${support.name}" }
    return SolveFingerprinting.sha256(
        listOf(
            "solverId=${descriptor.solverId}",
            "backendName=${descriptor.backendName}",
            "backendVersion=${descriptor.backendVersion ?: ""}",
            "pluginVersion=${descriptor.pluginVersion ?: ""}",
            "modelTypes=$modelTypes",
            "nativeIIS=${capabilities.nativeIIS}",
            "dual=${capabilities.dual}",
            "farkas=${capabilities.farkas}",
            "warmStart=${capabilities.warmStart}",
            "solutionPool=${capabilities.solutionPool}",
            "callback=${capabilities.callback}",
            "interrupt=${capabilities.interrupt}",
            "checkpoint=${capabilities.checkpoint}",
            "resume=${capabilities.resume}",
            "constraintProgrammingFeatures=$constraintProgrammingFeatures"
        ).joinToString("\n"),
        schemaVersion = "solver-2"
    )
}

/** Add a structured diagnostic to an existing report without changing its terminal state. /
 * 向既有报告追加结构化诊断，不改变报告终态。 */
fun <V> SolveReport<V>.withDiagnostics(
    diagnostics: SolveDiagnostics<V>
): SolveReport<V> {
    return copy(
        diagnostics = SolveDiagnostics(
            constraintEvaluations = diagnostics.constraintEvaluations.ifEmpty { this.diagnostics.constraintEvaluations },
            variableBoundEvaluations = diagnostics.variableBoundEvaluations.ifEmpty { this.diagnostics.variableBoundEvaluations },
            infeasibilityEvidence = diagnostics.infeasibilityEvidence ?: this.diagnostics.infeasibilityEvidence,
            warnings = this.diagnostics.warnings + diagnostics.warnings,
            errors = this.diagnostics.errors + diagnostics.errors
        )
    )
}

/**
 * Add backend provenance and fingerprints to a linear report. /
 * 向线性报告追加 backend provenance 和指纹。
 *
 * @param model 线性模型 / Linear model
 * @param config 求解器配置 / Solver configuration
 * @param descriptor backend 描述符 / Backend descriptor
 * @return enriched report / 增强后的报告
 */
fun SolveReport<Flt64>.withLinearBackendMetadata(
    model: LinearTriadModelView,
    config: SolverConfig,
    descriptor: SolverDescriptor
): SolveReport<Flt64> {
    return copy(
        provenance = provenance ?: solverProvenance(descriptor, config),
        fingerprints = linearSolveFingerprints(model, config, descriptor)
    ).withModelDiagnostics(model)
}

/**
 * Add backend provenance and fingerprints to a quadratic report. /
 * 向二次报告追加 backend provenance 和指纹。
 *
 * @param model 二次模型 / Quadratic model
 * @param config 求解器配置 / Solver configuration
 * @param descriptor backend 描述符 / Backend descriptor
 * @return enriched report / 增强后的报告
 */
fun SolveReport<Flt64>.withQuadraticBackendMetadata(
    model: QuadraticTetradModelView,
    config: SolverConfig,
    descriptor: SolverDescriptor
): SolveReport<Flt64> {
    return copy(
        provenance = provenance ?: solverProvenance(descriptor, config),
        fingerprints = quadraticSolveFingerprints(model, config, descriptor)
    ).withModelDiagnostics(model)
}
