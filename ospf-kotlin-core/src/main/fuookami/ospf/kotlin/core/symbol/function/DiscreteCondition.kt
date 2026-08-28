/**
 * 离散条件到严格正数条件的转换工具。 / Utilities for converting discrete conditions to strict-positive conditions.
 */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.ordinary.gcd
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 离散关系转换后的严格正数条件。 / A strict-positive condition produced from a discrete relation.
 *
 * 对关系 `d relation 0`，返回的多项式 `q` 使用统一的严格正数语义 `q > 0`。
 * For `d relation 0`, the returned polynomial `q` uses the unified strict-positive semantics `q > 0`.
 *
 * @property polynomial 严格正数条件多项式 / strict-positive condition polynomial
 */
data class StrictPositiveCondition<V>(
    val polynomial: LinearPolynomial<V>
) where V : RealNumber<V>, V : NumberField<V>

/** 归一化后的离散条件线性化数据。 / Normalized linearization data for a discrete condition. */
internal data class DiscreteConditionLinearization<V>(
    val polynomial: LinearPolynomial<V>,
    val bounds: ConditionBounds<V>,
    val fixedValue: TruthValue?,
    val trueBranchPossible: Boolean,
    val falseBranchPossible: Boolean
) where V : RealNumber<V>, V : NumberField<V>

private fun <V> V.isDiscreteConditionFinite(): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!isFinite()) {
            false
        } else {
            val nan = constants.nan
            nan == null || this != nan
        }
    } catch (_: RuntimeException) {
        false
    }
}

/** 检查数值是否能作为求解器系数使用。 / Check whether a value is usable as a solver coefficient. */
private fun <V> V.isDiscreteConditionSolverValue(): Boolean
        where V : RealNumber<V>, V : NumberField<V> {
    if (!isDiscreteConditionFinite()) {
        return false
    }
    return try {
        val solverValue = toFltX()
        val nan = solverValue.constants.nan
        solverValue.isFinite() &&
            (nan == null || solverValue != nan) &&
            solverValue.compareTo(FltX.minimum) > 0 &&
            solverValue.compareTo(FltX.maximum) < 0
    } catch (_: RuntimeException) {
        false
    }
}

private fun <T> discreteConditionFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}

private fun <V> validateFinitePolynomial(
    polynomial: LinearPolynomial<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!polynomial.constant.isDiscreteConditionSolverValue()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "条件多项式的常数必须为有限且可编码的 solver 值。 / The condition polynomial constant must be finite and solver-representable."
            )
        }
        val zero = polynomial.constant - polynomial.constant
        val coefficients = LinkedHashMap<Symbol, V>()
        for (monomial in polynomial.monomials) {
            if (!monomial.coefficient.isDiscreteConditionSolverValue()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "条件多项式的系数必须为有限且可编码的 solver 值。 / Condition polynomial coefficients must be finite and solver-representable."
                )
            }
            val coefficient = (coefficients[monomial.symbol] ?: zero) + monomial.coefficient
            if (!coefficient.isDiscreteConditionSolverValue()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "合并后的条件多项式系数必须为有限且可编码的 solver 值。 / Combined condition-polynomial coefficients must be finite and solver-representable."
                )
            }
            coefficients[monomial.symbol] = coefficient
        }
        ok
    } catch (_: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "条件多项式校验失败。 / Condition polynomial validation failed."
        )
    }
}

/**
 * 合并同一符号的系数，得到实际参与值域证明的线性项。 /
 * Combine coefficients for the same symbol before proving value-domain spacing.
 */
private fun <V> effectiveDiscreteConditionMonomials(
    polynomial: LinearPolynomial<V>
): List<LinearMonomial<V>> where V : RealNumber<V>, V : NumberField<V> {
    val zero = polynomial.constant - polynomial.constant
    val coefficientOfSymbol = LinkedHashMap<Symbol, V>()
    for (monomial in polynomial.monomials) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: zero) + monomial.coefficient
    }
    return coefficientOfSymbol.mapNotNull { (symbol, coefficient) ->
        if (coefficient.compareTo(zero) == 0) {
            null
        } else {
            LinearMonomial(coefficient = coefficient, symbol = symbol)
        }
    }
}

