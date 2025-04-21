package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.variable.*

data class QuadraticConstraintCell(
    override val rowIndex: Int,
    val colIndex1: Int,
    val colIndex2: Int?,
    override val coefficient: Flt64
) : ConstraintCell, Cloneable, Copyable<QuadraticConstraintCell> {
    override fun copy() = QuadraticConstraintCell(rowIndex, colIndex1, colIndex2, coefficient.copy())
    override fun clone() = copy()
}

typealias QuadraticConstraint = Constraint<QuadraticConstraintCell>

data class QuadraticObjectiveCell(
    val colIndex1: Int,
    val colIndex2: Int?,
    override val coefficient: Flt64
) : Cell, Cloneable, Copyable<QuadraticObjectiveCell> {
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
                            constraints.size,
                            variable.index,
                            null,
                            Flt64.one
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
                            constraints.size,
                            variable.index,
                            null,
                            Flt64.one
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
                val coefficient = if (j != 0) {
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
    override val objective: QuadraticObjective,
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraint by impl::constraints
    override val name: String by impl::name

    companion object {
        private val logger = logger()

        suspend operator fun invoke(model: QuadraticMechanismModel, concurrent: Boolean? = null): QuadraticTetradModel {
            logger.trace("Creating QuadraticTetradModel for $model")
            val tetradModel = if (concurrent ?: model.concurrent) {
                coroutineScope {
                    val variablePromise = async(Dispatchers.Default) {
                        dumpVariables(model)
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpConstraintsAsync(model)
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpObjectives(model)
                    }

                    QuadraticTetradModel(
                        BasicQuadraticTetradModel(
                            variablePromise.await(),
                            constraintPromise.await(),
                            model.name
                        ),
                        objectivePromise.await()
                    )
                }
            } else {
                QuadraticTetradModel(
                    BasicQuadraticTetradModel(
                        dumpVariables(model),
                        dumpConstraints(model),
                        model.name
                    ),
                    dumpObjectives(model)
                )
            }

            logger.trace("QuadraticTetradModel created for $model")
            System.gc()
            return tetradModel
        }

        private fun dumpVariables(model: QuadraticMechanismModel): List<Variable> {
            val tokens = model.tokens.tokens
            val tokenIndexes = model.tokens.tokenIndexMap

            val variables = ArrayList<Variable?>()
            for (i in tokens.indices) {
                variables.add(null)
            }
            for (token in tokens) {
                val index = tokenIndexes[token]!!
                variables[index] = Variable(
                    index,
                    token.lowerBound!!.value.unwrap(),
                    token.upperBound!!.value.unwrap(),
                    token.variable.type,
                    token.variable.name,
                    token.result
                )
            }
            return variables.map { it!! }
        }

        private fun dumpConstraints(model: QuadraticMechanismModel): QuadraticConstraint {
            val tokenIndexes = model.tokens.tokenIndexMap

            val constraints = model.constraints.withIndex().map { (index, constraint) ->
                val lhs = ArrayList<QuadraticConstraintCell>()
                for (cell in constraint.lhs) {
                    val temp = cell as QuadraticCell
                    lhs.add(
                        QuadraticConstraintCell(
                            index,
                            tokenIndexes[temp.token1]!!,
                            temp.token2?.let { tokenIndexes[it]!! },
                            temp.coefficient.let {
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
                lhs
            }

            val lhs = ArrayList<List<QuadraticConstraintCell>>()
            val signs = ArrayList<Sign>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            for ((index, constraint) in model.constraints.withIndex()) {
                lhs.add(constraints[index])
                signs.add(constraint.sign)
                rhs.add(constraint.rhs)
                names.add(constraint.name)
            }
            return QuadraticConstraint(lhs, signs, rhs, names)
        }

        private suspend fun dumpConstraintsAsync(model: QuadraticMechanismModel): QuadraticConstraint {
            val tokenIndexes = model.tokens.tokenIndexMap
            return if (Runtime.getRuntime().availableProcessors() > 2 && model.constraints.size > Runtime.getRuntime().availableProcessors()) {
                val factor = Flt64(model.constraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
                val segment = if (factor >= 1) {
                    pow(UInt64.ten, factor).toInt()
                } else {
                    10
                }
                coroutineScope {
                    val constraintPromises = (0..(model.constraints.size / segment)).map {
                        async(Dispatchers.Default) {
                            val constraints = ArrayList<List<QuadraticConstraintCell>>()
                            for (i in (it * segment) until minOf(model.constraints.size, (it + 1) * segment)) {
                                val constraint = model.constraints[i]
                                val lhs = ArrayList<QuadraticConstraintCell>()
                                for (cell in constraint.lhs) {
                                    val temp = cell as QuadraticCell
                                    lhs.add(
                                        QuadraticConstraintCell(
                                            rowIndex = i,
                                            colIndex1 = tokenIndexes[temp.token1]!!,
                                            colIndex2 = temp.token2?.let { token2 -> tokenIndexes[token2]!! },
                                            coefficient = temp.coefficient.let { coefficient ->
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
                                }
                                constraints.add(lhs)
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
                    for ((index, constraint) in model.constraints.withIndex()) {
                        lhs.add(constraintPromises[index / segment].await()[index % segment])
                        signs.add(constraint.sign)
                        rhs.add(constraint.rhs)
                        names.add(constraint.name)
                    }
                    System.gc()
                    QuadraticConstraint(lhs, signs, rhs, names)
                }
            } else {
                val lhs = ArrayList<List<QuadraticConstraintCell>>()
                val signs = ArrayList<Sign>()
                val rhs = ArrayList<Flt64>()
                val names = ArrayList<String>()

                for ((index, constraint) in model.constraints.withIndex()) {
                    val thisLhs = ArrayList<QuadraticConstraintCell>()
                    for (cell in constraint.lhs) {
                        val temp = cell as QuadraticCell
                        thisLhs.add(
                            QuadraticConstraintCell(
                                rowIndex = index,
                                colIndex1 = tokenIndexes[temp.token1]!!,
                                colIndex2 = temp.token2?.let { tokenIndexes[it]!! },
                                coefficient = temp.coefficient.let {
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
                }
                System.gc()
                QuadraticConstraint(lhs, signs, rhs, names)
            }
        }

        private fun dumpObjectives(model: QuadraticMechanismModel): QuadraticObjective {
            val tokens = model.tokens.tokens
            val tokenIndexes = model.tokens.tokenIndexMap

            val objectiveCategory = if (model.objectFunction.subObjects.size == 1) {
                model.objectFunction.subObjects.first().category
            } else {
                model.objectFunction.category
            }

            val coefficient = tokens.indices.map { HashMap<Int?, Flt64>() }.toMutableList()
            for (subObject in model.objectFunction.subObjects) {
                if (subObject.category == objectiveCategory) {
                    for (cell in subObject.cells) {
                        val temp = cell as QuadraticCell
                        val value = coefficient[tokenIndexes[temp.token1]!!][temp.token2?.let { tokenIndexes[it]!! }] ?: Flt64.zero
                        coefficient[tokenIndexes[temp.token1]!!][temp.token2?.let { tokenIndexes[it]!! }] = value + temp.coefficient
                    }
                } else {
                    for (cell in subObject.cells) {
                        val temp = cell as QuadraticCell
                        val value = coefficient[tokenIndexes[temp.token1]!!][temp.token2?.let { tokenIndexes[it]!! }] ?: Flt64.zero
                        coefficient[tokenIndexes[temp.token1]!!][temp.token2?.let { tokenIndexes[it]!! }] = value - temp.coefficient
                    }
                }
            }

            val objective = ArrayList<QuadraticObjectiveCell>()
            for (i in tokens.indices) {
                for ((j, value) in coefficient[i]) {
                    objective.add(
                        QuadraticObjectiveCell(
                            i,
                            j,
                            value.let {
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

    override fun copy() = QuadraticTetradModel(impl.copy(), objective.copy())
    override fun clone() = copy()

    fun normalized() {
        impl.normalized()
    }

    fun linearRelax() {
        impl.linearRelax()
    }

    fun normalize() {
        impl.normalize()
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
