package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*

typealias DualSolution = Map<Constraint, Flt64>
typealias LinearDualSolution = Map<LinearConstraint, Flt64>
typealias QuadraticDualSolution = Map<QuadraticConstraint, Flt64>

data class MetaDualSolution(
    val constraints: Map<Inequality<*, *>, Flt64>,
    val symbols: Map<IntermediateSymbol, List<Pair<Constraint, Flt64>>>
)

@JvmName("linearDualSolutionToMetaDualSolution")
fun LinearDualSolution.toMeta(): MetaDualSolution {
    return MetaDualSolution(
        constraints = this
            .filterKeys { it.origin != null }
            .mapKeys { it.key.origin!! },
        symbols = this
            .filterKeys { it.from != null }
            .entries
            .groupBy { it.key.from!! }
            .mapValues { prices -> prices.value.map { it.toPair() } }
    )
}

@JvmName("quadraticDualSolutionToMetaDualSolution")
fun QuadraticDualSolution.toMeta(): MetaDualSolution {
    return MetaDualSolution(
        constraints = this
            .filterKeys { it.origin != null }
            .mapKeys { it.key.origin!! },
        symbols = this
            .filterKeys { it.from != null }
            .entries
            .groupBy { it.key.from!! }
            .mapValues { prices -> prices.value.map { it.toPair() } }
    )
}

sealed class Constraint(
    open val lhs: List<Cell>,
    val sign: Sign,
    val rhs: Flt64,
    val name: String = "",
    open val origin: Inequality<*, *>? = null,
    open val from: IntermediateSymbol? = null
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
    override val lhs: List<LinearCell>,
    sign: Sign,
    rhs: Flt64,
    name: String = "",
    origin: Inequality<*, *>? = null,
    from: IntermediateSymbol? = null,
) : Constraint(lhs, sign, rhs, name, origin, from) {
    companion object {
        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable,
            origin: Boolean = false,
            from: IntermediateSymbol? = null,
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
            return LinearConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name, if (origin) { inequality } else { null }, from)
        }
    }
}

class QuadraticConstraint(
    override val lhs: List<QuadraticCell>,
    sign: Sign,
    rhs: Flt64,
    name: String = "",
    origin: Inequality<*, *>? = null,
    from: IntermediateSymbol? = null
) : Constraint(lhs, sign, rhs, name, origin, from) {
    companion object {
        @JvmName("constructByLinearInequality")
        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable,
            origin: Boolean = false,
            from: IntermediateSymbol? = null
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
            return QuadraticConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name, if (origin) { inequality } else { null }, from)
        }

        @JvmName("constructByQuadraticInequality")
        operator fun invoke(
            inequality: Inequality<*, QuadraticMonomialCell>,
            tokens: AbstractTokenTable,
            origin: Boolean = false,
            from: IntermediateSymbol? = null
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
            return QuadraticConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name, if (origin) { inequality } else { null }, from)
        }
    }
}
