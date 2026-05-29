/**
 * 不等式扁平化工具
 * Inequality flattening utilities
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.Failed

private val solverValueConverter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

// ========== Recursive expansion of intermediate symbols ==========

/**
 * 递归展开可能为 [LinearIntermediateSymbol] 的 [LinearMonomial]，
 * 返回 (变量单项式列表, 常量贡献) 对。
 * Recursively expand a [LinearMonomial] whose symbol may be a [LinearIntermediateSymbol]
 * into a pair of (variable-item monomials, constant contribution).
 *
 * - [AbstractVariableItem]: 直接返回，常量贡献为零 / returned as-is with zero constant contribution
 * - [LinearIntermediateSymbol]: 其多项式单项式按原系数缩放后递归展开，常量也缩放累加
 *   its monomials are scaled by the original coefficient and recursively expanded
 * - 其他符号类型：视为错误（返回 [Failed] 结果）/ Other symbol types: treated as an error
 */
@Suppress("UNCHECKED_CAST")
private fun <V> expandLinearMonomial(mono: LinearMonomial<V>): Result<Pair<List<LinearMonomial<V>>, V>>
        where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    return when (val sym = mono.symbol) {
        is AbstractVariableItem<*, *> -> Result.success(Pair(listOf(mono), (mono.coefficient - mono.coefficient)))
        is LinearIntermediateSymbol<*> -> {
            val intermediate = sym as LinearIntermediateSymbol<V>
            val expanded = mutableListOf<LinearMonomial<V>>()
            var constantContribution = mono.coefficient * intermediate.polynomial.constant
            for (inner in intermediate.polynomial.monomials) {
                val scaled = LinearMonomial(mono.coefficient * inner.coefficient, inner.symbol)
                val innerResult = expandLinearMonomial(scaled).getOrElse { return Result.failure(it) }
                expanded.addAll(innerResult.first)
                constantContribution += innerResult.second
            }
            Result.success(Pair(expanded, constantContribution))
        }
        else -> Result.failure(
            IllegalArgumentException("Cannot flatten monomial with symbol type ${sym::class.simpleName}: ${sym.name}")
        )
    }
}

/**
 * 展开 [LinearPolynomial] 中的所有单项式，仅返回变量单项式。
 * 将中间符号展开的常量累加到多项式常量中。
 * Expand all monomials in a [LinearPolynomial], returning only variable-item monomials.
 * Accumulates constants from intermediate symbol expansion into the polynomial's constant.
 */
private fun <V> expandLinearPolynomial(poly: LinearPolynomial<V>): Result<Pair<List<LinearMonomial<V>>, V>>
        where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    val expanded = mutableListOf<LinearMonomial<V>>()
    var totalConstant = poly.constant
    for (mono in poly.monomials) {
        val result = expandLinearMonomial(mono).getOrElse { return Result.failure(it) }
        for (m in result.first) {
            expanded.add(m)
        }
        totalConstant += result.second
    }
    return Result.success(Pair(expanded, totalConstant))
}

// ========== Converter-based flatten: V-typed inequality -> V-typed flatten data ==========

/**
 * 将 V 类型 [LinearInequality] 扁平化为 [LinearFlattenData]<V>（恒等扁平化，无转换）。
 * Flatten a V-typed [LinearInequality] into [LinearFlattenData]<V> (identity flatten, no conversion).
 *
 * 中间符号 ([LinearIntermediateSymbol]) 在合并前递归展开为变量单项式。
 * 不支持的符号类型会产生 [Failed] 结果而非 [ClassCastException]。
 * Intermediate symbols are recursively expanded before merging.
 * Unsupported symbol types produce a [Failed] result instead of a [ClassCastException].
 */
