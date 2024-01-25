package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

data class QuadraticConstraintCell(
    override val rowIndex: Int,
    val colIndex: Int,
    val quadraticColIndex: Int?,
    override val coefficient: Flt64
) : ConstraintCell, Cloneable, Copyable<QuadraticConstraintCell> {
    override fun copy() = QuadraticConstraintCell(rowIndex, colIndex, quadraticColIndex, coefficient.copy())
    override fun clone() = copy()
}

typealias QuadraticConstraint = Constraint<QuadraticConstraintCell>

data class QuadraticObjectiveCell(
    val colIndex: Int,
    val quadraticColIndex: Int?,
    override val coefficient: Flt64
) : Cell, Cloneable, Copyable<QuadraticObjectiveCell> {
    override fun copy() = QuadraticObjectiveCell(colIndex, quadraticColIndex, coefficient.copy())
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

    override fun exportLP(writer: FileWriter): Try {
        writer.append("Subject To\n")
        var i = 0
        var j = 0
        while (i != constraints.size) {
            writer.append(" ${constraints.names[i]}: ")
            var flag = false
            var k = 0
            while (j != constraints.lhs.size && i == constraints.lhs[j].rowIndex) {
                val coefficient = if (k != 0) {
                    if (constraints.lhs[j].coefficient leq Flt64.zero) {
                        writer.append(" - ")
                    } else {
                        writer.append(" + ")
                    }
                    abs(constraints.lhs[j].coefficient)
                } else {
                    constraints.lhs[j].coefficient
                }
                if (coefficient neq Flt64.zero) {
                    if (coefficient neq Flt64.one) {
                        writer.append("$coefficient ")
                    }
                    if (constraints.lhs[j].quadraticColIndex == null) {
                        writer.append("${variables[constraints.lhs[j].colIndex]}")
                    } else {
                        writer.append("${variables[constraints.lhs[j].colIndex]} ${variables[constraints.lhs[j].quadraticColIndex!!]}")
                    }
                }
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
        return Ok(success)
    }
}

typealias QuadraticTetradModelView = ModelView<QuadraticConstraintCell, QuadraticObjectiveCell>

data class QuadraticTetradModel(
    private val impl: BasicQuadraticTetradModel,
    override val objective: QuadraticObjective,
) : QuadraticTetradModelView, Cloneable, Copyable<QuadraticTetradModel> {
    override fun copy() = QuadraticTetradModel(impl.copy(), objective.copy())
    override fun clone() = copy()

    override val variables: List<Variable> by impl::variables
    override val constraints: QuadraticConstraint by impl::constraints
    override val name: String by impl::name

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
            if (coefficient neq Flt64.one) {
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
                Ok(success)
            }
        }
    }
}
