@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

/** 离散 CVaR 函数符号 / Discrete CVaR function symbols */

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

private fun <V> validateCvarInputs(
    losses: List<LinearPolynomial<V>>,
    probabilities: List<V>,
    alpha: V,
    converter: IntoValue<V>
): Ret<List<CapturedLinearPolynomialBounds<V>>> where V : RealNumber<V>, V : NumberField<V> {
    if (losses.isEmpty() || losses.size != probabilities.size) {
        return Failed(
            ErrorCode.IllegalArgument,
            "CVaR 损失和概率必须非空且数量相同。 / CVaR losses and probabilities must be non-empty and have equal sizes."
        )
    }
    if (!alpha.isFinite() || alpha.compareTo(converter.zero) < 0 || alpha.compareTo(converter.one) >= 0) {
        return Failed(ErrorCode.IllegalArgument, "CVaR alpha 必须属于 [0, 1)。 / CVaR alpha must be in [0, 1).")
    }
    if (probabilities.any { !it.isFinite() || it.compareTo(converter.zero) < 0 }) {
        return Failed(ErrorCode.IllegalArgument, "CVaR 概率必须有限且非负。 / CVaR probabilities must be finite and non-negative.")
    }
    var probabilitySum = converter.zero
    for (probability in probabilities) probabilitySum += probability
    if (probabilitySum.compareTo(converter.one) != 0) {
        return Failed(ErrorCode.IllegalArgument, "CVaR 概率之和必须为 1。 / CVaR probabilities must sum to one.")
    }
    val snapshots = captureFinitePolynomialBounds(losses, converter, "CvarFunction")
    if (snapshots !is Ok) return snapshots
    return try {
        val denominator = converter.one - alpha
        val globalLower = snapshots.value.map { it.lower }.reduce { current, value ->
            if (value.compareTo(current) < 0) value else current
        }
        val globalUpper = snapshots.value.map { it.upper }.reduce { current, value ->
            if (value.compareTo(current) > 0) value else current
        }
        if (!(globalUpper - globalLower).isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "CVaR 损失范围宽度不可编码。 / CVaR loss range width is not finite.")
        }
        for (bounds in snapshots.value) {
            if (!(bounds.upper - globalLower).isFinite()) {
                return Failed(ErrorCode.IllegalArgument, "CVaR 过量变量上界不可编码。 / CVaR excess upper bound is not finite.")
            }
        }
        val candidateLowerBounds = mutableListOf<V>()
        val candidateUpperBounds = mutableListOf<V>()
        for (threshold in snapshots.value) {
            var candidateLower = threshold.lower
            var candidateUpper = threshold.upper
            for ((index, loss) in snapshots.value.withIndex()) {
                val differenceLower = loss.lower - threshold.upper
                val differenceUpper = loss.upper - threshold.lower
                if (!differenceLower.isFinite() || !differenceUpper.isFinite()) {
                    return Failed(ErrorCode.IllegalArgument, "CVaR 损失差值范围不可编码。 / CVaR loss-difference bounds are not finite.")
                }
                val excessUpper = if (differenceUpper.compareTo(converter.zero) > 0) differenceUpper else converter.zero
                val excessLower = if (differenceLower.compareTo(converter.zero) < 0) differenceLower else converter.zero
                if (!(excessUpper - excessLower).isFinite()) {
                    return Failed(ErrorCode.IllegalArgument, "CVaR 正部范围宽度不可编码。 / CVaR positive-part range width is not finite.")
                }
                val weight = probabilities[index] / denominator
                val weightedLower = weight * excessLower
                val weightedUpper = weight * excessUpper
                if (!weight.isFinite() || !weightedLower.isFinite() || !weightedUpper.isFinite()) {
                    return Failed(ErrorCode.IllegalArgument, "CVaR 线性化系数不可编码。 / CVaR linearization coefficients are not finite.")
                }
                candidateLower += weightedLower
                candidateUpper += weightedUpper
            }
            if (!candidateLower.isFinite() || !candidateUpper.isFinite()) {
                return Failed(ErrorCode.IllegalArgument, "CVaR 候选阈值范围不可编码。 / CVaR candidate threshold bounds are not finite.")
            }
            candidateLowerBounds += candidateLower
            candidateUpperBounds += candidateUpper
        }
        val lowest = candidateLowerBounds.reduce { current, value ->
            if (value.compareTo(current) < 0) value else current
        }
        val highest = candidateUpperBounds.reduce { current, value ->
            if (value.compareTo(current) > 0) value else current
        }
        if ((highest - lowest).isFinite()) snapshots else Failed(
            ErrorCode.IllegalArgument,
            "CVaR 候选范围宽度不可编码。 / CVaR candidate range width is not finite."
        )
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "CVaR 范围验证失败：${error.message ?: "无效范围"} / Failed to validate CVaR bounds."
        )
    }
}