internal fun <V> LinearInequality<V>.toLinearFlattenData(): Result<LinearFlattenData<V>>
        where V : RealNumber<V>, V : NumberField<V> {
    val (lhsExpanded, lhsExtra) = expandLinearPolynomial(lhs).getOrElse { return Result.failure(it) }
    val (rhsExpanded, rhsExtra) = expandLinearPolynomial(rhs).getOrElse { return Result.failure(it) }

    val merged = HashMap<VariableItemKey, LinearMonomial<V>>()

    for (mono in lhsExpanded) {
        val variable = mono.symbol as? AbstractVariableItem<*, *>
            ?: return Result.failure(
                IllegalArgumentException("Cannot flatten lhs monomial with non-variable symbol: ${mono.symbol::class.simpleName}: ${mono.symbol.name}")
            )
        val key = variable.key
        merged[key] = LinearMonomial(mono.coefficient, mono.symbol)
    }
    for (mono in rhsExpanded) {
        val variable = mono.symbol as? AbstractVariableItem<*, *>
            ?: return Result.failure(
                IllegalArgumentException("Cannot flatten rhs monomial with non-variable symbol: ${mono.symbol::class.simpleName}: ${mono.symbol.name}")
            )
        val key = variable.key
        val existing = merged[key]
        merged[key] = if (existing != null) {
            LinearMonomial(existing.coefficient - mono.coefficient, existing.symbol)
        } else {
            LinearMonomial(-mono.coefficient, mono.symbol)
        }
    }

    return Result.success(LinearFlattenData<V>(
        monomials = merged.values.toList(),
        constant = lhsExtra - rhsExtra
    ))
}

/**
 * 将 V 类型 [QuadraticInequalityOf] 扁平化为 [QuadraticFlattenData]<V>（恒等扁平化，无转换）。
 * 将 lhs - rhs 转换为单一二次形式。
 * Flatten a V-typed [QuadraticInequalityOf] into [QuadraticFlattenData]<V> (identity flatten, no conversion).
 * Converts lhs - rhs into a single quadratic form.
 */
internal fun <V> QuadraticInequalityOf<V>.toQuadraticFlattenData(): QuadraticFlattenData<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<QuadraticMonomialKey, QuadraticMonomial<V>>()

    for (mono in lhs.monomials) {
        val key = QuadraticMonomialKey.from(mono)
        merged[key] = QuadraticMonomial(
            coefficient = mono.coefficient,
            symbol1 = mono.symbol1,
            symbol2 = mono.symbol2
        )
    }
    for (mono in rhs.monomials) {
        val key = QuadraticMonomialKey.from(mono)
        val existing = merged[key]
        merged[key] = if (existing != null) {
            QuadraticMonomial(
                coefficient = existing.coefficient - mono.coefficient,
                symbol1 = existing.symbol1,
                symbol2 = existing.symbol2
            )
        } else {
            QuadraticMonomial(
                coefficient = -mono.coefficient,
                symbol1 = mono.symbol1,
                symbol2 = mono.symbol2
            )
        }
    }

    return QuadraticFlattenData<V>(
        monomials = merged.values.toList(),
        constant = lhs.constant - rhs.constant
    )
}

// ========== Converter-based flatten: V-typed inequality -> Flt64 flatten data ==========

/**
 * 使用显式转换器将 V 类型 [LinearInequality] 扁平化为 [LinearFlattenData]<Flt64>。
 * Flatten a V-typed [LinearInequality] into [LinearFlattenData]<Flt64> using an explicit converter.
 *
 * 中间符号在合并前递归展开。不支持的符号类型会产生 [Failed] 结果。
 * Intermediate symbols are recursively expanded before merging.
 * Unsupported symbol types produce a [Failed] result instead of a [ClassCastException].
 */
