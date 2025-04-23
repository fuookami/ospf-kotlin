package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*

sealed class Constraint(
    val lhs: List<Cell>,
    val sign: Sign,
    val rhs: Flt64,
    val name: String = ""
) {
    fun isTrue(): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluate() ?: return null
        }
        return sign(lhsValue, rhs)
    }

    fun isTrue(results: Solution): Boolean {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluate(results)
        }
        return sign(lhsValue, rhs)
    }
}

class LinearConstraint(
    lhs: List<LinearCell>,
    sign: Sign,
    rhs: Flt64,
    name: String = ""
) : Constraint(lhs, sign, rhs, name) {
    companion object {
        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable
        ): LinearConstraint {
            val lhs = ArrayList<LinearCell>()
            var rhs = Flt64.zero
            for (cell in inequality.cells) {
                when (val temp = cell.cell) {
                    is Either.Left -> {
                        val token = tokens.find(temp.value.variable)
                        if (token != null && temp.value.coefficient neq Flt64.zero) {
                            lhs.add(LinearCell(tokens, temp.value.coefficient, token))
                        }
                    }

                    is Either.Right -> {
                        rhs += temp.value
                    }
                }
            }
            return LinearConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name)
        }
    }
}

class QuadraticConstraint(
    lhs: List<QuadraticCell>,
    sign: Sign,
    rhs: Flt64,
    name: String = ""
) : Constraint(lhs, sign, rhs, name) {
    companion object {
        @JvmName("constructByLinearInequality")
        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            var rhs = Flt64.zero
            for (cell in inequality.cells) {
                when (val temp = cell.cell) {
                    is Either.Left -> {
                        val token = tokens.find(temp.value.variable)
                        if (token != null && temp.value.coefficient neq Flt64.zero) {
                            lhs.add(QuadraticCell(tokens, temp.value.coefficient, token))
                        }
                    }

                    is Either.Right -> {
                        rhs += temp.value
                    }
                }
            }
            return QuadraticConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name)
        }

        @JvmName("constructByQuadraticInequality")
        operator fun invoke(
            inequality: Inequality<*, QuadraticMonomialCell>,
            tokens: AbstractTokenTable
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            var rhs = Flt64.zero
            for (cell in inequality.cells) {
                when (val temp = cell.cell) {
                    is Either.Left -> {
                        val token1 = tokens.find(temp.value.variable1)
                        val token2 = if (temp.value.variable2 != null) {
                            tokens.find(temp.value.variable2!!) ?: continue
                        } else {
                            null
                        }
                        if (token1 != null && temp.value.coefficient neq Flt64.zero) {
                            lhs.add(QuadraticCell(tokens, temp.value.coefficient, token1, token2))
                        }
                    }

                    is Either.Right -> {
                        rhs += temp.value
                    }
                }
            }
            return QuadraticConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name)
        }
    }
}
