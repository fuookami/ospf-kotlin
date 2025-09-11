package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class Inequality<Self : Inequality<Self, Cell>, Cell : MonomialCell<Cell>>(
    open val lhs: Polynomial<*, *, Cell>,
    open val rhs: Polynomial<*, *, Cell>,
    val sign: Sign,
    var name: String = "",
    var displayName: String? = null
) {
    protected var _cells: List<Cell> = emptyList()
    open val cells: List<Cell>
        get() {
            if (_cells.isEmpty()) {
                val notConstantCells = lhs.cells.filter { !it.isConstant } + rhs.cells.filter { !it.isConstant }.map { -it }
                val constant = lhs.cells.mapNotNull { it.constant }.sum() + rhs.cells.mapNotNull { it.constant }.sum()
                _cells = notConstantCells + listOf(MonomialCell(constant, lhs.category))
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

    fun isTrue(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = lhs.evaluate(tokenList, zeroIfNone)
            ?: return null
        val rhsValue = rhs.evaluate(tokenList, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = lhs.evaluate(tokenTable, zeroIfNone)
            ?: return null
        val rhsValue = rhs.evaluate(tokenTable, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(
        result: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = lhs.evaluate(result, tokenList, zeroIfNone)
            ?: return null
        val rhsValue = rhs.evaluate(result, tokenList, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(
        result: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = lhs.evaluate(result, tokenTable, zeroIfNone)
            ?: return null
        val rhsValue = rhs.evaluate(result, tokenTable, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = lhs.evaluate(values, tokenList, zeroIfNone)
            ?: return null
        val rhsValue = rhs.evaluate(values, tokenList, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    fun isTrue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = lhs.evaluate(values, tokenTable, zeroIfNone)
            ?: return null
        val rhsValue = rhs.evaluate(values, tokenTable, zeroIfNone)
            ?: return null
        return sign(lhsValue, rhsValue)
    }

    abstract fun reverse(name: String? = null, displayName: String? = null): Self
    abstract fun normalize(): Self
    abstract fun normalizeToLessEqual(): Self

    override fun toString(): String {
        return displayName ?: name
    }

    fun toRawString(unfold: UInt64 = UInt64.zero): String {
        return "${lhs.toRawString(unfold)} $sign ${rhs.toRawString(unfold)}"
    }
}
