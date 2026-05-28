@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.iis

import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver

/**
 * 二次模型 IIS 计算
 * Quadratic model IIS computation
 */

/**
 * 计算二次模型的不可行子系统（IIS）。
 * Compute the Irreducible Infeasible Subsystem (IIS) for a quadratic model.
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

        return Ok(dump(model, elasticFilteringResult.second))
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

    return if (relaxedComponents.isEmpty()) {
        Ok(snapshotQuadraticModel(model))
    } else {
        Ok(dump(model, relaxedComponents))
    }
}

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

private data class RelatedVariable(
    val variable: Variable,
    val lowerBound: Flt64?,
    val upperBound: Flt64?
)

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

private fun filterConstraintByRowIndex(
    constraints: QuadraticConstraintBatch,
    rows: List<Int>,
    oldToNewVariableIndexMap: Map<Int, Int>
): QuadraticConstraintBatch {
    val relatedRows = rows.distinct().sorted()

    @Suppress("DEPRECATION")
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
        priorities = relatedRows.map { constraints.priorities[it] }
    )
}

private fun dump(
    model: QuadraticTetradModelView,
    elasticFilter: Map<Variable, Flt64>
): QuadraticTetradModel {
    return dump(model, elasticFilter.keys)
}

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
        constant = model.objective.constant.copy()
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
                    initialResult = relatedVariable.variable.initialResult
                )
            },
            constraints = constraints,
            name = "${model.name}_iis"
        ),
        tokensInSolver = tokensInSolver,
        objective = objective
    )
}

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

    val result = when (val result = solver(elasticModel)) {
        is Ok -> {
            result.value
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
        if (variable.slack != null && result.solution.size > variable.index && result.solution[variable.index] geq tolerance) {
            variable to result.solution[variable.index]
        } else {
            null
        }
    }
    return Ok(true to relaxedComponents)
}

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
                true
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
                constant = model.objective.constant.copy()
            )
        )
    }
}
