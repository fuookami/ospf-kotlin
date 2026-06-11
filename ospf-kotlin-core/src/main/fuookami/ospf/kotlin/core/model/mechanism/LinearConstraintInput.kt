/**
 * 线性约束输入
 * Linear constraint input
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 线性约束输入
 * Linear constraint input
 *
 * 表示一个线性约束的输入数据，包含扁平化的表达式、比较符号和范围信息。
 * Represents the input data for a linear constraint, containing flattened expression, comparison sign, and range info.
 *
 * @param V 数值类型 / The number type
 * @property flattenData 扁平化的线性数据 / Flattened linear data
 * @property sign 比较符号 / Comparison sign
 * @property lhsRange 左侧值范围 / Left-hand side value range
 * @property name 约束名称 / Constraint name
 * @property displayName 显示名称 / Display name
 * @property rhsConstant 右侧常量 / Right-hand side constant
 */
data class LinearConstraintInput<V>(
    val flattenData: LinearFlattenData<V>,
    val sign: Comparison,
    val lhsRange: ValueRange<V>,
    val name: String = "",
    val displayName: String? = null,
    val rhsConstant: V
) where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 判断约束是否满足
     * Check if the constraint is satisfied
     *
     * @param results 求解结果列表 / Solution result list
     * @param tokenTable 符号表 / Token table
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
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
        /**
         * 从线性不等式创建约束输入
         * Create constraint input from linear inequality
         *
         * @param V 数值类型 / The number type
         * @param relation 线性不等式 / Linear inequality
         * @param lhsRange 左侧值范围 / Left-hand side value range
         * @param rhsConstant 右侧常量 / Right-hand side constant
         * @param name 约束名称 / Constraint name
         * @param displayName 显示名称 / Display name
         * @return 约束输入 / Constraint input
         */
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

        /**
         * 从线性不等式和转换器创建约束输入
         * Create constraint input from linear inequality with converter
         *
         * @param V 数值类型 / The number type
         * @param relation 线性不等式 / Linear inequality
         * @param converter 值转换器 / Value converter
         * @param lhsRange Flt64 左侧值范围 / Flt64 left-hand side value range
         * @param rhsConstant 右侧常量 / Right-hand side constant
         * @param name 约束名称 / Constraint name
         * @param displayName 显示名称 / Display name
         * @return 约束输入 / Constraint input
         */
        fun <V> from(
            relation: LinearInequality<V>,
            converter: IntoValue<V>,
            lhsRange: ValueRange<Flt64>,
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

/**
 * 将 Flt64 约束输入转换为泛型约束输入
 * Convert Flt64 constraint input to generic constraint input
 *
 * @param V 目标数值类型 / Target number type
 * @param converter 值转换器 / Value converter
 * @return 泛型约束输入 / Generic constraint input
 */
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

/** 将 Flt64 值范围转换为泛型值范围 / Convert an Flt64 value range to a generic value range */
private fun <V> ValueRange<Flt64>.toValueRange(
    converter: IntoValue<V>
): ValueRange<V> where V : RealNumber<V>, V : NumberField<V> {
    val constants = converter.zero.constants
    fun convertBound(bound: Bound<Flt64>): Bound<V> {
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
 * Flt64LinearConstraintInput - 旧回调路径使用的 Flt64 边界数据。
 * Flt64LinearConstraintInput - Flt64 boundary data used by legacy callback paths.
 *
 * 携带以下信息：
 * - `LinearFlattenData<Flt64>` 用于约束表达式（单项式 + 常量）
 * - `Comparison` 用于关系类型
 * - 范围元数据（`lhsRange`），Big-M 寄存器公式需要
 * - `name` / `displayName` 用于标识
 *
 * It carries:
 * - `LinearFlattenData<Flt64>` for the constraint expression (monomials + constant)
 * - `Comparison` for the relation type
 * - Range metadata (`lhsRange`) needed by the Big-M register formulation
 * - `name` / `displayName` for identification
 *
 * 构造方式：
 * - 从 `LinearInequality<Flt64>` 创建：`Flt64LinearConstraintInput.from(relation, lhsRange)`
 * - 直接构造：`Flt64LinearConstraintInput(flattenData, sign, lhsRange, name, displayName)`
 *
 * Construction:
 * - From `LinearInequality<Flt64>`: `Flt64LinearConstraintInput.from(relation, lhsRange)`
 * - Direct: `Flt64LinearConstraintInput(flattenData, sign, lhsRange, name, displayName)`
 */
data class Flt64LinearConstraintInput(
    val flattenData: LinearFlattenData<Flt64>,
    val sign: Comparison,
    val lhsRange: ValueRange<Flt64>,
    val name: String = "",
    val displayName: String? = null,
    /**
     * 归一化前的原始右侧常量值（lhs - rhs <= 0 形式）。
     * 函数符号基于范围的预计算需要此值（如 IfFunction.possibleRange）。
     * The original RHS constant value before normalization (lhs - rhs <= 0 form).
     * Needed by function symbols for range-based pre-computation (e.g., IfFunction.possibleRange).
     */
    val rhsConstant: Flt64 = Flt64.zero
) {
    // Cached bounds for Big-M formulation / Big-M 公式使用的缓存边界
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
            lhsRange: ValueRange<Flt64>,
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
     * 判断约束是否满足（使用符号表）
     * Check if constraint is satisfied (using token table)
     *
     * Replaces `LinearInequality.isTrue()` for function symbol runtime evaluation.
     *
     * @param tokenTable 符号表 / Token table
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
    fun <V> isTrue(tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): Boolean?
            where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlattenData(flattenData, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * 判断约束是否满足（使用符号值映射和符号表）
     * Check if constraint is satisfied (using symbol value map and token table)
     *
     * @param values 符号到值的映射 / Symbol to value mapping
     * @param tokenTable 符号表（可选） / Token table (optional)
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
    fun <V> isTrue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlattenDataWithValues(flattenData, values, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * 判断约束是否满足（使用求解结果和符号表）
     * Check if constraint is satisfied (using results and token table)
     *
     * @param results 求解结果列表 / Solution result list
     * @param tokenTable 符号表 / Token table
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
    fun <V> isTrue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlt64FlattenDataWithResults(flattenData, results, tokenTable, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * 判断约束是否满足（使用 V 类型求解结果和转换器）
     * Check if constraint is satisfied (using V-generic solution results and converter)
     *
     * Converts V to Flt64 via converter, then delegates to Flt64 evaluation.
     *
     * @param results 求解结果列表 / Solution result list
     * @param converter 值转换器 / Value converter
     * @param tokenTable 符号表 / Token table
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
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

    /**
     * 判断约束是否满足（使用符号列表）
     * Check if constraint is satisfied (using token list)
     *
     * @param tokenList 符号列表 / Token list
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
    internal fun isTrue(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean = false): Boolean? {
        val lhsValue = evaluateFlattenDataFromTokenList(flattenData, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * 判断约束是否满足（使用求解结果和符号列表）
     * Check if constraint is satisfied (using results and token list)
     *
     * @param results 求解结果列表 / Solution result list
     * @param tokenList 符号列表 / Token list
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
    fun isTrue(
        results: List<Flt64>,
        tokenList: AbstractTokenList<Flt64>,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithResultsFromTokenList(flattenData, results, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    /**
     * 判断约束是否满足（使用符号值映射和符号列表）
     * Check if constraint is satisfied (using symbol value map and token list)
     *
     * Falls back to token list for unresolved variables.
     *
     * @param values 符号到值的映射 / Symbol to value mapping
     * @param tokenList 符号列表（可选） / Token list (optional)
     * @param zeroIfNone 是否将缺失值视为零 / Whether to treat missing values as zero
     * @return 是否满足约束，如果无法判断返回 null / Whether constraint is satisfied, or null if undetermined
     */
    fun isTrue(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList<Flt64>?,
        zeroIfNone: Boolean = false
    ): Boolean? {
        val lhsValue = evaluateFlattenDataWithValuesAndTokenList(flattenData, values, tokenList, zeroIfNone = zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }
}

/**
 * 使用符号表评估 LinearFlattenData<Flt64>。
 * Evaluate LinearFlattenData<Flt64> against a token table.
 */
internal fun <V> evaluateFlattenData(
    data: LinearFlattenData<Flt64>,
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

/** 使用符号值映射和可选符号表评估线性扁平化数据 / Evaluate linear flatten data with symbol value map and optional token table */
private fun <V> evaluateFlattenDataWithValues(
    data: LinearFlattenData<Flt64>,
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
 * 使用值映射和 AbstractTokenList<Flt64> 回退来评估 LinearFlattenData<Flt64>。
 * Evaluate LinearFlattenData<Flt64> with values map and AbstractTokenList<Flt64> fallback.
 */
private fun evaluateFlattenDataWithValuesAndTokenList(
    data: LinearFlattenData<Flt64>,
    values: Map<Symbol, Flt64>,
    tokenList: AbstractTokenList<Flt64>?,
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
    data: LinearFlattenData<Flt64>,
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
 * Evaluate QuadraticFlattenData<Flt64> with values from `results` and symbol index from token table.
 * 使用 `results` 中的值并结合 token table 的索引来评估 QuadraticFlattenData<Flt64>。
 */
internal fun <V> evaluateFlt64QuadraticFlattenDataWithResults(
    data: QuadraticFlattenData<Flt64>,
    results: List<Flt64>,
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

/** 使用符号列表评估 Flt64 线性扁平化数据 / Evaluate Flt64 linear flatten data using a token list */
private fun evaluateFlattenDataFromTokenList(
    data: LinearFlattenData<Flt64>,
    tokenList: AbstractTokenList<Flt64>,
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

/** 使用求解结果和符号列表评估 Flt64 线性扁平化数据 / Evaluate Flt64 linear flatten data with results and token list */
private fun evaluateFlattenDataWithResultsFromTokenList(
    data: LinearFlattenData<Flt64>,
    results: List<Flt64>,
    tokenList: AbstractTokenList<Flt64>,
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
 * 比较辅助函数 - 返回 `value` 是否满足相对于 `rhs` 的关系。
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
 * 使用符号表和求解值评估二次扁平化数据。
 * Evaluate quadratic flatten data given token table and solution values.
 */
internal fun <V> evaluateQuadraticFlattenData(
    data: QuadraticFlattenData<Flt64>,
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
