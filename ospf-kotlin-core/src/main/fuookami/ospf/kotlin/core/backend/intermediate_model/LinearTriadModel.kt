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
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.Sign
import fuookami.ospf.kotlin.core.backend.solver.*

typealias OriginLinearConstraint = fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearConstraint

private fun OriginLinearConstraint.isBound(): Boolean {
    return lhs.size == 1
            && lhs.first().coefficient eq Flt64.one
            // && from?.second != true
}

class LinearConstraintCell(
    override val rowIndex: Int,
    val colIndex: Int,
    coefficient: Flt64
) : ConstraintCell<LinearConstraintCell>, Cloneable, Copyable<LinearConstraintCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): LinearConstraintCell {
        return LinearConstraintCell(rowIndex, colIndex, -coefficient)
    }

    override fun copy() = LinearConstraintCell(rowIndex, colIndex, coefficient.copy())
    override fun clone() = copy()

    override fun toString(): String {
        return "(${rowIndex}, ${colIndex}, ${coefficient})"
    }
}

class LinearConstraint(
    lhs: List<List<LinearConstraintCell>>,
    signs: List<Sign>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    origins: List<OriginLinearConstraint?> = (0 until lhs.size).map { null },
    froms: List<Pair<IntermediateSymbol, Boolean>?> = (0 until lhs.size).map { null }
) : Constraint<LinearConstraintCell>(lhs, signs, rhs, names, sources) {
    private val _origins: MutableList<OriginLinearConstraint?> = origins.toMutableList()
    val origins: List<OriginLinearConstraint?> by ::_origins

    private val _froms: MutableList<Pair<IntermediateSymbol, Boolean>?> = froms.toMutableList()
    val froms: List<Pair<IntermediateSymbol, Boolean>?> by ::_froms

    fun filter(condition: (Int) -> Boolean): LinearConstraint {
        return LinearConstraint(
            lhs = lhs.filterIndexed { i, _ -> condition(i) },
            signs = signs.filterIndexed { i, _ -> condition(i) },
            rhs = rhs.filterIndexed { i, _ -> condition(i) },
            names = names.filterIndexed { i, _ -> condition(i) },
            sources = sources.filterIndexed { i, _ -> condition(i) },
            origins = origins.filterIndexed { i, _ -> condition(i) },
            froms = froms.filterIndexed { i, _ -> condition(i) }
        )
    }

    override fun copy() = LinearConstraint(
        lhs.map { line -> line.map { it.copy() } },
        signs.toList(),
        rhs.map { it.copy() },
        names.toList(),
        sources.toList(),
        origins.toList(),
        froms.toList()
    )

    override fun close() {
        _origins.clear()
        _froms.clear()
        super.close()
    }
}

class LinearObjectiveCell(
    val colIndex: Int,
    coefficient: Flt64
) : Cell<LinearObjectiveCell>, Cloneable, Copyable<LinearObjectiveCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): LinearObjectiveCell {
        return LinearObjectiveCell(colIndex, -coefficient)
    }

    override fun copy() = LinearObjectiveCell(colIndex, coefficient.copy())
    override fun clone() = copy()

    override fun toString(): String {
        return "(${colIndex}, ${coefficient})"
    }
}

typealias LinearObjective = Objective<LinearObjectiveCell>

typealias BasicLinearTriadModelView = BasicModelView<LinearConstraintCell>