internal fun <V> LinearInequality<V>.toLinearFlattenDataFlt64(converter: IntoValue<V>): Result<LinearFlattenData<Flt64>>
        where V : RealNumber<V>, V : NumberField<V> {
    val (lhsExpanded, lhsExtra) = expandLinearPolynomial(lhs).getOrElse { return Result.failure(it) }
    val (rhsExpanded, rhsExtra) = expandLinearPolynomial(rhs).getOrElse { return Result.failure(it) }

    val merged = HashMap<VariableItemKey, LinearMonomial<Flt64>>()

    for (mono in lhsExpanded) {
        val variable = mono.symbol as? AbstractVariableItem<*, *>
            ?: return Result.failure(
                IllegalArgumentException("Cannot flatten lhs monomial with non-variable symbol: ${mono.symbol::class.simpleName}: ${mono.symbol.name}")
            )
        val key = variable.key
        val flt64Coeff = converter.fromValue(mono.coefficient)
        merged[key] = LinearMonomial(flt64Coeff, mono.symbol)
    }
    for (mono in rhsExpanded) {
        val variable = mono.symbol as? AbstractVariableItem<*, *>
            ?: return Result.failure(
                IllegalArgumentException("Cannot flatten rhs monomial with non-variable symbol: ${mono.symbol::class.simpleName}: ${mono.symbol.name}")
            )
        val key = variable.key
        val flt64Coeff = converter.fromValue(mono.coefficient)
        val existing = merged[key]
        merged[key] = if (existing != null) {
            LinearMonomial(existing.coefficient - flt64Coeff, existing.symbol)
        } else {
            LinearMonomial(-flt64Coeff, mono.symbol)
        }
    }

    return Result.success(LinearFlattenData<Flt64>(
        monomials = merged.values.toList(),
        constant = converter.fromValue(lhsExtra) - converter.fromValue(rhsExtra)
    ))
}

/**
 * 使用显式转换器将 V 类型 [QuadraticInequalityOf] 扁平化为 [QuadraticFlattenData]<Flt64>。
 * Flatten a V-typed [QuadraticInequalityOf] into [QuadraticFlattenData]<Flt64> using an explicit converter.
 *
 * 将 lhs - rhs 转换为单一二次形式。/ Converts lhs - rhs into a single quadratic form.
 */
internal fun <V> QuadraticInequalityOf<V>.toQuadraticFlattenDataFlt64(converter: IntoValue<V>): QuadraticFlattenData<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val merged = HashMap<QuadraticMonomialKey, QuadraticMonomial<Flt64>>()

    for (mono in lhs.monomials) {
        val key = QuadraticMonomialKey.from(mono, converter)
        val flt64Coeff = converter.fromValue(mono.coefficient)
        merged[key] = QuadraticMonomial(
            coefficient = flt64Coeff,
            symbol1 = mono.symbol1,
            symbol2 = mono.symbol2
        )
    }
    for (mono in rhs.monomials) {
        val key = QuadraticMonomialKey.from(mono, converter)
        val flt64Coeff = converter.fromValue(mono.coefficient)
        val existing = merged[key]
        merged[key] = if (existing != null) {
            QuadraticMonomial(
                coefficient = existing.coefficient - flt64Coeff,
                symbol1 = existing.symbol1,
                symbol2 = existing.symbol2
            )
        } else {
            QuadraticMonomial(
                coefficient = -flt64Coeff,
                symbol1 = mono.symbol1,
                symbol2 = mono.symbol2
            )
        }
    }

    return QuadraticFlattenData<Flt64>(
        monomials = merged.values.toList(),
        constant = converter.fromValue(lhs.constant) - converter.fromValue(rhs.constant)
    )
}

// ========== Flt64-specific flatten extensions (for Flt64-typed inequalities) ==========

/** 比较类型的别名，对应旧的 Relation.sign 属性 / Alias for comparison, matching the old Relation.sign property */
internal val LinearInequality<Flt64>.sign: Comparison get() = comparison

/** 比较类型的别名，对应旧的 Relation.sign 属性 / Alias for comparison, matching the old Relation.sign property */
internal val QuadraticInequalityOf<Flt64>.sign: Comparison get() = comparison

