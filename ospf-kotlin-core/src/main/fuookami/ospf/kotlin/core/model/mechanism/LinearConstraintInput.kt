package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.AbstractTokenListFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal

/**
 * LinearConstraintInput - Abstraction for linear constraint data used by function symbols.
 *
 * It carries:
 * - `LinearFlattenDataFlt64` for the constraint expression (monomials + constant)
 * - `Comparison` for the relation type
 * - Range metadata (`lhsRange`) needed by the Big-M register formulation
 * - `name` / `displayName` for identification
 *
 * Construction:
 * - From `Flt64LinearInequality`: `LinearConstraintInput.from(relation, lhsRange)`
 * - Direct: `LinearConstraintInput(flattenData, sign, lhsRange, name, displayName)`
 */
data class LinearConstraintInput(
    val flattenData: LinearFlattenDataFlt64,
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
        /**
         * Create LinearConstraintInput from math LinearInequality<V>.
         * Adapter boundary: casts to Flt64LinearInequality for flattenData access; safe when V=Flt64.
         */
        fun <V> from(
            relation: LinearInequality<V>,
            lhsRange: ValueRange<Flt64>,
            rhsConstant: Flt64 = Flt64.zero,
            name: String = "",
            displayName: String? = null
        ): LinearConstraintInput where V : RealNumber<V>, V : NumberField<V> {
            @Suppress("UNCHECKED_CAST")
            val flt64Relation = relation as Flt64LinearInequality
            val flattenData = flt64Relation.flattenData
            return LinearConstraintInput(
                flattenData = flattenData,
                sign = flt64Relation.comparison,
                lhsRange = lhsRange,
                name = name,
                displayName = displayName,
                rhsConstant = rhsConstant
            )
        }
    }

    /**
     * Evaluate whether this constraint is satisfied given token values.
     * Replaces `LinearInequality.isTrue()` for function symbol runtime evaluation.
     */
    fun <V> isTrue(tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): Boolean?
            where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlattenData(flattenData, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun <V> isTrue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlattenDataWithValues(flattenData, values, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun <V> isTrue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlattenDataWithResults(flattenData, results, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * Evaluate constraint with V-typed solution values.
     * Converts V to Flt64 via converter, then delegates to Flt64 evaluation.
     */
    fun <V> isTrue(
        results: List<V>,
        converter: IntoValue<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val flt64Results = results.map { converter.fromValue(it) }
        return isTrue(flt64Results, tokenTable, zeroIfNone)
    }

    fun isTrue(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = evaluateFlattenDataFromTokenList(flattenData, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun isTrue(
        results: List<Flt64>,
        tokenList: AbstractTokenListFlt64,
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
        tokenList: AbstractTokenListFlt64?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithValuesAndTokenList(flattenData, values, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }
}

/**
 * Evaluate LinearFlattenDataFlt64 against a token table.
 */
internal fun <V> evaluateFlattenData(
    data: LinearFlattenDataFlt64,
    tokenTable: AbstractTokenTable<V>,
    zeroIfNone: Boolean
): Flt64? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokenTable.find(symbol) ?: continue
        val value = token.resultFlt64 ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

private fun <V> evaluateFlattenDataWithValues(
    data: LinearFlattenDataFlt64,
    values: Map<Symbol, Flt64>,
    tokenTable: AbstractTokenTable<V>?,
    zeroIfNone: Boolean
): Flt64? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol
        val value = values[symbol]
            ?: (symbol as? AbstractVariableItem<*, *>)?.let { tokenTable?.find(it)?.resultFlt64 }
            ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

/**
 * Evaluate LinearFlattenDataFlt64 with values map and AbstractTokenListFlt64 fallback.
 */
private fun evaluateFlattenDataWithValuesAndTokenList(
    data: LinearFlattenDataFlt64,
    values: Map<Symbol, Flt64>,
    tokenList: AbstractTokenListFlt64?,
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

internal fun <V> evaluateFlattenDataWithResults(
    data: LinearFlattenDataFlt64,
    results: List<Flt64>,
    tokenTable: AbstractTokenTable<V>,
    zeroIfNone: Boolean
): Flt64? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val idx = tokenTable.indexOf(symbol) ?: if (zeroIfNone) 0 else return null
        val value = results.getOrElse(idx) { if (zeroIfNone) Flt64.zero else return null }
        result = result + monomial.coefficient * value
    }
    return result
}

/**
 * Evaluate QuadraticFlattenDataFlt64 with values from `results` and symbol index from token table.
 * 使用 `results` 中的值并结合 token table 的索引来评估 QuadraticFlattenDataFlt64。
 */
internal fun <V> evaluateQuadraticFlattenDataWithResults(
    data: QuadraticFlattenDataFlt64,
    results: Solution<Flt64>,
    tokenTable: AbstractTokenTable<V>,
    zeroIfNone: Boolean
): Flt64? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val index1 = tokenTable.indexOf(symbol1) ?: if (zeroIfNone) 0 else return null
        val value1 = results.getOrElse(index1) { if (zeroIfNone) Flt64.zero else return null }
        val value2 = if (monomial.symbol2 != null) {
            val symbol2 = monomial.symbol2 as? AbstractVariableItem<*, *> ?: continue
            val index2 = tokenTable.indexOf(symbol2) ?: if (zeroIfNone) 0 else return null
            results.getOrElse(index2) { if (zeroIfNone) Flt64.zero else return null }
        } else {
            value1
        }
        result = result + monomial.coefficient * value1 * value2
    }
    return result
}

private fun evaluateFlattenDataFromTokenList(
    data: LinearFlattenDataFlt64,
    tokenList: AbstractTokenListFlt64,
    zeroIfNone: Boolean
): Flt64? {
    var result = data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokenList.find(symbol) ?: continue
        val value = token.resultFlt64 ?: if (zeroIfNone) Flt64.zero else return null
        result = result + monomial.coefficient * value
    }
    return result
}

private fun evaluateFlattenDataWithResultsFromTokenList(
    data: LinearFlattenDataFlt64,
    results: List<Flt64>,
    tokenList: AbstractTokenListFlt64,
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
internal fun Comparison.compare(value: Flt64, rhs: Flt64): Boolean = when (this) {
    Comparison.LT -> value ls rhs
    Comparison.LE -> value leq rhs
    Comparison.GT -> value gr rhs
    Comparison.GE -> value geq rhs
    Comparison.EQ -> value eq rhs
    Comparison.NE -> value neq rhs
}

/**
 * Evaluate quadratic flatten data given token table and solution values.
 */
internal fun <V> evaluateQuadraticFlattenData(
    data: QuadraticFlattenDataFlt64,
    tokenTable: AbstractTokenTable<V>,
    zeroIfNone: Boolean
): Flt64? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    for (monomial in data.monomials) {
        val sym1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val token1 = tokenTable.find(sym1) ?: continue
        val val1 = token1.resultFlt64 ?: if (zeroIfNone) Flt64.zero else return null
        val val2 = if (monomial.symbol2 != null) {
            val sym2 = monomial.symbol2 as? AbstractVariableItem<*, *> ?: continue
            val token2 = tokenTable.find(sym2) ?: continue
            token2.resultFlt64 ?: if (zeroIfNone) Flt64.zero else return null
        } else {
            val1
        }
        result = result + monomial.coefficient * val1 * val2
    }
    return result
}

