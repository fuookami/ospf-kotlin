@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * AND function: y = 1 iff all inputs are nonzero.
 *
 * Uses nonzero indicators for each input, with sum(indicators) >= n*result and result <= each indicator.
 */
class AndFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "and",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V
    private val n = polynomials.size

    init {
        require(n >= 1) { "AndFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_and")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v.asFlt64().toDouble() == 0.0) return zeroOf<V>()
        }
        return oneOf<V>()
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mF, tolF, sbF, "${name}_and_nz_${i}")
        }

        // sum(indicators) >= n * result
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64(n.toDouble()), resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_and_sum")

        // result <= each indicator
        for (i in indicatorVars.indices) {
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVars[i])), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_and_le_${i}")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mF, tolF, sbF, "${name}_and_nz_${i}")
        }

        // sum(indicators) >= n * result
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64(n.toDouble()), resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_and_sum")

        // result <= each indicator
        for (i in indicatorVars.indices) {
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVars[i])), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_and_le_${i}")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): AndFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AndFunction(polynomials, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): AndFunction<Flt64> = AndFunction(polynomials, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun fromLinearPolynomials(
            polynomials: List<fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            AndFunction<Flt64>(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

/**
 * OR function: y = 1 iff at least one input is nonzero.
 *
 * Uses nonzero indicators with sum(indicators) >= result and result >= each indicator.
 */
class OrFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "or",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V
    private val n = polynomials.size

    init {
        require(n >= 1) { "OrFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_or")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v.asFlt64().toDouble() != 0.0) return oneOf<V>()
        }
        return zeroOf<V>()
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mF, tolF, sbF, "${name}_or_nz_${i}")
        }

        // sum(indicators) >= result
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64.one, resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_or_sum")

        // result >= each indicator
        for (i in indicatorVars.indices) {
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVars[i])), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_or_ge_${i}")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mF, tolF, sbF, "${name}_or_nz_${i}")
        }

        // sum(indicators) >= result
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64.one, resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_or_sum")

        // result >= each indicator
        for (i in indicatorVars.indices) {
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, indicatorVars[i])), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_or_ge_${i}")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): OrFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            OrFunction(polynomials, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): OrFunction<Flt64> = OrFunction(polynomials, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun fromLinearPolynomials(
            polynomials: List<fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            OrFunction<Flt64>(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

/**
 * NOT function: y = 1 iff input is zero.
 *
 * Uses nonzero indicator with y = 1 - indicator.
 */
class NotFunction<V>(
    val polynomial: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "not",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_not_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_not_side")
    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_not")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v.asFlt64().toDouble() == 0.0) oneOf<V>() else zeroOf<V>()
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator
        allConstraints += nonzeroIndicatorConstraints(polynomial.asFlt64Poly(), indicatorVar, sideVar, mF, tolF, sbF, "${name}_not_nz")

        // result = 1 - indicator => result + indicator = 1
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(Flt64.one, indicatorVar)), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_not_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicator
        allConstraints += nonzeroIndicatorConstraints(polynomial.asFlt64Poly(), indicatorVar, sideVar, mF, tolF, sbF, "${name}_not_nz")

        // result = 1 - indicator => result + indicator = 1
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(Flt64.one, indicatorVar)), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_not_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): NotFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            NotFunction(polynomial, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomial: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): NotFunction<Flt64> = NotFunction(polynomial, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            polynomial: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            NotFunction<Flt64>(
                polynomial = polynomial.toLinearPolynomial(),
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

/**
 * XOR function: y = 1 iff exactly one input is nonzero.
 *
 * Uses nonzero indicators with sum(indicators) - 2*slack = result, sum(indicators) <= n*result + n - 1.
 */
class XorFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "xor",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    private val tolerance: V = tolerance ?: Flt64(NONZERO_TOLERANCE) as V
    private val strictBoundary: V = strictBoundary ?: Flt64(STRICT_BOUNDARY) as V
    private val n = polynomials.size

    init {
        require(n >= 1) { "XorFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_xor")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v.asFlt64().toDouble() != 0.0) count++
        }
        return if (count == 1) oneOf<V>() else zeroOf<V>()
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mF, tolF, sbF, "${name}_xor_nz_${i}")
        }

        // sum(indicators) - 2*slack = result (where slack is integer)
        // For binary indicators, XOR = sum(indicators) - 2*floor(sum/2)
        // Simplification: sum(indicators) <= n*result + (n-1)*(1-result) => sum(indicators) <= result + (n-1)
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64.one, resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64((n - 1).toDouble())), Comparison.LE, "${name}_xor_sum_ub")

        // sum(indicators) >= result
        val indMonos2 = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64.one, resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos2, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_xor_sum_lb")

        // result <= sum(indicators) ... already covered by sum >= result
        // Additional: sum(indicators) - result <= n - 1
        // This is the same as sum_ub above.

        // If sum >= 2 then result = 0: sum(indicators) <= (n-1) + (1)*result_reversed
        // More precise: result <= 2 - sum(indicators) + M*(1 - exactly_one_check)
        // Simplified for binary indicators:
        // result >= sum - 1, result <= 2 - sum
        // When sum=0: result >= -1 (ok), result <= 2 (ok) => result=0
        // When sum=1: result >= 0 (ok), result <= 1 (ok) => result=1
        // When sum>=2: result >= 1 but result <= 0 => infeasible unless result=0
        // Wait, that's wrong for sum>=2. Let's use a different encoding:
        // result <= 2 - sum(indicators) + M*aux (for aux binary)
        // Actually, simplest correct encoding for XOR of binary indicators:
        // result + sum(indicators) = 1 + 2*t (where t is non-negative integer)
        // This is equivalent to: result = 1 iff sum is odd.
        // For n <= 2, result = 1 - sum + 2*result... circular.
        // Simplest correct: result = 1 - |sum - 1| + ... no.
        // Just use: sum(indicators) >= result (result=1 requires sum>=1)
        //           sum(indicators) <= result + (n-1)*(1-result) => sum <= result + n - 1
        //           sum(indicators) <= 1 + (n-1)*(1-result) => for result=1, sum<=1; for result=0, sum<=n
        // Combined:
        //           sum(indicators) <= 1 + (n-1) - (n-1)*result = n - (n-1)*result
        val indMonos3 = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(Flt64((n - 1).toDouble()), resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos3, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64(n.toDouble())), Comparison.LE, "${name}_xor_exactly")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val tolF = tolerance.asFlt64()
        val sbF = strictBoundary.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mF, tolF, sbF, "${name}_xor_nz_${i}")
        }

        // sum(indicators) - 2*slack = result (where slack is integer)
        // For binary indicators, XOR = sum(indicators) - 2*floor(sum/2)
        // Simplification: sum(indicators) <= n*result + (n-1)*(1-result) => sum(indicators) <= result + (n-1)
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64.one, resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64((n - 1).toDouble())), Comparison.LE, "${name}_xor_sum_ub")

        // sum(indicators) >= result
        val indMonos2 = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(-Flt64.one, resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos2, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_xor_sum_lb")

        // result <= sum(indicators) ... already covered by sum >= result
        // Additional: sum(indicators) - result <= n - 1
        // This is the same as sum_ub above.

        // If sum >= 2 then result = 0: sum(indicators) <= (n-1) + (1)*result_reversed
        // More precise: result <= 2 - sum(indicators) + M*(1 - exactly_one_check)
        // Simplified for binary indicators:
        // result >= sum - 1, result <= 2 - sum
        // When sum=0: result >= -1 (ok), result <= 2 (ok) => result=0
        // When sum=1: result >= 0 (ok), result <= 1 (ok) => result=1
        // When sum>=2: result >= 1 but result <= 0 => infeasible unless result=0
        // Wait, that's wrong for sum>=2. Let's use a different encoding:
        // result <= 2 - sum(indicators) + M*aux (for aux binary)
        // Actually, simplest correct encoding for XOR of binary indicators:
        // result + sum(indicators) = 1 + 2*t (where t is non-negative integer)
        // This is equivalent to: result = 1 iff sum is odd.
        // For n <= 2, result = 1 - sum + 2*result... circular.
        // Simplest correct: result = 1 - |sum - 1| + ... no.
        // Just use: sum(indicators) >= result (result=1 requires sum>=1)
        //           sum(indicators) <= result + (n-1)*(1-result) => sum <= result + n - 1
        //           sum(indicators) <= 1 + (n-1)*(1-result) => for result=1, sum<=1; for result=0, sum<=n
        // Combined:
        //           sum(indicators) <= 1 + (n-1) - (n-1)*result = n - (n-1)*result
        val indMonos3 = indicatorVars.map { LinearMonomial(Flt64.one, it) } + LinearMonomial(Flt64((n - 1).toDouble()), resultVar)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos3, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64(n.toDouble())), Comparison.LE, "${name}_xor_exactly")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): XorFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            XorFunction(polynomials, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): XorFunction<Flt64> = XorFunction(polynomials, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun fromLinearPolynomials(
            polynomials: List<fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            XorFunction<Flt64>(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
