package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.*

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

    fun evaluate(results: Solution): Flt64 {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results)
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
        operator fun invoke(
            category: ObjectCategory,
            poly: Polynomial<*, *, LinearMonomialCell>,
            tokens: AbstractTokenTable,
            name: String
        ): LinearSubObject {
            val cells = ArrayList<LinearCell>()
            var constant = Flt64.zero
            for (cell in poly.cells) {
                if (cell.isPair) {
                    val pair = cell.pair!!
                    val token = tokens.find(pair.variable)
                    if (token != null && pair.coefficient neq Flt64.zero) {
                        cells.add(LinearCell(tokens, pair.coefficient, token))
                    }
                } else {
                    constant = cell.constant!!
                }
            }
            return LinearSubObject(category, cells, constant, name)
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
        operator fun invoke(
            category: ObjectCategory,
            poly: Polynomial<*, *, QuadraticMonomialCell>,
            tokens: AbstractTokenTable,
            name: String
        ): QuadraticSubObject {
            val cells = ArrayList<QuadraticCell>()
            var constant = Flt64.zero
            for (cell in poly.cells) {
                if (cell.isTriple) {
                    val pair = cell.triple!!
                    val token1 = tokens.find(pair.variable1)
                    val token2 = if (pair.variable2 != null) {
                        tokens.find(pair.variable2) ?: continue
                    } else {
                        null
                    }
                    if (token1 != null && pair.coefficient neq Flt64.zero) {
                        cells.add(QuadraticCell(tokens, pair.coefficient, token1, token2))
                    }
                } else {
                    constant = cell.constant!!
                }
            }
            return QuadraticSubObject(category, cells, constant, name)
        }
    }
}
