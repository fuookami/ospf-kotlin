@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

private data class ElementPlan<V>(
    val bounds: List<ConditionBounds<V>>,
    val observedBounds: List<ConditionBounds<V>?>,
    val resultBounds: ConditionBounds<V>,
    val selectors: List<BinVar>,
    val maskedValues: List<RealVar>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 使用整数决策索引选择常量表或线性表达式表中的一个值。one-hot 与索引绑定保证越界索引不可行。
 * Selects a constant or linear expression from a table using an integer decision index.
 * One-hot selection and index binding make out-of-range indices infeasible.
 *
 * @property index 整数索引变量 / integer index variable
 * @property values 线性表达式表，常量可用常数多项式表示 / table of linear expressions; constants use constant polynomials
 * @property resultVar 查表结果变量 / lookup result variable
 * @param converter 数值类型转换器 / numeric value converter
 * @param lowerIndex 表中首项对应的索引 / index associated with the first table entry
 * @param valueBounds 可选的各表达式有限界 / optional finite bounds for each table expression
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class ElementFunction<V>(
    val index: AbstractVariableItem<*, *>,
    values: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    private val lowerIndex: Int = 0,
    private val valueBounds: List<ConditionBounds<V>>? = null,
    override var name: String = "element",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val values = values.map { LinearPolynomial(it.monomials.toList(), it.constant) }
    private var plan: ElementPlan<V>? = null

    private val resultVariable = RealVar("${name}_element")
    val resultVar: RealVar
        get() {
            resolvePlan()
            return resultVariable
        }

    override val resultPolynomial: LinearPolynomial<V>
        get() {
            resolvePlan()
            return LinearPolynomial(listOf(LinearMonomial(converter.one, resultVariable)), converter.zero)
        }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() {
            val current = resolvePlan() ?: return listOf(resultVariable)
            return listOf(resultVariable) + current.selectors + current.maskedValues
        }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val current = resolvePlan() ?: return null
        val indexValue = values[index] ?: return null
        val slot = tableSlot(indexValue) ?: return null
        return try {
            val selected = this.values[slot].evaluateWith(values) ?: return null
            if (selected.compareTo(current.bounds[slot].lower) < 0 || selected.compareTo(current.bounds[slot].upper) > 0) {
                null
            } else {
                selected
            }
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val current = resolvePlan()
            ?: return elementFailure("Element 要求整数索引、非空表和有限表项范围。 / Element requires an integer index, a non-empty table, and finite entry bounds.")
        if (!rangesRemainCovered(current)) {
            return elementFailure("Element 表项范围超出了已捕获范围。 / Element entry bounds exceed the captured bounds.")
        }
        return when (val result = tokens.add(listOf(resultVariable) + current.selectors + current.maskedValues)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val current = resolvePlan()
            ?: return elementFailure("Element 要求整数索引、非空表和有限表项范围。 / Element requires an integer index, a non-empty table, and finite entry bounds.")
        if (!rangesRemainCovered(current)) {
            return elementFailure("Element 表项范围超出了已捕获范围。 / Element entry bounds exceed the captured bounds.")
        }

        val constraints = try {
            buildElementConstraints(current)
        } catch (_: RuntimeException) {
            return elementFailure("Element 约束系数无效或溢出。 / Element constraint coefficients are invalid or overflowed.")
        } ?: return elementFailure("Element 索引系数无法表示。 / Element index coefficients are not representable.")
        if (!constraints.all { row ->
                hasUsableConditionFlattenedValues(row.lhs, converter) &&
                    hasUsableConditionFlattenedValues(row.rhs, converter)
            }) {
            return elementFailure("Element 约束包含非有限系数。 / Element constraints contain non-finite coefficients.")
        }
        return addConstraints(model, constraints) ?: ok
    }

    private fun resolvePlan(): ElementPlan<V>? {
        plan?.let { return it }
        try {
            if (!index.type.isIntegerType || values.isEmpty()) return null
            val lastIndex = lowerIndex.toLong() + values.lastIndex.toLong()
            if (lastIndex !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
            if (valueBounds != null && valueBounds.size != values.size) return null

            val observed = values.map { input -> input.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) } }
            val bounds = values.indices.map { slot -> valueBounds?.get(slot) ?: observed[slot] ?: return null }
            if (bounds.indices.any { slot ->
                    !validBounds(bounds[slot])
                }) {
                return null
            }

            var resultLower = bounds.first().lower
            var resultUpper = bounds.first().upper
            for (entryBounds in bounds.drop(1)) {
                if (entryBounds.lower.compareTo(resultLower) < 0) resultLower = entryBounds.lower
                if (entryBounds.upper.compareTo(resultUpper) > 0) resultUpper = entryBounds.upper
            }
            val selectors = values.indices.map { BinVar("${name}_element_select_$it") }
            val maskedValues = values.indices.map { RealVar("${name}_element_value_$it") }
            resultVariable.range.geq(converter.fromValue(resultLower))
            resultVariable.range.leq(converter.fromValue(resultUpper))
            for (slot in values.indices) {
                val lower = if (bounds[slot].lower.compareTo(converter.zero) < 0) bounds[slot].lower else converter.zero
                val upper = if (bounds[slot].upper.compareTo(converter.zero) > 0) bounds[slot].upper else converter.zero
                maskedValues[slot].range.geq(converter.fromValue(lower))
                maskedValues[slot].range.leq(converter.fromValue(upper))
            }
            val candidate = ElementPlan(bounds, observed, ConditionBounds(resultLower, resultUpper), selectors, maskedValues)
            plan = candidate
            return candidate
        } catch (_: RuntimeException) {
            return null
        }
    }

    private fun rangesRemainCovered(current: ElementPlan<V>): Boolean {
        for (slot in values.indices) {
            val actual = values[slot].finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            if (actual == null && current.observedBounds[slot] != null) return false
            val captured = current.observedBounds[slot]
            if (actual != null && captured != null && !contains(captured, actual)) return false
        }
        return true
    }

    private fun tableSlot(indexValue: V): Int? {
        for (slot in values.indices) {
            val value = lowerIndex.toLong() + slot.toLong()
            val expected = try {
                converter.intoValue(Flt64(java.lang.Double.valueOf(value.toString())))
            } catch (_: RuntimeException) {
                return null
            }
            if (indexValue.compareTo(expected) == 0) return slot
        }
        return null
    }

    private fun buildElementConstraints(current: ElementPlan<V>): List<LinearInequality<V>>? {
        val zero = converter.zero
        val one = converter.one
        val constraints = mutableListOf<LinearInequality<V>>()
        val selectorTerms = current.selectors.map { LinearMonomial(one, it) }
        constraints += LinearInequality(
            LinearPolynomial(selectorTerms, zero),
            LinearPolynomial(emptyList(), one),
            Comparison.EQ,
            "${name}_element_one_hot"
        )

        val indexTerms = mutableListOf(LinearMonomial(one, index))
        val offsetTerms = mutableListOf<LinearMonomial<V>>()
        for (slot in current.selectors.indices) {
            val selector = current.selectors[slot]
            val offset = try {
                converter.intoValue(Flt64(slot))
            } catch (_: RuntimeException) {
                return null
            }
            offsetTerms += LinearMonomial(offset, selector)
            constraints += LinearInequality(
                lhs = values[slot],
                rhs = LinearPolynomial(emptyList(), current.bounds[slot].upper),
                comparison = Comparison.LE,
                name = "${name}_element_entry_${slot}_upper"
            )
            constraints += LinearInequality(
                lhs = values[slot],
                rhs = LinearPolynomial(emptyList(), current.bounds[slot].lower),
                comparison = Comparison.GE,
                name = "${name}_element_entry_${slot}_lower"
            )
            constraints += maskingConstraints(
                input = values[slot],
                lower = current.bounds[slot].lower,
                upper = current.bounds[slot].upper,
                selector = selector,
                result = current.maskedValues[slot],
                namePrefix = "${name}_element_mask_$slot"
            )
        }
        indexTerms += offsetTerms.map { LinearMonomial(-it.coefficient, it.symbol) }
        val lowerValue = try {
            converter.intoValue(Flt64(lowerIndex))
        } catch (_: RuntimeException) {
            return null
        }
        constraints += LinearInequality(
            LinearPolynomial(indexTerms, zero),
            LinearPolynomial(emptyList(), lowerValue),
            Comparison.EQ,
            "${name}_element_index"
        )
        val resultTerms = mutableListOf(LinearMonomial(one, resultVariable))
        resultTerms += current.maskedValues.map { LinearMonomial(-one, it) }
        constraints += LinearInequality(
            LinearPolynomial(resultTerms, zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.EQ,
            "${name}_element_result"
        )
        return constraints
    }

    private fun maskingConstraints(
        input: LinearPolynomial<V>,
        lower: V,
        upper: V,
        selector: AbstractVariableItem<*, *>,
        result: AbstractVariableItem<*, *>,
        namePrefix: String
    ): List<LinearInequality<V>> {
        val zero = converter.zero
        val resultMonomial = LinearMonomial(converter.one, result)
        val negativeInput = input.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        return listOf(
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial, LinearMonomial(-upper, selector)), zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.LE,
                "${namePrefix}_zero_upper"
            ),
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial, LinearMonomial(-lower, selector)), zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.GE,
                "${namePrefix}_zero_lower"
            ),
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial) + negativeInput + LinearMonomial(-lower, selector), -input.constant),
                LinearPolynomial(emptyList(), -lower),
                Comparison.LE,
                "${namePrefix}_one_upper"
            ),
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial) + negativeInput + LinearMonomial(-upper, selector), -input.constant),
                LinearPolynomial(emptyList(), -upper),
                Comparison.GE,
                "${namePrefix}_one_lower"
            )
        )
    }

    private fun validBounds(bounds: ConditionBounds<V>): Boolean {
        return bounds.lower.compareTo(bounds.upper) <= 0 &&
            isUsableConditionBound(bounds.lower, converter) &&
            isUsableConditionBound(bounds.upper, converter)
    }

    private fun contains(outer: ConditionBounds<V>, inner: ConditionBounds<V>): Boolean {
        return outer.lower.compareTo(inner.lower) <= 0 && outer.upper.compareTo(inner.upper) >= 0
    }

    companion object {
        /** 创建查表函数符号。 / Create a table-lookup function symbol.
         * @param index 整数索引变量 / integer index variable
         * @param values 线性表达式表 / table of linear expressions
         * @param converter 数值类型转换器 / numeric value converter
         * @param lowerIndex 表中首项对应的索引 / index associated with the first table entry
         * @param valueBounds 可选的各表达式有限界 / optional finite bounds for each table expression
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 查表函数符号 / table-lookup function symbol
         */
        operator fun <V> invoke(
            index: AbstractVariableItem<*, *>,
            values: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            lowerIndex: Int = 0,
            valueBounds: List<ConditionBounds<V>>? = null,
            name: String = "element",
            displayName: String? = null
        ): ElementFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return ElementFunction(
                index = index,
                values = values,
                converter = converter,
                lowerIndex = lowerIndex,
                valueBounds = valueBounds,
                name = name,
                displayName = displayName
            )
        }

        /** 从常数表创建查表函数符号。 / Create a lookup function from a constant table.
         * @param index 整数索引变量 / integer index variable
         * @param values 常数表项 / constant table entries
         * @param converter 数值类型转换器 / numeric value converter
         * @param lowerIndex 表中首项对应的索引 / index associated with the first table entry
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 常数查表函数符号 / constant table-lookup function symbol
         */
        fun <V> fromConstants(
            index: AbstractVariableItem<*, *>,
            values: List<V>,
            converter: IntoValue<V>,
            lowerIndex: Int = 0,
            name: String = "element",
            displayName: String? = null
        ): ElementFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            val polynomials = values.map { LinearPolynomial(emptyList(), it) }
            return ElementFunction(
                index = index,
                values = polynomials,
                converter = converter,
                lowerIndex = lowerIndex,
                valueBounds = null,
                name = name,
                displayName = displayName
            )
        }
    }
}

/** 决策索引查表函数的别名。 / Alias for decision-indexed table lookup. */
typealias LookupFunction<V> = ElementFunction<V>

private fun <T> elementFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}