/**
 * 从 [LinearInequality]<Flt64> 计算 [LinearFlattenData]<Flt64>。
 * 将 lhs - rhs 展平为单一线性形式。
 * 这是当已知 V=Flt64 时的 Flt64 特定便捷属性。
 * Compute [LinearFlattenData]<Flt64> from [LinearInequality]<Flt64>.
 * Flattens lhs - rhs into a single linear form.
 *
 * This is the Flt64-specific convenience for when V=Flt64 is already known.
 */
internal val LinearInequality<Flt64>.flattenData: Result<LinearFlattenData<Flt64>>
    get() = toLinearFlattenDataFlt64(solverValueConverter)

/**
 * 从 QuadraticInequality (Flt64) 计算 [QuadraticFlattenData]<Flt64>。
 * 将 lhs - rhs 展平为单一二次形式。
 * 这是当已知 V=Flt64 时的 Flt64 特定便捷属性。
 * Compute [QuadraticFlattenData]<Flt64> from QuadraticInequality (Flt64).
 * Flattens lhs - rhs into a single quadratic form.
 *
 * This is the Flt64-specific convenience for when V=Flt64 is already known.
 */
internal val QuadraticInequalityOf<Flt64>.flattenData: QuadraticFlattenData<Flt64>
    get() = toQuadraticFlattenDataFlt64(solverValueConverter)

// ========== Internal key for merging quadratic monomials ==========

/**
 * 合并二次单项式的内部键（处理 x*y = y*x 的交换性）。
 * Internal key for merging quadratic monomials (handles commutativity of x*y = y*x).
 *
 * @property sym1Id 第一个符号的 identityHashCode / identityHashCode of first symbol
 * @property sym2Id 第二个符号的 identityHashCode（null 表示线性项）/ identityHashCode of second symbol (null for linear term)
 */
private data class QuadraticMonomialKey(
    val sym1Id: Int,
    val sym2Id: Int?
) {
    companion object {
        @JvmName("fromGeneric")
        fun <V> from(mono: QuadraticMonomial<V>): QuadraticMonomialKey
                where V : RealNumber<V>, V : NumberField<V> {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }

        @JvmName("fromGenericWithConverter")
        fun <V> from(mono: QuadraticMonomial<V>, @Suppress("UNUSED_PARAMETER") converter: IntoValue<V>): QuadraticMonomialKey
                where V : RealNumber<V>, V : NumberField<V> {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }

        @JvmName("fromFlt64")
        private fun from(mono: QuadraticMonomial<Flt64>): QuadraticMonomialKey {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }
    }
}

// ========== Conversion from math types to frontend types ==========

/**
 * 从数学 [LinearPolynomial] 直接创建 [LinearFlattenData]<Flt64>。
 * 仅需要不等式一侧时使用。
 * Create [LinearFlattenData]<Flt64> directly from math [LinearPolynomial].
 * Used when only one side of the inequality is needed.
 */
internal fun LinearPolynomial<Flt64>.toFlattenData(): LinearFlattenData<Flt64> {
    return LinearFlattenData<Flt64>(
        monomials = monomials.map { LinearMonomial(it.coefficient, it.symbol) },
        constant = constant
    )
}

/**
 * 从数学 [QuadraticPolynomial] 直接创建 [QuadraticFlattenData]<Flt64>。
 * 仅需要不等式一侧时使用。
 * Create [QuadraticFlattenData]<Flt64> directly from math [QuadraticPolynomial].
 * Used when only one side of the inequality is needed.
 */
internal fun QuadraticPolynomial<Flt64>.toFlattenData(): QuadraticFlattenData<Flt64> {
    return QuadraticFlattenData<Flt64>(
        monomials = monomials.map {
            QuadraticMonomial(
                coefficient = it.coefficient,
                symbol1 = it.symbol1,
                symbol2 = it.symbol2
            )
        },
        constant = constant
    )
}