private fun <V> cvarValue(
    losses: List<V>,
    probabilities: List<V>,
    alpha: V,
    converter: IntoValue<V>
): V where V : RealNumber<V>, V : NumberField<V> {
    val denominator = converter.one - alpha
    var minimum: V? = null
    for (threshold in losses) {
        var candidate = threshold
        for (index in losses.indices) {
            val excess = losses[index] - threshold
            if (excess.compareTo(converter.zero) > 0) {
                candidate += probabilities[index] * excess / denominator
            }
        }
        if (minimum == null || candidate.compareTo(minimum) < 0) minimum = candidate
    }
    return minimum!!
}

/**
 * 离散概率加权损失的精确 CVaR；枚举每个损失值作为阈值并精确取最小值。
 * Exact CVaR for a discrete probability-weighted loss distribution, minimizing over each observed loss threshold.
 *
 * @property losses 离散情景损失线性多项式 / scenario loss linear polynomials
 * @property probabilities 与损失对应的概率 / probabilities paired with losses
 * @property alpha 置信水平，范围为 [0, 1) / confidence level in [0, 1)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class CvarFunction<V> private constructor(
    val losses: List<LinearPolynomial<V>>,
    val probabilities: List<V>,
    val alpha: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : CompositeMathFunction<V>(name, displayName, inputBounds, converter) where V : RealNumber<V>, V : NumberField<V> {
    private val positiveParts = losses.indices.flatMap { thresholdIndex ->
        losses.indices.map { lossIndex ->
            val difference = differencePolynomial(losses[lossIndex], losses[thresholdIndex])
            MaxFunction(
                polynomials = listOf(difference, zeroPolynomial(converter)),
                converter = converter,
                name = "${name}_excess_${thresholdIndex}_$lossIndex"
            )
        }
    }

    private val candidates = losses.indices.map { thresholdIndex ->
        val denominator = converter.one - alpha
        val tail = positiveParts.subList(thresholdIndex * losses.size, (thresholdIndex + 1) * losses.size)
            .mapIndexed { lossIndex, positivePart ->
                scalePolynomial(
                    positivePart.resultPolynomial,
                    probabilities[lossIndex] / denominator
                )
            }
        val weightedTail = sumLinearPolynomials(tail, converter.zero)
        LinearPolynomial(
            losses[thresholdIndex].monomials + weightedTail.monomials,
            losses[thresholdIndex].constant + weightedTail.constant
        )
    }
    private val minimum = MinFunction(candidates, converter = converter, name = "${name}_minimum")

    override val innerFunctions: List<MathFunctionSymbol<V>> get() = positiveParts + minimum

    override val resultPolynomial: LinearPolynomial<V> get() = minimum.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        val evaluatedLosses = losses.map { it.evaluateWith(values) ?: return null }
        return cvarValue(evaluatedLosses, probabilities, alpha, converter)
    }

    companion object {
        /** 校验并创建精确离散 CVaR 函数；要求损失有有限范围。 / Validate and create an exact discrete CVaR function with finite loss bounds.
         *
         * @param losses 离散情景损失 / discrete scenario losses
         * @param probabilities 情景概率 / scenario probabilities
         * @param alpha 置信水平 / confidence level
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            losses: List<LinearPolynomial<V>>,
            probabilities: List<V>,
            alpha: V,
            converter: IntoValue<V>,
            name: String = "cvar",
            displayName: String? = null
        ): Ret<CvarFunction<V>> where V : RealNumber<V>, V : NumberField<V> = checkedFunctionWithBounds(
            operation = "CvarFunction",
            snapshots = validateCvarInputs(losses, probabilities, alpha, converter)
        ) { inputBounds ->
            CvarFunction(
                losses = inputBounds.map { it.polynomial },
                probabilities = probabilities.toList(),
                alpha = alpha,
                converter = converter,
                name = name,
                displayName = displayName,
                inputBounds = inputBounds
            )
        }
    }
}

/**
 * CVaR 的 LP epigraph 表示，结果只保证在最小化或用于上界约束时收紧。
 * LP epigraph representation of CVaR; it is tight only when minimized or used in an upper-bound constraint.
 *
 * @property losses 离散情景损失线性多项式 / scenario loss linear polynomials
 * @property probabilities 与损失对应的概率 / probabilities paired with losses
 * @property alpha 置信水平，范围为 [0, 1) / confidence level in [0, 1)
 * @property thresholdVar CVaR 阈值辅助变量 / CVaR threshold auxiliary variable
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class CvarEpigraphFunction<V> private constructor(
    val losses: List<LinearPolynomial<V>>,
    val probabilities: List<V>,
    val alpha: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    private val inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val lowerBound = inputBounds.map { it.lower }.reduce { current, value -> if (value.compareTo(current) < 0) value else current }
    private val upperBound = inputBounds.map { it.upper }.reduce { current, value -> if (value.compareTo(current) > 0) value else current }

    val thresholdVar: RealVar = RealVar("${name}_threshold")
    private val excessVars: List<URealVar> = losses.indices.map { URealVar("${name}_excess_$it") }

    init {
        thresholdVar.range.geq(converter.fromValue(lowerBound))
        thresholdVar.range.leq(converter.fromValue(upperBound))
        for (index in losses.indices) {
            val upper = inputBounds[index].upper - lowerBound
            excessVars[index].range.leq(converter.fromValue(if (upper.compareTo(converter.zero) > 0) upper else converter.zero))
        }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>> get() = listOf(thresholdVar) + excessVars

    override val resultPolynomial: LinearPolynomial<V>
        get() {
            val denominator = converter.one - alpha
            val excess = excessVars.mapIndexed { index, variable ->
                LinearMonomial(probabilities[index] / denominator, variable)
            }
            return LinearPolynomial(
                listOf(LinearMonomial(converter.one, thresholdVar)) + excess,
                converter.zero
            )
        }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val evaluatedLosses = losses.map { it.evaluateWith(values) ?: return null }
        return cvarValue(evaluatedLosses, probabilities, alpha, converter)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try =
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "CvarEpigraphFunction")) {
            is Ok -> when (val result = tokens.add(helperVariables)) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "CvarEpigraphFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val rows = losses.mapIndexed { index, loss ->
            LinearInequality(
                lhs = LinearPolynomial(
                    loss.monomials +
                        LinearMonomial(-converter.one, thresholdVar) +
                        LinearMonomial(-converter.one, excessVars[index]),
                    loss.constant
                ),
                rhs = LinearPolynomial(emptyList(), converter.zero),
                comparison = Comparison.LE,
                name = "${name}_excess_lb_$index"
            )
        }
        return addConstraints(model, rows) ?: ok
    }

    companion object {
        /** 校验并创建仅用于最小化或上界约束的 CVaR epigraph。 / Validate and create a CVaR epigraph for minimization or upper-bound constraints.
         *
         * @param losses 离散情景损失 / discrete scenario losses
         * @param probabilities 情景概率 / scenario probabilities
         * @param alpha 置信水平 / confidence level
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            losses: List<LinearPolynomial<V>>,
            probabilities: List<V>,
            alpha: V,
            converter: IntoValue<V>,
            name: String = "cvar_epigraph",
            displayName: String? = null
        ): Ret<CvarEpigraphFunction<V>> where V : RealNumber<V>, V : NumberField<V> = checkedFunctionWithBounds(
            operation = "CvarEpigraphFunction",
            snapshots = validateCvarInputs(losses, probabilities, alpha, converter)
        ) { inputBounds ->
            CvarEpigraphFunction(
                losses = inputBounds.map { it.polynomial },
                probabilities = probabilities.toList(),
                alpha = alpha,
                converter = converter,
                name = name,
                displayName = displayName,
                inputBounds = inputBounds
            )
        }
    }
}