private fun <V> isIntegerValuedDiscreteConditionNumber(value: V): Boolean
        where V : RealNumber<V>, V : NumberField<V> {
    if (!value.isDiscreteConditionFinite()) {
        return false
    }

    // Convert through FltX so this check also works for rational and arbitrary-precision values. /
    // 统一转换到 FltX，使有理数和任意精度数值也能执行整数性检查。
    val floating = value.toFltX()
    if (!floating.isFinite()) {
        return false
    }
    val nan = floating.constants.nan
    if (nan != null && floating == nan) {
        return false
    }
    return floating.round().compareTo(floating) == 0
}

private fun <V> isDiscreteConditionDomainPolynomial(
    polynomial: LinearPolynomial<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    if (!polynomial.constant.isDiscreteConditionFinite() ||
        polynomial.monomials.any { !it.coefficient.isDiscreteConditionFinite() }
    ) {
        return false
    }

    return effectiveDiscreteConditionMonomials(polynomial).all { monomial ->
        val zero = polynomial.constant - polynomial.constant
        if (monomial.coefficient.compareTo(zero) == 0) {
            true
        } else {
            when (val symbol = monomial.symbol) {
                is AbstractVariableItem<*, *> -> symbol.type.isIntegerType
                is LinearIntermediateSymbol<*> -> symbol.discrete
                else -> false
            }
        }
    }
}

private fun <V> isDiscreteConditionPolynomial(
    polynomial: LinearPolynomial<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    if (!isDiscreteConditionDomainPolynomial(polynomial)) {
        return false
    }

    return effectiveDiscreteConditionMonomials(polynomial)
        .all { isIntegerValuedDiscreteConditionNumber(it.coefficient) }
}

private fun <V> validateDiscreteConditionPolynomial(
    polynomial: LinearPolynomial<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    return if (isDiscreteConditionPolynomial(polynomial)) {
        ok
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "delta 大于 strictBoundary 时，条件必须由整数系数和离散变量组成。 / When delta is greater than strictBoundary, the condition must use integer-valued coefficients and discrete variables."
        )
    }
}

/**
 * 计算整数系数的最小离散间隔。 / Compute the minimum discrete spacing induced by integer coefficients.
 */
private fun <V> discreteConditionCoefficientGcd(
    polynomial: LinearPolynomial<V>
): FltX? where V : RealNumber<V>, V : NumberField<V> {
    val coefficients = effectiveDiscreteConditionMonomials(polynomial)
        .map { it.coefficient.toFltX().abs() }
        .filter { it.compareTo(FltX.zero) != 0 }
    return if (coefficients.isEmpty()) {
        null
    } else {
        gcd(coefficients)
    }
}

/**
 * 检查多项式值是否具有足够的离散格点证明。 / Check whether polynomial values have a sufficiently strong discrete-lattice proof.
 *
 * 最小间隔由非零整数系数的 gcd 决定，常数只平移格点而不改变间隔。
 * The minimum spacing is determined by the gcd of non-zero integer coefficients; the constant shifts the lattice but does not change its spacing.
 */
