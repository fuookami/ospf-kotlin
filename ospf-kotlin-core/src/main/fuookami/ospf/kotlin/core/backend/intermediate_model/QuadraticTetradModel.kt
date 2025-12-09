package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol

typealias OriginQuadraticConstraint = fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticConstraint

class QuadraticConstraintCell(
    override val rowIndex: Int,
    val colIndex1: Int,
    val colIndex2: Int?,
    coefficient: Flt64
) : ConstraintCell<QuadraticConstraintCell>, Cloneable, Copyable<QuadraticConstraintCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): QuadraticConstraintCell {
        return QuadraticConstraintCell(rowIndex, colIndex1, colIndex2, -coefficient)
    }

    override fun copy() = QuadraticConstraintCell(rowIndex, colIndex1, colIndex2, coefficient.copy())
    override fun clone() = copy()
}

class QuadraticConstraint(
    lhs: List<List<QuadraticConstraintCell>>,
    signs: List<Sign>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    val origins: List<OriginQuadraticConstraint?> = (0 until lhs.size).map { null },
    val froms: List<IntermediateSymbol?> = (0 until lhs.size).map { null }
) : Constraint<QuadraticConstraintCell>(lhs, signs, rhs, names, sources) {
    override fun copy() = QuadraticConstraint(
        lhs.map { line -> line.map { it.copy() } },
        signs.toList(),
        rhs.map { it.copy() },
        names.toList(),
        sources.toList(),
        origins.toList(),
        froms.toList()
    )
}

class QuadraticObjectiveCell(
    val colIndex1: Int,
    val colIndex2: Int?,
    coefficient: Flt64
) : Cell<QuadraticObjectiveCell>, Cloneable, Copyable<QuadraticObjectiveCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): QuadraticObjectiveCell {
        return QuadraticObjectiveCell(colIndex1, colIndex2, -coefficient)
    }

    override fun copy() = QuadraticObjectiveCell(colIndex1, colIndex2, coefficient.copy())
    override fun clone() = copy()
}

typealias QuadraticObjective = Objective<QuadraticObjectiveCell>

