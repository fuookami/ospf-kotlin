package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.utils.math.*

sealed class SubObject<C : Category>(
    val category: ObjectCategory,
    val name: String = ""
) {
    abstract val cells: List<Cell<C>>
    abstract val constant: Flt64

    fun value(): Flt64? {
        var ret = constant
        for (cell in cells) {
            ret += cell.value() ?: return null
        }
        return ret
    }
    fun value(results: List<Flt64>): Flt64 {
        var ret = constant
        for (cell in cells) {
            ret += cell.value(results)
        }
        return ret
    }
}

class LinearSubObject(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell>,
    override val constant: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<Linear>(category, name) {
    companion object {
        operator fun invoke(
            parent: LinearMetaModel,
            category: ObjectCategory,
            poly: Polynomial<Linear>,
            tokens: TokenTable<Linear>,
            name: String
        ): LinearSubObject {
            val cells = ArrayList<LinearCell>()
            var constant = Flt64.zero
            for (cell in poly.cells) {
                if ((cell as LinearMonomialCell).isPair()) {
                    val pair = cell.pair()!!
                    val token = tokens.token(pair.variable)
                    if (token != null && pair.coefficient neq Flt64.zero) {
                        cells.add(LinearCell(parent, pair.coefficient, token))
                    }
                } else {
                    constant = cell.constant()!!
                }
            }
            return LinearSubObject(category, cells, constant, name)
        }
    }
}
