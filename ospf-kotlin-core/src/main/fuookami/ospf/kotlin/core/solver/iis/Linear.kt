@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 线性模型 IIS 计算 / Linear model IIS computation */
package fuookami.ospf.kotlin.core.solver.iis

import java.io.OutputStreamWriter
import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidence
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.diagnosticConstraintId
import fuookami.ospf.kotlin.core.solver.report.diagnosticVariableId

/**
 * 线性 IIS 模型，包含不可行子系统和可选的守卫约束。 / Linear IIS model, containing the infeasible subsystem and optional guard constraints.
 *
 * @property impl 基础线性三元模型实现 / Basic linear triad model implementation
 * @property guardConstraints 守卫约束（可选）/ Guard constraints (optional)
 * @property origin 原始模型视图 / Original model view
 * @property relaxedFeasible 是否为松弛可行（无守卫约束）/ Whether relaxed feasible (no guard constraints)
*/
data class LinearIISModel(
    private val impl: BasicLinearTriadModel,
    val guardConstraints: LinearConstraintBatch?,
    val origin: LinearTriadModelView
) : BasicLinearTriadModelView {
    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraintBatch by impl::constraints
    override val name: String by impl::name
    val relaxedFeasible: Boolean get() = guardConstraints == null

    /**
     * 将模型导出为 LP 格式。 / Export the model in LP format.
     *
     * @param writer 输出写入器 / Output writer
     * @return 操作结果 / Operation result
    */
    override fun exportLP(writer: OutputStreamWriter): Try {
        return impl.exportLP(writer)
    }
}