class BasicQuadraticTetradModel(
    override val variables: List<Variable>,
    override val constraints: QuadraticConstraint,
    override val name: String
) : BasicModelView<QuadraticConstraintCell>, Cloneable, Copyable<BasicQuadraticTetradModel> {
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

                is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer -> {
                    it._type = Continuous
                }

                else -> {}
            }
        }
    }

    fun linearRelaxed(): BasicQuadraticTetradModel {
        return BasicQuadraticTetradModel(
            variables.map {
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

                    is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer -> {
                        val ret = it.copy()
                        ret._type = Continuous
                        ret
                    }

                    else -> it.copy()
                }
            },
            constraints.copy(),
            name
        )
    }

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
    override val constraints: QuadraticConstraint
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
    val tokensInSolver: List<Token>,
    override val objective: QuadraticObjective,
    internal val dualOrigin: QuadraticTetradModelView? = null
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraint by impl::constraints
    override val name: String by impl::name
    override val dual get() = dualOrigin != null

    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            model: QuadraticMechanismModel,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            concurrent: Boolean? = null,
            withDumpingBounds: Boolean? = null,
            withForceDumpingBounds: Boolean? = null
        ): QuadraticTetradModel {
            logger.trace("Creating QuadraticTetradModel for $model")
            val tokensInSolver = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokensInSolver
            } else {
                model.tokens.tokensInSolverWithout(fixedVariables.keys)
            }
            val tokenIndexMap = tokensInSolver.withIndex().associate { (index, token) -> token to index }
            val bounds = model.constraints
                .flatMap { constraint ->
                    if ((withDumpingBounds ?: true)
                        && constraint.lhs.size == 1
                        && constraint.lhs.first().coefficient eq Flt64.one
                        && constraint.lhs.first().token2 == null
                    ) {
                        listOf(Quadruple(constraint, constraint.lhs.first().token1, constraint.sign, constraint.rhs))
                    } else if (withForceDumpingBounds ?: false) {
                        if (constraint.lhs.size == 1 && constraint.lhs.first().token2 == null) {
                            listOf(Quadruple(constraint, constraint.lhs.first().token1, constraint.sign, constraint.rhs / constraint.lhs.first().coefficient))
                        } else if (constraint.lhs.all { it.coefficient eq Flt64.one && it.token2 == null && it.token1.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == Sign.LessEqual || constraint.sign == Sign.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token1, Sign.Equal, Flt64.zero) }
                        } else if (constraint.lhs.all { it.coefficient eq -Flt64.one && it.token2 == null && it.token1.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == Sign.GreaterEqual || constraint.sign == Sign.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token1, Sign.Equal, Flt64.zero) }
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
                        dumpVariables(model, tokenIndexMap, bounds)
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpConstraintsAsync(model, tokenIndexMap, bounds, fixedVariables)
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpObjectives(model, tokenIndexMap, fixedVariables)
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
                        variables = dumpVariables(model, tokenIndexMap, bounds),
                        constraints = dumpConstraints(model, tokenIndexMap, bounds, fixedVariables),
                        name = model.name
                    ),
                    tokensInSolver = tokensInSolver,
                    objective = dumpObjectives(model, tokenIndexMap, fixedVariables)
                )
            }

            logger.trace("QuadraticTetradModel created for $model")
            System.gc()
            return tetradModel
        }

        private fun dumpVariables(
            model: QuadraticMechanismModel,
            tokenIndexes: Map<Token, Int>,
            bounds: Map<Token, List<Quadruple<OriginQuadraticConstraint, Token, Sign, Flt64>>>
        ): List<Variable> {
            val variables = ArrayList<Variable?>()
            for ((_, _) in tokenIndexes) {
                variables.add(null)
            }
            for ((token, i) in tokenIndexes) {
                val thisBounds = bounds[token] ?: emptyList()
                val lb = thisBounds
                    .filter { it.third == Sign.GreaterEqual || it.third == Sign.Equal }
                    .maxOfOrNull { it.fourth }
                val ub = thisBounds
                    .filter { it.third == Sign.LessEqual || it.third == Sign.Equal }
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
            model: QuadraticMechanismModel,
            tokenIndexes: Map<Token, Int>,
            bounds: Map<Token, List<Quadruple<OriginQuadraticConstraint, Token, Sign, Flt64>>>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): QuadraticConstraint {
            val boundConstraints = bounds.values.flatMap { thisBounds ->
                thisBounds.map { it.first }
            }.distinct().toSet()
            val notBoundConstraints = model.constraints.filter { !boundConstraints.contains(it) }

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
                                coefficient = cell.coefficient.let { coefficient ->
                                    if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        coefficient
                                    }
                                }
                            )
                        )
                    } else if (tokenIndexes.containsKey(cell.token1)) {
                        assert(cell.token2 != null)
                        lhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token1]!!,
                                colIndex2 = null,
                                coefficient = cell.coefficient.let {
                                    val coefficient = it * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)
                                    if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        coefficient
                                    }
                                }
                            )
                        )
                    } else if (tokenIndexes.containsKey(cell.token2)) {
                        assert(cell.token2 != null)
                        lhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token2]!!,
                                colIndex2 = null,
                                coefficient = cell.coefficient.let {
                                    val coefficient = it * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)
                                    if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        coefficient
                                    }
                                }
                            )
                        )
                    } else {
                        rhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable) ?: Flt64.one)
                    }
                }
                lhs to rhs
            }

            val lhs = ArrayList<List<QuadraticConstraintCell>>()
            val signs = ArrayList<Sign>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            val sources = ArrayList<ConstraintSource>()
            val origins = ArrayList<OriginQuadraticConstraint>()
            val froms = ArrayList<IntermediateSymbol?>()
            for ((index, constraint) in notBoundConstraints.withIndex()) {
                lhs.add(constraints[index].first)
                signs.add(constraint.sign)
                rhs.add(constraints[index].second)
                names.add(constraint.name)
                sources.add(ConstraintSource.Origin)
                origins.add(constraint)
                froms.add(constraint.from)
            }
            return QuadraticConstraint(lhs, signs, rhs, names, sources, origins, froms)
        }

        private suspend fun dumpConstraintsAsync(
            model: QuadraticMechanismModel,
            tokenIndexes: Map<Token, Int>,
            bounds: Map<Token, List<Quadruple<OriginQuadraticConstraint, Token, Sign, Flt64>>>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): QuadraticConstraint {
            val boundConstraints = bounds.values.flatMap { thisBounds ->
                thisBounds.map { it.first }
            }.distinct().toSet()
            val notBoundConstraints = model.constraints.filter { !boundConstraints.contains(it) }

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
                                                coefficient = cell.coefficient.let { coefficient ->
                                                    if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                                        -Flt64.decimalPrecision.reciprocal()
                                                    } else {
                                                        coefficient
                                                    }
                                                }
                                            )
                                        )
                                    } else if (tokenIndexes.containsKey(cell.token1)) {
                                        assert(cell.token2 != null)
                                        lhs.add(
                                            QuadraticConstraintCell(
                                                rowIndex = i,
                                                colIndex1 = tokenIndexes[cell.token1]!!,
                                                colIndex2 = null,
                                                coefficient = cell.coefficient.let { originCoefficient ->
                                                    val coefficient = originCoefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)
                                                    if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                                        -Flt64.decimalPrecision.reciprocal()
                                                    } else {
                                                        coefficient
                                                    }
                                                }
                                            )
                                        )
                                    } else if (tokenIndexes.containsKey(cell.token2)) {
                                        assert(cell.token2 != null)
                                        lhs.add(
                                            QuadraticConstraintCell(
                                                rowIndex = i,
                                                colIndex1 = tokenIndexes[cell.token2]!!,
                                                colIndex2 = null,
                                                coefficient = cell.coefficient.let { originCoefficient ->
                                                    val coefficient = originCoefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)
                                                    if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                                        -Flt64.decimalPrecision.reciprocal()
                                                    } else {
                                                        coefficient
                                                    }
                                                }
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
                    val signs = ArrayList<Sign>()
                    val rhs = ArrayList<Flt64>()
                    val names = ArrayList<String>()
                    val sources = ArrayList<ConstraintSource>()
                    val origins = ArrayList<OriginQuadraticConstraint>()
                    val froms = ArrayList<IntermediateSymbol?>()
                    for ((index, constraint) in notBoundConstraints.withIndex()) {
                        val (thisLhs, thisRhs) = constraintPromises[index / segment].await()[index % segment]
                        lhs.add(thisLhs)
                        signs.add(constraint.sign)
                        rhs.add(thisRhs)
                        names.add(constraint.name)
                        sources.add(ConstraintSource.Origin)
                        origins.add(constraint)
                        froms.add(constraint.from)
                    }
                    System.gc()
                    QuadraticConstraint(lhs, signs, rhs, names, sources, origins, froms)
                }
            } else {
                val lhs = ArrayList<List<QuadraticConstraintCell>>()
                val signs = ArrayList<Sign>()
                val rhs = ArrayList<Flt64>()
                val names = ArrayList<String>()
                val sources = ArrayList<ConstraintSource>()
                val origins = ArrayList<OriginQuadraticConstraint>()
                val froms = ArrayList<IntermediateSymbol?>()
                for ((index, constraint) in notBoundConstraints.withIndex()) {
                    val thisLhs = ArrayList<QuadraticConstraintCell>()
                    for (cell in constraint.lhs) {
                        if (tokenIndexes.containsKey(cell.token1) && (cell.token2 == null || tokenIndexes.containsKey(cell.token2))) {
                            thisLhs.add(
                                QuadraticConstraintCell(
                                    rowIndex = index,
                                    colIndex1 = tokenIndexes[cell.token1]!!,
                                    colIndex2 = cell.token2?.let { token -> tokenIndexes[token]!! },
                                    coefficient = cell.coefficient.let { coefficient ->
                                        if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                            Flt64.decimalPrecision.reciprocal()
                                        } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                            -Flt64.decimalPrecision.reciprocal()
                                        } else {
                                            coefficient
                                        }
                                    }
                                )
                            )
                        } else if (tokenIndexes.containsKey(cell.token1)) {
                            assert(cell.token2 != null)
                            thisLhs.add(
                                QuadraticConstraintCell(
                                    rowIndex = index,
                                    colIndex1 = tokenIndexes[cell.token1]!!,
                                    colIndex2 = null,
                                    coefficient = cell.coefficient.let { originCoefficient ->
                                        val coefficient = originCoefficient * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)
                                        if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                            Flt64.decimalPrecision.reciprocal()
                                        } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                            -Flt64.decimalPrecision.reciprocal()
                                        } else {
                                            coefficient
                                        }
                                    }
                                )
                            )
                        } else if (tokenIndexes.containsKey(cell.token2)) {
                            assert(cell.token2 != null)
                            thisLhs.add(
                                QuadraticConstraintCell(
                                    rowIndex = index,
                                    colIndex1 = tokenIndexes[cell.token2]!!,
                                    colIndex2 = null,
                                    coefficient = cell.coefficient.let { originCoefficient ->
                                        val coefficient = originCoefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)
                                        if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                            Flt64.decimalPrecision.reciprocal()
                                        } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                            -Flt64.decimalPrecision.reciprocal()
                                        } else {
                                            coefficient
                                        }
                                    }
                                )
                            )
                        } else {
                            rhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable)
                                ?: Flt64.one)
                        }
                    }
                    lhs.add(thisLhs)
                    signs.add(constraint.sign)
                    rhs.add(constraint.rhs)
                    names.add(constraint.name)
                    sources.add(ConstraintSource.Origin)
                    origins.add(constraint)
                    froms.add(constraint.from)
                }
                System.gc()
                QuadraticConstraint(lhs, signs, rhs, names, sources, origins, froms)
            }
        }

        private fun dumpObjectives(
            model: QuadraticMechanismModel,
            tokenIndexes: Map<Token, Int>,
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
                        if (fixedVariables?.containsKey(cell.token1.variable) == true && (cell.token2 == null || fixedVariables[cell.token2.variable] != null)) {
                            constant += cell.coefficient * fixedVariables[cell.token1.variable]!! * (fixedVariables[cell.token2?.variable] ?: Flt64.one)
                        } else if (fixedVariables?.containsKey(cell.token1.variable) == true) {
                            assert(cell.token2 != null)
                            val index = tokenIndexes[cell.token2] ?: continue
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
                        if (fixedVariables?.containsKey(cell.token1.variable) == true && (cell.token2 == null || fixedVariables[cell.token2.variable] != null)) {
                            constant -= cell.coefficient * fixedVariables[cell.token1.variable]!! * (fixedVariables[cell.token2?.variable] ?: Flt64.one)
                        } else if (fixedVariables?.containsKey(cell.token1.variable) == true) {
                            assert(cell.token2 != null)
                            val index = tokenIndexes[cell.token2] ?: continue
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
                            coefficient = value.let { coefficient ->
                                if (coefficient.isInfinity() || coefficient geq Flt64.decimalPrecision.reciprocal()) {
                                    Flt64.decimalPrecision.reciprocal()
                                } else if (coefficient.isNegativeInfinity() || coefficient leq -Flt64.decimalPrecision.reciprocal()) {
                                    -Flt64.decimalPrecision.reciprocal()
                                } else {
                                    coefficient
                                }
                            }
                        )
                    )
                }
            }
            return QuadraticObjective(objectiveCategory, objective)
        }
    }

    override fun copy() = QuadraticTetradModel(impl.copy(), tokensInSolver, objective.copy())
    override fun clone() = copy()

    override fun linearRelax(): QuadraticTetradModel {
        impl.linearRelax()
        return this
    }

    override fun linearRelaxed(): QuadraticTetradModel {
        return QuadraticTetradModel(impl.linearRelaxed(), tokensInSolver, objective.copy())
    }

    suspend fun dual(): QuadraticTetradModel {
        TODO("not implemented yet")
    }

    override suspend fun farkasDual(): QuadraticTetradModel {
        TODO("not implemented yet")
    }

    override fun feasibility(): QuadraticTetradModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Variable>()
        val artifactVariables = ArrayList<Variable>()
        val constraints = QuadraticConstraint(
            lhs = this.constraints.indices.map {
                when (if (this.constraints.rhs[it] ls Flt64.zero) {
                    this.constraints.signs[it].reverse
                } else {
                    this.constraints.signs[it]
                }) {
                    Sign.LessEqual -> {
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

                    Sign.GreaterEqual -> {
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

                    Sign.Equal -> {
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
            },
            signs = this.constraints.indices.map {
                Sign.Equal
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

    override fun elastic(): QuadraticTetradModel {
        TODO("not implemented yet")
    }

    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.write("${objective.category}\n")
        var i = 0
        for (cell in objective.obj) {
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
            is Failed -> {
                Failed(result.error)
            }

            is Ok -> {
                ok
            }
        }
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
    }
}
