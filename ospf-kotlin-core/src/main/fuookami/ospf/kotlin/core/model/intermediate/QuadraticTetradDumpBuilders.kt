/**
 * 二次四元模型转储构建器
 * Quadratic tetrad model dump builders
 */
package fuookami.ospf.kotlin.core.model.intermediate

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.Quadruple
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

internal fun buildQuadraticSparseLhs(rows: List<List<QuadraticConstraintCell>>): SparseQuadraticMatrix {
    val matrix = SparseQuadraticMatrix()
    for (row in rows) {
        val sparseVector = SparseQuadraticVector()
        for (cell in row) {
            sparseVector.add(cell.colIndex1, cell.colIndex2, cell.coefficient)
        }
        matrix.addRow(sparseVector)
    }
    return matrix
}

internal fun dumpQuadraticTetradVariables(
    tokenIndexes: Map<Token<Flt64>, Int>,
    bounds: Map<Token<Flt64>, List<Quadruple<QuadraticConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>>
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

internal fun dumpQuadraticTetradConstraints(
    model: QuadraticMechanismModel<Flt64>,
    tokenIndexes: Map<Token<Flt64>, Int>,
    bounds: Map<Token<Flt64>, List<Quadruple<QuadraticConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
): QuadraticConstraintBatch {
    val boundConstraints = bounds.values.flatMap { thisBounds ->
        thisBounds.map { it.first }
    }.distinct().toSet()
    val notBoundConstraints = model.quadraticConstraints.filter { !boundConstraints.contains(it) }

    val constraints = notBoundConstraints.withIndex().map { (index, constraint) ->
        val lhs = ArrayList<QuadraticConstraintCell>()
        var rhs = constraint.rhs
        for (cell in constraint.lhs) {
            if (tokenIndexes.containsKey(cell.token1) && (cell.token2 == null || tokenIndexes.containsKey(cell.token2))) {
                lhs.add(
                    QuadraticConstraintCell(
                        rowIndex = index,
                        colIndex1 = tokenIndexes[cell.token1]!!,
                        colIndex2 = cell.token2?.let { tokenIndexes[it]!! },
                        coefficient = cell.coefficient.clampCoefficient()
                    )
                )
            } else if (tokenIndexes.containsKey(cell.token1)) {
                lhs.add(
                    QuadraticConstraintCell(
                        rowIndex = index,
                        colIndex1 = tokenIndexes[cell.token1]!!,
                        colIndex2 = null,
                        coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                    )
                )
            } else if (tokenIndexes.containsKey(cell.token2)) {
                lhs.add(
                    QuadraticConstraintCell(
                        rowIndex = index,
                        colIndex1 = tokenIndexes[cell.token2]!!,
                        colIndex2 = null,
                        coefficient = (cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)).clampCoefficient()
                    )
                )
            } else {
                rhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable) ?: Flt64.one)
            }
        }
        lhs to rhs
    }

    val lhs = ArrayList<List<QuadraticConstraintCell>>()
    val signs = ArrayList<ConstraintRelation>()
    val rhs = ArrayList<Flt64>()
    val names = ArrayList<String>()
    val sources = ArrayList<ConstraintSource>()
    val origins = ArrayList<QuadraticConstraintImpl<Flt64>>()
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
    return QuadraticConstraintBatch(
        sparseLhs = buildQuadraticSparseLhs(lhs),
        signs = signs,
        rhs = rhs,
        names = names,
        sources = sources,
        origins = origins,
        froms = froms,
        priorities = priorities
    )
}

