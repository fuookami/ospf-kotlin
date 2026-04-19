package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.toFlt64
import fuookami.ospf.kotlin.math.symbol.Symbol

/**
 * Expression interface - provides range and evaluation capabilities.
 *
 * Re-created after type migration: uses AbstractTokenListF64 and LegacyAbstractTokenTable.
 */
interface Expression {
    /** for lp **/
    var name: String

    /** for opm */
    var displayName: String?

    val discrete: Boolean get() = false

    val range: ExpressionRange<Flt64>
    val lowerBound get() = range.lowerBound?.toFlt64()
    val upperBound get() = range.upperBound?.toFlt64()
    val fixedValue get() = range.fixedValue

    fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(tokenTable: LegacyAbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(
            tokenList = tokenTable.tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(results: List<Flt64>, tokenTable: LegacyAbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(
            results = results,
            tokenList = tokenTable.tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64? = null, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(values: Map<Symbol, Flt64>, tokenTable: LegacyAbstractTokenTable? = null, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(
            values = values,
            tokenList = tokenTable?.tokenList,
            zeroIfNone = zeroIfNone
        )
    }
}