private fun <V> hasSafeDiscreteShiftProof(
    polynomial: LinearPolynomial<V>,
    relation: Comparison,
    delta: V
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    if (!isDiscreteConditionPolynomial(polynomial) ||
        (relation != Comparison.GE && relation != Comparison.LE)
    ) {
        return false
    }

    return try {
        val deltaFltX = delta.toFltX()
        if (!deltaFltX.isFinite() || deltaFltX <= FltX.zero) {
            return false
        }

        val constant = polynomial.constant.toFltX()
        if (!constant.isFinite()) {
            return false
        }

        val spacing = discreteConditionCoefficientGcd(polynomial)
        if (spacing == null) {
            return when (relation) {
                Comparison.GE -> constant.compareTo(FltX.zero) >= 0 ||
                    constant.compareTo(-deltaFltX) <= 0
                Comparison.LE -> constant.compareTo(FltX.zero) <= 0 ||
                    constant.compareTo(deltaFltX) >= 0
                Comparison.GT, Comparison.LT, Comparison.EQ, Comparison.NE -> false
            }
        }
        if (!spacing.isFinite() || spacing <= FltX.zero || spacing.compareTo(deltaFltX) < 0) {
            return false
        }

        // Values of an integer-coefficient polynomial are contained in
        // constant + spacing * Z. Check the closest lattice point on the
        // side that would be misclassified by the GE/LE shift. /
        // 整数系数多项式的值属于 constant + spacing * Z；检查可能被 GE/LE
        // 平移误判的一侧最近格点，而不是只检查变量和系数是否为整数。
        val rawRemainder = constant % spacing
        val remainder = if (rawRemainder.compareTo(FltX.zero) < 0) {
            rawRemainder + spacing
        } else {
            rawRemainder
        }
        val distanceToMisclassifiedSide = when (relation) {
            Comparison.GE -> if (remainder.compareTo(FltX.zero) == 0) {
                spacing
            } else {
                spacing - remainder
            }
            Comparison.LE -> if (remainder.compareTo(FltX.zero) == 0) {
                spacing
            } else {
                remainder
            }
            Comparison.GT, Comparison.LT, Comparison.EQ, Comparison.NE -> return false
        }
        return distanceToMisclassifiedSide.compareTo(deltaFltX) >= 0
    } catch (_: RuntimeException) {
        false
    }
}

/**
 * 检查 GT/LT 的离散值域是否避开严格正数间隔。 /
 * Check whether the discrete value lattice for GT/LT avoids the strict-positive gap.
 *
 * GT/LT 不对多项式做 delta 平移，但其正分支仍从 strictBoundary 开始。
 * A GT/LT condition does not shift the polynomial by delta, but its true branch
 * still starts at strictBoundary.
 */
private fun <V> hasSafeStrictPositiveLatticeProof(
    polynomial: LinearPolynomial<V>,
    relation: Comparison,
    strictBoundary: V
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    if (!isDiscreteConditionPolynomial(polynomial) ||
        (relation != Comparison.GT && relation != Comparison.LT)
    ) {
        return false
    }

    return try {
        val strictBoundaryFltX = strictBoundary.toFltX()
        if (!strictBoundaryFltX.isFinite() || strictBoundaryFltX <= FltX.zero) {
            return false
        }

        // Normalize LT to q = -d, so both relations use q > 0. /
        // 将 LT 归一化为 q = -d，使两种关系都使用 q > 0。
        val normalizedConstant = if (relation == Comparison.LT) {
            -polynomial.constant.toFltX()
        } else {
            polynomial.constant.toFltX()
        }
        if (!normalizedConstant.isFinite()) {
            return false
        }

        val spacing = discreteConditionCoefficientGcd(polynomial)
        if (spacing == null) {
            return normalizedConstant.compareTo(FltX.zero) <= 0 ||
                normalizedConstant.compareTo(strictBoundaryFltX) >= 0
        }
        if (!spacing.isFinite() || spacing <= FltX.zero) {
            return false
        }

        val rawRemainder = normalizedConstant % spacing
        val remainder = if (rawRemainder.compareTo(FltX.zero) < 0) {
            rawRemainder + spacing
        } else {
            rawRemainder
        }
        val nearestPositive = if (remainder.compareTo(FltX.zero) == 0) {
            spacing
        } else {
            remainder
        }
        nearestPositive.compareTo(strictBoundaryFltX) >= 0
    } catch (_: RuntimeException) {
        false
    }
}

