@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

/** 复合线性函数符号 / Composite linear function symbols */

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Object as MechanismObject
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.*

internal fun <V> zeroPolynomial(converter: IntoValue<V>): LinearPolynomial<V>
    where V : RealNumber<V>, V : NumberField<V> = LinearPolynomial(emptyList(), converter.zero)

internal fun <V> snapshotLinearPolynomial(polynomial: LinearPolynomial<V>): LinearPolynomial<V>
    where V : RealNumber<V>, V : NumberField<V> = LinearPolynomial(polynomial.monomials.toList(), polynomial.constant)

internal fun <V> differencePolynomial(
    left: LinearPolynomial<V>,
    right: LinearPolynomial<V>
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> = LinearPolynomial(
    left.monomials + right.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
    left.constant - right.constant
)

internal fun <V> scalePolynomial(
    polynomial: LinearPolynomial<V>,
    coefficient: V
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> = LinearPolynomial(
    polynomial.monomials.map { LinearMonomial(it.coefficient * coefficient, it.symbol) },
    polynomial.constant * coefficient
)

internal fun <V> sumLinearPolynomials(
    polynomials: Iterable<LinearPolynomial<V>>,
    zero: V
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    val monomials = mutableListOf<LinearMonomial<V>>()
    var constant = zero
    for (polynomial in polynomials) {
        monomials += polynomial.monomials
        constant += polynomial.constant
    }
    return LinearPolynomial(monomials, constant)
}

internal fun <V> validateFinitePolynomial(
    polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    operation: String
): Try where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (polynomial.finiteBounds(converter) != null) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "$operation 需要有限输入范围。 / $operation requires finite input bounds."
            )
        }
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "$operation 输入范围验证失败：${error.message ?: "无效范围"} / $operation failed to validate input bounds."
        )
    }
}

internal fun <V> validateFinitePolynomials(
    polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    operation: String
): Try where V : RealNumber<V>, V : NumberField<V> {
    for (polynomial in polynomials) {
        when (val result = validateFinitePolynomial(polynomial, converter, operation)) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

internal data class CapturedLinearPolynomialBounds<V>(
    val polynomial: LinearPolynomial<V>,
    val lower: V,
    val upper: V
) where V : RealNumber<V>, V : NumberField<V>

internal fun <V> captureFinitePolynomialBounds(
    polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    operation: String
): Ret<List<CapturedLinearPolynomialBounds<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val captured = polynomials.map(::snapshotLinearPolynomial).map { polynomial ->
            val bounds = polynomial.finiteBounds(converter)
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "$operation 需要有限输入范围。 / $operation requires finite input bounds."
                )
            CapturedLinearPolynomialBounds(polynomial, bounds.lower, bounds.upper)
        }
        Ok(captured)
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "$operation 输入范围验证失败：${error.message ?: "无效范围"} / $operation failed to validate input bounds."
        )
    }
}

internal fun <V> validateCapturedPolynomialBounds(
    snapshots: List<CapturedLinearPolynomialBounds<V>>,
    converter: IntoValue<V>,
    operation: String
): Try where V : RealNumber<V>, V : NumberField<V> {
    return try {
        for (snapshot in snapshots) {
            val current = snapshot.polynomial.finiteBounds(converter) ?: return Failed(
                ErrorCode.IllegalArgument,
                "$operation 输入范围不再有限。 / $operation input bounds are no longer finite."
            )
            if (current.lower.compareTo(snapshot.lower) < 0 || current.upper.compareTo(snapshot.upper) > 0) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "$operation 输入范围在构造后扩大。 / $operation input bounds expanded after construction."
                )
            }
        }
        ok
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "$operation 输入范围验证失败：${error.message ?: "无效范围"} / $operation failed to validate current input bounds."
        )
    }
}

