package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import io.michaelrocks.bimap.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

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

typealias QuadraticConstraint = Constraint<QuadraticConstraintCell>

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

    fun normalized(): Boolean {
        return variables.any {
            !(it.lowerBound.isNegativeInfinity() || (it.lowerBound eq Flt64.zero))
                    || !(it.upperBound.isInfinity() || (it.upperBound eq Flt64.zero))
        }
    }

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

    fun normalize() {
        for (variable in variables) {
            if (!(variable.lowerBound.isNegativeInfinity() || (variable.lowerBound eq Flt64.zero))) {
                constraints._lhs.add(
                    listOf(
                        QuadraticConstraintCell(
                            rowIndex = constraints.size,
                            colIndex1 = variable.index,
                            colIndex2 = null,
                            coefficient = Flt64.one
                        )
                    )
                )
                constraints._signs.add(Sign.GreaterEqual)
                constraints._rhs.add(variable.lowerBound)
                constraints._names.add("${variable.name}_lb")
                variable._lowerBound = Flt64.negativeInfinity
            }
            if (!(variable.upperBound.isInfinity() || (variable.upperBound eq Flt64.zero))) {
                constraints._lhs.add(
                    listOf(
                        QuadraticConstraintCell(
                            rowIndex = constraints.size,
                            colIndex1 = variable.index,
                            colIndex2 = null,
                            coefficient = Flt64.one
                        )
                    )
                )
                constraints._signs.add(Sign.LessEqual)
                constraints._rhs.add(variable.upperBound)
                constraints._names.add("${variable.name}_ub")
                variable._upperBound = Flt64.infinity
            }
        }
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

typealias QuadraticTetradModelView = ModelView<QuadraticConstraintCell, QuadraticObjectiveCell>

data class QuadraticTetradModel(
    private val impl: BasicQuadraticTetradModel,
    val tokenIndexMap: BiMap<Token, Int>,
    override val objective: QuadraticObjective,
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraint by impl::constraints
    override val name: String by impl::name

    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            model: QuadraticMechanismModel,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            concurrent: Boolean? = null
        ): QuadraticTetradModel {
            logger.trace("Creating QuadraticTetradModel for $model")
            val tokenIndexMap = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokenIndexMap
            } else {
                model.tokens.tokenIndexMapWithout(fixedVariables.keys)
            }
            val tetradModel = if (concurrent ?: model.concurrent) {
                coroutineScope {
                    val variablePromise = async(Dispatchers.Default) {
                        dumpVariables(model, tokenIndexMap)
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpConstraintsAsync(model, tokenIndexMap, fixedVariables)
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpObjectives(model, tokenIndexMap, fixedVariables)
                    }

                    QuadraticTetradModel(
                        BasicQuadraticTetradModel(
                            variablePromise.await(),
                            constraintPromise.await(),
                            model.name
                        ),
                        tokenIndexMap,
                        objectivePromise.await()
                    )
                }
            } else {
                QuadraticTetradModel(
                    BasicQuadraticTetradModel(
                        dumpVariables(model, tokenIndexMap),
                        dumpConstraints(model, tokenIndexMap, fixedVariables),
                        model.name
                    ),
                    tokenIndexMap,
                    dumpObjectives(model, tokenIndexMap, fixedVariables)
                )
            }

            logger.trace("QuadraticTetradModel created for $model")
            System.gc()
            return tetradModel
        }

        private fun dumpVariables(
            model: QuadraticMechanismModel,
            tokenIndexMap: BiMap<Token, Int>
        ): List<Variable> {
            val variables = ArrayList<Variable?>()
            for ((_, _) in tokenIndexMap) {
                variables.add(null)
            }
            for ((token, i) in tokenIndexMap) {
                val bounds = model.constraints.filter {
                    it.lhs.all { cell -> cell.token1 == token && cell.token2 == null }
                }
                val lb = bounds
                    .filter { it.sign == Sign.GreaterEqual || it.sign == Sign.Equal }
                    .maxOfOrNull {
                        val lhs = it.lhs.sumOf { cell -> cell.coefficient }
                        if (lhs neq Flt64.zero) {
                            it.rhs / lhs
                        } else if (it.rhs gr Flt64.zero) {
                            Flt64.infinity
                        } else {
                            Flt64.negativeInfinity
                        }
                    }
                val ub = bounds
                    .filter { it.sign == Sign.LessEqual || it.sign == Sign.Equal }
                    .minOfOrNull {
                        val lhs = it.lhs.sumOf { cell -> cell.coefficient }
                        if (lhs neq Flt64.zero) {
                            it.rhs / lhs
                        } else if (it.rhs gr Flt64.zero) {
                            Flt64.infinity
                        } else {
                            Flt64.negativeInfinity
                        }
                    }
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
                    name = token.variable.name,
                    initialResult = token.result
                )
            }
            return variables.map { it!! }
        }

        private fun dumpConstraints(
            model: QuadraticMechanismModel,
            tokenIndexes: BiMap<Token, Int>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): QuadraticConstraint {
            val notBoundConstraints = model.constraints.filter {
                it.lhs.isEmpty() || it.lhs.any { cell -> cell.token2 != null || cell.token1 != it.lhs.first().token1 }
            }

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
                                coefficient = cell.coefficient.let {
                                    if (it.isInfinity()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (it.isNegativeInfinity()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        it
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
                                    if (it.isInfinity()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (it.isNegativeInfinity()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        it
                                    }
                                } * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)
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
                                    if (it.isInfinity()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (it.isNegativeInfinity()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        it
                                    }
                                } * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)
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
            for ((index, constraint) in notBoundConstraints.withIndex()) {
                lhs.add(constraints[index].first)
                signs.add(constraint.sign)
                rhs.add(constraints[index].second)
                names.add(constraint.name)
                sources.add(ConstraintSource.Origin)
            }
            return QuadraticConstraint(lhs, signs, rhs, names, sources)
        }

        private suspend fun dumpConstraintsAsync(
            model: QuadraticMechanismModel,
            tokenIndexes: BiMap<Token, Int>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): QuadraticConstraint {
            val notBoundConstraints = model.constraints.filter {
                it.lhs.isEmpty() || it.lhs.any { cell -> cell.token2 != null || cell.token1 != it.lhs.first().token1 }
            }

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
                                                    if (coefficient.isInfinity()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity()) {
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
                                                coefficient = cell.coefficient.let { coefficient ->
                                                    if (coefficient.isInfinity()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity()) {
                                                        -Flt64.decimalPrecision.reciprocal()
                                                    } else {
                                                        coefficient
                                                    }
                                                } * (fixedVariables?.get(cell.token2!!.variable) ?: Flt64.one)
                                            )
                                        )
                                    } else if (tokenIndexes.containsKey(cell.token2)) {
                                        assert(cell.token2 != null)
                                        lhs.add(
                                            QuadraticConstraintCell(
                                                rowIndex = i,
                                                colIndex1 = tokenIndexes[cell.token2]!!,
                                                colIndex2 = null,
                                                coefficient = cell.coefficient.let { coefficient ->
                                                    if (coefficient.isInfinity()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity()) {
                                                        -Flt64.decimalPrecision.reciprocal()
                                                    } else {
                                                        coefficient
                                                    }
                                                } * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one)
                                            )
                                        )
                                    } else {
                                        rhs -= cell.coefficient * (fixedVariables?.get(cell.token1.variable) ?: Flt64.one) * (fixedVariables?.get(cell.token2?.variable) ?: Flt64.one)
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
                    for ((index, constraint) in notBoundConstraints.withIndex()) {
                        val (thisLhs, thisRhs) = constraintPromises[index / segment].await()[index % segment]
                        lhs.add(thisLhs)
                        signs.add(constraint.sign)
                        rhs.add(thisRhs)
                        names.add(constraint.name)
                        sources.add(ConstraintSource.Origin)
                    }
                    System.gc()
                    QuadraticConstraint(lhs, signs, rhs, names, sources)
                }
            } else {
                val lhs = ArrayList<List<QuadraticConstraintCell>>()
                val signs = ArrayList<Sign>()
                val rhs = ArrayList<Flt64>()
                val names = ArrayList<String>()
                val sources = ArrayList<ConstraintSource>()
                for ((index, constraint) in notBoundConstraints.withIndex()) {
                    val thisLhs = ArrayList<QuadraticConstraintCell>()
                    for (cell in constraint.lhs) {
                        thisLhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[cell.token1]!!,
                                colIndex2 = cell.token2?.let { tokenIndexes[it]!! },
                                coefficient = cell.coefficient.let {
                                    if (it.isInfinity()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (it.isNegativeInfinity()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
                    }
                    lhs.add(thisLhs)
                    signs.add(constraint.sign)
                    rhs.add(constraint.rhs)
                    names.add(constraint.name)
                    sources.add(ConstraintSource.Origin)
                }
                System.gc()
                QuadraticConstraint(lhs, signs, rhs, names, sources)
            }
        }

        private fun dumpObjectives(
            model: QuadraticMechanismModel,
            tokenIndexes: BiMap<Token, Int>,
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
                            coefficient = value.let {
                                if (it.isInfinity()) {
                                    Flt64.decimalPrecision.reciprocal()
                                } else if (it.isNegativeInfinity()) {
                                    -Flt64.decimalPrecision.reciprocal()
                                } else {
                                    it
                                }
                            }
                        )
                    )
                }
            }
            return QuadraticObjective(objectiveCategory, objective)
        }
    }

    override fun copy() = QuadraticTetradModel(impl.copy(), tokenIndexMap, objective.copy())
    override fun clone() = copy()

    fun normalized(): Boolean {
        return impl.normalized()
    }

    fun linearRelax(): QuadraticTetradModel {
        impl.linearRelax()
        return this
    }

    fun normalize(): QuadraticTetradModel {
        impl.normalize()
        return this
    }

    suspend fun dual(): QuadraticTetradModel {
        TODO("not implemented yet")
    }

    suspend fun farkasDual(): QuadraticTetradModel {
        TODO("not implemented yet")
    }

    suspend fun feasibility(): QuadraticTetradModel {
        assert(normalized())
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
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack"
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
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack"
                        )
                        colIndex += 1
                        val artifact = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact"
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
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact"
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
            tokenIndexMap = tokenIndexMap,
            objective = QuadraticObjective(ObjectCategory.Minimum, objective)
        )
    }

    override fun exportLP(writer: FileWriter): Try {
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
