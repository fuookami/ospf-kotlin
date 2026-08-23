@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 二次模型 IIS 计算 / Quadratic model IIS computation */
package fuookami.ospf.kotlin.core.solver.iis

import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver
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
 * Legacy quadratic IIS artifact and its actual filtering source. / 旧二次 IIS artifact 及其实际过滤来源。
 *
 * @property model 物化的二次 IIS 模型 / Materialized quadratic IIS model
 * @property source 实际过滤证据来源 / Actual filtering evidence source
 */
data class LegacyQuadraticIISResult(
    val model: QuadraticTetradModel,
    val source: InfeasibilityEvidenceSource
)

/**
 * 计算二次模型的不可行子系统（IIS）。 / Compute the Irreducible Infeasible Subsystem (IIS) for a quadratic model.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param solver 二次求解器 / Quadratic solver
 * @param config IIS 配置 / IIS configuration
 * @return IIS 模型 / IIS model
*/
@OptIn(ExperimentalTime::class)
suspend fun computeIIS(
    model: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
    config: IISConfig
): Ret<QuadraticTetradModel> {
    val analyzers = solver.diagnosticAnalyzers(config) + LegacyElasticQuadraticInfeasibilityAnalyzer(solver, config)
    val orchestrator = InfeasibilityDiagnosticOrchestrator(
        analyzers = analyzers,
        solverCapabilities = solver.descriptor.capabilities
    )
    return when (val result = orchestrator.analyzeMaterialized<QuadraticTetradModel>(model)) {
        is Ok -> {
            result.value.artifact?.let(::Ok)
                ?: materializeNativeIIS(model, result.value.evidence)?.let(::Ok)
                ?: when (val fallback = LegacyElasticQuadraticInfeasibilityAnalyzer(solver, config).analyzeMaterialized(model)) {
                    is Ok -> fallback.value.artifact?.let(::Ok)
                        ?: Failed(ErrorCode.Other, "Legacy 二次 IIS 未返回模型 artifact / Legacy quadratic IIS did not return a model artifact")
                    is Failed -> Failed(fallback.error)
                    is Fatal -> Fatal(fallback.errors)
                }
        }

        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/** 将原生 IIS 证据物化为兼容二次模型。 / Materialize native IIS evidence as a compatible quadratic model. */
private fun materializeNativeIIS(
    model: QuadraticTetradModelView,
    evidence: InfeasibilityEvidence
): QuadraticTetradModel? {
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
        model.constraints.sparseLhs.forEachEntry(row) { colIndex1, colIndex2, _ ->
            referencedVariables += colIndex1
            colIndex2?.let { referencedVariables += it }
        }
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
    val variables = selectedIndices.map { oldIndex ->
        val original = model.variables[oldIndex]
        val selected = setOf(
            original.diagnosticVariableId(),
            VariableId("variable-$oldIndex")
        ).flatMap { boundByVariable[it].orEmpty() }
        Variable(
            index = oldToNewVariableIndexMap.getValue(oldIndex),
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
    val constraints = filterConstraintByRowIndex(model.constraints, rows, oldToNewVariableIndexMap)
    val objective = QuadraticObjective(
        category = model.objective.category,
        objective = model.objective.objective.mapNotNull { cell ->
            val newColIndex1 = oldToNewVariableIndexMap[cell.colIndex1] ?: return@mapNotNull null
            val newColIndex2 = cell.colIndex2?.let { oldToNewVariableIndexMap[it] ?: return@mapNotNull null }
            QuadraticObjectiveCell(newColIndex1, newColIndex2, cell.coefficient.copy())
        },
        constant = model.objective.constant.copy(),
        id = model.objective.id,
        identityScope = model.objective.identityScope,
        identityOrigin = model.objective.identityOrigin,
        identityNamespace = model.objective.identityNamespace,
        identitySchemaVersion = model.objective.identitySchemaVersion,
        identityProvenance = model.objective.identityProvenance
    )
    val tokensInSolver = if (model is QuadraticTetradModel) {
        selectedIndices.mapNotNull { model.tokensInSolver.getOrNull(it) }
    } else {
        emptyList()
    }
    return QuadraticTetradModel(
        impl = BasicQuadraticTetradModel(variables, constraints, "${model.name}_iis"),
        tokensInSolver = tokensInSolver,
        objective = objective
    )
}

/**
 * 执行旧二次弹性/删除过滤算法。 / Execute the legacy quadratic elastic/deletion filtering algorithm.
 *
 * @param model 二次模型视图 / Quadratic model view
 * @param solver 二次求解器 / Quadratic solver
 * @param config IIS 配置 / IIS configuration
 * @return 物化 artifact 与证据来源 / Materialized artifact and evidence source
 */
@OptIn(ExperimentalTime::class)
suspend fun computeLegacyIIS(
    model: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
    config: IISConfig
): Ret<LegacyQuadraticIISResult> {
    val startTime = Clock.System.now()
    val elasticModel = model.elastic()
    val boundAmount = UInt64(elasticModel.constraints.sources.count {
        it == ConstraintSource.ElasticLowerBound || it == ConstraintSource.ElasticUpperBound
    })
    val constraintAmount = UInt64(elasticModel.constraints.sources.count {
        it == ConstraintSource.Elastic
    })

    val elasticFilteringResult = when (val result = performElasticFiltering(
        elasticModel = elasticModel,
        solver = solver,
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
    if (elasticFilteringResult.first) {
        when (val callbackResult = config.computingStatusCallBack?.invoke(
            true,
            Clock.System.now() - startTime,
            IISComputingStatus(
                restBoundAmount = UInt64(elasticFilteringResult.second.keys.count {
                    it.slack?.lowerBound != null || it.slack?.upperBound != null
                }),
                totalBoundAmount = boundAmount,
                restConstraintAmount = UInt64(elasticFilteringResult.second.keys.count {
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

        return Ok(
            LegacyQuadraticIISResult(
                model = dump(model, elasticFilteringResult.second),
                source = InfeasibilityEvidenceSource.ElasticFilter
            )
        )
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
        is Failed -> {
            return Failed(callbackResult.error)
        }

        is Fatal -> {
            return Fatal(callbackResult.errors)
        }
    }

    val relaxedComponents = when (val result = performDeletionFiltering(
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

    return if (relaxedComponents.isEmpty()) {
        Ok(
            LegacyQuadraticIISResult(
                model = snapshotQuadraticModel(model),
                source = InfeasibilityEvidenceSource.DeletionFilter
            )
        )
    } else {
        Ok(
            LegacyQuadraticIISResult(
                model = dump(model, relaxedComponents),
                source = InfeasibilityEvidenceSource.DeletionFilter
            )
        )
    }
}

/**
 * 获取与松弛变量关联的约束索引列表。 / Get constraint indices related to slack variables.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param slackVariables 松弛变量集合 / Set of slack variables
 * @return 关联的约束索引列表 / List of related constraint indices
*/
private fun getRelatedConstraints(
    model: QuadraticTetradModelView,
    slackVariables: Set<Variable>
): List<Int> {
    val originConstraints = slackVariables
        .mapNotNull { it.slack?.constraint }
        .toSet()
    if (originConstraints.isEmpty()) {
        return emptyList()
    }

    return model.constraints.indices.filter { i ->
        model.constraints.origins[i]?.let { it in originConstraints } == true
    }
}

/**
 * IIS 计算中关联变量的数据持有类。 / Data holder for related variables in IIS computation.
 *
 * @property variable 关联的变量 / The related variable
 * @property lowerBound 变量下界（可空）/ Variable lower bound (nullable)
 * @property upperBound 变量上界（可空）/ Variable upper bound (nullable)
*/
private data class RelatedVariable(
    val variable: Variable,
    val lowerBound: Flt64?,
    val upperBound: Flt64?
)

/**
 * 标记变量的边界信息。 / Mark variable bound information.
 *
 * @param marks 标记映射 / Marks map
 * @param index 变量索引 / Variable index
 * @param lowerBound 下界标记 / Lower bound mark
 * @param upperBound 上界标记 / Upper bound mark
*/
private fun markVariable(
    marks: MutableMap<Int, Pair<Boolean, Boolean>>,
    index: Int,
    lowerBound: Boolean,
    upperBound: Boolean
) {
    val previous = marks[index]
    val newLowerBound = previous?.first == true || lowerBound
    val newUpperBound = previous?.second == true || upperBound
    marks[index] = newLowerBound to newUpperBound
}

/**
 * 获取与过滤变量和约束关联的变量列表。 / Get variables related to filter variables and constraints.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param filter 过滤变量集合 / Set of filter variables
 * @param relatedConstraints 关联约束索引列表 / List of related constraint indices
 * @return 关联变量列表 / List of related variables
*/
private fun getRelatedVariables(
    model: QuadraticTetradModelView,
    filter: Set<Variable>,
    relatedConstraints: List<Int>
): List<RelatedVariable> {
    val marks = mutableMapOf<Int, Pair<Boolean, Boolean>>()

    filter.forEach { variable ->
        variable.slack?.lowerBound?.let {
            markVariable(marks, it.index, lowerBound = true, upperBound = false)
        }
        variable.slack?.upperBound?.let {
            markVariable(marks, it.index, lowerBound = false, upperBound = true)
        }
    }
    relatedConstraints.forEach { rowIndex ->
        model.constraints.sparseLhs.forEachEntry(rowIndex) { colIndex1, colIndex2, _ ->
            markVariable(marks, colIndex1, lowerBound = false, upperBound = false)
            colIndex2?.let {
                markVariable(marks, it, lowerBound = false, upperBound = false)
            }
        }
    }

    return marks
        .toList()
        .sortedBy { it.first }
        .map { (index, bounds) ->
            val variable = model.variables[index]
            RelatedVariable(
                variable = variable,
                lowerBound = if (bounds.first) {
                    variable.lowerBound
                } else {
                    null
                },
                upperBound = if (bounds.second) {
                    variable.upperBound
                } else {
                    null
                }
            )
        }
}

/**
 * 按行索引过滤约束并重映射变量索引。 / Filter constraints by row index and remap variable indices.
 *
 * @param constraints 二次约束批处理 / Quadratic constraint batch
 * @param rows 行索引列表 / List of row indices
 * @param oldToNewVariableIndexMap 旧索引到新索引的映射 / Map from old to new variable index
 * @return 过滤后的二次约束批处理 / Filtered quadratic constraint batch
*/
private fun filterConstraintByRowIndex(
    constraints: QuadraticConstraintBatch,
    rows: List<Int>,
    oldToNewVariableIndexMap: Map<Int, Int>
): QuadraticConstraintBatch {
    val relatedRows = rows.distinct().sorted()
    val lhs = relatedRows.mapIndexed { newRowIndex, rowIndex ->
        constraints.lhs[rowIndex].mapNotNull { cell ->
            val newColIndex1 = oldToNewVariableIndexMap[cell.colIndex1] ?: return@mapNotNull null
            val newColIndex2 = cell.colIndex2?.let { oldToNewVariableIndexMap[it] ?: return@mapNotNull null }

            QuadraticConstraintCell(
                rowIndex = newRowIndex,
                colIndex1 = newColIndex1,
                colIndex2 = newColIndex2,
                coefficient = cell.coefficient.copy()
            )
        }
    }
    val sparseLhs = SparseQuadraticMatrix().also { mat ->
        for (row in lhs) {
            val sv = SparseQuadraticVector()
            for (cell in row) {
                sv.add(cell.colIndex1, cell.colIndex2, cell.coefficient)
            }
            mat.addRow(sv)
        }
    }
    return QuadraticConstraintBatch(
        sparseLhs = sparseLhs,
        signs = relatedRows.map { constraints.signs[it] },
        rhs = relatedRows.map { constraints.rhs[it].copy() },
        names = relatedRows.map { constraints.names[it] },
        sources = relatedRows.map { constraints.sources[it] },
        origins = relatedRows.map { constraints.origins[it] },
        froms = relatedRows.map { constraints.froms[it] },
        priorities = relatedRows.map { constraints.priorities[it] },
        ids = relatedRows.map { row ->
            constraints.ids.getOrNull(row)
                ?: ConstraintId("model-local-constraint:$row")
        },
        identityNamespace = constraints.identityNamespace,
        identitySchemaVersion = constraints.identitySchemaVersion,
        identityScopes = relatedRows.map { row ->
            if (constraints.ids.getOrNull(row) == null) {
                ModelElementScope.ModelLocal
            } else {
                constraints.identityScopeAt(row)
            }
        },
        identityOrigins = relatedRows.map { row ->
            if (constraints.ids.getOrNull(row) == null) null else constraints.identityOriginAt(row)
        },
        identityProvenance = relatedRows.map { row ->
            if (constraints.ids.getOrNull(row) == null) emptyList() else constraints.identityProvenanceAt(row)
        }
    )
}

/**
 * 从弹性过滤结果构建 IIS 模型。 / Build IIS model from elastic filter result.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param elasticFilter 弹性过滤结果映射 / Elastic filter result map
 * @return IIS 二次四元模型 / IIS quadratic tetrad model
*/
private fun dump(
    model: QuadraticTetradModelView,
    elasticFilter: Map<Variable, Flt64>
): QuadraticTetradModel {
    return dump(model, elasticFilter.keys)
}

/**
 * 从松弛变量集合构建 IIS 模型。 / Build IIS model from slack variable set.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param slackVariables 松弛变量集合 / Set of slack variables
 * @return IIS 二次四元模型 / IIS quadratic tetrad model
*/
private fun dump(
    model: QuadraticTetradModelView,
    slackVariables: Set<Variable>
): QuadraticTetradModel {
    val relatedConstraints = getRelatedConstraints(
        model = model,
        slackVariables = slackVariables
    )
    val relatedVariables = getRelatedVariables(
        model = model,
        filter = slackVariables,
        relatedConstraints = relatedConstraints
    )
    if (relatedVariables.isEmpty()) {
        return snapshotQuadraticModel(model)
    }

    val oldToNewVariableIndexMap = relatedVariables.withIndex().associate { (newIndex, relatedVariable) ->
        relatedVariable.variable.index to newIndex
    }
    val constraints = filterConstraintByRowIndex(
        constraints = model.constraints,
        rows = relatedConstraints,
        oldToNewVariableIndexMap = oldToNewVariableIndexMap
    )
    val objective = QuadraticObjective(
        category = model.objective.category,
        objective = model.objective.objective.mapNotNull { cell ->
            val newColIndex1 = oldToNewVariableIndexMap[cell.colIndex1] ?: return@mapNotNull null
            val newColIndex2 = cell.colIndex2?.let { oldToNewVariableIndexMap[it] ?: return@mapNotNull null }

            QuadraticObjectiveCell(
                colIndex1 = newColIndex1,
                colIndex2 = newColIndex2,
                coefficient = cell.coefficient.copy()
            )
        },
        constant = model.objective.constant.copy(),
        id = model.objective.id,
        identityScope = model.objective.identityScope,
        identityOrigin = model.objective.identityOrigin,
        identityNamespace = model.objective.identityNamespace,
        identitySchemaVersion = model.objective.identitySchemaVersion,
        identityProvenance = model.objective.identityProvenance
    )

    val tokensInSolver = if (model is QuadraticTetradModel) {
        relatedVariables.mapNotNull { relatedVariable ->
            model.tokensInSolver.getOrNull(relatedVariable.variable.index)
        }
    } else {
        emptyList()
    }

    return QuadraticTetradModel(
        impl = BasicQuadraticTetradModel(
            variables = relatedVariables.mapIndexed { newIndex, relatedVariable ->
                Variable(
                    index = newIndex,
                    lowerBound = relatedVariable.lowerBound ?: Flt64.negativeInfinity,
                    upperBound = relatedVariable.upperBound ?: Flt64.infinity,
                    type = relatedVariable.variable.type,
                    origin = relatedVariable.variable.origin,
                    dualOrigin = relatedVariable.variable.dualOrigin,
                    slack = relatedVariable.variable.slack,
                    name = relatedVariable.variable.name,
                    initialResult = relatedVariable.variable.initialResult,
                    id = relatedVariable.variable.id,
                    identityScope = relatedVariable.variable.identityScope,
                    identityOrigin = relatedVariable.variable.identityOrigin,
                    identityNamespace = relatedVariable.variable.identityNamespace,
                    identitySchemaVersion = relatedVariable.variable.identitySchemaVersion,
                    identityProvenance = relatedVariable.variable.identityProvenance
                )
            },
            constraints = constraints,
            name = "${model.name}_iis"
        ),
        tokensInSolver = tokensInSolver,
        objective = objective
    )
}

/**
 * 执行弹性过滤以识别不可行组件。 / Perform elastic filtering to identify infeasible components.
 *
 * @param elasticModel 弹性二次四元模型视图 / Elastic quadratic tetrad model view
 * @param solver 二次求解器 / Quadratic solver
 * @param config IIS 配置 / IIS configuration
 * @return 是否找到不可行组件及松弛变量映射 / Whether infeasible components were found and the mapping of relaxed variables
*/
private suspend fun performElasticFiltering(
    elasticModel: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
    config: IISConfig
): Ret<Pair<Boolean, Map<Variable, Flt64>>> {
    val relaxVariableBoundsResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable ->
        variable.slack?.lowerBound != null || variable.slack?.upperBound != null
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
 * 松弛满足条件的特定组件。 / Relax specific components satisfying the condition.
 *
 * @param elasticModel 弹性二次四元模型视图 / Elastic quadratic tetrad model view
 * @param solver 二次求解器 / Quadratic solver
 * @param tolerance 松弛容差 / Slack tolerance
 * @param relaxCondition 判断变量是否应松弛的条件 / Condition to determine whether a variable should be relaxed
 * @return 是否找到可行解及松弛变量映射 / Whether a feasible solution was found and the mapping of relaxed variables
*/
private suspend fun relaxSpecificComponents(
    elasticModel: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
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
                ?: return Failed(Err(ErrorCode.ORSolutionInvalid, "Elastic quadratic solve completed without a solution."))
            ProblemStatus.Infeasible,
            ProblemStatus.InfeasibleOrUnbounded -> return Ok(false to emptyMap())
            else -> return Failed(
                ErrorCode.OREngineSolvingException,
                "Elastic quadratic solve returned an inconclusive terminal status: ${result.value.problemStatus}."
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

/**
 * 执行删除过滤以精简不可行组件。 / Perform deletion filtering to refine infeasible components.
 *
 * @param elasticModel 弹性二次四元模型视图 / Elastic quadratic tetrad model view
 * @param solver 二次求解器 / Quadratic solver
 * @param startTime 开始时间 / Start time
 * @param boundAmount 边界数量 / Bound amount
 * @param constraintAmount 约束数量 / Constraint amount
 * @param config IIS 配置 / IIS configuration
 * @return 活跃的松弛变量集合 / Set of active relaxed components
*/
@OptIn(ExperimentalTime::class)
private suspend fun performDeletionFiltering(
    elasticModel: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
    startTime: Instant,
    boundAmount: UInt64,
    constraintAmount: UInt64,
    config: IISConfig
): Ret<Set<Variable>> {
    val slackVariables = elasticModel.variables.filter { it.slack != null }.sortedBy { it.index }
    if (slackVariables.isEmpty()) {
        return Ok(emptySet())
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

    return Ok(activeRelaxedComponents)
}

/**
 * 创建二次模型的快照副本。 / Create a snapshot copy of the quadratic model.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @return 二次模型快照 / Snapshot of the quadratic model
*/
private fun snapshotQuadraticModel(model: QuadraticTetradModelView): QuadraticTetradModel {
    return if (model is QuadraticTetradModel) {
        model.copy()
    } else {
        QuadraticTetradModel(
            BasicQuadraticTetradModel(
                variables = model.variables.map { it.copy() },
                constraints = model.constraints.copy(),
                name = "${model.name}_iis"
            ),
            emptyList(),
            QuadraticObjective(
                category = model.objective.category,
                objective = model.objective.objective.map { it.copy() },
                constant = model.objective.constant.copy(),
                id = model.objective.id,
                identityScope = model.objective.identityScope,
                identityOrigin = model.objective.identityOrigin,
                identityNamespace = model.objective.identityNamespace,
                identitySchemaVersion = model.objective.identitySchemaVersion,
                identityProvenance = model.objective.identityProvenance
            )
        )
    }
}