/**
 * 检查有限范围是否已经落在某关系的单一安全分支。 /
 * Check whether finite bounds lie in one safe branch of a relation.
 *
 * 先将关系统一为 q > 0，再检查 q 的范围；返回分支而不是布尔值，
 * 避免把范围证明隐式当作一般离散格点证明。
 * Normalize the relation to q > 0 first and return the branch instead of a Boolean,
 * so a range proof cannot be mistaken for a general discrete-lattice proof.
 */
private fun <V> boundsAvoidDiscreteConditionGap(
    relation: Comparison,
    bounds: ConditionBounds<V>,
    strictBoundary: V
): TruthValue? where V : RealNumber<V>, V : NumberField<V> {
    val zero = strictBoundary - strictBoundary
    return when (relation) {
        // Folding is based on the original relation. The transformed polynomial
        // may move a safe false branch into the positive half-line when delta >
        // strictBoundary, but a fixed indicator does not need the transformed
        // relation constraints. /
        // 折叠依据原始关系。delta > strictBoundary 时，转换多项式可能把安全
        // 假分支移到正半轴；固定指示变量时不需要再生成转换后的关系约束。
        Comparison.GE -> when {
            bounds.lower.compareTo(zero) >= 0 -> TruthValue.True
            bounds.upper.compareTo(-strictBoundary) <= 0 -> TruthValue.False
            else -> null
        }
        Comparison.LE -> when {
            bounds.upper.compareTo(zero) <= 0 -> TruthValue.True
            bounds.lower.compareTo(strictBoundary) >= 0 -> TruthValue.False
            else -> null
        }
        Comparison.GT -> when {
            bounds.lower.compareTo(strictBoundary) >= 0 -> TruthValue.True
            bounds.upper.compareTo(zero) <= 0 -> TruthValue.False
            else -> null
        }
        Comparison.LT -> when {
            bounds.upper.compareTo(-strictBoundary) <= 0 -> TruthValue.True
            bounds.lower.compareTo(zero) >= 0 -> TruthValue.False
            else -> null
        }
        Comparison.EQ, Comparison.NE -> null
    }
}

private fun <V> validateDiscreteShiftProof(
    polynomial: LinearPolynomial<V>,
    relation: Comparison,
    delta: V
): Try where V : RealNumber<V>, V : NumberField<V> {
    if (relation != Comparison.GE && relation != Comparison.LE) {
        return ok
    }
    return if (hasSafeDiscreteShiftProof(
        polynomial = polynomial,
        relation = relation,
        delta = delta
    )) {
        ok
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "GE/LE 使用非单位 delta 平移前必须证明条件值的离散步长；请使用单位步长或提供可证明的 delta 对齐离散格点。 / GE/LE shifting with a non-unit delta requires proof of the condition-value spacing; use a unit step or provide a provably delta-aligned discrete lattice."
        )
    }
}

