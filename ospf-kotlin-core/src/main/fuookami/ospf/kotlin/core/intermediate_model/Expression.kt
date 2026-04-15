package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.toFlt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality

/**
 * Expression interface - provides range and evaluation capabilities.
 *
 * Moved here from `core.expression` to break the expression directory dependency chain.
 * The expression package version re-exports this for backward compatibility.
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

    fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(
            tokenList = tokenTable.tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(
            results = results,
            tokenList = tokenTable.tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList? = null, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable? = null, zeroIfNone: Boolean = false): Flt64? {
        return evaluate(
            values = values,
            tokenList = tokenTable?.tokenList,
            zeroIfNone = zeroIfNone
        )
    }
}
