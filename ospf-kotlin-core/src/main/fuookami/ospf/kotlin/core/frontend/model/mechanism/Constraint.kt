package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*

typealias DualSolution = Map<Constraint, Flt64>
typealias LinearDualSolution = Map<LinearConstraint, Flt64>
typealias QuadraticDualSolution = Map<QuadraticConstraint, Flt64>

data class MetaDualSolution(
    val constraints: Map<MetaConstraint<*>, Flt64>,
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
    val lazy: Boolean,
    val name: String = "",
    open val origin: MetaConstraint<*>? = null,
    open val from: IntermediateSymbol? = null
) {
    fun isTrue(): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluate() ?: return null
        }
        return sign(lhsValue, rhs)
    }

    fun isTrue(results: Solution): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluate(results) ?: return null
        }
        return sign(lhsValue, rhs)
    }
}

class LinearConstraint(
    override val lhs: List<LinearCell>,
    sign: Sign,
    rhs: Flt64,
    lazy: Boolean = false,
    name: String = "",
    origin: MetaConstraint<*>? = null,
    from: IntermediateSymbol? = null,
) : Constraint(lhs, sign, rhs, lazy, name, origin, from) {
    companion object {
        operator fun <Ineq : Inequality<*, LinearMonomialCell>> invoke(
            inequality: MetaConstraint<Ineq>,
            tokens: AbstractTokenTable
        ): LinearConstraint {
            val lhs = ArrayList<LinearCell>()
            var rhs = Flt64.zero
            for (cell in inequality.constraint.cells) {
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
            return LinearConstraint(
                lhs = lhs,
                sign = Sign(inequality.constraint.sign),
                rhs = -rhs,
                lazy = inequality.lazy,
                name = inequality.constraint.name,
                origin = inequality
            )
        }

        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable,
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
            return LinearConstraint(
                lhs = lhs,
                sign = Sign(inequality.sign),
                rhs = -rhs,
                name = inequality.name,
                origin = null,
                from = from
            )
        }
    }
}

class QuadraticConstraint(
    override val lhs: List<QuadraticCell>,
    sign: Sign,
    rhs: Flt64,
    lazy: Boolean = false,
    name: String = "",
    origin: MetaConstraint<*>? = null,
    from: IntermediateSymbol? = null
) : Constraint(lhs, sign, rhs, lazy, name, origin, from) {
    companion object {
        @JvmName("constructByLinearInequality")
        operator fun <Ineq : Inequality<*, LinearMonomialCell>> invoke(
            inequality: MetaConstraint<Ineq>,
            tokens: AbstractTokenTable
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            var rhs = Flt64.zero
            for (cell in inequality.constraint.cells) {
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
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.constraint.sign),
                rhs = -rhs,
                lazy = inequality.lazy,
                name = inequality.constraint.name,
                origin = inequality,
                from = null
            )
        }

        @JvmName("constructByLinearInequality")
        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable,
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
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.sign),
                rhs = -rhs,
                name = inequality.name,
                origin = null,
                from = from
            )
        }

        @JvmName("constructByQuadraticInequality")
        operator fun <Ineq : Inequality<*, QuadraticMonomialCell>> invoke(
            inequality: MetaConstraint<Ineq>,
            tokens: AbstractTokenTable
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            var rhs = Flt64.zero
            for (cell in inequality.constraint.cells) {
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
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.constraint.sign),
                rhs = -rhs,
                name = inequality.constraint.name,
                origin = inequality,
                from = null
            )
        }

        @JvmName("constructByQuadraticInequality")
        operator fun invoke(
            inequality: Inequality<*, QuadraticMonomialCell>,
            tokens: AbstractTokenTable,
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
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.sign),
                rhs = -rhs,
                name = inequality.name,
                origin = null,
                from = from
            )
        }
    }
}