internal suspend fun dumpQuadraticTetradConstraintsAsync(
    model: QuadraticMechanismModel<Flt64>,
    tokenIndexes: Map<Token<Flt64>, Int>,
    bounds: Map<Token<Flt64>, List<Quadruple<QuadraticConstraintImpl<Flt64>, Token<Flt64>, ConstraintRelation, Flt64>>>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
): QuadraticConstraintBatch {
    val boundConstraints = bounds.values.flatMap { thisBounds ->
        thisBounds.map { it.first }
    }.distinct().toSet()
    val notBoundConstraints = model.quadraticConstraints.filter { !boundConstraints.contains(it) }

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
                    val constraints = ArrayList<Pair<List<QuadraticConstraintCell>, Flt64>>()
                    for (i in slice.fromIndex until slice.toIndexExclusive) {
                        val constraint = notBoundConstraints[i]
                        val lhs = ArrayList<QuadraticConstraintCell>()
                        var rhs = constraint.rhs
                        for (cell in constraint.lhs) {
                            if (tokenIndexes.containsKey(cell.token1) && (cell.token2 == null || tokenIndexes.containsKey(cell.token2))) {
                                lhs.add(
                                    QuadraticConstraintCell(
                                        rowIndex = i,
                                        colIndex1 = tokenIndexes[cell.token1]!!,
                                        colIndex2 = cell.token2?.let { token -> tokenIndexes[token]!! },
                                        coefficient = cell.coefficient.clampCoefficient()
                                    )
                                )
                            } else if (tokenIndexes.containsKey(cell.token1)) {
                                lhs.add(
                                    QuadraticConstraintCell(
                                        rowIndex = i,
                                        colIndex1 = tokenIndexes[cell.token1]!!,
                                        colIndex2 = null,
                                        coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                                    )
                                )
                            } else if (tokenIndexes.containsKey(cell.token2)) {
                                lhs.add(
                                    QuadraticConstraintCell(
                                        rowIndex = i,
                                        colIndex1 = tokenIndexes[cell.token2]!!,
                                        colIndex2 = null,
                                        coefficient = (cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)).clampCoefficient()
                                    )
                                )
                            } else {
                                rhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable)
                                    ?: Flt64.one)
                            }
                        }
                        constraints.add(lhs to rhs)
                    }
                    MemoryCleanupPolicy.cleanupOnPressure()
                    constraints
                }
            }

            val lhs = ArrayList<List<QuadraticConstraintCell>>()
            val signs = ArrayList<ConstraintRelation>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            val sources = ArrayList<ConstraintSource>()
            val origins = ArrayList<QuadraticConstraintImpl<Flt64>>()
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
            MemoryCleanupPolicy.cleanupAfterBatch()
            QuadraticConstraintBatch(
                sparseLhs = buildQuadraticSparseLhs(lhs),
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
        val lhs = ArrayList<List<QuadraticConstraintCell>>()
        val signs = ArrayList<ConstraintRelation>()
        val rhs = ArrayList<Flt64>()
        val names = ArrayList<String>()
        val sources = ArrayList<ConstraintSource>()
        val origins = ArrayList<QuadraticConstraintImpl<Flt64>>()
        val froms = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
        val priorities = ArrayList<Int?>()
        for ((index, constraint) in notBoundConstraints.withIndex()) {
            val thisLhs = ArrayList<QuadraticConstraintCell>()
            var thisRhs = constraint.rhs
            for (cell in constraint.lhs) {
                if (tokenIndexes.containsKey(cell.token1) && (cell.token2 == null || tokenIndexes.containsKey(cell.token2))) {
                    thisLhs.add(
                        QuadraticConstraintCell(
                            rowIndex = index,
                            colIndex1 = tokenIndexes[cell.token1]!!,
                            colIndex2 = cell.token2?.let { token -> tokenIndexes[token]!! },
                            coefficient = cell.coefficient.clampCoefficient()
                        )
                    )
                } else if (tokenIndexes.containsKey(cell.token1)) {
                    thisLhs.add(
                        QuadraticConstraintCell(
                            rowIndex = index,
                            colIndex1 = tokenIndexes[cell.token1]!!,
                            colIndex2 = null,
                            coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                        )
                    )
                } else if (tokenIndexes.containsKey(cell.token2)) {
                    thisLhs.add(
                        QuadraticConstraintCell(
                            rowIndex = index,
                            colIndex1 = tokenIndexes[cell.token2]!!,
                            colIndex2 = null,
                            coefficient = (cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)).clampCoefficient()
                        )
                    )
                } else {
                    thisRhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable)
                        ?: Flt64.one)
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
        QuadraticConstraintBatch(
            sparseLhs = buildQuadraticSparseLhs(lhs),
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

internal fun dumpQuadraticTetradObjectives(
    model: QuadraticMechanismModel<Flt64>,
    tokenIndexes: Map<Token<Flt64>, Int>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
): QuadraticObjective {
    val objectiveCategory = if (model.objectFunction.subObjects.size == 1) {
        model.objectFunction.subObjects.first().category
    } else {
        model.objectFunction.category
    }

    val coefficient = (0 until tokenIndexes.size).map { HashMap<Int?, Flt64>() }.toMutableList()
    var constant = Flt64.zero
    for (subObject in model.objectFunction.subObjects) {
        if (subObject.category == objectiveCategory) {
            for (cell in subObject.cells) {
                val t2 = cell.token2
                if (fixedVariables?.containsKey(cell.token1.variable) == true && (t2 == null || fixedVariables[t2.variable] != null)) {
                    constant += cell.coefficient * fixedVariables[cell.token1.variable]!! * (fixedVariables[t2?.variable] ?: Flt64.one)
                } else if (fixedVariables?.containsKey(cell.token1.variable) == true) {
                    val index = tokenIndexes[t2] ?: continue
                    coefficient[index][null] = (coefficient[index][null] ?: Flt64.zero) + cell.coefficient * fixedVariables[cell.token1.variable]!!
                } else if (fixedVariables?.containsKey(cell.token2?.variable) == true) {
                    val index = tokenIndexes[cell.token1] ?: continue
                    coefficient[index][null] = (coefficient[index][null] ?: Flt64.zero) + cell.coefficient * fixedVariables[cell.token2!!.variable]!!
                } else {
                    val index = tokenIndexes[cell.token1] ?: continue
                    val index2 = if (cell.token2 != null) {
                        tokenIndexes[cell.token2] ?: continue
                    } else {
                        null
                    }
                    coefficient[index][index2] = (coefficient[index][index2] ?: Flt64.zero) + cell.coefficient
                }
            }
            constant += subObject.constant
        } else {
            for (cell in subObject.cells) {
                val t2 = cell.token2
                if (fixedVariables?.containsKey(cell.token1.variable) == true && (t2 == null || fixedVariables[t2.variable] != null)) {
                    constant -= cell.coefficient * fixedVariables[cell.token1.variable]!! * (fixedVariables[t2?.variable] ?: Flt64.one)
                } else if (fixedVariables?.containsKey(cell.token1.variable) == true) {
                    val index = tokenIndexes[t2] ?: continue
                    coefficient[index][null] = (coefficient[index][null] ?: Flt64.zero) - cell.coefficient * fixedVariables[cell.token1.variable]!!
                } else if (fixedVariables?.containsKey(cell.token2?.variable) == true) {
                    val index = tokenIndexes[cell.token1] ?: continue
                    coefficient[index][null] = (coefficient[index][null] ?: Flt64.zero) - cell.coefficient * fixedVariables[cell.token2!!.variable]!!
                } else {
                    val index = tokenIndexes[cell.token1] ?: continue
                    val index2 = if (cell.token2 != null) {
                        tokenIndexes[cell.token2] ?: continue
                    } else {
                        null
                    }
                    coefficient[index][index2] = (coefficient[index][index2] ?: Flt64.zero) + cell.coefficient
                }
            }
            constant -= subObject.constant
        }
    }

    val objective = ArrayList<QuadraticObjectiveCell>()
    for ((_, i) in tokenIndexes) {
        for ((j, value) in coefficient[i]) {
            objective.add(
                QuadraticObjectiveCell(
                    colIndex1 = i,
                    colIndex2 = j,
                    coefficient = value.clampCoefficient()
                )
            )
        }
    }
    return QuadraticObjective(
        category = objectiveCategory,
        objective = objective,
        constant = constant
    )
}
