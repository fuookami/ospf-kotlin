package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.solver.QuadraticSolver
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.intermediate_model.ObjectCategory
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticDualSolution
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticMechanismModelF64
import fuookami.ospf.kotlin.core.intermediate_model.ConstraintRelation
import fuookami.ospf.kotlin.core.token.TokenF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.BalancedTernary
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.Integer
import fuookami.ospf.kotlin.core.variable.Percentage
import fuookami.ospf.kotlin.core.variable.Ternary
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.core.variable.VariableCombinationItem
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.math.operator.pow
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.operator.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.kotlin.logger
import java.io.OutputStreamWriter

typealias OriginQuadraticConstraint = fuookami.ospf.kotlin.core.intermediate_model.QuadraticConstraintImpl

private fun buildSparseLhs(rows: List<List<QuadraticConstraintCell>>): SparseQuadraticMatrix {
    val mat = SparseQuadraticMatrix()
    for (row in rows) {
        val sv = SparseQuadraticVector()
        for (cell in row) {
            sv.add(cell.colIndex1, cell.colIndex2, cell.coefficient)
        }
        mat.addRow(sv)
    }
    return mat
}

private fun OriginQuadraticConstraint.isBound(): Boolean {
    return lhs.size == 1
            && lhs.first().coefficient eq Flt64.one
            && lhs.first().token2 == null
    // && from?.second != true
}

class QuadraticConstraintCell(
    override val rowIndex: Int,
    val colIndex1: Int,
    val colIndex2: Int?,
    coefficient: Flt64
) : ConstraintCell<QuadraticConstraintCell>, Cloneable, Copyable<QuadraticConstraintCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): QuadraticConstraintCell {
        return QuadraticConstraintCell(
            rowIndex = rowIndex,
            colIndex1 = colIndex1,
            colIndex2 = colIndex2,
            coefficient = -coefficient
        )
    }

    override fun copy() = QuadraticConstraintCell(
        rowIndex = rowIndex,
        colIndex1 = colIndex1,
        colIndex2 = colIndex2,
        coefficient = coefficient.copy()
    )

    override fun clone() = copy()
}

class QuadraticConstraintBatch(
    val sparseLhs: SparseQuadraticMatrix,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    origins: List<OriginQuadraticConstraint?> = (0 until sparseLhs.numRows()).map { null },
    froms: List<Pair<IntermediateSymbol<*>, Boolean>?> = (0 until sparseLhs.numRows()).map { null },
    priorities: List<Int?> = (0 until sparseLhs.numRows()).map { null }
) : ModelConstraint<QuadraticConstraintCell>(sparseLhs.numRows(), signs, rhs, names, sources) {
    /**
     * Sparse representation of the quadratic LHS matrix.
     * Each row is a SparseQuadraticVector where entries carry (colIndex1, colIndex2?, coefficient).
     * This is the primary constraint representation.
     */

    @Deprecated("Use sparseLhs instead. Will be removed in E7.", level = DeprecationLevel.WARNING)
    override val lhs: List<List<QuadraticConstraintCell>> by lazy {
        sparseLhs.rows.mapIndexed { rowIndex, row ->
            row.entries.map { entry ->
                QuadraticConstraintCell(
                    rowIndex = rowIndex,
                    colIndex1 = entry.colIndex1,
                    colIndex2 = entry.colIndex2,
                    coefficient = entry.coefficient
                )
            }
        }
    }

    private val _origins: MutableList<OriginQuadraticConstraint?> = origins.toMutableList()
    val origins: List<OriginQuadraticConstraint?> by ::_origins

    private val _froms: MutableList<Pair<IntermediateSymbol<*>, Boolean>?> = froms.toMutableList()
    val froms: List<Pair<IntermediateSymbol<*>, Boolean>?> by ::_froms

    private val _priorities: MutableList<Int?> = priorities.toMutableList()
    val priorities: List<Int?> by ::_priorities

    override fun copy() = QuadraticConstraintBatch(
        SparseQuadraticMatrix().also { mat ->
            for (row in sparseLhs.rows) {
                val newRow = SparseQuadraticVector()
                for (entry in row.entries) {
                    newRow.add(entry.colIndex1, entry.colIndex2, entry.coefficient.copy())
                }
                mat.addRow(newRow)
            }
        },
        signs.toList(),
        rhs.map { it.copy() },
        names.toList(),
        sources.toList(),
        origins.toList(),
        froms.toList(),
        priorities.toList()
    )

    override fun close() {
        _origins.clear()
        _froms.clear()
        _priorities.clear()
        super.close()
    }
}

