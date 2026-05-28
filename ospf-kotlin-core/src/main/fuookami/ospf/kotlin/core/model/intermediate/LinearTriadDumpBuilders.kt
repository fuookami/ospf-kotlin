/**
 * 线性三元模型转储构建器
 * Linear triad model dump builders
 */
package fuookami.ospf.kotlin.core.model.intermediate

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.Quadruple
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

internal fun buildLinearSparseLhs(rows: List<List<LinearConstraintCell>>): SparseMatrix<Flt64> {
    val matrix = SparseMatrix<Flt64>()
    for (row in rows) {
        val sparseVector = SparseVector<Flt64>()
        for (cell in row) {
            sparseVector.add(cell.colIndex, cell.coefficient)
        }
        matrix.addRow(sparseVector)
    }
    return matrix
}

internal fun dumpLinearTriadVariables(
    tokenIndexes: Map<Token<Flt64>, Int>,
    bounds: Map<Token<Flt64>, List<Quadruple<LinearConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>>
): List<Variable> {
    val variables = ArrayList<Variable?>()
    for ((_, _) in tokenIndexes) {
        variables.add(null)
    }
    for ((token, i) in tokenIndexes) {
        val thisBounds = bounds[token] ?: emptyList()
        val lb = thisBounds
            .filter { it.third == ConstraintRelation.GreaterEqual || it.third == ConstraintRelation.Equal }
            .maxOfOrNull { it.fourth }
        val ub = thisBounds
            .filter { it.third == ConstraintRelation.LessEqual || it.third == ConstraintRelation.Equal }
            .minOfOrNull { it.fourth }
        variables[i] = Variable(
            index = i,
            lowerBound = if (lb != null) {
                max(lb, token.lowerBound!!.value.unwrap())
            } else {
                token.lowerBound!!.value.unwrap()
            },
            upperBound = if (ub != null) {
                min(ub, token.upperBound!!.value.unwrap())
            } else {
                token.upperBound!!.value.unwrap()
            },
            type = token.variable.type,
            origin = token.variable,
            dualOrigin = null,
            slack = null,
            name = token.variable.name,
            initialResult = token.result
        )
    }
    return variables.map { it!! }
}

internal fun dumpLinearTriadConstraints(
    model: LinearMechanismModel<Flt64>,
    tokenIndexes: Map<Token<Flt64>, Int>,
    bounds: Map<Token<Flt64>, List<Quadruple<LinearConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
): LinearConstraintBatch {
    val boundConstraints = bounds.values.flatMap { thisBounds ->
        thisBounds.map { it.first }
    }.distinct().toSet()
    val notBoundConstraints = model.linearConstraints.filter { !boundConstraints.contains(it) }

    val constraints = notBoundConstraints.withIndex().map { (index, constraint) ->
        val lhs = ArrayList<LinearConstraintCell>()
        var rhs = constraint.rhs
        for (cell in constraint.lhs) {
            if (tokenIndexes.containsKey(cell.token)) {
                lhs.add(
                    LinearConstraintCell(
                        rowIndex = index,
                        colIndex = tokenIndexes[cell.token]!!,
                        coefficient = cell.coefficient.clampCoefficient()
                    )
                )
            } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                rhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
            }
        }
        lhs to rhs
    }

    val lhs = ArrayList<List<LinearConstraintCell>>()
    val signs = ArrayList<ConstraintRelation>()
    val rhs = ArrayList<Flt64>()
    val names = ArrayList<String>()
    val sources = ArrayList<ConstraintSource>()
    val origins = ArrayList<LinearConstraintImpl<Flt64>>()
    val froms = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
    val priorities = ArrayList<Int?>()
    for ((index, constraint) in notBoundConstraints.withIndex()) {
        lhs.add(constraints[index].first)
        signs.add(constraint.sign)
        rhs.add(constraints[index].second)
        names.add(constraint.name)
        sources.add(ConstraintSource.Origin)
        origins.add(constraint)
        froms.add(constraint.from)
        priorities.add(constraint.origin?.priority)
    }
    return LinearConstraintBatch(
        sparseLhs = buildLinearSparseLhs(lhs),
        signs = signs,
        rhs = rhs,
        names = names,
        sources = sources,
        origins = origins,
        froms = froms,
        priorities = priorities
    )
}