internal fun <T> checkedFunctionConstruction(
    operation: String,
    validation: Try,
    constructor: () -> T
): Ret<T> {
    return when (validation) {
        is Ok -> try {
            Ok(constructor())
        } catch (error: RuntimeException) {
            Failed(
                ErrorCode.IllegalArgument,
                "$operation 构造失败：${error.message ?: "无效参数"} / Failed to construct $operation."
            )
        }
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

internal fun <V, T> checkedFunctionWithBounds(
    operation: String,
    snapshots: Ret<List<CapturedLinearPolynomialBounds<V>>>,
    constructor: (List<CapturedLinearPolynomialBounds<V>>) -> T
): Ret<T> where V : RealNumber<V>, V : NumberField<V> {
    return when (snapshots) {
        is Ok -> checkedFunctionConstruction(operation, ok) { constructor(snapshots.value) }
        is Failed -> Failed(snapshots.error)
        is Fatal -> Fatal(snapshots.errors)
    }
}

private class ConstraintBufferModel<V>(
    private val delegate: AbstractLinearMechanismModel<V>
) : AbstractLinearMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
    val pending = mutableListOf<LinearInequality<V>>()

    override val name: String get() = delegate.name
    override val constraints: List<Constraint<V, *>> get() = emptyList()
    override val objectFunction: MechanismObject get() = delegate.objectFunction
    override val tokens: AbstractTokenTable<V> get() = delegate.tokens
    override val identityRegistry get() = delegate.identityRegistry

    override fun addConstraint(
        relation: LinearInequality<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        pending += relation
        return ok
    }

    override fun rollbackConstraintsTo(size: Int): Try {
        if (size < 0 || size > pending.size) {
            return Failed(ErrorCode.IllegalArgument, "无效的临时约束回滚位置。 / Invalid temporary constraint rollback position.")
        }
        pending.subList(size, pending.size).clear()
        return ok
    }
}

internal fun <V> registerFunctionsAtomically(
    model: AbstractLinearMechanismModel<V>,
    functions: List<MathFunctionSymbol<V>>
): Try where V : RealNumber<V>, V : NumberField<V> {
    val buffer = ConstraintBufferModel(model)
    for (function in functions) {
        val result = try {
            function.registerConstraints(buffer)
        } catch (error: RuntimeException) {
            return Failed(
                ErrorCode.ApplicationError,
                "注册复合函数约束失败：${error.message ?: "约束无效"} / Failed to validate composite function constraints."
            )
        }
        when (result) {
            is Ok -> Unit
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return addConstraints(model, buffer.pending) ?: ok
}

/**
 * 多个精确线性化子函数的公共基类，统一辅助变量和原子约束注册。
 * Base for exact linear compositions, with shared auxiliary registration and atomic constraint writes.
 *
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
abstract class CompositeMathFunction<V> internal constructor(
    override var name: String,
    override var displayName: String?,
    private val inputBounds: List<CapturedLinearPolynomialBounds<V>>,
    private val boundConverter: IntoValue<V>
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    protected abstract val innerFunctions: List<MathFunctionSymbol<V>>

    private fun validateInputBounds(): Try =
        validateCapturedPolynomialBounds(inputBounds, boundConverter, "CompositeMathFunction")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = innerFunctions.flatMap { it.helperVariables }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = validateInputBounds()) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val validation = validateInputBounds()) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return registerFunctionsAtomically(model, innerFunctions)
    }
}

/**
 * 精确正部函数 `max(input, 0)`。
 * Exact positive-part function `max(input, 0)`.
 *
 * @property input 输入线性多项式 / input linear polynomial
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class PositivePartFunction<V> private constructor(
    val input: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val maximum = MaxFunction(
        polynomials = listOf(input, zeroPolynomial(converter)),
        converter = converter,
        name = "${name}_max"
    )

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = listOf(maximum)

    override val resultPolynomial: LinearPolynomial<V> get() = maximum.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        val value = input.evaluateWith(values) ?: return null
        return if (value.compareTo(converter.zero) > 0) value else converter.zero
    }

    companion object {
        /** 创建正部函数，输入范围必须有限。 / Create a positive-part function with finite input bounds.
         *
         * @param input 输入线性多项式 / input linear polynomial
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            converter: IntoValue<V>,
            name: String = "positive_part",
            displayName: String? = null
        ): Ret<PositivePartFunction<V>> where V : RealNumber<V>, V : NumberField<V> = checkedFunctionWithBounds(
            operation = "PositivePartFunction",
            snapshots = captureFinitePolynomialBounds(listOf(input), converter, "PositivePartFunction")
        ) { inputBounds ->
            PositivePartFunction(
                input = inputBounds.single().polynomial,
                converter = converter,
                name = name,
                displayName = displayName,
                inputBounds = inputBounds
            )
        }
    }
}

/**
 * 精确截断函数 `min(max(input, lower), upper)`。
 * Exact clamp function `min(max(input, lower), upper)`.
 *
 * @property input 输入线性多项式 / input linear polynomial
 * @property lower 下界 / lower bound
 * @property upper 上界 / upper bound
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class ClampFunction<V> private constructor(
    val input: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val maximum = MaxFunction(
        polynomials = listOf(input, LinearPolynomial(emptyList(), lower)),
        converter = converter,
        name = "${name}_lower"
    )
    private val minimum = MinFunction(
        polynomials = listOf(maximum.resultPolynomial, LinearPolynomial(emptyList(), upper)),
        converter = converter,
        name = "${name}_upper"
    )

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = listOf(maximum, minimum)

    override val resultPolynomial: LinearPolynomial<V> get() = minimum.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        val value = input.evaluateWith(values) ?: return null
        return when {
            value.compareTo(lower) < 0 -> lower
            value.compareTo(upper) > 0 -> upper
            else -> value
        }
    }

    companion object {
        /** 创建截断函数；要求有限输入范围及 `lower <= upper`。 / Create a clamp function with finite input bounds and `lower <= upper`.
         *
         * @param input 输入线性多项式 / input linear polynomial
         * @param lower 输出下界 / output lower bound
         * @param upper 输出上界 / output upper bound
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            lower: V,
            upper: V,
            converter: IntoValue<V>,
            name: String = "clamp",
            displayName: String? = null
        ): Ret<ClampFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val snapshots = when {
                !lower.isFinite() || !upper.isFinite() || lower.compareTo(upper) > 0 -> Failed(
                    ErrorCode.IllegalArgument,
                    "Clamp 上下界必须有限且 lower <= upper。 / Clamp bounds must be finite and ordered."
                )
                else -> captureFinitePolynomialBounds(listOf(input), converter, "ClampFunction")
            }
            return when (snapshots) {
                is Ok -> checkedFunctionConstruction("ClampFunction", ok) {
                    ClampFunction(
                        input = snapshots.value.single().polynomial,
                        lower = lower,
                        upper = upper,
                        converter = converter,
                        name = name,
                        displayName = displayName,
                        inputBounds = snapshots.value
                    )
                }
                is Failed -> Failed(snapshots.error)
                is Fatal -> Fatal(snapshots.errors)
            }
        }
    }
}

/**
 * 精确死区惩罚 `max(abs(input) - delta, 0)`。
 * Exact dead-zone penalty `max(abs(input) - delta, 0)`.
 *
 * @property input 输入线性多项式 / input linear polynomial
 * @property delta 非负死区宽度 / non-negative dead-zone width
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class DeadZoneFunction<V> private constructor(
    val input: LinearPolynomial<V>,
    val delta: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val absolute = AbsFunction(input, converter, name = "${name}_abs")
    private val excess = differencePolynomial(
        absolute.resultPolynomial,
        LinearPolynomial(emptyList(), delta)
    )
    private val positivePart = MaxFunction(
        polynomials = listOf(excess, zeroPolynomial(converter)),
        converter = converter,
        name = "${name}_positive_part"
    )

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = listOf(absolute, positivePart)

    override val resultPolynomial: LinearPolynomial<V> get() = positivePart.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        val value = input.evaluateWith(values) ?: return null
        val excessValue = value.abs() - delta
        return if (excessValue.compareTo(converter.zero) > 0) excessValue else converter.zero
    }

    companion object {
        /** 创建死区函数；要求有限输入范围及非负有限 delta。 / Create a dead-zone function with finite input bounds and finite non-negative delta.
         *
         * @param input 输入线性多项式 / input linear polynomial
         * @param delta 死区宽度 / dead-zone width
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            delta: V,
            converter: IntoValue<V>,
            name: String = "dead_zone",
            displayName: String? = null
        ): Ret<DeadZoneFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val validation = if (!delta.isFinite() || delta.compareTo(converter.zero) < 0) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "DeadZone delta 必须有限且非负。 / DeadZone delta must be finite and non-negative."
                )
            } else {
                captureFinitePolynomialBounds(listOf(input), converter, "DeadZoneFunction")
            }
            return when (validation) {
                is Ok -> checkedFunctionConstruction("DeadZoneFunction", ok) {
                    DeadZoneFunction(
                        input = validation.value.single().polynomial,
                        delta = delta,
                        converter = converter,
                        name = name,
                        displayName = displayName,
                        inputBounds = validation.value
                    )
                }
                is Failed -> Failed(validation.error)
                is Fatal -> Fatal(validation.errors)
            }
        }
    }
}

/**
 * L1 距离：两组等长线性向量之差的绝对值之和；空向量距离为 0。
 * L1 distance: sum of absolute component differences; the empty-vector distance is 0.
 *
 * @property left 左侧向量 / left vector
 * @property right 右侧向量 / right vector
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class L1DistanceFunction<V> private constructor(
    val left: List<LinearPolynomial<V>>,
    val right: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val absoluteValues = left.indices.map { index ->
        AbsFunction(
            polynomial = differencePolynomial(left[index], right[index]),
            converter = converter,
            name = "${name}_abs_$index"
        )
    }

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = absoluteValues

    override val resultPolynomial: LinearPolynomial<V>
        get() = sumLinearPolynomials(absoluteValues.map { it.resultPolynomial }, converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var result = converter.zero
        for (index in left.indices) {
            val leftValue = left[index].evaluateWith(values) ?: return null
            val rightValue = right[index].evaluateWith(values) ?: return null
            result += (leftValue - rightValue).abs()
        }
        return result
    }

    companion object {
        /** 创建 L1 距离函数；向量必须等长且具有有限范围。 / Create an L1 distance function for equal-length vectors with finite bounds.
         *
         * @param left 左侧向量 / left vector
         * @param right 右侧向量 / right vector
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            left: List<LinearPolynomial<V>>,
            right: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            name: String = "l1_distance",
            displayName: String? = null
        ): Ret<L1DistanceFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val snapshots = if (left.size != right.size) {
                Failed(ErrorCode.IllegalArgument, "L1 距离向量必须等长。 / L1 distance vectors must have equal dimensions.")
            } else {
                captureFinitePolynomialBounds(left + right, converter, "L1DistanceFunction")
            }
            return when (snapshots) {
                is Ok -> checkedFunctionConstruction("L1DistanceFunction", ok) {
                    L1DistanceFunction(
                        left = snapshots.value.take(left.size).map { it.polynomial },
                        right = snapshots.value.drop(left.size).map { it.polynomial },
                        converter = converter,
                        name = name,
                        displayName = displayName,
                        inputBounds = snapshots.value
                    )
                }
                is Failed -> Failed(snapshots.error)
                is Fatal -> Fatal(snapshots.errors)
            }
        }
    }
}

/**
 * L∞ 距离：两组等长非空线性向量之差的最大绝对值。
 * L-infinity distance: maximum absolute component difference for equal-length non-empty vectors.
 *
 * @property left 左侧向量 / left vector
 * @property right 右侧向量 / right vector
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class LInfinityDistanceFunction<V> private constructor(
    val left: List<LinearPolynomial<V>>,
    val right: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val absoluteValues = left.indices.map { index ->
        AbsFunction(
            polynomial = differencePolynomial(left[index], right[index]),
            converter = converter,
            name = "${name}_abs_$index"
        )
    }
    private val maximum = MaxFunction(
        polynomials = absoluteValues.map { it.resultPolynomial },
        converter = converter,
        name = "${name}_max"
    )

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = absoluteValues + maximum

    override val resultPolynomial: LinearPolynomial<V> get() = maximum.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        var result: V? = null
        for (index in left.indices) {
            val leftValue = left[index].evaluateWith(values) ?: return null
            val rightValue = right[index].evaluateWith(values) ?: return null
            val distance = (leftValue - rightValue).abs()
            if (result == null || distance.compareTo(result) > 0) result = distance
        }
        return result
    }

    companion object {
        /** 创建 L∞ 距离函数；向量必须等长、非空且具有有限范围。 / Create an L-infinity distance function for equal-length, non-empty vectors with finite bounds.
         *
         * @param left 左侧向量 / left vector
         * @param right 右侧向量 / right vector
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            left: List<LinearPolynomial<V>>,
            right: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            name: String = "l_infinity_distance",
            displayName: String? = null
        ): Ret<LInfinityDistanceFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val snapshots = if (left.isEmpty() || left.size != right.size) {
                Failed(ErrorCode.IllegalArgument, "L∞ 距离向量必须等长且非空。 / L-infinity distance vectors must have equal non-zero dimensions.")
            } else {
                captureFinitePolynomialBounds(left + right, converter, "LInfinityDistanceFunction")
            }
            return when (snapshots) {
                is Ok -> checkedFunctionConstruction("LInfinityDistanceFunction", ok) {
                    LInfinityDistanceFunction(
                        left = snapshots.value.take(left.size).map { it.polynomial },
                        right = snapshots.value.drop(left.size).map { it.polynomial },
                        converter = converter,
                        name = name,
                        displayName = displayName,
                        inputBounds = snapshots.value
                    )
                }
                is Failed -> Failed(snapshots.error)
                is Fatal -> Fatal(snapshots.errors)
            }
        }
    }
}

/**
 * 极差函数 `max(inputs) - min(inputs)`。
 * Range function `max(inputs) - min(inputs)`.
 *
 * @property inputs 输入线性多项式 / input linear polynomials
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class RangeFunction<V> private constructor(
    val inputs: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val maximum = MaxFunction(inputs, converter = converter, name = "${name}_max")
    private val minimum = MinFunction(inputs, converter = converter, name = "${name}_min")

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = listOf(maximum, minimum)

    override val resultPolynomial: LinearPolynomial<V>
        get() = differencePolynomial(maximum.resultPolynomial, minimum.resultPolynomial)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val evaluated = inputs.map { it.evaluateWith(values) ?: return null }
        var minimumValue = evaluated.first()
        var maximumValue = evaluated.first()
        for (value in evaluated.drop(1)) {
            if (value.compareTo(minimumValue) < 0) minimumValue = value
            if (value.compareTo(maximumValue) > 0) maximumValue = value
        }
        return maximumValue - minimumValue
    }

    companion object {
        /** 创建极差函数；要求输入非空且范围有限。 / Create a range function for non-empty inputs with finite bounds.
         *
         * @param inputs 输入线性多项式 / input linear polynomials
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            inputs: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            name: String = "range",
            displayName: String? = null
        ): Ret<RangeFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val snapshots = if (inputs.isEmpty()) {
                Failed(ErrorCode.IllegalArgument, "Range 输入不能为空。 / Range inputs cannot be empty.")
            } else {
                captureFinitePolynomialBounds(inputs, converter, "RangeFunction")
            }
            return when (snapshots) {
                is Ok -> checkedFunctionConstruction("RangeFunction", ok) {
                    RangeFunction(
                        inputs = snapshots.value.map { it.polynomial },
                        converter = converter,
                        name = name,
                        displayName = displayName,
                        inputBounds = snapshots.value
                    )
                }
                is Failed -> Failed(snapshots.error)
                is Fatal -> Fatal(snapshots.errors)
            }
        }
    }
}
