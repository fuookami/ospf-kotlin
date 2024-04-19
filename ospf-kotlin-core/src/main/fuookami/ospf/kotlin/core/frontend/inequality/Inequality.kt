package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class Inequality<Self : Inequality<Self, Cell, C>, Cell : MonomialCell<Cell, C>, C : Category>(
    open val lhs: Polynomial<*, *, Cell, C>,
    open val rhs: Polynomial<*, *, Cell, C>,
    val sign: Sign,
    var name: String = "",
    var displayName: String? = null
) {
    protected var _cells: List<Cell> = emptyList()
    open val cells: List<Cell>
        get() {
            if (_cells.isEmpty()) {
                val notConstantCells =
                    lhs.cells.filter { !it.isConstant } + rhs.cells.filter { !it.isConstant }.map { -it }
                val constant =
                    lhs.cells.mapNotNull { it.constant }.sum() + rhs.cells.mapNotNull { it.constant }.sum()
                _cells = notConstantCells + listOf(MonomialCell.invoke(constant, lhs.category))
            }
            return _cells
        }

    fun flush(force: Boolean = false) {
        lhs.flush(force)
        rhs.flush(force)
        if (force || !lhs.cached || !rhs.cached) {
            _cells = emptyList()
        }
    }

    fun isTrue(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = lhs.value(tokenList, zeroIfNone)
            ?: return null
        val rhsValue = rhs.value(tokenList, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(tokenTable: TokenTable<Cell, C>, zeroIfNone: Boolean = false): Boolean? {
        return isTrue(tokenTable.tokenList, zeroIfNone)
    }

    fun isTrue(result: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = lhs.value(result, tokenList, zeroIfNone)
            ?: return null
        val rhsValue = rhs.value(result, tokenList, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(result: List<Flt64>, tokenTable: TokenTable<Cell, C>, zeroIfNone: Boolean = false): Boolean? {
        return isTrue(result, tokenTable.tokenList, zeroIfNone)
    }

    abstract fun reverse(name: String? = null, displayName: String? = null): Self
    abstract fun normalize(): Self

    override fun toString(): String {
        return displayName ?: name
    }

    fun toRawString(unfold: Boolean = false): String {
        return "${lhs.toRawString(unfold)} $sign ${rhs.toRawString(unfold)}"
    }
}