internal suspend fun dumpLinearTriadConstraintsAsync(
    model: LinearMechanismModel<Flt64>,
    tokenIndexes: Map<Token<Flt64>, Int>,
    bounds: Map<Token<Flt64>, List<Quadruple<LinearConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
): LinearConstraintBatch {
    val boundConstraints = bounds.values.flatMap { thisBounds ->
        thisBounds.map { it.first }
    }.distinct().toSet()
    val notBoundConstraints = model.linearConstraints.filter { !boundConstraints.contains(it) }

    val dispatchPlan = computeBatchDispatchPlan(notBoundConstraints.size)
    return if (dispatchPlan.shouldUseParallelPath) {
        val segment = dispatchPlan.segmentSize
        coroutineScope {
            val slices = buildBatchSlices(
                itemCount = notBoundConstraints.size,
                segmentSize = segment
            )
            val constraintPromises = slices.map { slice ->
                async(Dispatchers.Default) {
                    val constraints = ArrayList<Pair<List<LinearConstraintCell>, Flt64>>()
                    for (i in slice.fromIndex until slice.toIndexExclusive) {
                        val constraint = notBoundConstraints[i]
                        val lhs = ArrayList<LinearConstraintCell>()
                        var rhs = constraint.rhs
                        for (cell in constraint.lhs) {
                            if (tokenIndexes.containsKey(cell.token)) {
                                lhs.add(
                                    LinearConstraintCell(
                                        rowIndex = i,
                                        colIndex = tokenIndexes[cell.token]!!,
                                        coefficient = cell.coefficient.clampCoefficient()
                                    )
                                )
                            } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                                rhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
                            }
                        }
                        constraints.add(lhs to rhs)
                    }
                    MemoryCleanupPolicy.cleanupOnPressure()
                    constraints
                }
            }

            val lhs = ArrayList<List<LinearConstraintCell>>()
            val signs = ArrayList<ConstraintRelation>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            val sources = ArrayList<ConstraintSource>()
            val origins = ArrayList<LinearConstraintImpl<Flt64>>()
            val froms = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
            val priorities = ArrayList<Int?>()
            for ((index, constraint) in notBoundConstraints.withIndex()) {
                val slice = slices[index / segment]
                val offset = index - slice.fromIndex
                val (thisLhs, thisRhs) = constraintPromises[index / segment].await()[offset]
                lhs.add(thisLhs)
                signs.add(constraint.sign)
                rhs.add(thisRhs)
                names.add(constraint.name)
                sources.add(ConstraintSource.Origin)
                origins.add(constraint)
                froms.add(constraint.from)
                priorities.add(constraint.origin?.priority)
            }
            LinearConstraintBatch(
                sparseLhs = buildLinearSparseLhs(lhs),
                signs = signs,
                rhs = rhs,
                names = names,
                sources = sources,
                origins = origins,
                froms = froms,
                priorities = priorities
            )
        }
    } else {
        val lhs = ArrayList<List<LinearConstraintCell>>()
        val signs = ArrayList<ConstraintRelation>()
        val rhs = ArrayList<Flt64>()
        val names = ArrayList<String>()
        val sources = ArrayList<ConstraintSource>()
        val origins = ArrayList<LinearConstraintImpl<Flt64>>()
        val froms = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
        val priorities = ArrayList<Int?>()
        for ((index, constraint) in notBoundConstraints.withIndex()) {
            val thisLhs = ArrayList<LinearConstraintCell>()
            var thisRhs = constraint.rhs
            for (cell in constraint.lhs) {
                if (tokenIndexes.containsKey(cell.token)) {
                    thisLhs.add(
                        LinearConstraintCell(
                            rowIndex = index,
                            colIndex = tokenIndexes[cell.token]!!,
                            coefficient = cell.coefficient.clampCoefficient()
                        )
                    )
                } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                    thisRhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
                }
            }
            lhs.add(thisLhs)
            signs.add(constraint.sign)
            rhs.add(thisRhs)
            names.add(constraint.name)
            sources.add(ConstraintSource.Origin)
            origins.add(constraint)
            froms.add(constraint.from)
            priorities.add(constraint.origin?.priority)
        }
        MemoryCleanupPolicy.cleanupAfterBatch()
        LinearConstraintBatch(
            sparseLhs = buildLinearSparseLhs(lhs),
            signs = signs,
            rhs = rhs,
            names = names,
            sources = sources,
            origins = origins,
            froms = froms,
            priorities = priorities
        )
    }
}

internal fun dumpLinearTriadObjectives(
    model: LinearMechanismModel<Flt64>,
    tokenIndexes: Map<Token<Flt64>, Int>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
): LinearObjective {
    val objectiveCategory = if (model.objectFunction.subObjects.size == 1) {
        model.objectFunction.subObjects.first().category
    } else {
        model.objectFunction.category
    }
    val coefficient = (0 until tokenIndexes.size).map { Flt64.zero }.toMutableList()
    var constant = Flt64.zero
    for (subObject in model.objectFunction.subObjects) {
        if (subObject.category == objectiveCategory) {
            for (cell in subObject.cells) {
                if (fixedVariables?.containsKey(cell.token.variable) == true) {
                    constant += cell.coefficient * fixedVariables[cell.token.variable]!!
                } else {
                    val index = tokenIndexes[cell.token] ?: continue
                    coefficient[index] = coefficient[index] + cell.coefficient
                }
            }
            constant += subObject.constant
        } else {
            for (cell in subObject.cells) {
                if (fixedVariables?.containsKey(cell.token.variable) == true) {
                    constant -= cell.coefficient * fixedVariables[cell.token.variable]!!
                } else {
                    val index = tokenIndexes[cell.token] ?: continue
                    coefficient[index] = coefficient[index] - cell.coefficient
                }
            }
            constant -= subObject.constant
        }
    }
    val objective = ArrayList<LinearObjectiveCell>()
    for ((_, i) in tokenIndexes) {
        objective.add(
            LinearObjectiveCell(
                colIndex = i,
                coefficient = coefficient[i].clampCoefficient()
            )
        )
    }
    return LinearObjective(
        category = objectiveCategory,
        objective = objective,
        constant = constant
    )
}
