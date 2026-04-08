package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.Inequality
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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
            .groupBy { it.key.from!!.first }
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
            .groupBy { it.key.from!!.first }
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
    open val from: Pair<IntermediateSymbol, Boolean>? = null
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
    from: Pair<IntermediateSymbol, Boolean>? = null,
) : Constraint(
    lhs = lhs,
    sign = sign,
    rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) {
    companion object {
        operator fun <Ineq : Inequality<*, LinearMonomialCell>> invoke(
            inequality: MetaConstraint<Ineq>,
            tokens: AbstractTokenTable
        ): LinearConstraint {
            val lhs = ArrayList<LinearCell>()
            val flattenData = inequality.constraint.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        LinearCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token = token
                        )
                    )
                }
            }
            return LinearConstraint(
                lhs = lhs,
                sign = Sign(inequality.constraint.sign),
                rhs = -flattenData.constant,
                lazy = inequality.lazy,
                name = inequality.constraint.name,
                origin = inequality
            )
        }

        operator fun invoke(
            inequality: Inequality<*, LinearMonomialCell>,
            tokens: AbstractTokenTable,
            from: Pair<IntermediateSymbol, Boolean>? = null,
        ): LinearConstraint {
            val lhs = ArrayList<LinearCell>()
            val flattenData = inequality.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        LinearCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token = token
                        )
                    )
                }
            }
            return LinearConstraint(
                lhs = lhs,
                sign = Sign(inequality.sign),
                rhs = -flattenData.constant,
                name = inequality.name,
                origin = null,
                from = from
            )
        }

        /**
         * Create LinearConstraint from LinearRelation (new API)
         */
        operator fun invoke(
            relation: LinearRelation,
            tokens: AbstractTokenTable,
            lazy: Boolean = false,
            name: String = "",
            origin: MetaConstraint<*>? = null,
            from: Pair<IntermediateSymbol, Boolean>? = null,
        ): LinearConstraint {
            val lhs = ArrayList<LinearCell>()
            val flattenData = relation.flattenData
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        LinearCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token = token
                        )
                    )
                }
            }
            return LinearConstraint(
                lhs = lhs,
                sign = Sign(relation.sign),
                rhs = -flattenData.constant,
                lazy = lazy,
                name = name.ifEmpty { relation.name },
                origin = origin,
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
    from: Pair<IntermediateSymbol, Boolean>? = null
) : Constraint(
    lhs = lhs,
    sign = sign,
    rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) {
    companion object {
        @JvmName("constructByLinearInequality")
        operator fun <Ineq : Inequality<*, LinearMonomialCell>> invoke(
            inequality: MetaConstraint<Ineq>,
            tokens: AbstractTokenTable
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            val flattenData = inequality.constraint.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        QuadraticCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token1 = token
                        )
                    )
                }
            }
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.constraint.sign),
                rhs = -flattenData.constant,
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
            from: Pair<IntermediateSymbol, Boolean>? = null
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            val flattenData = inequality.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        QuadraticCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token1 = token
                        )
                    )
                }
            }
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.sign),
                rhs = -flattenData.constant,
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
            val flattenData = inequality.constraint.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        QuadraticCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token1 = token1,
                            token2 = token2
                        )
                    )
                }
            }
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.constraint.sign),
                rhs = -flattenData.constant,
                name = inequality.constraint.name,
                origin = inequality,
                from = null
            )
        }

        @JvmName("constructByQuadraticInequality")
        operator fun invoke(
            inequality: Inequality<*, QuadraticMonomialCell>,
            tokens: AbstractTokenTable,
            from: Pair<IntermediateSymbol, Boolean>? = null
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            val flattenData = inequality.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        QuadraticCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token1 = token1,
                            token2 = token2
                        )
                    )
                }
            }
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(inequality.sign),
                rhs = -flattenData.constant,
                name = inequality.name,
                origin = null,
                from = from
            )
        }

        /**
         * Create QuadraticConstraint from QuadraticRelation (new API)
         */
        operator fun invoke(
            relation: QuadraticRelation,
            tokens: AbstractTokenTable,
            lazy: Boolean = false,
            name: String = "",
            origin: MetaConstraint<*>? = null,
            from: Pair<IntermediateSymbol, Boolean>? = null,
        ): QuadraticConstraint {
            val lhs = ArrayList<QuadraticCell>()
            val flattenData = relation.flattenData
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    lhs.add(
                        QuadraticCell(
                            tokenTable = tokens,
                            coefficient = monomial.coefficient,
                            token1 = token1,
                            token2 = token2
                        )
                    )
                }
            }
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(relation.sign),
                rhs = -flattenData.constant,
                lazy = lazy,
                name = name.ifEmpty { relation.name },
                origin = origin,
                from = from
            )
        }
    }
}