private fun <V> transformToStrictPositiveCondition(
    polynomial: LinearPolynomial<V>,
    relation: Comparison,
    delta: V
): Ret<LinearPolynomial<V>> where V : RealNumber<V>, V : NumberField<V> {
    val transformed = when (relation) {
        Comparison.GT -> polynomial
        Comparison.GE -> shiftedPolynomial(polynomial, delta)
        Comparison.LT -> negatedPolynomial(polynomial)
        Comparison.LE -> shiftedPolynomial(negatedPolynomial(polynomial), delta)
        Comparison.EQ, Comparison.NE -> {
            return discreteConditionFailure(
                "关系 ${relation.symbol} 不支持离散条件转换。 / Relation ${relation.symbol} is unsupported by discrete-condition conversion."
            )
        }
    }
    return when (val validation = validateFinitePolynomial(transformed)) {
        is Ok -> Ok(transformed)
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 为关系指示线性化构造带业务间隔的正向多项式。 /
 * Build the positive-direction polynomial used by relation-indicator linearization.
 *
 * 这里的平移必须使用 strictBoundary，而不是离散步长 delta。下游的 GT 指示器
 * 使用 `q >= strictBoundary` 和 `q <= 0`；因此 GE 的 `q = d + strictBoundary`
 * 才能还原 `d >= 0`/`d <= -strictBoundary`，LE 同理。delta 只用于证明离散
 * 条件转换安全，不能直接成为带业务间隔模型的平移量。
 * The shift here must use strictBoundary rather than the discrete step delta. The
 * downstream GT indicator uses `q >= strictBoundary` and `q <= 0`; consequently
 * `q = d + strictBoundary` is required to recover GE's `d >= 0`/`d <= -strictBoundary`
 * branches, and likewise for LE. delta proves the discrete conversion but must not
 * become the shift of the business-gap model.
 */
private fun <V> transformForRelationIndicator(
    polynomial: LinearPolynomial<V>,
    relation: Comparison,
    strictBoundary: V
): Ret<LinearPolynomial<V>> where V : RealNumber<V>, V : NumberField<V> {
    val transformed = when (relation) {
        Comparison.GT -> polynomial
        Comparison.GE -> shiftedPolynomial(polynomial, strictBoundary)
        Comparison.LT -> negatedPolynomial(polynomial)
        Comparison.LE -> shiftedPolynomial(negatedPolynomial(polynomial), strictBoundary)
        Comparison.EQ, Comparison.NE -> {
            return discreteConditionFailure(
                "关系 ${relation.symbol} 不支持离散条件转换。 / Relation ${relation.symbol} is unsupported by discrete-condition conversion."
            )
        }
    }
    return when (val validation = validateFinitePolynomial(transformed)) {
        is Ok -> Ok(transformed)
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 校验离散条件的 delta 和严格边界。 / Validate delta and the strict boundary of a discrete condition.
 *
 * @param delta 离散步长，必须有限且大于零 / finite positive discrete step
 * @param strictBoundary 真、假分支之间的严格边界 / strict boundary between true and false branches
 * @return 校验结果 / validation result
 */
fun <V> validateDiscreteConditionParameters(
    delta: V,
    strictBoundary: V
): Try where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!delta.isDiscreteConditionFinite()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "delta 必须为有限值。 / delta must be finite."
            )
        }
        val zero = delta - delta
        if (delta.compareTo(zero) <= 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "delta 必须大于 0。 / delta must be greater than 0."
            )
        }
        if (!strictBoundary.isDiscreteConditionFinite()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "strictBoundary 必须为有限值。 / strictBoundary must be finite."
            )
        }
        val strictZero = strictBoundary - strictBoundary
        if (strictBoundary.compareTo(strictZero) <= 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "strictBoundary 必须大于 0。 / strictBoundary must be greater than 0."
            )
        }
        if (strictBoundary.compareTo(delta) > 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "strictBoundary 必须满足 strictBoundary <= delta。 / strictBoundary must satisfy strictBoundary <= delta."
            )
        }
        if (!delta.isDiscreteConditionSolverValue() ||
            !strictBoundary.isDiscreteConditionSolverValue()
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "delta 和 strictBoundary 必须为有限且可编码的 solver 值。 / delta and strictBoundary must be finite and solver-representable."
            )
        }
        ok
    } catch (_: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "delta 或 strictBoundary 校验失败。 / Failed to validate delta or strictBoundary."
        )
    }
}

private fun <V> validateRelation(relation: Comparison): Try where V : RealNumber<V>, V : NumberField<V> {
    return if (relation == Comparison.GT ||
        relation == Comparison.GE ||
        relation == Comparison.LT ||
        relation == Comparison.LE
    ) {
        ok
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "关系 ${relation.symbol} 不支持离散条件转换。 / Relation ${relation.symbol} is unsupported by discrete-condition conversion."
        )
    }
}

private fun <V> shiftedPolynomial(
    polynomial: LinearPolynomial<V>,
    shift: V
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials = polynomial.monomials,
        constant = polynomial.constant + shift
    )
}

private fun <V> negatedPolynomial(
    polynomial: LinearPolynomial<V>
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials = polynomial.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        constant = -polynomial.constant
    )
}

