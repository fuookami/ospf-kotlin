package fuookami.ospf.kotlin.core.backend.intermediate_model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.isDirectory

data class Variable(
    val index: Int,
    var lowerBound: Flt64,
    var upperBound: Flt64,
    var type: VariableType<*>,
    val name: String
) : fuookami.ospf.kotlin.utils.concept.Cloneable<Variable> {
    override fun clone() = Variable(index, lowerBound, upperBound, type, name)

    override fun toString() = name
}

data class ConstraintCell(
    val rowIndex: Int,
    val colIndex: Int,
    val coefficient: Flt64
) : fuookami.ospf.kotlin.utils.concept.Cloneable<ConstraintCell> {
    override fun clone() = ConstraintCell(rowIndex, colIndex, coefficient.clone())
}

data class Constraint(
    val lhs: MutableList<ConstraintCell>,
    val signs: MutableList<Sign>,
    val rhs: MutableList<Flt64>,
    val names: MutableList<String>
) : fuookami.ospf.kotlin.utils.concept.Cloneable<Constraint> {
    val size: Int get() = rhs.size

    override fun clone() = Constraint(
        lhs.asSequence().map { it.clone() }.toMutableList(), 
        signs.toMutableList(),
        rhs.asSequence().map { it.clone() }.toMutableList(), 
        names.toMutableList()
    )
}

data class ObjectiveCell(
    val colIndex: Int,
    var coefficient: Flt64
) : fuookami.ospf.kotlin.utils.concept.Cloneable<ObjectiveCell> {
    override fun clone() = ObjectiveCell(colIndex, coefficient.clone())
}

data class Objective(
    val category: ObjectCategory,
    val obj: List<ObjectiveCell>
) : fuookami.ospf.kotlin.utils.concept.Cloneable<Objective> {
    override fun clone() = Objective(category, obj.toList())
}

interface LinearTraitModelView {
    val variables: List<Variable>
    val constraints: Constraint
    val objective: Objective
    val name: String

    fun export(format: ModelFileFormat): Try<Error> {
        return export(kotlin.io.path.Path("."), format)
    }

    fun export(name: String, format: ModelFileFormat): Try<Error> {
        return export(kotlin.io.path.Path(".").resolve(name), format)
    }

    fun export(path: Path, format: ModelFileFormat): Try<Error> {
        val file = if (path.isDirectory()) {
            path.resolve("$name.${format}").toFile()
        } else {
            path.toFile()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file)
        val result = when (format) {
            ModelFileFormat.LP -> {
                exportLP(writer)
            }
        }
        writer.flush()
        writer.close()
        return result
    }

    fun exportLP(writer: FileWriter): Try<Error>
}