class QuadraticObjectiveCell(
    val colIndex1: Int,
    val colIndex2: Int?,
    coefficient: Flt64
) : ModelCell<QuadraticObjectiveCell>, Cloneable, Copyable<QuadraticObjectiveCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): QuadraticObjectiveCell {
        return QuadraticObjectiveCell(
            colIndex1 = colIndex1,
            colIndex2 = colIndex2,
            coefficient = -coefficient
        )
    }

    override fun copy() = QuadraticObjectiveCell(
        colIndex1 = colIndex1,
        colIndex2 = colIndex2,
        coefficient = coefficient.copy()
    )

    override fun clone() = copy()
}

typealias QuadraticObjective = Objective<QuadraticObjectiveCell>

/**
 * A basic quadratic intermediate model (tetrad: variables + constraints, no objective).
 *
 * This is the solver-standard form for quadratic problems without an objective function.
 * It is used directly by IIS (Irreducible Infeasible Subsystem) computation and
 * as the [impl] delegate inside [QuadraticTetradModel].
 *
 * ### Construction
 *
 * Direct constructor:
 * ```kotlin
 * BasicQuadraticTetradModel(variables, constraints, name)
 * ```
 *
 * Factory from a [QuadraticMechanismModel]:
 * ```kotlin
 * BasicQuadraticTetradModel.from(mechanismModel, tokenIndexMap, bounds, fixedVariables)
 * ```
 *
 * ### Relationship to [QuadraticTetradModel]
 *
 * [QuadraticTetradModel] wraps a [BasicQuadraticTetradModel] as its `impl`, adding
 * objective function and token-to-solver mapping. [BasicQuadraticTetradModel] is
 * the subset that only contains variables and constraints.
 *
 * @param variables   solver-indexed variable list
 * @param constraints quadratic constraint batch (sparse lhs, signs, rhs, origins)
 * @param name        model name for logging and debugging
 */