/**
 * 将离散关系转换为严格正数条件。 / Convert a discrete relation to a strict-positive condition.
 *
 * 转换表为 GT: `d`、GE: `d + delta`、LT: `-d`、LE: `-d + delta`。
 * The conversion table is GT: `d`, GE: `d + delta`, LT: `-d`, and LE: `-d + delta`.
 *
 * @param relation `d` 与零的离散比较关系 / discrete comparison of `d` with zero
 * @param delta 离散步长，必须有限且大于零 / finite positive discrete step
 * @return 严格正数条件多项式；参数非法时失败 / strict-positive condition polynomial, or failure for invalid arguments
 */
fun <V> LinearPolynomial<V>.toStrictPositiveCondition(
    relation: Comparison,
    delta: V
): Ret<LinearPolynomial<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        when (val validation = validateRelation<V>(relation)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val validation = validateFinitePolynomial(this)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!delta.isDiscreteConditionSolverValue()) {
            return discreteConditionFailure(
                "delta 必须为有限且可编码的 solver 值。 / delta must be finite and solver-representable."
            )
        }
        val zero = delta - delta
        if (delta.compareTo(zero) <= 0) {
            return discreteConditionFailure(
                "delta 必须大于 0。 / delta must be greater than 0."
            )
        }

        when (val validation = validateDiscreteShiftProof(
            polynomial = this,
            relation = relation,
            delta = delta
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        transformToStrictPositiveCondition(
            polynomial = this,
            relation = relation,
            delta = delta
        )
    } catch (_: RuntimeException) {
        discreteConditionFailure(
            "离散条件转换失败。 / Failed to convert the discrete condition."
        )
    }
}

/**
 * 将线性不等式转换为严格正数条件。 / Convert a linear inequality to a strict-positive condition.
 *
 * 先使用 `lhs - rhs` 形成差值，再应用离散关系转换表。
 * The method first forms `lhs - rhs`, then applies the discrete-relation conversion table.
 *
 * @param delta 离散步长，必须有限且大于零 / finite positive discrete step
 * @return 严格正数条件多项式；参数非法时失败 / strict-positive condition polynomial, or failure for invalid arguments
 */
fun <V> LinearInequality<V>.toStrictPositiveCondition(
    delta: V
): Ret<LinearPolynomial<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val difference = LinearPolynomial(
            monomials = lhs.monomials + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
            constant = lhs.constant - rhs.constant
        )
        difference.toStrictPositiveCondition(
            relation = comparison,
            delta = delta
        )
    } catch (_: RuntimeException) {
        discreteConditionFailure(
            "线性不等式差值计算失败。 / Failed to calculate the linear-inequality difference."
        )
    }
}

/**
 * 离散条件转换工厂。 / Factory for discrete-condition conversion.
 */
object DiscreteCondition {
    /**
     * 将离散多项式关系转换为严格正数条件。 / Convert a discrete polynomial relation to a strict-positive condition.
     *
     * @param condition 差值多项式 `d` / difference polynomial `d`
     * @param relation `d` 与零的比较关系 / comparison of `d` with zero
     * @param delta 离散步长 / discrete step
     * @return 严格正数条件结果 / strict-positive condition result
     */
    fun <V> toStrictPositive(
        condition: LinearPolynomial<V>,
        relation: Comparison,
        delta: V
    ): Ret<LinearPolynomial<V>> where V : RealNumber<V>, V : NumberField<V> {
        return condition.toStrictPositiveCondition(
            relation = relation,
            delta = delta
        )
    }

    /**
     * 将离散线性不等式转换为严格正数条件。 / Convert a discrete linear inequality to a strict-positive condition.
     *
     * @param inequality 线性不等式 / linear inequality
     * @param delta 离散步长 / discrete step
     * @return 严格正数条件结果 / strict-positive condition result
     */
    fun <V> toStrictPositive(
        inequality: LinearInequality<V>,
        delta: V
    ): Ret<LinearPolynomial<V>> where V : RealNumber<V>, V : NumberField<V> {
        return inequality.toStrictPositiveCondition(delta)
    }
}