/** 通过诊断编排器计算线性 IIS，必要时回退到 legacy 算法。 / Compute linear IIS through the diagnostic orchestrator, falling back to the legacy algorithm when necessary. */
@OptIn(ExperimentalTime::class)
suspend fun computeIIS(
    model: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<LinearIISModel> {
    val analyzers = solver.diagnosticAnalyzers(config) + LegacyElasticInfeasibilityAnalyzer(solver, config)
    val orchestrator = InfeasibilityDiagnosticOrchestrator(
        analyzers = analyzers,
        solverCapabilities = solver.descriptor.capabilities
    )
    return when (val result = orchestrator.analyzeMaterialized<LinearIISModel>(model)) {
        is Ok -> {
            result.value.artifact?.let(::Ok)
                ?: materializeNativeIIS(model, result.value.evidence)?.let(::Ok)
                ?: when (val fallback = LegacyElasticInfeasibilityAnalyzer(solver, config).analyzeMaterialized(model)) {
                    is Ok -> fallback.value.artifact?.let(::Ok)
                        ?: Failed(ErrorCode.Other, "Legacy IIS 未返回模型 artifact / Legacy IIS did not return a model artifact")
                    is Failed -> Failed(fallback.error)
                    is Fatal -> Fatal(fallback.errors)
                }
        }

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/** 将原生 IIS 证据物化为兼容线性模型。 / Materialize native IIS evidence as a compatible linear model. */
private fun materializeNativeIIS(
    model: LinearTriadModelView,
    evidence: InfeasibilityEvidence
): LinearIISModel? {
    if (evidence.source != InfeasibilityEvidenceSource.NativeIIS) {
        return null
    }
    val rows = model.constraints.indices.filter { index ->
        val currentId = model.diagnosticConstraintId(index)
        currentId in evidence.constraintIds ||
            ConstraintId("constraint-$index") in evidence.constraintIds
    }
    val boundByVariable = evidence.variableBoundRefs.groupBy { it.variableId }
    val referencedVariables = linkedSetOf<Int>()
    rows.forEach { row ->
        model.constraints.sparseLhs.forEachEntry(row) { column, _ -> referencedVariables += column }
    }
    model.variables.forEachIndexed { index, variable ->
        val ids = setOf(
            variable.diagnosticVariableId(),
            VariableId("variable-$index")
        )
        if (ids.any(boundByVariable::containsKey) || evidence.variableDomainRefs.any { it.variableId in ids }) {
            referencedVariables += index
        }
    }
    if (rows.isEmpty() && referencedVariables.isEmpty()) {
        return null
    }
    val selectedIndices = referencedVariables.toList().sorted()
    val oldToNewVariableIndexMap = selectedIndices.withIndex().associate { (newIndex, oldIndex) ->
        oldIndex to newIndex
    }
    val variables = selectedIndices.mapIndexed { newIndex, oldIndex ->
        val original = model.variables[oldIndex]
        val selected = setOf(
            original.diagnosticVariableId(),
            VariableId("variable-$oldIndex")
        ).flatMap { boundByVariable[it].orEmpty() }
        Variable(
            index = newIndex,
            lowerBound = if (selected.any { it.side == BoundSide.Lower }) original.lowerBound else Flt64.negativeInfinity,
            upperBound = if (selected.any { it.side == BoundSide.Upper }) original.upperBound else Flt64.infinity,
            type = original.type,
            origin = original.origin,
            dualOrigin = original.dualOrigin,
            slack = original.slack,
            name = original.name,
            initialResult = original.initialResult,
            id = original.id,
            identityScope = original.identityScope,
            identityOrigin = original.identityOrigin,
            identityNamespace = original.identityNamespace,
            identitySchemaVersion = original.identitySchemaVersion,
            identityProvenance = original.identityProvenance
        )
    }
    val sparseLhs = SparseMatrix<Flt64>()
    rows.forEach { row ->
        val sparseRow = SparseVector<Flt64>()
        model.constraints.sparseLhs.forEachEntry(row) { oldColumn, coefficient ->
            oldToNewVariableIndexMap[oldColumn]?.let { newColumn -> sparseRow.add(newColumn, coefficient.copy()) }
        }
        sparseLhs.addRow(sparseRow)
    }
    val constraints = LinearConstraintBatch(
        sparseLhs = sparseLhs,
        signs = rows.map { model.constraints.signs[it] },
        rhs = rows.map { model.constraints.rhs[it].copy() },
        names = rows.map { model.constraints.names[it] },
        sources = rows.map { model.constraints.sources[it] },
        origins = rows.map { model.constraints.origins[it] },
        froms = rows.map { model.constraints.froms[it] },
        priorities = rows.map { model.constraints.priorities[it] },
        ids = rows.map { row ->
            model.constraints.ids.getOrNull(row)
                ?: ConstraintId("model-local-constraint:$row")
        },
        identityNamespace = model.constraints.identityNamespace,
        identitySchemaVersion = model.constraints.identitySchemaVersion,
        identityScopes = rows.map { row ->
            if (model.constraints.ids.getOrNull(row) == null) {
                ModelElementScope.ModelLocal
            } else {
                model.constraints.identityScopeAt(row)
            }
        },
        identityOrigins = rows.map { row ->
            if (model.constraints.ids.getOrNull(row) == null) null else model.constraints.identityOriginAt(row)
        },
        identityProvenance = rows.map { row ->
            if (model.constraints.ids.getOrNull(row) == null) {
                emptyList()
            } else {
                model.constraints.identityProvenanceAt(row)
            }
        }
    )
    return LinearIISModel(
        impl = BasicLinearTriadModel(variables, constraints, "${model.name}_iis"),
        guardConstraints = null,
        origin = model
    )
}

/**
 * 计算线性模型的不可行子系统（IIS）。 / Compute the Irreducible Infeasible Subsystem (IIS) for a linear model.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param solver 线性求解器 / Linear solver
 * @param config IIS 配置 / IIS configuration
 * @return IIS 模型 / IIS model
*/
@OptIn(ExperimentalTime::class)
suspend fun computeLegacyIIS(
    model: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<LinearIISModel> {
    val startTime = Clock.System.now()
    val elasticModel = model.elastic(minSlackAmount = UInt64.two to config.slackTolerance)
    val boundAmount = UInt64(elasticModel.constraints.sources.count {
        it == ConstraintSource.ElasticLowerBound || it == ConstraintSource.ElasticUpperBound
    })
    val constraintAmount = UInt64(elasticModel.constraints.sources.count {
        it == ConstraintSource.Elastic
    })

    // todo: find impossible constraints

    when (val result = performElasticFiltering(
        elasticModel = elasticModel,
        solver = solver,
        config = config
    )) {
        is Ok -> {
            if (result.value.first) {
                when (val callbackResult = config.computingStatusCallBack?.invoke(
                    true,
                    Clock.System.now() - startTime,
                    IISComputingStatus(
                        restBoundAmount = UInt64(result.value.second.keys.count {
                            it.slack?.lowerBound != null || it.slack?.upperBound != null
                        }),
                        totalBoundAmount = boundAmount,
                        restConstraintAmount = UInt64(result.value.second.keys.count {
                            it.slack?.constraint != null
                        }),
                        totalConstraintAmount = constraintAmount,
                    )
                )) {
                    null -> {}
                    is Ok -> {}
                    is Failed -> {
                        return Failed(callbackResult.error)
                    }

                    is Fatal -> {
                        return Fatal(callbackResult.errors)
                    }
                }
                return Ok(dump(model, result.value.second))
            }
            result.value.second
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }

    when (val callbackResult = config.computingStatusCallBack?.invoke(
        true,
        Clock.System.now() - startTime,
        IISComputingStatus(
            restBoundAmount = boundAmount,
            totalBoundAmount = boundAmount,
            restConstraintAmount = constraintAmount,
            totalConstraintAmount = constraintAmount,
        )
    )) {
        null -> {}
        is Ok -> {}
        is Failed -> return Failed(callbackResult.error)
        is Fatal -> return Fatal(callbackResult.errors)
    }
    val (misConstraints, guardConstraints) = when (val result = performDeletionFiltering(
        elasticModel = elasticModel,
        solver = solver,
        startTime = startTime,
        boundAmount = boundAmount,
        constraintAmount = constraintAmount,
        config = config
    )) {
        is Ok -> result.value

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    return Ok(dump(model, misConstraints, guardConstraints))
}

/**
 * 获取与松弛变量关联的约束索引列表。 / Get constraint indices related to slack variables.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param slackVariables 松弛变量集合 / Slack variable set
 * @return 关联的约束索引列表 / Related constraint index list
*/
private fun getRelatedConstraints(
    model: LinearTriadModelView,
    slackVariables: Set<Variable>
): List<Int> {
    return slackVariables
        .mapNotNull { it.slack?.constraint }
        .let { originConstraints ->
            model.constraints.indices.filter { i ->
                model.constraints.origins[i]?.let { it in originConstraints } == true
            }
        }
}

/**
 * 获取与过滤变量和约束关联的变量列表。 / Get variables related to filter variables and constraints.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param filter 过滤的松弛变量集合 / Filtered slack variable set
 * @param relatedConstraints 关联的约束索引列表 / Related constraint index list
 * @return 变量及其下界和上界的 triple 列表 / List of variable with its lower and upper bounds as triples
*/
private fun getRelatedVariables(
    model: LinearTriadModelView,
    filter: Set<Variable>,
    relatedConstraints: List<Int>
): List<Triple<Variable, Flt64?, Flt64?>> {
    return (filter.mapNotNull { variable ->
        if (variable.slack?.lowerBound != null) {
            Triple(variable.slack.lowerBound, true, false)
        } else if (variable.slack?.upperBound != null) {
            Triple(variable.slack.upperBound, false, true)
        } else {
            null
        }
    } + relatedConstraints.flatMap { i ->
        model.constraints.lhs[i].map { cell ->
            Triple(model.variables[cell.colIndex], false, false)
        }
    })
        .groupBy { it.first }
        .toList()
        .sortedBy { it.first.index }
        .map { (variable, bounds) ->
            val lowerBound = if (bounds.any { it.second }) {
                variable.lowerBound
            } else {
                null
            }
            val upperBound = if (bounds.any { it.third }) {
                variable.upperBound
            } else {
                null
            }
            Triple(variable, lowerBound, upperBound)
        }
}

/**
 * 从弹性过滤结果构建线性 IIS 模型。 / Build linear IIS model from elastic filter result.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param elasticFilter 弹性过滤结果，松弛变量到值的映射 / Elastic filter result, mapping of slack variables to values
 * @return 构建的线性 IIS 模型 / Built linear IIS model
*/
private fun dump(
    model: LinearTriadModelView,
    elasticFilter: Map<Variable, Flt64>
): LinearIISModel {
    val relatedConstraints = getRelatedConstraints(
        model = model,
        slackVariables = elasticFilter.keys
    )
    val relatedVariables = getRelatedVariables(
        model = model,
        filter = elasticFilter.keys,
        relatedConstraints = relatedConstraints
    )

    return LinearIISModel(
        impl = BasicLinearTriadModel(
            variables = relatedVariables.map {
                it.first.copy().apply {
                    _lowerBound = it.second ?: Flt64.negativeInfinity
                    _upperBound = it.third ?: Flt64.infinity
                }
            },
            constraints = model.constraints.filter { row -> row in relatedConstraints },
            name = "${model.name}_iis"
        ),
        guardConstraints = null,
        origin = model
    )
}

/**
 * 从 MIS 和守卫约束构建线性 IIS 模型。 / Build linear IIS model from MIS and guard constraints.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param misConstraints 不可行子系统约束对应的松弛变量集合 / Slack variable set for MIS constraints
 * @param guardConstraints 守卫约束对应的松弛变量集合 / Slack variable set for guard constraints
 * @return 构建的线性 IIS 模型 / Built linear IIS model
*/
private fun dump(
    model: LinearTriadModelView,
    misConstraints: Set<Variable>,
    guardConstraints: Set<Variable>
): LinearIISModel {
    val relatedMISConstraints = getRelatedConstraints(
        model = model,
        slackVariables = misConstraints
    )
    val relatedGuardConstraints = getRelatedConstraints(
        model = model,
        slackVariables = guardConstraints
    )
    val relatedConstraints = (relatedMISConstraints + relatedGuardConstraints).distinct().sorted()
    val relatedVariables = getRelatedVariables(
        model = model,
        filter = misConstraints + guardConstraints,
        relatedConstraints = relatedConstraints
    )

    return LinearIISModel(
        impl = BasicLinearTriadModel(
            variables = relatedVariables.map {
                it.first.copy().apply {
                    _lowerBound = it.second ?: Flt64.negativeInfinity
                    _upperBound = it.third ?: Flt64.infinity
                }
            },
            constraints = model.constraints.filter { row -> row in relatedMISConstraints },
            name = "${model.name}_iis"
        ),
        guardConstraints = model.constraints.filter { row -> row in relatedGuardConstraints },
        origin = model
    )
}

/**
 * 执行弹性过滤以识别不可行组件。 / Perform elastic filtering to identify infeasible components.
 *
 * @param elasticModel 弹性线性三元模型视图 / Elastic linear triad model view
 * @param solver 线性求解器 / Linear solver
 * @param config IIS 配置 / IIS configuration
 * @return 是否直接可行及松弛变量映射 / Whether directly feasible and slack variable mapping
*/
private suspend fun performElasticFiltering(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<Pair<Boolean, Map<Variable, Flt64>>> {
    val relaxVariableBoundsResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable ->
        variable.slack?.lowerBound != null || variable.slack?.upperBound != null
    }) {
        is Ok -> result.value

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    if (relaxVariableBoundsResult.first) {
        return Ok(true to relaxVariableBoundsResult.second)
    }

    val relaxInequalitiesAndVariableBoundsResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable ->
        when (variable.slack?.constraint?.sign) {
            ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                true
            }

            else -> {
                false
            }
        } || variable.slack?.lowerBound != null || variable.slack?.upperBound != null
    }) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    if (relaxInequalitiesAndVariableBoundsResult.first) {
        return Ok(true to relaxInequalitiesAndVariableBoundsResult.second)
    }

    val relaxAllResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable -> variable.slack != null }) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    if (relaxAllResult.first) {
        return Ok(true to relaxAllResult.second)
    }

    return Ok(false to emptyMap())
}

/**
 * 执行删除过滤以精简不可行组件。 / Perform deletion filtering to refine infeasible components.
 *
 * @param elasticModel 弹性线性三元模型视图 / Elastic linear triad model view
 * @param solver 线性求解器 / Linear solver
 * @param startTime 计算开始时间 / Computation start time
 * @param boundAmount 变量界约束总数 / Total number of variable bound constraints
 * @param constraintAmount 约束总数 / Total number of constraints
 * @param config IIS 配置 / IIS configuration
 * @return MIS 约束集合与守卫约束集合 / MIS constraint set and guard constraint set
*/
@OptIn(ExperimentalTime::class)
private suspend fun performDeletionFiltering(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    startTime: Instant,
    boundAmount: UInt64,
    constraintAmount: UInt64,
    config: IISConfig
): Ret<Pair<Set<Variable>, Set<Variable>>> {
    val slackVariables = elasticModel.variables.filter { it.slack != null }.sortedBy { it.index }
    if (slackVariables.isEmpty()) {
        return Ok(emptySet<Variable>() to emptySet())
    }

    val activeRelaxedComponents = slackVariables.toMutableSet()
    for (candidate in slackVariables) {
        if (candidate !in activeRelaxedComponents) {
            continue
        }

        candidate._upperBound = Flt64.zero
        val feasible = when (val result = solver(elasticModel)) {
            is Ok -> {
                when (result.value.problemStatus) {
                    ProblemStatus.Feasible -> true
                    ProblemStatus.Infeasible,
                    ProblemStatus.InfeasibleOrUnbounded -> false
                    else -> {
                        return Failed(
                            ErrorCode.OREngineSolvingException,
                            "IIS 删除过滤收到无法判定可行性的终态：${result.value.problemStatus} / " +
                                "IIS deletion filtering received an inconclusive terminal status: ${result.value.problemStatus}"
                        )
                    }
                }
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                    false
                } else {
                    return Failed(result.error)
                }
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (feasible) {
            activeRelaxedComponents.remove(candidate)
        } else {
            candidate._upperBound = Flt64.infinity
        }

        when (val callbackResult = config.computingStatusCallBack?.invoke(
            true,
            Clock.System.now() - startTime,
            IISComputingStatus(
                restBoundAmount = UInt64(activeRelaxedComponents.count {
                    it.slack?.lowerBound != null || it.slack?.upperBound != null
                }),
                totalBoundAmount = boundAmount,
                restConstraintAmount = UInt64(activeRelaxedComponents.count {
                    it.slack?.constraint != null
                }),
                totalConstraintAmount = constraintAmount,
            )
        )) {
            null -> {}
            is Ok -> {}
            is Failed -> {
                return Failed(callbackResult.error)
            }

            is Fatal -> {
                return Fatal(callbackResult.errors)
            }
        }
    }

    return Ok(activeRelaxedComponents to emptySet())
}

/**
 * 松弛满足条件的特定组件。 / Relax specific components satisfying the condition.
 *
 * @param elasticModel 弹性线性三元模型视图 / Elastic linear triad model view
 * @param solver 线性求解器 / Linear solver
 * @param tolerance 松弛变量容差 / Slack variable tolerance
 * @param relaxCondition 判断是否松弛的谓词 / Predicate to determine whether to relax
 * @return 是否可行及松弛变量映射 / Whether feasible and slack variable mapping
*/
private suspend fun relaxSpecificComponents(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    tolerance: Flt64,
    relaxCondition: (Variable) -> Boolean
): Ret<Pair<Boolean, Map<Variable, Flt64>>> {
    elasticModel.variables.forEach { variable ->
        if (variable.slack != null) {
            variable._upperBound = if (relaxCondition(variable)) {
                Flt64.infinity
            } else {
                Flt64.zero
            }
        }
    }

    val result = when (val result = solver.solveReport(elasticModel)) {
        is Ok -> when (result.value.problemStatus) {
            ProblemStatus.Feasible -> result.value.solution?.values
                ?: return Failed(Err(ErrorCode.ORSolutionInvalid, "Elastic linear solve completed without a solution."))
            ProblemStatus.Infeasible,
            ProblemStatus.InfeasibleOrUnbounded -> return Ok(false to emptyMap())
            else -> return Failed(
                ErrorCode.OREngineSolvingException,
                "Elastic linear solve returned an inconclusive terminal status: ${result.value.problemStatus}."
            )
        }

        is Failed -> {
            return if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                Ok(false to emptyMap())
            } else {
                Failed(result.error)
            }
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }

    val relaxedComponents = elasticModel.variables.associateNotNull { variable ->
        if (variable.slack != null && result.size > variable.index && result[variable.index] geq tolerance) {
            variable to result[variable.index]
        } else {
            null
        }
    }
    return Ok(true to relaxedComponents)
}
