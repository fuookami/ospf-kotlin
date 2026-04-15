@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64

sealed class SubObject(
    val category: ObjectCategory,
    val name: String = ""
) {
    abstract val cells: List<Cell>
    abstract val constant: Flt64

    fun evaluate(): Flt64? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    fun evaluate(results: Solution): Flt64? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }
}

class LinearSubObject(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell>,
    override val constant: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject(category, name) {
    companion object {
        /**
         * Create LinearSubObject from Polynomial (legacy API).
         * Prefer the FlattenData-based constructor for new code.
         */
        @Deprecated(
            message = "Use LinearSubObject(category, flattenData, tokens, name) instead. Will be removed in M9.",
            level = DeprecationLevel.WARNING,
            replaceWith = ReplaceWith(
                "LinearSubObject(category, poly.flattenedMonomials, tokens, name)",
                "fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData"
            )
        )
        @Suppress("DEPRECATION")
        operator fun invoke(
            category: ObjectCategory,
            poly: Polynomial<*, *, LinearMonomialCell>,
            tokens: AbstractTokenTable,
            name: String
        ): LinearSubObject {
            val cells = ArrayList<LinearCell>()
            val flattenData = poly.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(LinearCell(tokens, monomial.coefficient, token))
                }
            }
            return LinearSubObject(
                category = category,
                cells = cells,
                constant = flattenData.constant,
                name = name
            )
        }

        /**
         * Create LinearSubObject from LinearFlattenData (new API)
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenData,
            tokens: AbstractTokenTable,
            name: String = ""
        ): LinearSubObject {
            val cells = ArrayList<LinearCell>()
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(LinearCell(tokens, monomial.coefficient, token))
                }
            }
            return LinearSubObject(
                category = category,
                cells = cells,
                constant = flattenData.constant,
                name = name
            )
        }
    }
}

class QuadraticSubObject(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCell>,
    override val constant: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject(category, name) {
    companion object {
        /**
         * Create QuadraticSubObject from Polynomial (legacy API).
         * Prefer the FlattenData-based constructor for new code.
         */
        @Deprecated(
            message = "Use QuadraticSubObject(category, flattenData, tokens, name) instead. Will be removed in M9.",
            level = DeprecationLevel.WARNING,
            replaceWith = ReplaceWith(
                "QuadraticSubObject(category, poly.flattenedMonomials, tokens, name)",
                "fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData"
            )
        )
        @Suppress("DEPRECATION")
        operator fun invoke(
            category: ObjectCategory,
            poly: Polynomial<*, *, QuadraticMonomialCell>,
            tokens: AbstractTokenTable,
            name: String
        ): QuadraticSubObject {
            val cells = ArrayList<QuadraticCell>()
            val flattenData = poly.flattenedMonomials
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(QuadraticCell(tokens, monomial.coefficient, token1, token2))
                }
            }
            return QuadraticSubObject(
                category = category,
                cells = cells,
                constant = flattenData.constant,
                name = name
            )
        }

        /**
         * Create QuadraticSubObject from QuadraticFlattenData (new API)
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenData,
            tokens: AbstractTokenTable,
            name: String = ""
        ): QuadraticSubObject {
            val cells = ArrayList<QuadraticCell>()
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(QuadraticCell(tokens, monomial.coefficient, token1, token2))
                }
            }
            return QuadraticSubObject(
                category = category,
                cells = cells,
                constant = flattenData.constant,
                name = name
            )
        }
    }
}