/**
 * 按离散关系对标量差值执行严格正数三值判定。 / Classify a scalar difference with discrete strict-positive semantics.
 *
 * @param d 差值 / difference value
 * @param relation `d` 与零的比较关系 / comparison of `d` with zero
 * @param delta 离散步长 / discrete step
 * @param strictBoundary 严格正数边界 / strict-positive boundary
 * @return 三值判定结果 / three-valued classification result
 */
fun <V> classifyDiscreteCondition(
    d: V,
    relation: Comparison,
    delta: V,
    strictBoundary: V
): Ret<TruthValue> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        when (val validation = validateDiscreteConditionParameters(
            delta = delta,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!d.isDiscreteConditionFinite()) {
            return discreteConditionFailure(
                "条件值 d 必须为有限值。 / Condition value d must be finite."
            )
        }

        // Classification uses the relation matrix directly. The delta is needed by
        // polynomial conversion, while a scalar value has no variable-domain metadata. /
        // 标量判定直接使用关系矩阵；delta 用于多项式转换，而标量本身没有变量域元数据。
        classify(
            d = d,
            relation = relation,
            strictBoundary = strictBoundary
        )
    } catch (_: RuntimeException) {
        discreteConditionFailure(
            "离散条件判定失败。 / Failed to classify the discrete condition."
        )
    }
}

/**
 * 用有限范围归一化离散关系，供条件函数构建线性约束。 / Normalize a discrete relation over finite bounds for conditional-function linearization.
 *
 * @param poly 差值多项式 / difference polynomial
 * @param relation 差值与零的离散关系 / discrete relation of the difference to zero
 * @param bounds 差值有限范围 / finite difference range
 * @param delta 离散步长 / discrete step
 * @param strictBoundary 严格正数边界 / strict-positive boundary
 * @return 严格正数多项式、范围和可折叠分支 / strict-positive polynomial, bounds, and foldable branch
 */
