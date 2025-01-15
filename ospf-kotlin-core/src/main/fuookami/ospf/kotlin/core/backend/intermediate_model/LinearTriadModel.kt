package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class LinearConstraintCell(
    override val rowIndex: Int,
    val colIndex: Int,
    override val coefficient: Flt64
) : ConstraintCell, Cloneable, Copyable<LinearConstraintCell> {
    override fun copy() = LinearConstraintCell(rowIndex, colIndex, coefficient.copy())
    override fun clone() = copy()
}

typealias LinearConstraint = Constraint<LinearConstraintCell>

data class LinearObjectiveCell(
    val colIndex: Int,
    override val coefficient: Flt64
) : Cell, Cloneable, Copyable<LinearObjectiveCell> {
    override fun copy() = LinearObjectiveCell(colIndex, coefficient.copy())
    override fun clone() = copy()
}

typealias LinearObjective = Objective<LinearObjectiveCell>

class BasicLinearTriadModel(
    override val variables: List<Variable>,
    override val constraints: LinearConstraint,
    override val name: String
) : BasicModelView<LinearConstraintCell>, Cloneable, Copyable<BasicLinearTriadModel> {
    override fun copy() = BasicLinearTriadModel(
        variables.map { it.copy() },
        constraints.copy(),
        name
    )

    override fun clone() = copy()

    fun normalized(): Boolean {
        return variables.all {
            (it.lowerBound.isNegativeInfinity() || it.lowerBound eq Flt64.zero) && (it.upperBound.isInfinity() || it.upperBound eq Flt64.zero)
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
                        LinearConstraintCell(
                            constraints.size,
                            variable.index,
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
                        LinearConstraintCell(
                            constraints.size,
                            variable.index,
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

typealias LinearTriadModelView = ModelView<LinearConstraintCell, LinearObjectiveCell>

data class LinearTriadModel(
    private val impl: BasicLinearTriadModel,
    override val objective: LinearObjective,
) : LinearTriadModelView, Cloneable, Copyable<LinearTriadModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraint by impl::constraints
    override val name: String by impl::name

    companion object {
        private val logger = logger()

        suspend operator fun invoke(model: LinearMechanismModel, concurrent: Boolean? = null): LinearTriadModel {
            logger.trace("Creating LinearTriadModel for $model")
            val triadModel = if (concurrent ?: model.concurrent) {
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

                    LinearTriadModel(
                        BasicLinearTriadModel(
                            variablePromise.await(),
                            constraintPromise.await(),
                            model.name
                        ),
                        objectivePromise.await()
                    )
                }
            } else {
                LinearTriadModel(
                    BasicLinearTriadModel(
                        dumpVariables(model),
                        dumpConstraints(model),
                        model.name
                    ),
                    dumpObjectives(model)
                )
            }

            logger.trace("LinearTriadModel created for $model")
            System.gc()
            return triadModel
        }

        private fun dumpVariables(model: LinearMechanismModel): List<Variable> {
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

        private fun dumpConstraints(model: LinearMechanismModel): LinearConstraint {
            val tokenIndexes = model.tokens.tokenIndexMap

            val constraints = model.constraints.withIndex().map { (index, constraint) ->
                val lhs = ArrayList<LinearConstraintCell>()
                for (cell in constraint.lhs) {
                    val temp = cell as LinearCell
                    lhs.add(
                        LinearConstraintCell(
                            index,
                            tokenIndexes[temp.token]!!,
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

            val lhs = ArrayList<List<LinearConstraintCell>>()
            val signs = ArrayList<Sign>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            for ((index, constraint) in model.constraints.withIndex()) {
                lhs.add(constraints[index])
                signs.add(constraint.sign)
                rhs.add(constraint.rhs)
                names.add(constraint.name)
            }
            return LinearConstraint(lhs, signs, rhs, names)
        }

        private suspend fun dumpConstraintsAsync(model: LinearMechanismModel): LinearConstraint {
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
                            val constraints = ArrayList<List<LinearConstraintCell>>()
                            for (i in (it * segment) until minOf(model.constraints.size, (it + 1) * segment)) {
                                val constraint = model.constraints[i]
                                val lhs = ArrayList<LinearConstraintCell>()
                                for (cell in constraint.lhs) {
                                    val temp = cell as LinearCell
                                    lhs.add(
                                        LinearConstraintCell(
                                            rowIndex = i,
                                            colIndex = tokenIndexes[temp.token]!!,
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
                                constraints.add(lhs)
                            }
                            constraints
                        }
                    }

                    val lhs = ArrayList<List<LinearConstraintCell>>()
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
                    LinearConstraint(lhs, signs, rhs, names)
                }
            } else {
                val lhs = ArrayList<List<LinearConstraintCell>>()
                val signs = ArrayList<Sign>()
                val rhs = ArrayList<Flt64>()
                val names = ArrayList<String>()
                for ((index, constraint) in model.constraints.withIndex()) {
                    val thisLhs = ArrayList<LinearConstraintCell>()
                    for (cell in constraint.lhs) {
                        val temp = cell as LinearCell
                        thisLhs.add(
                            LinearConstraintCell(
                                rowIndex = index,
                                colIndex = tokenIndexes[temp.token]!!,
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
                LinearConstraint(lhs, signs, rhs, names)
            }
        }

        private fun dumpObjectives(model: LinearMechanismModel): LinearObjective {
            val tokens = model.tokens.tokens
            val tokenIndexes = model.tokens.tokenIndexMap

            val objectiveCategory = if (model.objectFunction.subObjects.size == 1) {
                model.objectFunction.subObjects.first().category
            } else {
                model.objectFunction.category
            }
            val coefficient = tokens.indices.map { Flt64.zero }.toMutableList()
            for (subObject in model.objectFunction.subObjects) {
                if (subObject.category == objectiveCategory) {
                    for (cell in subObject.cells) {
                        val temp = cell as LinearCell
                        coefficient[tokenIndexes[temp.token]!!] = coefficient[tokenIndexes[temp.token]!!] + temp.coefficient
                    }
                } else {
                    for (cell in subObject.cells) {
                        val temp = cell as LinearCell
                        coefficient[tokenIndexes[temp.token]!!] = coefficient[tokenIndexes[temp.token]!!] - temp.coefficient
                    }
                }
            }
            val objective = ArrayList<LinearObjectiveCell>()
            for (i in tokens.indices) {
                objective.add(
                    LinearObjectiveCell(
                        i,
                        coefficient[i].let {
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
            return LinearObjective(objectiveCategory, objective)
        }
    }

    override fun copy() = LinearTriadModel(impl.copy(), objective.copy())
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

    suspend fun dual(): LinearTriadModel {
        val variables = this.constraints.indices.map {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (this.constraints.signs[it]) {
                Sign.LessEqual -> {
                    upperBound = Flt64.zero
                }

                Sign.GreaterEqual -> {
                    lowerBound = Flt64.zero
                }

                else -> {}
            }

            Variable(
                it,
                lowerBound,
                upperBound,
                Continuous,
                "${this.constraints.names[it]}_dual"
            )
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this.constraints.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }
        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map {
                async(Dispatchers.Default) {
                    coefficients[it].map { cell ->
                        LinearConstraintCell(
                            it,
                            cell.first,
                            cell.second
                        )
                    }
                }
            }
            constraintPromises.map { it.await() }
        }
        val signs = this.variables.map {
            if (it.lowerBound.isNegativeInfinity() && it.upperBound.isInfinity()) {
                Sign.Equal
            } else if (it.lowerBound.isNegativeInfinity()) {
                Sign.GreaterEqual
            } else if (it.upperBound.isInfinity()) {
                Sign.LessEqual
            } else {
                Sign.Equal
            }
        }
        val rhs = this.variables.map { this.objective.obj[it.index].coefficient }
        val names = this.variables.map { "${it.name}_dual" }
        val objective = this.constraints.indices.map {
            LinearObjectiveCell(
                it,
                this.constraints.rhs[it]
            )
        }

        return LinearTriadModel(
            BasicLinearTriadModel(
                variables,
                LinearConstraint(lhs, signs, rhs, names),
                "$name-dual"
            ),
            LinearObjective(this.objective.category.reverse(), objective)
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

    override fun toString(): String {
        return name
    }
}
