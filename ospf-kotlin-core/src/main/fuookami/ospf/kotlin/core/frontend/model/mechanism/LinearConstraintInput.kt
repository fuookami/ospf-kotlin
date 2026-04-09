package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign as InequalitySign
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal

/**
 * LinearConstraintInput - Abstraction for linear constraint data used by function symbols.
 *
 * This type replaces direct dependency on `LinearInequality` in function symbol files.
 * It carries:
 * - `LinearFlattenData` for the constraint expression (monomials + constant)
 * - `Sign` for the relation type
 * - Range metadata (`lhsRange`) needed by the Big-M register formulation
 * - `name` / `displayName` for identification
 *
 * Construction paths:
 * 1. From `LinearInequality`: `LinearConstraintInput.from(inequality)`
 * 2. From `LinearRelation`: `LinearConstraintInput.from(relation, lhsRange)`
 * 3. Direct: `LinearConstraintInput(flattenData, sign, lhsRange, name, displayName)`
 */
data class LinearConstraintInput(
    val flattenData: LinearFlattenData,
    val sign: Comparison,
    val lhsRange: ValueRange<Flt64>,
    val name: String = "",
    val displayName: String? = null,
    /**
     * The original RHS constant value before normalization (lhs - rhs <= 0 form).
     * Needed by function symbols for range-based pre-computation (e.g., IfFunction.possibleRange).
     */
    val rhsConstant: Flt64 = Flt64.zero
) {
    // Cached bounds for Big-M formulation
    val lowerBound: Flt64? get() = lhsRange.lowerBound?.value?.unwrap()
    val upperBound: Flt64? get() = lhsRange.upperBound?.value?.unwrap()

    companion object {
        @Deprecated(
            message = "Use LinearConstraintInput.from(relation, lhsRange) instead",
            replaceWith = ReplaceWith("LinearConstraintInput.from(relation, lhsRange)", "fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality")
        )
        fun from(inequality: LinearInequality): LinearConstraintInput {
            val normalized = inequality.normalize()
            return LinearConstraintInput(
                flattenData = normalized.flattenedMonomials,
                sign = normalized.sign.toComparison(),
                lhsRange = normalized.lhs.range.valueRange!!,
                name = normalized.name,
                displayName = normalized.displayName,
                rhsConstant = normalized.rhs.constant
            )
        }

        /**
         * Create LinearConstraintInput from math LinearInequality
         */
        fun from(
            relation: MathLinearInequality,
            lhsRange: ValueRange<Flt64>,
            rhsConstant: Flt64 = Flt64.zero
        ): LinearConstraintInput {
            val flattenData = relation.flattenData
            return LinearConstraintInput(
                flattenData = flattenData,
                sign = relation.comparison,
                lhsRange = lhsRange,
                name = relation.name,
                displayName = relation.displayName,
                rhsConstant = rhsConstant
            )
        }
    }

    /**
     * Evaluate whether this constraint is satisfied given token values.
     * Replaces `LinearInequality.isTrue()` for function symbol runtime evaluation.
     */
    fun isTrue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = evaluateFlattenData(flattenData, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun isTrue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithValues(flattenData, values, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun isTrue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithResults(flattenData, results, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun isTrue(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = evaluateFlattenDataFromTokenList(flattenData, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun isTrue(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithResultsFromTokenList(flattenData, results, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * Evaluate with symbol values, falling back to token list for unresolved variables.
     */
    fun isTrue(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithValuesAndTokenList(flattenData, values, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }
}

/**
 * Evaluate LinearFlattenData against a token table.
 */
private fun evaluateFlattenData(
    data: LinearFlattenData,
    tokenTable: AbstractTokenTable,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokenTable.find(symbol) ?: continue
        val value = token.result ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

private fun evaluateFlattenDataWithValues(
    data: LinearFlattenData,
    values: Map<Symbol, Flt64>,
    tokenTable: AbstractTokenTable?,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol
        val value = values[symbol]
            ?: (symbol as? AbstractVariableItem<*, *>)?.let { tokenTable?.find(it)?.result }
            ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

/**
 * Evaluate LinearFlattenData with values map and AbstractTokenList fallback.
 */
private fun evaluateFlattenDataWithValuesAndTokenList(
    data: LinearFlattenData,
    values: Map<Symbol, Flt64>,
    tokenList: AbstractTokenList?,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol
        val value = values[symbol]
            ?: (symbol as? AbstractVariableItem<*, *>)?.let { tokenList?.find(it)?.result }
            ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

private fun evaluateFlattenDataWithResults(
    data: LinearFlattenData,
    results: List<Flt64>,
    tokenTable: AbstractTokenTable,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokenTable.find(symbol) ?: continue
        val idx = tokenTable.indexOf(token) ?: if (zeroIfNone) 0 else return null
        val value = results.getOrElse(idx) { if (zeroIfNone) Flt64.zero else return null }
        result = result + monomial.coefficient * value
    }
    return result
}

private fun evaluateFlattenDataFromTokenList(
    data: LinearFlattenData,
    tokenList: AbstractTokenList,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokenList.find(symbol) ?: continue
        val value = token.result ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

private fun evaluateFlattenDataWithResultsFromTokenList(
    data: LinearFlattenData,
    results: List<Flt64>,
    tokenList: AbstractTokenList,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokenList.find(symbol) ?: continue
        val idx = tokenList.indexOf(token) ?: if (zeroIfNone) 0 else return null
        val value = results.getOrElse(idx) { if (zeroIfNone) Flt64.zero else return null }
        result = result + monomial.coefficient * value
    }
    return result
}

/**
 * Comparison helper - returns whether `value` satisfies the relation against `rhs`.
 */
private fun Comparison.compare(value: Flt64, rhs: Flt64): Boolean = when (this) {
    Comparison.LT -> value ls rhs
    Comparison.LE -> value leq rhs
    Comparison.GT -> value gr rhs
    Comparison.GE -> value geq rhs
    Comparison.EQ -> value eq rhs
    Comparison.NE -> value neq rhs
}

/**
 * Convert frontend.inequality.Sign to math.symbol.inequality.Comparison.
 */
private fun InequalitySign.toComparison(): Comparison = when (this) {
    InequalitySign.Less -> Comparison.LT
    InequalitySign.LessEqual -> Comparison.LE
    InequalitySign.Greater -> Comparison.GT
    InequalitySign.GreaterEqual -> Comparison.GE
    InequalitySign.Equal -> Comparison.EQ
    InequalitySign.Unequal -> Comparison.NE
}
