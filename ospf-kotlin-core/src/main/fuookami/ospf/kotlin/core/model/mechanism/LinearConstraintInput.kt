/**
 * 线性约束输入
 * Linear constraint input
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

data class LinearConstraintInput<V>(
    val flattenData: LinearFlattenData<V>,
    val sign: Comparison,
    val lhsRange: ValueRange<V>,
    val name: String = "",
    val displayName: String? = null,
    val rhsConstant: V
) where V : RealNumber<V>, V : NumberField<V> {
    fun isTrue(
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithResults(flattenData, results, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue.toFlt64(), Flt64.zero)
    }

    companion object {
        fun <V> from(
            relation: LinearInequality<V>,
            lhsRange: ValueRange<V>,
            rhsConstant: V,
            name: String = "",
            displayName: String? = null
        ): LinearConstraintInput<V> where V : RealNumber<V>, V : NumberField<V> {
            return LinearConstraintInput(
                flattenData = relation.toLinearFlattenData().getOrThrow(),
                sign = relation.comparison,
                lhsRange = lhsRange,
                name = name,
                displayName = displayName,
                rhsConstant = rhsConstant
            )
        }

        fun <V> from(
            relation: LinearInequality<V>,
            converter: IntoValue<V>,
            lhsRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            rhsConstant: V,
            name: String = "",
            displayName: String? = null
        ): LinearConstraintInput<V> where V : RealNumber<V>, V : NumberField<V> {
            return from(
                relation = relation,
                lhsRange = lhsRange.toValueRange(converter),
                rhsConstant = rhsConstant,
                name = name,
                displayName = displayName
            )
        }
    }
}

fun <V> Flt64LinearConstraintInput.toLinearConstraintInput(
    converter: IntoValue<V>
): LinearConstraintInput<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearConstraintInput(
        flattenData = LinearFlattenData(
            monomials = flattenData.monomials.map {
                LinearMonomial(converter.intoValue(it.coefficient), it.symbol)
            },
            constant = converter.intoValue(flattenData.constant)
        ),
        sign = sign,
        lhsRange = lhsRange.toValueRange(converter),
        name = name,
        displayName = displayName,
        rhsConstant = converter.intoValue(rhsConstant)
    )
}

private fun <V> ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toValueRange(
    converter: IntoValue<V>
): ValueRange<V> where V : RealNumber<V>, V : NumberField<V> {
    val constants = converter.zero.constants
    fun convertBound(bound: Bound<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Bound<V> {
        val value = when {
            bound.value.isInfinity -> ValueWrapper.Infinity(constants)
            bound.value.isNegativeInfinity -> ValueWrapper.NegativeInfinity(constants)
            else -> ValueWrapper(converter.intoValue(bound.value.unwrap()), constants).value!!
        }
        return Bound(value, bound.interval)
    }
    return ValueRange(
        lowerBound = convertBound(lowerBound),
        upperBound = convertBound(upperBound),
        constants = constants
    )
}

/**
 * Flt64LinearConstraintInput - Flt64 boundary data used by legacy callback paths.
 * Flt64LinearConstraintInput - 旧回调路径使用的 Flt64 边界数据。
 *
 * It carries:
 * - `LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>` for the constraint expression (monomials + constant)
 * - `Comparison` for the relation type
 * - Range metadata (`lhsRange`) needed by the Big-M register formulation
 * - `name` / `displayName` for identification
 *
 * Construction:
 * - From `LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>`: `Flt64LinearConstraintInput.from(relation, lhsRange)`
 * - Direct: `Flt64LinearConstraintInput(flattenData, sign, lhsRange, name, displayName)`
 */
data class Flt64LinearConstraintInput(
    val flattenData: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    val sign: Comparison,
    val lhsRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
         * Create Flt64LinearConstraintInput from math LinearInequality<V> using an explicit converter.
         * 使用显式 converter 从数学层 LinearInequality<V> 创建 Flt64LinearConstraintInput。
         */
        fun <V> from(
            relation: LinearInequality<V>,
            converter: IntoValue<V>,
            lhsRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            rhsConstant: Flt64 = Flt64.zero,
            name: String = "",
            displayName: String? = null
        ): Flt64LinearConstraintInput where V : RealNumber<V>, V : NumberField<V> {
            val flattenData = relation.toLinearFlattenDataFlt64(converter).getOrThrow()
            return Flt64LinearConstraintInput(
                flattenData = flattenData,
                sign = relation.comparison,
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
        results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlt64FlattenDataWithResults(flattenData, results, tokenTable, zeroIfNone = zeroIfNone)
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

    internal fun isTrue(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = evaluateFlattenDataFromTokenList(flattenData, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    fun isTrue(
        results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
        tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithValuesAndTokenList(flattenData, values, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }
}

/**
 * Evaluate LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> against a token table.
 */
internal fun <V> evaluateFlattenData(
    data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
    data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
 * Evaluate LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> with values map and AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64> fallback.
 */
private fun evaluateFlattenDataWithValuesAndTokenList(
    data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    values: Map<Symbol, Flt64>,
    tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?,
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

internal fun <V> evaluateFlt64FlattenDataWithResults(
    data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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

internal fun <V> evaluateFlattenDataWithResults(
    data: LinearFlattenData<V>,
    results: List<V>,
    tokenTable: AbstractTokenTable<V>,
    zeroIfNone: Boolean
): V? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    val zero = data.constant - data.constant
    for (monomial in data.monomials) {
        val symbol = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val idx = tokenTable.indexOf(symbol) ?: if (zeroIfNone) 0 else return null
        val value = results.getOrElse(idx) { if (zeroIfNone) zero else return null }
        result = result + monomial.coefficient * value
    }
    return result
}

/**
 * Evaluate QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> with values from `results` and symbol index from token table.
 * 使用 `results` 中的值并结合 token table 的索引来评估 QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>。
 */
internal fun <V> evaluateFlt64QuadraticFlattenDataWithResults(
    data: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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

internal fun <V> evaluateQuadraticFlattenDataWithResults(
    data: QuadraticFlattenData<V>,
    results: List<V>,
    tokenTable: AbstractTokenTable<V>,
    zeroIfNone: Boolean
): V? where V : RealNumber<V>, V : NumberField<V> {
    var result = data.constant
    val zero = data.constant - data.constant
    for (monomial in data.monomials) {
        val symbol1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val index1 = tokenTable.indexOf(symbol1) ?: if (zeroIfNone) 0 else return null
        val value1 = results.getOrElse(index1) { if (zeroIfNone) zero else return null }
        val value2 = if (monomial.symbol2 != null) {
            val symbol2 = monomial.symbol2 as? AbstractVariableItem<*, *> ?: continue
            val index2 = tokenTable.indexOf(symbol2) ?: if (zeroIfNone) 0 else return null
            results.getOrElse(index2) { if (zeroIfNone) zero else return null }
        } else {
            value1
        }
        result = result + monomial.coefficient * value1 * value2
    }
    return result
}

private fun evaluateFlattenDataFromTokenList(
    data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
    data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
    data: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