class BasicLinearTriadModel(
    override val variables: List<Variable>,
    override val constraints: LinearConstraint,
    override val name: String
) : BasicLinearTriadModelView, Cloneable, Copyable<BasicLinearTriadModel> {
    override fun copy() = BasicLinearTriadModel(
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

    fun linearRelaxed(): BasicLinearTriadModel {
        return BasicLinearTriadModel(
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
            constraints,
            name
        )
    }

    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.append("Subject To\n")
        for (i in constraints.indices) {
            writer.append(" ${constraints.names[i].ifEmpty { "cons$i" }}: ")
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
                    writer.append("${variables[constraints.lhs[i][j].colIndex]}")
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
            if (variable.free) {
                writer.append(" $variable free\n")
            } else if (variable.positiveNormalized) {
                writer.append(" $variable >= 0\n")
            } else if (variable.negativeNormalized) {
                writer.append(" $variable <= 0\n")
            } else if (variable.positiveFree) {
                writer.append(" $variable >= ${variable.lowerBound}\n")
            } else if (variable.negativeFree) {
                writer.append(" $variable <= ${variable.upperBound}\n")
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

interface LinearTriadModelView: ModelView<LinearConstraintCell, LinearObjectiveCell> {
    override val constraints: LinearConstraint
    val dual: Boolean

    fun linearRelax(): LinearTriadModelView
    fun linearRelaxed(): LinearTriadModelView
    suspend fun farkasDual(): LinearTriadModelView
    fun feasibility(): LinearTriadModelView
    fun elastic(
        minmaxSlack: Boolean = false,
        minSlackAmount: Pair<UInt64, Flt64>? = null
    ): LinearTriadModelView

    fun tidyDualSolution(solution: Solution): LinearDualSolution {
        return if (dual) {
            variables.associateNotNull {
                if (it.dualOrigin != null && solution.size > it.index && solution[it.index] neq Flt64.zero) {
                    (it.dualOrigin as OriginLinearConstraint) to solution[it.index]
                } else {
                    null
                }
            }
        } else {
            constraints.indices.associateNotNull {
                if (constraints.origins[it] != null && solution.size > it && solution[it] neq Flt64.zero) {
                    constraints.origins[it]!! to solution[it]
                } else {
                    null
                }
            }
        }
    }
}

data class LinearTriadModel(
    private val impl: BasicLinearTriadModel,
    val tokensInSolver: List<Token>,
    override val objective: LinearObjective,
    internal val dualOrigin: LinearTriadModelView? = null
) : LinearTriadModelView, Cloneable, Copyable<LinearTriadModel> {
    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            model: LinearMechanismModel,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            dumpConstraintsToBounds: Boolean? = null,
            forceDumpBounds: Boolean? = null,
            concurrent: Boolean? = null
        ): LinearTriadModel {
            logger.trace("Creating LinearTriadModel for $model")
            val tokensInSolver = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokensInSolver
            } else {
                model.tokens.tokensInSolverWithout(fixedVariables.keys)
            }
            val tokenIndexMap = tokensInSolver.withIndex().associate { (index, token) -> token to index }
            val bounds = model.constraints
                .flatMap { constraint ->
                    if ((dumpConstraintsToBounds ?: true) && constraint.isBound()) {
                        listOf(Quadruple(constraint, constraint.lhs.first().token, constraint.sign, constraint.rhs))
                    } else if (forceDumpBounds ?: false) {
                        if (constraint.lhs.size == 1) {
                            listOf(Quadruple(constraint, constraint.lhs.first().token, constraint.sign, constraint.rhs / constraint.lhs.first().coefficient))
                        } else if (constraint.lhs.all { it.coefficient eq Flt64.one && it.token.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == Sign.LessEqual || constraint.sign == Sign.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token, Sign.Equal, Flt64.zero) }
                        } else if (constraint.lhs.all { it.coefficient eq -Flt64.one && it.token.lowerBound!!.value.unwrap() geq Flt64.zero }
                            && (constraint.sign == Sign.GreaterEqual || constraint.sign == Sign.Equal)
                            && constraint.rhs eq Flt64.zero
                        ) {
                            constraint.lhs.map { Quadruple(constraint, it.token, Sign.Equal, Flt64.zero) }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }.groupBy { it.second }
            val triadModel = if (concurrent ?: model.concurrent) {
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

                    LinearTriadModel(
                        impl = BasicLinearTriadModel(
                            variables = variablePromise.await(),
                            constraints = constraintPromise.await(),
                            name = model.name
                        ),
                        tokensInSolver = tokensInSolver,
                        objective = objectivePromise.await()
                    )
                }
            } else {
                LinearTriadModel(
                    impl = BasicLinearTriadModel(
                        variables = dumpVariables(model, tokenIndexMap, bounds),
                        constraints = dumpConstraints(model, tokenIndexMap, bounds, fixedVariables),
                        name = model.name
                    ),
                    tokensInSolver = tokensInSolver,
                    objective = dumpObjectives(model, tokenIndexMap, fixedVariables)
                )
            }

            logger.trace("LinearTriadModel created for $model")
            System.gc()
            return triadModel
        }

        private fun dumpVariables(
            model: LinearMechanismModel,
            tokenIndexMap: Map<Token, Int>,
            bounds: Map<Token, List<Quadruple<OriginLinearConstraint, Token, Sign, Flt64>>>
        ): List<Variable> {
            val variables = ArrayList<Variable?>()
            for ((_, _) in tokenIndexMap) {
                variables.add(null)
            }
            for ((token, i) in tokenIndexMap) {
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
            model: LinearMechanismModel,
            tokenIndexes: Map<Token, Int>,
            bounds: Map<Token, List<Quadruple<OriginLinearConstraint, Token, Sign, Flt64>>>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): LinearConstraint {
            val boundConstraints = bounds.values.flatMap { thisBounds ->
                thisBounds.map { it.first }
            }.distinct().toSet()
            val notBoundConstraints = model.constraints.filter { !boundConstraints.contains(it) }

            val constraints = notBoundConstraints.withIndex().map { (index, constraint) ->
                val lhs = ArrayList<LinearConstraintCell>()
                var rhs = constraint.rhs
                for (cell in constraint.lhs) {
                    if (tokenIndexes.containsKey(cell.token)) {
                        lhs.add(
                            LinearConstraintCell(
                                rowIndex = index,
                                colIndex = tokenIndexes[cell.token]!!,
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
                    } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                        rhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
                    }
                }
                lhs to rhs
            }

            val lhs = ArrayList<List<LinearConstraintCell>>()
            val signs = ArrayList<Sign>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            val sources = ArrayList<ConstraintSource>()
            val origins = ArrayList<OriginLinearConstraint>()
            val froms = ArrayList<Pair<IntermediateSymbol, Boolean>?>()
            for ((index, constraint) in notBoundConstraints.withIndex()) {
                lhs.add(constraints[index].first)
                signs.add(constraint.sign)
                rhs.add(constraints[index].second)
                names.add(constraint.name)
                sources.add(ConstraintSource.Origin)
                origins.add(constraint)
                froms.add(constraint.from)
            }
            return LinearConstraint(lhs, signs, rhs, names, sources, origins, froms)
        }

        private suspend fun dumpConstraintsAsync(
            model: LinearMechanismModel,
            tokenIndexes: Map<Token, Int>,
            bounds: Map<Token, List<Quadruple<OriginLinearConstraint, Token, Sign, Flt64>>>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): LinearConstraint {
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
                            val constraints = ArrayList<Pair<List<LinearConstraintCell>, Flt64>>()
                            for (i in (it * segment) until minOf(notBoundConstraints.size, (it + 1) * segment)) {
                                val constraint = notBoundConstraints[i]
                                val lhs = ArrayList<LinearConstraintCell>()
                                var rhs = constraint.rhs
                                for (cell in constraint.lhs) {
                                    if (tokenIndexes.containsKey(cell.token)) {
                                        lhs.add(
                                            LinearConstraintCell(
                                                rowIndex = i,
                                                colIndex = tokenIndexes[cell.token]!!,
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
                                    } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                                        rhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
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

                    val lhs = ArrayList<List<LinearConstraintCell>>()
                    val signs = ArrayList<Sign>()
                    val rhs = ArrayList<Flt64>()
                    val names = ArrayList<String>()
                    val sources = ArrayList<ConstraintSource>()
                    val origins = ArrayList<OriginLinearConstraint>()
                    val froms = ArrayList<Pair<IntermediateSymbol, Boolean>?>()
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
                    LinearConstraint(lhs, signs, rhs, names, sources, origins, froms)
                }
            } else {
                val lhs = ArrayList<List<LinearConstraintCell>>()
                val signs = ArrayList<Sign>()
                val rhs = ArrayList<Flt64>()
                val names = ArrayList<String>()
                val sources = ArrayList<ConstraintSource>()
                val origins = ArrayList<OriginLinearConstraint>()
                val froms = ArrayList<Pair<IntermediateSymbol, Boolean>?>()
                for ((index, constraint) in notBoundConstraints.withIndex()) {
                    val thisLhs = ArrayList<LinearConstraintCell>()
                    var thisRhs = constraint.rhs
                    for (cell in constraint.lhs) {
                        if (tokenIndexes.containsKey(cell.token)) {
                            thisLhs.add(
                                LinearConstraintCell(
                                    rowIndex = index,
                                    colIndex = tokenIndexes[cell.token]!!,
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
                }
                System.gc()
                LinearConstraint(lhs, signs, rhs, names, sources, origins, froms)
            }
        }

        private fun dumpObjectives(
            model: LinearMechanismModel,
            tokenIndexes: Map<Token, Int>,
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
                        coefficient = coefficient[i].let { coefficient ->
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
            return LinearObjective(objectiveCategory, objective, constant)
        }
    }

    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraint by impl::constraints
    override val name: String by impl::name
    override val dual get() = dualOrigin != null

    override fun copy() = LinearTriadModel(impl.copy(), tokensInSolver, objective.copy())
    override fun clone() = copy()

    override fun linearRelax(): LinearTriadModel {
        impl.linearRelax()
        return this
    }

    override fun linearRelaxed(): LinearTriadModel {
        return LinearTriadModel(impl.linearRelaxed(), tokensInSolver, objective.copy())
    }

    suspend fun dual(): LinearTriadModel {
        val dualVariables = this.constraints.indices.map {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (this.objective.category) {
                ObjectCategory.Maximum -> {
                    when (this.constraints.signs[it]) {
                        Sign.LessEqual -> {
                            // ≤ => y ≥ 0
                            lowerBound = Flt64.zero
                        }

                        Sign.GreaterEqual -> {
                            // ≥ => y ≤ 0
                            upperBound = Flt64.zero
                        }

                        else -> {}
                    }
                }

                ObjectCategory.Minimum -> {
                    when (this.constraints.signs[it]) {
                        Sign.LessEqual -> {
                            // ≤ => y ≤ 0
                            upperBound = Flt64.zero
                        }

                        Sign.GreaterEqual -> {
                            // ≥ => y ≥ 0
                            lowerBound = Flt64.zero
                        }

                        else -> {}
                    }
                }
            }

            Variable(
                index = it,
                lowerBound = lowerBound,
                upperBound = upperBound,
                type = Continuous,
                origin = null,
                dualOrigin = this.constraints.origins[it],
                slack = null,
                name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_dual",
                initialResult = Flt64.zero
            )
        }
        var colIndex = this.constraints.size
        val boundDualVariables = this.variables.map {
            when (this.objective.category) {
                ObjectCategory.Maximum -> {
                    if (it.negativeNormalized || it.positiveNormalized || it.free) {
                        null to null
                    } else if (it.positiveFree) {
                        // x ≥ lb => λ ≤ 0
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable to null
                    } else if (it.negativeFree) {
                        // x ≤ ub => λ ≥ 0
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        null to variable
                    } else {
                        // lb ≤ x ≤ ub => λ ≤ 0, λ' ≥ 0
                        val variable1 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        val variable2 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable1 to variable2
                    }
                }

                ObjectCategory.Minimum -> {
                    if (it.negativeNormalized || it.positiveNormalized || it.free) {
                        null to null
                    } else if (it.positiveFree) {
                        // x ≥ lb => λ ≥ 0
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable to null
                    } else if (it.negativeFree) {
                        // x ≤ ub => λ ≤ 0
                        val variable = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        null to variable
                    } else {
                        // lb ≤ x ≤ ub => λ ≥ 0, λ' ≤ 0
                        val variable1 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_lb_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        val variable2 = Variable(
                            index = colIndex,
                            lowerBound = Flt64.negativeInfinity,
                            upperBound = Flt64.zero,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = null,
                            name = "${it.name}_ub_dual",
                            initialResult = Flt64.zero
                        )
                        colIndex += 1
                        variable1 to variable2
                    }
                }
            }
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this@LinearTriadModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }
        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map { col ->
                async(Dispatchers.Default) {
                    coefficients[col].map { cell ->
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = cell.first,
                            coefficient = cell.second
                        )
                    } + listOfNotNull(boundDualVariables[col].first, boundDualVariables[col].second).map {
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = it.index,
                            coefficient = Flt64.one
                        )
                    }
                }
            }
            constraintPromises.awaitAll()
        }
        val signs = this.variables.map {
            if (!it.normalized) {
                Sign.Equal
            } else if (it.negativeNormalized) {
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        // ≤ 0 => ≤
                        Sign.LessEqual
                    }

                    ObjectCategory.Minimum -> {
                        // ≤ 0 => ≥
                        Sign.GreaterEqual
                    }
                }
            } else if (it.positiveNormalized) {
                // ≥ 0
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        // ≥ 0 => ≥
                        Sign.GreaterEqual
                    }

                    ObjectCategory.Minimum -> {
                        // ≥ 0 => ≤
                        Sign.LessEqual
                    }
                }
            } else {
                Sign.Equal
            }
        }
        val rhs = this.variables.map { col ->
            this.objective.obj.find { it.colIndex == col.index }?.coefficient ?: Flt64.zero
        }
        val names = this.variables.map { "${it.name}_dual" }
        val sources = this.variables.map { ConstraintSource.Dual }

        val objective = constraints.indices.map {
            LinearObjectiveCell(
                colIndex = it,
                coefficient = this.constraints.rhs[it]
            )
        } + boundDualVariables.flatMapIndexed { col, (lb, ub) ->
            listOfNotNull(
                lb?.let {
                    LinearObjectiveCell(
                        colIndex = lb.index,
                        coefficient = this.variables[col].lowerBound
                    )
                },
                ub?.let {
                    LinearObjectiveCell(
                        colIndex = it.index,
                        coefficient = this.variables[col].upperBound
                    )
                }
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = (dualVariables + boundDualVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = LinearConstraint(lhs, signs, rhs, names, sources),
                name = "$name-dual"
            ),
            tokensInSolver = tokensInSolver,
            objective = LinearObjective(this.objective.category.reverse, objective),
            dualOrigin = this
        )
    }

    override suspend fun farkasDual(): LinearTriadModel {
        var colIndex = this.constraints.size
        val farkasVariables = ArrayList<Variable>()
        val posFarkasVariables = ArrayList<Variable>()
        val negFarkasVariables = ArrayList<Variable>()
        val slackVariables = ArrayList<Variable>()
        for (i in this.constraints.indices) {
            when (this.constraints.signs[i]) {
                Sign.LessEqual -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = this.constraints.origins[i],
                        slack = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)
                    posFarkasVariables.add(variable)
                }

                Sign.GreaterEqual -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64.zero,
                        type = Continuous,
                        origin = null,
                        dualOrigin = this.constraints.origins[i],
                        slack = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)
                    negFarkasVariables.add(variable)
                }

                Sign.Equal -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = this.constraints.origins[i],
                        slack = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)

                    val posSlack = Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            constraint = this.constraints.origins[i]
                        ),
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_pos_slack",
                        initialResult = Flt64.zero
                    )
                    colIndex += 1

                    val negSlack = Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            constraint = this.constraints.origins[i]
                        ),
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_neg_slack",
                        initialResult = Flt64.zero
                    )
                    colIndex += 1

                    slackVariables.add(posSlack)
                    slackVariables.add(negSlack)
                }
            }
        }
        val boundVariables = this.variables.map {
            if (it.free) {
                null to null
            } else if (it.positiveFree) {
                // x ≥ lb => λ ≤ 0
                val variable = Variable(
                    index = colIndex,
                    lowerBound = Flt64.negativeInfinity,
                    upperBound = Flt64.zero,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_lb_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                variable to null
            } else if (it.negativeFree) {
                // x ≤ ub => λ ≥ 0
                val variable = Variable(
                    index = colIndex,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.infinity,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_ub_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                null to variable
            } else {
                // lb ≤ x ≤ ub => λ ≤ 0, λ' ≥ 0
                val variable1 = Variable(
                    index = colIndex,
                    lowerBound = Flt64.negativeInfinity,
                    upperBound = Flt64.zero,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_lb_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                val variable2 = Variable(
                    index = colIndex,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.infinity,
                    type = Continuous,
                    origin = null,
                    dualOrigin = null,
                    slack = null,
                    name = "${it.name}_ub_dual",
                    initialResult = Flt64.zero
                )
                colIndex += 1
                variable1 to variable2
            }
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this@LinearTriadModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }

        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map { col ->
                async(Dispatchers.Default) {
                    coefficients[col].map { cell ->
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = cell.first,
                            coefficient = cell.second
                        )
                    } + listOfNotNull(boundVariables[col].first, boundVariables[col].second).map {
                        LinearConstraintCell(
                            rowIndex = col,
                            colIndex = it.index,
                            coefficient = Flt64.one
                        )
                    }
                }
            } + listOf(async(Dispatchers.Default) {
                this@LinearTriadModel.constraints.indices.map {
                    LinearConstraintCell(
                        rowIndex = this@LinearTriadModel.variables.size,
                        colIndex = farkasVariables[it].index,
                        coefficient = this@LinearTriadModel.constraints.rhs[it]
                    )
                } + this@LinearTriadModel.variables.flatMapIndexed { col, variable ->
                    listOfNotNull(
                        boundVariables[col].first?.let {
                            LinearConstraintCell(
                                rowIndex = this@LinearTriadModel.variables.size,
                                colIndex = it.index,
                                coefficient = variable.lowerBound
                            )
                        },
                        boundVariables[col].second?.let {
                            LinearConstraintCell(
                                rowIndex = this@LinearTriadModel.variables.size,
                                colIndex = it.index,
                                coefficient = variable.upperBound
                            )
                        }
                    )
                }
            })
            val slackConstraintPromises = async(Dispatchers.Default) {
                var rowIndex = this@LinearTriadModel.variables.size + 1
                var i = 0
                this@LinearTriadModel.constraints.indices.mapNotNull {
                    when (this@LinearTriadModel.constraints.signs[it]) {
                        Sign.LessEqual, Sign.GreaterEqual -> {
                            null
                        }

                        Sign.Equal -> {
                            val result = listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = farkasVariables[it].index,
                                    coefficient = Flt64.one
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = slackVariables[2 * i].index,
                                    coefficient = -Flt64.one
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = slackVariables[2 * i + 1].index,
                                    coefficient = Flt64.one
                                )
                            )
                            i += 1
                            rowIndex += 1
                            result
                        }
                    }
                }
            }
            constraintPromises.awaitAll() + slackConstraintPromises.await()
        }

        val signs = this.variables.indices.map { Sign.Equal } + listOf(Sign.Equal) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    Sign.Equal
                }
            }
        }
        val rhs = this.variables.indices.map { Flt64.zero } + listOf(-Flt64.one) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    Flt64.zero
                }
            }
        }
        val names = this.variables.map { "${it.name}_farkas_dual" } + listOf("normalization") + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    "${this.constraints.names[it].ifEmpty { "cons${it}" }}_abs"
                }
            }
        }
        val sources = this.variables.map { ConstraintSource.FarkasDual } + listOf(ConstraintSource.FarkasDual) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    ConstraintSource.FarkasDual
                }
            }
        }

        val objective = posFarkasVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        } + negFarkasVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = -Flt64.one
            )
        } + slackVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = (farkasVariables + slackVariables + boundVariables.flatMapNotNull { listOf(it.first, it.second) }).sortedBy { it.index },
                constraints = LinearConstraint(lhs, signs, rhs, names, sources),
                name = "$name-farkas-dual"
            ),
            tokensInSolver = tokensInSolver,
            objective = LinearObjective(ObjectCategory.Minimum, objective),
            dualOrigin = this
        )
    }

    override fun feasibility(): LinearTriadModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Variable>()
        val artifactVariables = ArrayList<Variable>()
        val constraints = LinearConstraint(
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
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = slack.index,
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
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = slack.index,
                                coefficient = -Flt64.one
                            ),
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = artifact.index,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    Sign.Equal -> {
                        val artifact = Variable(
                            index = colIndex,
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
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = artifact.index,
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
                abs(this.constraints.rhs[it])
            },
            names = this.constraints.indices.map {
                "${this.constraints.names[it].ifEmpty { "cons${it}" }}_feasibility"
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
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = this.variables + (slackVariables + artifactVariables).sortedBy { it.index },
                constraints = constraints,
                name = "$name-feasibility"
            ),
            tokensInSolver = tokensInSolver,
            objective = LinearObjective(ObjectCategory.Minimum, objective)
        )
    }

    override fun elastic(
        minmaxSlack: Boolean,
        minSlackAmount: Pair<UInt64, Flt64>?
    ): LinearTriadModel {
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Pair<Variable?, Variable?>>()
        val slackBinVariables = ArrayList<Pair<Variable, Variable>>()
        for (i in this.constraints.indices) {
            when (this.constraints.signs[i]) {
                Sign.LessEqual -> {
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

                Sign.GreaterEqual -> {
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

                Sign.Equal -> {
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
        for ((j, variable) in this.variables.withIndex()) {
            if (variable.free || variable.positiveNormalized || variable.negativeNormalized) {
                slackVariables.add(
                    null to null
                )
            } else if (variable.positiveFree) {
                // x ≥ lb
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
                // x ≤ ub
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
                // lb ≤ x ≤ ub
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
        if (minSlackAmount != null) {
            slackBinVariables.addAll(slackVariables.flatMap { it.toList().filterNotNull() }.mapIndexed { j, slack ->
                slack to Variable(
                    index = colIndex + j,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.one,
                    type = Binary,
                    origin = null,
                    dualOrigin = null,
                    name = "${slack.name}_bin",
                    initialResult = Flt64.zero
                )
            })
            colIndex += slackBinVariables.size
        }
        val minmaxSlackVariable = if (minmaxSlack) {
            val minmax = Variable(
                index = colIndex,
                lowerBound = Flt64.zero,
                upperBound = Flt64.infinity,
                type = Continuous,
                origin = null,
                dualOrigin = null,
                name = "minmax_slack",
                initialResult = Flt64.zero
            )
            colIndex += 1
            minmax
        } else {
            null
        }

        var rowIndex = this.variables.size
        val constraints = LinearConstraint(
            lhs = this.constraints.indices.map { i ->
                this.constraints.lhs[i] + listOfNotNull(
                    slackVariables[i].first?.let {
                        LinearConstraintCell(
                            rowIndex = i,
                            colIndex = it.index,
                            coefficient = -Flt64.one,
                        )
                    }, slackVariables[i].second?.let {
                        LinearConstraintCell(
                            rowIndex = i,
                            colIndex = it.index,
                            coefficient = Flt64.one,
                        )
                    }
                )
            } + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisLhs = ArrayList<List<LinearConstraintCell>>()
                if (slackVariables[jp].first != null) {
                    thisLhs.add(
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = j,
                                coefficient = Flt64.one,
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = slackVariables[jp].first!!.index,
                                coefficient = Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                if (slackVariables[jp].second != null) {
                    thisLhs.add(
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = j,
                                coefficient = Flt64.one,
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = slackVariables[jp].second!!.index,
                                coefficient = -Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                thisLhs
            } + if (minSlackAmount != null) {
                val thisLhs = slackBinVariables.flatMapIndexed { j, (slack, bin) ->
                    listOf(
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j,
                                colIndex = slack.index,
                                coefficient = Flt64.one,
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j,
                                colIndex = bin.index,
                                coefficient = -minSlackAmount.second - Flt64.decimalPrecision
                            )
                        ),
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j + 1,
                                colIndex = slack.index,
                                coefficient = Flt64.one
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j + 1,
                                colIndex = bin.index,
                                coefficient = -Flt64.decimalPrecision.reciprocal()
                            )
                        )
                    )
                } + listOf(
                    slackBinVariables.map { (_, bin) ->
                        LinearConstraintCell(
                            rowIndex = rowIndex + 2 * slackBinVariables.size + 1,
                            colIndex = bin.index,
                            coefficient = Flt64.one
                        )
                    }
                )
                rowIndex += 2 * slackBinVariables.size + 1
                thisLhs
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisLhs = ArrayList<List<LinearConstraintCell>>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisLhs.add(
                            listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = minmaxSlackVariable!!.index,
                                    coefficient = Flt64.one,
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = lbSlack.index,
                                    coefficient = -Flt64.one,
                                )
                            )
                        )
                        rowIndex += 1
                    }
                    if (ubSlack != null) {
                        thisLhs.add(
                            listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = minmaxSlackVariable!!.index,
                                    coefficient = Flt64.one,
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = ubSlack.index,
                                    coefficient = -Flt64.one,
                                )
                            )
                        )
                        rowIndex += 1
                    }
                }
                thisLhs
            } else {
                emptyList()
            },
            signs = this.constraints.signs + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisSigns = ArrayList<Sign>()
                if (slackVariables[jp].first != null) {
                    thisSigns.add(Sign.GreaterEqual)
                }
                if (slackVariables[jp].second != null) {
                    thisSigns.add(Sign.LessEqual)
                }
                thisSigns
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(Sign.GreaterEqual, Sign.LessEqual) } + listOf(Sign.GreaterEqual)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                slackVariables.flatMap { (lbSlack, ubSlack) ->
                    listOfNotNull(
                        lbSlack?.let { Sign.GreaterEqual },
                        ubSlack?.let { Sign.GreaterEqual }
                    )
                }
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(Flt64.zero, Flt64.zero) } + listOf(minSlackAmount.first.toFlt64())
            } else {
                emptyList()
            } + if (minmaxSlack) {
                slackVariables.flatMap { (lbSlack, ubSlack) ->
                    listOfNotNull(
                        lbSlack?.let { Flt64.zero },
                        ubSlack?.let { Flt64.zero }
                    )
                }
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { (slack, _) -> listOf("${slack.name}_bin_lb", "${slack.name}_bin_ub") } + listOf("min_slack_amount")
            } else {
                emptyList()
            } + if (minmaxSlack) {
                slackVariables.flatMap { (lbSlack, ubSlack) ->
                    listOfNotNull(
                        lbSlack?.let { "${lbSlack.name}_minmax" },
                        ubSlack?.let { "${ubSlack.name}_minmax" }
                    )
                }
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(ConstraintSource.ElasticSlackBinary, ConstraintSource.ElasticSlackBinary) } + listOf(ConstraintSource.ElasticSlackBinary)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisSources = ArrayList<ConstraintSource>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisSources.add(ConstraintSource.ElasticSlackMinmax)
                    }
                    if (ubSlack != null) {
                        thisSources.add(ConstraintSource.ElasticSlackMinmax)
                    }
                }
                thisSources
            } else {
                emptyList()
            },
            origins = this.constraints.origins + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<OriginLinearConstraint?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(null, null) } + listOf(null)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisOrigins = ArrayList<OriginLinearConstraint?>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisOrigins.add(null)
                    }
                    if (ubSlack != null) {
                        thisOrigins.add(null)
                    }
                }
                thisOrigins
            } else {
                emptyList()
            },
            froms = this.constraints.froms + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<Pair<IntermediateSymbol, Boolean>?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(null, null) } + listOf(null)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisOrigins = ArrayList<Pair<IntermediateSymbol, Boolean>?>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisOrigins.add(null)
                    }
                    if (ubSlack != null) {
                        thisOrigins.add(null)
                    }
                }
                thisOrigins
            } else {
                emptyList()
            },
        )

        val objective = slackVariables.flatMap { (posSlack, negSlack) ->
            listOfNotNull(
                posSlack?.let {
                    LinearObjectiveCell(
                        colIndex = it.index,
                        coefficient = Flt64.one
                    )
                },
                negSlack?.let {
                    LinearObjectiveCell(
                        colIndex = it.index,
                        coefficient = Flt64.one
                    )
                }
            )
        } + if (minmaxSlack) {
            listOf(
                LinearObjectiveCell(
                    colIndex = minmaxSlackVariable!!.index,
                    coefficient = Flt64.one
                )
            )
        } else {
            emptyList()
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = this.variables.map {
                    it.copy().apply {
                        if (!free && !positiveNormalized && !negativeNormalized) {
                            _lowerBound = Flt64.negativeInfinity
                            _upperBound = Flt64.infinity
                        }
                    }
                } + slackVariables.flatMap { it.toList().filterNotNull() }.sortedBy { it.index } + if (minSlackAmount != null) {
                    slackBinVariables.map { it.second }
                } else {
                    emptyList()
                } + if (minmaxSlack) {
                    listOf(minmaxSlackVariable!!)
                } else {
                    emptyList()
                },
                constraints = constraints,
                name = "$name-elastic"
            ),
            tokensInSolver = tokensInSolver,
            objective = LinearObjective(ObjectCategory.Minimum, objective)
        )
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
                writer.append("${variables[cell.colIndex]}")
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

    override fun close() {
        dualOrigin?.close()
        super.close()
    }

    override fun toString(): String {
        return name
    }
}

suspend fun solveDual(
    model: LinearTriadModel,
    solver: LinearSolver
): Ret<LinearDualSolution> {
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
    model: LinearTriadModelView,
    solver: LinearSolver
): Ret<LinearDualSolution> {
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