class BasicLinearTriadModel(
    val variables: List<Variable>,
    val constraints: Constraint,
    val name: String
) : fuookami.ospf.kotlin.utils.concept.Cloneable<BasicLinearTriadModel> {
    override fun clone() = BasicLinearTriadModel(
        variables.map { it.clone() },
        constraints.clone(),
        name
    )

    fun containsBinary(): Boolean {
        return variables.any { it.type.isBinaryType() }
    }

    fun containsInteger(): Boolean {
        return variables.any { it.type.isIntegerType() }
    }

    fun containsNotBinaryInteger(): Boolean {
        return variables.any { it.type.isNotBinaryIntegerType() }
    }

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
                    it.type = Percentage
                }

                is Ternary, is UInteger -> {
                    it.type = UContinuous
                }

                is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer -> {
                    it.type = Continuous
                }

                else -> {}
            }
        }
    }

    fun normalize() {
        for (variable in variables) {
            if (!(variable.lowerBound.isNegativeInfinity() || (variable.lowerBound eq Flt64.zero))) {
                constraints.lhs.add(
                    ConstraintCell(
                        constraints.size,
                        variable.index,
                        Flt64.one
                    )
                )
                constraints.signs.add(Sign.GreaterEqual)
                constraints.rhs.add(variable.lowerBound)
                constraints.names.add("${variable.name}_lb")
                variable.lowerBound = Flt64.negativeInfinity
            }
            if (!(variable.upperBound.isInfinity() || (variable.upperBound eq Flt64.zero))) {
                constraints.lhs.add(
                    ConstraintCell(
                        constraints.size,
                        variable.index,
                        Flt64.one
                    )
                )
                constraints.signs.add(Sign.LessEqual)
                constraints.rhs.add(variable.upperBound)
                constraints.names.add("${variable.name}_ub")
                variable.upperBound = Flt64.infinity
            }
        }
    }

    fun exportLP(writer: FileWriter): Try<Error> {
        writer.append("Subject To\n")
        var i = 0
        var j = 0
        while (i != constraints.size) {
            writer.append(" ${constraints.names[i]}: ")
            var flag = false
            var k = 0
            while (j != constraints.lhs.size && i == constraints.lhs[j].rowIndex) {
                if (k != 0) {
                    if (constraints.lhs[j].coefficient leq Flt64.zero) {
                        writer.append(" - ")
                    } else {
                        writer.append(" + ")
                    }
                    val coefficient = abs(constraints.lhs[j].coefficient)
                    if (!(coefficient eq Flt64.one)) {
                        writer.append("$coefficient ")
                    }
                } else {
                    if (!(constraints.lhs[j].coefficient eq Flt64.one)) {
                        writer.append("${constraints.lhs[j].coefficient} ")
                    }
                }
                writer.append("${variables[constraints.lhs[j].colIndex]}")
                flag = true
                ++j
                ++k
            }
            if (!flag) {
                writer.append("0")
            }
            writer.append(" ${constraints.signs[i]} ${constraints.rhs[i]}\n")
            ++i
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

        if (containsNotBinaryInteger()) {
            writer.append("Generals\n")
            for (variable in variables) {
                if (variable.type != Binary && variable.type.isIntegerType()) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        writer.append("End\n")
        return Ok(success)
    }
}

data class LinearTriadModel(
    private val impl: BasicLinearTriadModel,
    override val objective: Objective,
) : LinearTraitModelView,
    fuookami.ospf.kotlin.utils.concept.Cloneable<LinearTriadModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: Constraint by impl::constraints
    override val name: String by impl::name

    companion object {
        operator fun invoke(model: LinearModel): LinearTriadModel {
            val tokens = model.tokens.tokens
            val solverIndexes = model.tokens.solverIndexMap

            val variables = ArrayList<Variable?>()
            for (i in tokens.indices) {
                variables.add(null)
            }
            for (token in tokens) {
                val index = solverIndexes[token.solverIndex]!!
                variables[index] = Variable(
                    index,
                    token.lowerBound,
                    token.upperBound,
                    token.variable.type,
                    token.variable.name
                )
            }

            val lhs = ArrayList<ConstraintCell>()
            val signs = ArrayList<Sign>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            for ((index, constraint) in model.constraints.withIndex()) {
                for (cell in constraint.lhs) {
                    val temp = cell as LinearCell
                    lhs.add(
                        ConstraintCell(
                            index,
                            solverIndexes[temp.token.solverIndex]!!,
                            temp.coefficient
                        )
                    )
                }
                signs.add(constraint.sign)
                rhs.add(constraint.rhs)
                names.add(constraint.name)
            }

            val objective = ArrayList<ObjectiveCell>()
            for (i in tokens.indices) {
                objective.add(
                    ObjectiveCell(
                        i,
                        Flt64.zero
                    )
                )
            }
            for (subObject in model.objectFunction.subObjects) {
                if (subObject.category == model.objectFunction.category) {
                    for (cell in subObject.cells) {
                        val temp = cell as LinearCell
                        objective[solverIndexes[temp.token.solverIndex]!!].coefficient += temp.coefficient
                    }
                } else {
                    for (cell in subObject.cells) {
                        val temp = cell as LinearCell
                        objective[solverIndexes[temp.token.solverIndex]!!].coefficient -= temp.coefficient
                    }
                }
            }

            return LinearTriadModel(
                BasicLinearTriadModel(
                    variables.map { it!! },
                    Constraint(lhs, signs, rhs, names),
                    model.name
                ),
                Objective(model.objectFunction.category, objective)
            )
        }
    }

    override fun clone() =
        LinearTriadModel(impl.clone(), objective.clone())

    fun containsBinary(): Boolean = impl.containsBinary()
    fun containsInteger(): Boolean = impl.containsInteger()
    fun containsNotBinaryInteger(): Boolean = impl.containsNotBinaryInteger()
    fun normalized() {
        impl.normalized()
    }

    fun linearRelax() {
        impl.linearRelax()
    }

    fun normalize() {
        impl.normalize()
    }

    fun dual(): LinearTriadModel {
        val variables = ArrayList<Variable>()
        for (i in 0 until this.constraints.size) {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (this.constraints.signs[i]) {
                Sign.LessEqual -> {
                    upperBound = Flt64.zero
                }

                Sign.GreaterEqual -> {
                    lowerBound = Flt64.zero
                }

                else -> {}
            }
            variables.add(
                Variable(
                    i,
                    lowerBound,
                    upperBound,
                    Continuous,
                    "${this.constraints.names[i]}_dual"
                )
            )
        }

        val coefficients = ArrayList<ArrayList<Pair<Int, Flt64>>>()
        for (i in 0 until this.constraints.size) {
            coefficients.add(ArrayList())
        }
        for (cell in this.constraints.lhs) {
            coefficients[cell.colIndex].add(Pair(cell.rowIndex, cell.coefficient))
        }
        val lhs = ArrayList<ConstraintCell>()
        for (i in 0 until this.variables.size) {
            for (coefficient in coefficients[i]) {
                lhs.add(
                    ConstraintCell(
                        i,
                        coefficient.first,
                        coefficient.second
                    )
                )
            }
        }
        val signs = ArrayList<Sign>()
        val rhs = ArrayList<Flt64>()
        val names = ArrayList<String>()
        for (variable in this.variables) {
            if (variable.lowerBound.isNegativeInfinity() && variable.upperBound.isInfinity()) {
                signs.add(Sign.Equal)
            } else if (variable.lowerBound.isNegativeInfinity()) {
                signs.add(Sign.GreaterEqual)
            } else if (variable.upperBound.isInfinity()) {
                signs.add(Sign.LessEqual)
            }
            rhs.add(this.objective.obj[variable.index].coefficient)
            names.add("${variable.name}_dual")
        }

        val objective = ArrayList<ObjectiveCell>()
        for (i in 0 until this.constraints.size) {
            objective.add(
                ObjectiveCell(
                    i,
                    this.constraints.rhs[i]
                )
            )
        }

        return LinearTriadModel(
            BasicLinearTriadModel(
                variables,
                Constraint(lhs, signs, rhs, names),
                "$name-dual"
            ),
            Objective(this.objective.category.reverse(), objective)
        )
    }

    override fun exportLP(writer: FileWriter): Try<Error> {
        writer.write("${objective.category}\n")
        var i = 0
        for (cell in objective.obj) {
            if (cell.coefficient eq Flt64.zero) {
                continue
            }
            if (i != 0) {
                if (cell.coefficient leq Flt64.zero) {
                    writer.append(" - ")
                } else {
                    writer.append(" + ")
                }

                val coefficient = abs(cell.coefficient)
                if (!(coefficient eq Flt64.one)) {
                    writer.append("$coefficient ")
                }
            } else {
                if (!(cell.coefficient eq Flt64.one)) {
                    writer.append("${cell.coefficient} ")
                }
            }
            writer.append("${variables[cell.colIndex]}")
            ++i
        }
        writer.append("\n\n")

        return when (val result = impl.exportLP(writer)) {
            is Failed -> {
                Failed(result.error)
            }

            is Ok -> {
                Ok(success)
            }
        }
    }
}