class BasicQuadraticTetradModel(
    override val variables: List<Variable>,
    override val constraints: QuadraticConstraintBatch,
    override val name: String
) : BasicModelView<QuadraticConstraintCell>, Cloneable, Copyable<BasicQuadraticTetradModel> {
    companion object {
        /**
         * Create a [BasicQuadraticTetradModel] from a [QuadraticMechanismModelF64] by
         * extracting variables and constraints into solver-standard form.
         *
         * This is a convenience factory that mirrors the variable/constraint extraction
         * logic in [QuadraticTetradModel.invoke] without the objective function step.
         *
         * @param model           the source mechanism model
         * @param tokenIndexMap   mapping from tokens to solver column indices
         * @param bounds          pre-computed bound constraints per token
         * @param fixedVariables  variables fixed to constant values (substituted out)
         * @return a [BasicQuadraticTetradModel] containing the extracted variables and constraints
         */
        fun from(
            model: QuadraticMechanismModelF64,
            tokenIndexMap: Map<TokenF64, Int>,
            bounds: Map<TokenF64, List<Quadruple<OriginQuadraticConstraint, TokenF64, ConstraintRelation, Flt64>>> = emptyMap(),
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): BasicQuadraticTetradModel {
            val variables = dumpVariables(model, tokenIndexMap, bounds)
            val constraints = dumpConstraints(model, tokenIndexMap, bounds, fixedVariables)
            return BasicQuadraticTetradModel(variables, constraints, model.name)
        }

        private fun dumpVariables(
            model: QuadraticMechanismModelF64,
            tokenIndexes: Map<TokenF64, Int>,
            bounds: Map<TokenF64, List<Quadruple<OriginQuadraticConstraint, TokenF64, ConstraintRelation, Flt64>>>
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

        private fun dumpConstraints(
            model: QuadraticMechanismModelF64,
            tokenIndexes: Map<TokenF64, Int>,
            bounds: Map<TokenF64, List<Quadruple<OriginQuadraticConstraint, TokenF64, ConstraintRelation, Flt64>>>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): QuadraticConstraintBatch {
            val boundConstraints = bounds.values.flatMap { thisBounds ->
                thisBounds.map { it.first }
            }.distinct().toSet()
            val notBoundConstraints = model.quadraticConstraints.filter { !boundConstraints.contains(it) }

            val lhs = ArrayList<List<QuadraticConstraintCell>>()
            val signs = ArrayList<ConstraintRelation>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            val sources = ArrayList<ConstraintSource>()
            val origins = ArrayList<OriginQuadraticConstraint>()
            val froms = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
            val priorities = ArrayList<Int?>()
            for ((index, constraint) in notBoundConstraints.withIndex()) {
                val constraintLhs = ArrayList<QuadraticConstraintCell>()
                var constraintRhs = constraint.rhs
                for (cell in constraint.lhs) {
                    if (tokenIndexes.containsKey(cell.token1) && (cell.token2 == null || tokenIndexes.containsKey(cell.token2))) {
                        constraintLhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token1]!!,
                                colIndex2 = cell.token2?.let { tokenIndexes[it]!! },
                                coefficient = cell.coefficient.clampCoefficient()
                            )
                        )
                    } else if (tokenIndexes.containsKey(cell.token1)) {
                        constraintLhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token1]!!,
                                colIndex2 = null,
                                coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                            )
                        )
                    } else if (tokenIndexes.containsKey(cell.token2)) {
                        constraintLhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token2]!!,
                                colIndex2 = null,
                                coefficient = (cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)).clampCoefficient()
                            )
                        )
                    } else {
                        constraintRhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable) ?: Flt64.one)
                    }
                }
                lhs.add(constraintLhs)
                signs.add(constraint.sign)
                rhs.add(constraintRhs)
                names.add(constraint.name)
                sources.add(ConstraintSource.Origin)
                origins.add(constraint)
                froms.add(constraint.from)
                priorities.add(constraint.origin?.priority)
            }
            return QuadraticConstraintBatch(
                sparseLhs = buildSparseLhs(lhs),
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
    override fun copy() = BasicQuadraticTetradModel(
        variables.map { it.copy() },
        constraints.copy(),
        name
    )

    override fun clone() = copy()

    fun linearRelax() {
        variables.forEach {
            when (it.type) {
                is Binary -> {
                    it._type = Percentage
                }

                is Ternary, is UInteger -> {
                    it._type = UContinuous
                }

                is BalancedTernary, is fuookami.ospf.kotlin.core.variable.Integer -> {
                    it._type = Continuous
                }

                else -> {}
            }
        }
    }

    fun linearRelaxed(): BasicQuadraticTetradModel {
        return BasicQuadraticTetradModel(
            variables = variables.map {
                when (it.type) {
                    is Binary -> {
                        val ret = it.copy()
                        ret._type = Percentage
                        ret
                    }

                    is Ternary, is UInteger -> {
                        val ret = it.copy()
                        ret._type = UContinuous
                        ret
                    }

                    is BalancedTernary, is fuookami.ospf.kotlin.core.variable.Integer -> {
                        val ret = it.copy()
                        ret._type = Continuous
                        ret
                    }

                    else -> it.copy()
                }
            },
            constraints = constraints.copy(),
            name = name
        )
    }

    @Suppress("DEPRECATION")
    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.append("Subject To\n")
        for (i in constraints.indices) {
            writer.append(" ${constraints.names[i]}: ")
            var flag = false
            for (j in constraints.lhs[i].indices) {
                if (constraints.lhs[i][j].coefficient eq Flt64.zero) {
                    continue
                }

                val coefficient = if (flag) {
                    if (constraints.lhs[i][j].coefficient leq Flt64.zero) {
                        writer.append(" - ")
                    } else {
                        writer.append(" + ")
                    }
                    abs(constraints.lhs[i][j].coefficient)
                } else {
                    constraints.lhs[i][j].coefficient
                }
                if (coefficient neq Flt64.zero) {
                    if (coefficient neq Flt64.one) {
                        writer.append("$coefficient ")
                    }
                    if (constraints.lhs[i][j].colIndex2 == null) {
                        writer.append("${variables[constraints.lhs[i][j].colIndex1]}")
                    } else {
                        writer.append("${variables[constraints.lhs[i][j].colIndex1]} * ${variables[constraints.lhs[i][j].colIndex2!!]}")
                    }
                }
                flag = true
            }
            if (!flag) {
                writer.append("0")
            }
            writer.append(" ${constraints.signs[i]} ${constraints.rhs[i]}\n")
        }
        writer.append("\n")

        writer.append("Bounds\n")
        for (variable in variables) {
            val lowerInf = variable.lowerBound.isNegativeInfinity()
            val upperInf = variable.upperBound.isInfinity()
            if (lowerInf && upperInf) {
                writer.append(" $variable free\n")
            } else if (lowerInf) {
                writer.append(" $variable <= ${variable.upperBound}\n")
            } else if (upperInf) {
                writer.append(" $variable >= ${variable.lowerBound}\n")
            } else {
                if (variable.lowerBound eq variable.upperBound) {
                    writer.append(" $variable = ${variable.lowerBound}\n")
                } else {
                    writer.append(" ${variable.lowerBound} <= $variable <= ${variable.upperBound}\n")
                }
            }
        }
        writer.append("\n")

        if (containsBinary) {
            writer.append("Binaries\n")
            for (variable in variables) {
                if (variable.type.isBinaryType) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        if (containsNotBinaryInteger) {
            writer.append("Generals\n")
            for (variable in variables) {
                if (variable.type.isNotBinaryIntegerType) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        writer.append("End\n")
        return ok
    }
}

interface QuadraticTetradModelView : ModelView<QuadraticConstraintCell, QuadraticObjectiveCell> {
    override val constraints: QuadraticConstraintBatch
    val dual: Boolean

    fun linearRelax(): QuadraticTetradModelView
    fun linearRelaxed(): QuadraticTetradModelView
    suspend fun farkasDual(): QuadraticTetradModelView
    fun feasibility(): QuadraticTetradModelView
    fun elastic(): QuadraticTetradModelView

    fun tidyDualSolution(solution: Solution): QuadraticDualSolution {
        return if (dual) {
            variables.associateNotNull {
                if (it.dualOrigin != null && solution.size > it.index) {
                    (it.dualOrigin as OriginQuadraticConstraint) to solution[it.index]
                } else {
                    null
                }
            }
        } else {
            constraints.indices.associateNotNull {
                if (constraints.origins[it] != null && solution.size > it) {
                    constraints.origins[it]!! to solution[it]
                } else {
                    null
                }
            }
        }
    }
}

data class QuadraticTetradModel(
    private val impl: BasicQuadraticTetradModel,
    val tokensInSolver: List<TokenF64>,
    override val objective: QuadraticObjective,
    internal val dualOrigin: QuadraticTetradModelView? = null
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraintBatch by impl::constraints
    override val name: String by impl::name
    override val dual get() = dualOrigin != null

    companion object {
        private val logger = logger()

        /** V→Flt64 conversion boundary: generic V resolves to concrete Flt64 for quadratic intermediate model construction. */
        suspend operator fun invoke(
            model: QuadraticMechanismModelF64,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            dumpConstraintsToBounds: Boolean? = null,
            forceDumpBounds: Boolean? = null,
            concurrent: Boolean? = null
        ): QuadraticTetradModel {
            logger.trace("Creating QuadraticTetradModel for $model")
            val tokensInSolver = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokensInSolver
            } else {
                model.tokens.tokensInSolverWithout(fixedVariables.keys)
            }
            val tokenIndexMap = tokensInSolver.withIndex().associate { (index, token) -> token to index }
            val bounds = model.quadraticConstraints
                .flatMap { constraint ->
                    if ((dumpConstraintsToBounds ?: true) && constraint.isBound()) {
                        listOf(Quadruple(constraint, constraint.lhs.first().token1, constraint.sign, constraint.rhs))
                    } else if (forceDumpBounds ?: false) {
                        if (constraint.lhs.size == 1 && constraint.lhs.first().token2 == null) {
                            listOf(Quadruple(constraint, constraint.lhs.first().token1, constraint.sign, constraint.rhs / constraint.lhs.first().coefficient))
                        } else if (constraint.lhs.all { it.coefficient eq Flt64.one && it.token2 == null && it.token1.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == ConstraintRelation.LessEqual || constraint.sign == ConstraintRelation.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token1, ConstraintRelation.Equal, Flt64.zero) }
                        } else if (constraint.lhs.all { it.coefficient eq -Flt64.one && it.token2 == null && it.token1.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == ConstraintRelation.GreaterEqual || constraint.sign == ConstraintRelation.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token1, ConstraintRelation.Equal, Flt64.zero) }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }.groupBy { it.second }
            val tetradModel = if (concurrent ?: model.concurrent) {
                coroutineScope {
                    val variablePromise = async(Dispatchers.Default) {
                        dumpVariables(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds
                        )
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpConstraintsAsync(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables
                        )
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpObjectives(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            fixedVariables = fixedVariables
                        )
                    }

                    QuadraticTetradModel(
                        impl = BasicQuadraticTetradModel(
                            variables = variablePromise.await(),
                            constraints = constraintPromise.await(),
                            name = model.name
                        ),
                        tokensInSolver = tokensInSolver,
                        objective = objectivePromise.await()
                    )
                }
            } else {
                QuadraticTetradModel(
                    impl = BasicQuadraticTetradModel(
                        variables = dumpVariables(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds
                        ),
                        constraints = dumpConstraints(
                            model = model,
                            tokenIndexes = tokenIndexMap,
                            bounds = bounds,
                            fixedVariables = fixedVariables
                        ),
                        name = model.name
                    ),
                    tokensInSolver = tokensInSolver,
                    objective = dumpObjectives(
                        model = model,
                        tokenIndexes = tokenIndexMap,
                        fixedVariables = fixedVariables
                    )
                )
            }

            logger.trace("QuadraticTetradModel created for $model")
            System.gc()
            return tetradModel
        }

        @Suppress("UNUSED_PARAMETER")
        private fun dumpVariables(
            model: QuadraticMechanismModelF64,
            tokenIndexes: Map<TokenF64, Int>,
            bounds: Map<TokenF64, List<Quadruple<OriginQuadraticConstraint, TokenF64, ConstraintRelation, Flt64>>>
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

        private fun dumpConstraints(
            model: QuadraticMechanismModelF64,
            tokenIndexes: Map<TokenF64, Int>,
            bounds: Map<TokenF64, List<Quadruple<OriginQuadraticConstraint, TokenF64, ConstraintRelation, Flt64>>>,
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
                        assert(cell.token2 != null)
                        lhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token1]!!,
                                colIndex2 = null,
                                coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                            )
                        )
                    } else if (tokenIndexes.containsKey(cell.token2)) {
                        assert(cell.token2 != null)
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
            val origins = ArrayList<OriginQuadraticConstraint>()
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
                sparseLhs = buildSparseLhs(lhs),
                signs = signs,
                rhs = rhs,
                names = names,
                sources = sources,
                origins = origins,
                froms = froms,
                priorities = priorities
            )
        }

        private suspend fun dumpConstraintsAsync(
            model: QuadraticMechanismModelF64,
            tokenIndexes: Map<TokenF64, Int>,
            bounds: Map<TokenF64, List<Quadruple<OriginQuadraticConstraint, TokenF64, ConstraintRelation, Flt64>>>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): QuadraticConstraintBatch {
            val boundConstraints = bounds.values.flatMap { thisBounds ->
                thisBounds.map { it.first }
            }.distinct().toSet()
            val notBoundConstraints = model.quadraticConstraints.filter { !boundConstraints.contains(it) }

            return if (Runtime.getRuntime().availableProcessors() > 2 && notBoundConstraints.size > Runtime.getRuntime().availableProcessors()) {
                val factor = Flt64(notBoundConstraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
                val segment = if (factor >= 1) {
                    pow(UInt64.ten, factor).toInt()
                } else {
                    10
                }
                coroutineScope {
                    val constraintPromises = (0..(notBoundConstraints.size / segment)).map {
                        async(Dispatchers.Default) {
                            val constraints = ArrayList<Pair<List<QuadraticConstraintCell>, Flt64>>()
                            for (i in (it * segment) until minOf(notBoundConstraints.size, (it + 1) * segment)) {
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
                                        assert(cell.token2 != null)
                                        lhs.add(
                                            QuadraticConstraintCell(
                                                rowIndex = i,
                                                colIndex1 = tokenIndexes[cell.token1]!!,
                                                colIndex2 = null,
                                                coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                                            )
                                        )
                                    } else if (tokenIndexes.containsKey(cell.token2)) {
                                        assert(cell.token2 != null)
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
                            if (memoryUseOver()) {
                                System.gc()
                            }
                            constraints
                        }
                    }

                    val lhs = ArrayList<List<QuadraticConstraintCell>>()
                    val signs = ArrayList<ConstraintRelation>()
                    val rhs = ArrayList<Flt64>()
                    val names = ArrayList<String>()
                    val sources = ArrayList<ConstraintSource>()
                    val origins = ArrayList<OriginQuadraticConstraint>()
                    val froms = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
                    val priorities = ArrayList<Int?>()
                    for ((index, constraint) in notBoundConstraints.withIndex()) {
                        val (thisLhs, thisRhs) = constraintPromises[index / segment].await()[index % segment]
                        lhs.add(thisLhs)
                        signs.add(constraint.sign)
                        rhs.add(thisRhs)
                        names.add(constraint.name)
                        sources.add(ConstraintSource.Origin)
                        origins.add(constraint)
                        froms.add(constraint.from)
                        priorities.add(constraint.origin?.priority)
                    }
                    System.gc()
                    QuadraticConstraintBatch(
                        sparseLhs = buildSparseLhs(lhs),
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
                val origins = ArrayList<OriginQuadraticConstraint>()
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
                            assert(cell.token2 != null)
                            thisLhs.add(
                                QuadraticConstraintCell(
                                    rowIndex = index,
                                    colIndex1 = tokenIndexes[cell.token1]!!,
                                    colIndex2 = null,
                                    coefficient = (cell.coefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)).clampCoefficient()
                                )
                            )
                        } else if (tokenIndexes.containsKey(cell.token2)) {
                            assert(cell.token2 != null)
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
                System.gc()
                QuadraticConstraintBatch(
                    sparseLhs = buildSparseLhs(lhs),
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

        private fun dumpObjectives(
            model: QuadraticMechanismModelF64,
            tokenIndexes: Map<TokenF64, Int>,
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
                            assert(t2 != null)
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
                            assert(t2 != null)
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
            return QuadraticObjective(objectiveCategory, objective)
        }
    }

    override fun copy() = QuadraticTetradModel(
        impl = impl.copy(),
        tokensInSolver = tokensInSolver,
        objective = objective.copy()
    )

    override fun clone() = copy()

    override fun linearRelax(): QuadraticTetradModel {
        impl.linearRelax()
        return this
    }

    override fun linearRelaxed(): QuadraticTetradModel {
        return QuadraticTetradModel(
            impl = impl.linearRelaxed(),
            tokensInSolver = tokensInSolver,
            objective = objective.copy()
        )
    }

    @Deprecated("Quadratic dual is not supported — Rust has no public API for this")
    suspend fun dual(): QuadraticTetradModel {
        throw UnsupportedOperationException("Quadratic dual is not supported")
    }

    @Deprecated("Quadratic farkas dual is not supported — Rust has no public API for this")
    override suspend fun farkasDual(): QuadraticTetradModel {
        throw UnsupportedOperationException("Quadratic farkas dual is not supported")
    }

    @Suppress("DEPRECATION")
    override fun feasibility(): QuadraticTetradModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Variable>()
        val artifactVariables = ArrayList<Variable>()
        val lhs = this.constraints.indices.map {
                when (if (this.constraints.rhs[it] ls Flt64.zero) {
                    this.constraints.signs[it].reverse
                } else {
                    this.constraints.signs[it]
                }) {
                    ConstraintRelation.LessEqual -> {
                        val slack = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[it]
                            ),
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1

                        slackVariables.add(slack)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = slack.index,
                                colIndex2 = null,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    ConstraintRelation.GreaterEqual -> {
                        val slack = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[it]
                            ),
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        val artifact = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1

                        slackVariables.add(slack)
                        artifactVariables.add(artifact)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = slack.index,
                                colIndex2 = null,
                                coefficient = -Flt64.one
                            ),
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = artifact.index,
                                colIndex2 = null,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    ConstraintRelation.Equal -> {
                        val artifact = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1

                        artifactVariables.add(artifact)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            QuadraticConstraintCell(
                                rowIndex = it,
                                colIndex1 = artifact.index,
                                colIndex2 = null,
                                coefficient = Flt64.one
                            )
                        )
                    }
                }
            }
        val constraints = QuadraticConstraintBatch(
            sparseLhs = buildSparseLhs(lhs),
            signs = this.constraints.indices.map {
                ConstraintRelation.Equal
            },
            rhs = this.constraints.indices.map {
                this.constraints.rhs[it]
            },
            names = this.constraints.indices.map {
                this.constraints.names[it].ifEmpty { "cons${it}" }
            },
            sources = this.constraints.indices.map {
                ConstraintSource.Feasibility
            },
            origins = this.constraints.indices.map {
                this.constraints.origins[it]
            },
            froms = this.constraints.indices.map {
                this.constraints.froms[it]
            },
            priorities = this.constraints.indices.map {
                this.constraints.priorities[it]
            }
        )
        val objective = artifactVariables.map {
            QuadraticObjectiveCell(
                colIndex1 = it.index,
                colIndex2 = null,
                coefficient = Flt64.one
            )
        }

        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = this.variables + (slackVariables + artifactVariables).sortedBy { it.index },
                constraints = constraints,
                name = "$name-feasibility"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(ObjectCategory.Minimum, objective)
        )
    }

    @Suppress("DEPRECATION")
    override fun elastic(): QuadraticTetradModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Pair<Variable?, Variable?>>()
        for (i in this.constraints.indices) {
            when (this.constraints.signs[i]) {
                ConstraintRelation.LessEqual -> {
                    slackVariables.add(
                        Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_lb_slack",
                            initialResult = Flt64.zero
                        ) to null
                    )
                    colIndex += 1
                }

                ConstraintRelation.GreaterEqual -> {
                    slackVariables.add(
                        null to Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_ub_slack",
                            initialResult = Flt64.zero
                        )
                    )
                    colIndex += 1
                }

                ConstraintRelation.Equal -> {
                    slackVariables.add(
                        Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_lb_slack",
                            initialResult = Flt64.zero
                        ) to Variable(
                            index = colIndex + 1,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_ub_slack",
                            initialResult = Flt64.zero
                        )
                    )
                    colIndex += 2
                }
            }
        }
        for ((_, variable) in this.variables.withIndex()) {
            if (variable.free || variable.positiveNormalized || variable.negativeNormalized) {
                slackVariables.add(null to null)
            } else if (variable.positiveFree) {
                slackVariables.add(
                    Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            lowerBound = variable
                        ),
                        name = "${variable.name}_lb_slack",
                        initialResult = Flt64.zero
                    ) to null
                )
                colIndex += 1
            } else if (variable.negativeFree) {
                slackVariables.add(
                    null to Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            upperBound = variable
                        ),
                        name = "${variable.name}_ub_slack",
                        initialResult = Flt64.zero
                    )
                )
                colIndex += 1
            } else {
                slackVariables.add(
                    Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            lowerBound = variable
                        ),
                        name = "${variable.name}_lb_slack",
                        initialResult = Flt64.zero
                    ) to Variable(
                        index = colIndex + 1,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            upperBound = variable
                        ),
                        name = "${variable.name}_ub_slack",
                        initialResult = Flt64.zero
                    )
                )
                colIndex += 2
            }
        }

        var rowIndex = this.variables.size
        val lhs = this.constraints.indices.map { i ->
                this.constraints.lhs[i] + listOfNotNull(
                    slackVariables[i].first?.let {
                        QuadraticConstraintCell(
                            rowIndex = i,
                            colIndex1 = it.index,
                            colIndex2 = null,
                            coefficient = -Flt64.one,
                        )
                    },
                    slackVariables[i].second?.let {
                        QuadraticConstraintCell(
                            rowIndex = i,
                            colIndex1 = it.index,
                            colIndex2 = null,
                            coefficient = Flt64.one,
                        )
                    }
                )
            } + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisLhs = ArrayList<List<QuadraticConstraintCell>>()
                if (slackVariables[jp].first != null) {
                    thisLhs.add(
                        listOf(
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = j,
                                colIndex2 = null,
                                coefficient = Flt64.one,
                            ),
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = slackVariables[jp].first!!.index,
                                colIndex2 = null,
                                coefficient = Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                if (slackVariables[jp].second != null) {
                    thisLhs.add(
                        listOf(
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = j,
                                colIndex2 = null,
                                coefficient = Flt64.one,
                            ),
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = slackVariables[jp].second!!.index,
                                colIndex2 = null,
                                coefficient = -Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                thisLhs
            }
        val constraints = QuadraticConstraintBatch(
            sparseLhs = buildSparseLhs(lhs),
            signs = this.constraints.signs + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisConstraintRelations = ArrayList<ConstraintRelation>()
                if (slackVariables[jp].first != null) {
                    thisConstraintRelations.add(ConstraintRelation.GreaterEqual)
                }
                if (slackVariables[jp].second != null) {
                    thisConstraintRelations.add(ConstraintRelation.LessEqual)
                }
                thisConstraintRelations
            },
            rhs = this.constraints.rhs + this.variables.flatMapIndexed { j, variable ->
                val jp = this.constraints.size + j
                val thisRhs = ArrayList<Flt64>()
                if (slackVariables[jp].first != null) {
                    thisRhs.add(variable.lowerBound)
                }
                if (slackVariables[jp].second != null) {
                    thisRhs.add(variable.upperBound)
                }
                thisRhs
            },
            names = this.constraints.names.mapIndexed { i, name -> "${name.ifEmpty { "cons${i}" }}_elastic" } + this.variables.flatMapIndexed { j, variable ->
                val jp = this.constraints.size + j
                val thisNames = ArrayList<String>()
                if (slackVariables[jp].first != null) {
                    thisNames.add("${variable.name}_lb_slack")
                }
                if (slackVariables[jp].second != null) {
                    thisNames.add("${variable.name}_ub_slack")
                }
                thisNames
            },
            sources = this.constraints.sources.map { ConstraintSource.Elastic } + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisSources = ArrayList<ConstraintSource>()
                if (slackVariables[jp].first != null) {
                    thisSources.add(ConstraintSource.ElasticLowerBound)
                }
                if (slackVariables[jp].second != null) {
                    thisSources.add(ConstraintSource.ElasticUpperBound)
                }
                thisSources
            },
            origins = this.constraints.origins + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<OriginQuadraticConstraint?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            },
            froms = this.constraints.froms + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<Pair<IntermediateSymbol<*>, Boolean>?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            },
            priorities = this.constraints.priorities + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisPriorities = ArrayList<Int?>()
                if (slackVariables[jp].first != null) {
                    thisPriorities.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisPriorities.add(null)
                }
                thisPriorities
            }
        )

        val objective = slackVariables.flatMap { (posSlack, negSlack) ->
            listOfNotNull(
                posSlack?.let {
                    QuadraticObjectiveCell(
                        colIndex1 = it.index,
                        colIndex2 = null,
                        coefficient = Flt64.one
                    )
                },
                negSlack?.let {
                    QuadraticObjectiveCell(
                        colIndex1 = it.index,
                        colIndex2 = null,
                        coefficient = Flt64.one
                    )
                }
            )
        }

        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = this.variables.map {
                    it.copy().apply {
                        if (!free && !positiveNormalized && !negativeNormalized) {
                            _lowerBound = Flt64.negativeInfinity
                            _upperBound = Flt64.infinity
                        }
                    }
                } + slackVariables.flatMap { it.toList().filterNotNull() }.sortedBy { it.index },
                constraints = constraints,
                name = "$name-elastic"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(ObjectCategory.Minimum, objective)
        )
    }

    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.write("${objective.category}\n")
        var i = 0
        for (cell in objective.objective) {
            if (cell.coefficient eq Flt64.zero) {
                continue
            }
            val coefficient = if (i != 0) {
                if (cell.coefficient leq Flt64.zero) {
                    writer.append(" - ")
                } else {
                    writer.append(" + ")
                }
                abs(cell.coefficient)
            } else {
                cell.coefficient
            }
            if (coefficient neq Flt64.zero) {
                if (coefficient neq Flt64.one) {
                    writer.append("$coefficient ")
                }
                writer.append("${variables[cell.colIndex1]}")

                if (cell.colIndex2 != null) {
                    writer.append(" * ${variables[cell.colIndex2]}")
                }
            }
            ++i
        }
        writer.append("\n\n")

        return when (val result = impl.exportLP(writer)) {
            is Ok -> {
                ok
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                result
            }
        }
    }

    override fun close() {
        dualOrigin?.close()
        super.close()
    }

    override fun toString(): String {
        return name
    }
}

suspend fun solveDual(
    model: QuadraticTetradModel,
    solver: QuadraticSolver
): Ret<QuadraticDualSolution> {
    val dualModel = model.dual()

    return when (val result = solver(dualModel)) {
        is Ok -> {
            Ok(dualModel.tidyDualSolution(result.value.solution))
        }

        is Failed -> {
            Failed(result.error)
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}

suspend fun solveFarkasDual(
    model: QuadraticTetradModelView,
    solver: QuadraticSolver
): Ret<QuadraticDualSolution> {
    val dualModel = model.farkasDual()

    return when (val result = solver(dualModel)) {
        is Ok -> {
            Ok(dualModel.tidyDualSolution(result.value.solution))
        }

        is Failed -> {
            Failed(result.error)
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}