internal fun <V> normalizeDiscreteCondition(
    poly: LinearPolynomial<V>,
    relation: Comparison,
    bounds: ConditionBounds<V>,
    delta: V,
    strictBoundary: V
): Ret<DiscreteConditionLinearization<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
    when (val validation = validateDiscreteConditionParameters(
        delta = delta,
        strictBoundary = strictBoundary
    )) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    when (val validation = validateRelation<V>(relation)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    if (!bounds.lower.isDiscreteConditionSolverValue() ||
        !bounds.upper.isDiscreteConditionSolverValue()
    ) {
        return discreteConditionFailure(
            "条件范围端点必须为有限且可编码的 solver 值。 / Condition range endpoints must be finite and solver-representable."
        )
    }
    when (val validation = validateFinitePolynomial(poly)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    if (bounds.lower.compareTo(bounds.upper) > 0) {
        return discreteConditionFailure(
            "条件范围要求 lower <= upper。 / Condition bounds require lower <= upper."
        )
    }
    val singleBranchFold = try {
        boundsAvoidDiscreteConditionGap(
            relation = relation,
            bounds = bounds,
            strictBoundary = strictBoundary
        )
    } catch (_: RuntimeException) {
        null
    }
    val needsIntegerValuedCondition = delta.compareTo(strictBoundary) > 0 &&
        when (relation) {
            Comparison.GT, Comparison.LT -> singleBranchFold == null
            Comparison.GE, Comparison.LE -> singleBranchFold == null
            Comparison.EQ, Comparison.NE -> false
        }
    if (needsIntegerValuedCondition) {
        when (val validation = validateDiscreteConditionPolynomial(poly)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
    }
    if ((relation == Comparison.GT || relation == Comparison.LT) &&
        singleBranchFold == null &&
        (delta.compareTo(strictBoundary) > 0 || isDiscreteConditionDomainPolynomial(poly)) &&
        !hasSafeStrictPositiveLatticeProof(
            polynomial = poly,
            relation = relation,
            strictBoundary = strictBoundary
        )
    ) {
        return discreteConditionFailure(
            "GT/LT 使用离散 delta 前必须证明正分支避开 strictBoundary 间隔，不能只验证变量和系数为整数。 / GT/LT with a discrete delta must prove that the positive branch avoids the strictBoundary gap; integer variables and coefficients alone are insufficient."
        )
    }
    if ((relation == Comparison.GE || relation == Comparison.LE) &&
        singleBranchFold == null &&
        (delta.compareTo(strictBoundary) > 0 || isDiscreteConditionDomainPolynomial(poly)) &&
        !hasSafeDiscreteShiftProof(
            polynomial = poly,
            relation = relation,
            delta = delta
        )
    ) {
        return discreteConditionFailure(
            "非单位 delta 平移 GE/LE 条件缺少离散语义证明，不能安全归一化。 / A non-unit delta shift for a GE/LE condition lacks a discrete-semantic proof and cannot be normalized safely."
        )
    }
    val polynomial = when (val result = transformForRelationIndicator(
        polynomial = poly,
        relation = relation,
        strictBoundary = strictBoundary
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val transformedBounds = when (relation) {
        Comparison.GT -> bounds
        Comparison.GE -> ConditionBounds(
            lower = bounds.lower + strictBoundary,
            upper = bounds.upper + strictBoundary
        )
        Comparison.LT -> ConditionBounds(
            lower = -bounds.upper,
            upper = -bounds.lower
        )
        Comparison.LE -> ConditionBounds(
            lower = -bounds.upper + strictBoundary,
            upper = -bounds.lower + strictBoundary
        )
        Comparison.EQ, Comparison.NE -> {
            return discreteConditionFailure(
                "关系 ${relation.symbol} 不支持离散条件范围归一化。 / Relation ${relation.symbol} is unsupported by discrete-condition range normalization."
            )
        }
    }
    if (!transformedBounds.lower.isDiscreteConditionSolverValue() ||
        !transformedBounds.upper.isDiscreteConditionSolverValue()
    ) {
        return discreteConditionFailure(
            "离散条件范围转换产生非有限或不可编码端点。 / Discrete-condition range conversion produced a non-finite or unrepresentable endpoint."
        )
    }
    val zero = strictBoundary - strictBoundary
    val fixedValue = singleBranchFold ?: when {
        transformedBounds.lower.compareTo(strictBoundary) >= 0 -> TruthValue.True
        transformedBounds.upper.compareTo(zero) <= 0 -> TruthValue.False
        else -> null
    }
    if (fixedValue == null) {
        when (val validation = validateConditionBounds(
            bounds = transformedBounds,
            relation = Comparison.GT,
            strictBoundary = strictBoundary
        )) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
    }
    val trueBranchPossible = if (fixedValue == null) {
        transformedBounds.upper.compareTo(strictBoundary) >= 0
    } else {
        fixedValue == TruthValue.True
    }
    val falseBranchPossible = if (fixedValue == null) {
        transformedBounds.lower.compareTo(zero) <= 0
    } else {
        fixedValue == TruthValue.False
    }
    Ok(
        DiscreteConditionLinearization(
            polynomial = polynomial,
            bounds = transformedBounds,
            fixedValue = fixedValue,
            trueBranchPossible = trueBranchPossible,
            falseBranchPossible = falseBranchPossible
        )
    )
    } catch (_: RuntimeException) {
        discreteConditionFailure(
            "离散条件归一化失败。 / Failed to normalize the discrete condition."
        )
    }
}

/** 构造固定辅助变量等式。 / Build an equality that fixes an auxiliary variable. */
internal fun <V> fixedVariableEquality(
    variable: AbstractVariableItem<*, *>,
    value: V,
    zero: V,
    one: V,
    name: String
): LinearInequality<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearInequality(
        lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(one, variable)),
            constant = zero
        ),
        rhs = LinearPolynomial(emptyList(), value),
        comparison = Comparison.EQ,
        name = name
    )
}
