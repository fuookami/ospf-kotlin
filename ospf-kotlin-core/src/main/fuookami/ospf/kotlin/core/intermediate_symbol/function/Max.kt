@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

// ========== Max Function ==========

/**
 * Max function: result = max(polynomials[0], polynomials[1], ...).
 *
 * Creates a continuous result variable and binary selector variables.
 * Uses Big-M method to enforce that result >= each polynomial and
 * result <= polynomial_i + M*(1 - selector_i).
 */
class MaxFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val n = polynomials.size

    init {
        require(n >= 1) { "MaxFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_max")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_max_sel${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        var maxVal: T? = null
        var maxValD: Double = Double.NEGATIVE_INFINITY
        for (poly in polynomials) {
            val v = poly.evaluate(values) ?: return null
            val vD = v.asFlt64().toDouble()
            if (maxVal == null || vD > maxValD) {
                maxVal = v
                maxValD = vD
            }
        }
        return maxVal
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val allConstraints = mutableListOf<MathLinearInequality>()

        // For each polynomial: result >= poly (lower bound)
        for (i in polynomials.indices) {
            val poly = polynomials[i].asFlt64Poly()
            val lbMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += MathLinearInequality(
                LinearPolynomial(lbMonos, -poly.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE)
        }

        // For each polynomial: result - poly <= M*(1 - sel_i)
        // => result - poly + M*sel_i <= M
        val mD = bigM.asFlt64()
        for (i in polynomials.indices) {
            val poly = polynomials[i].asFlt64Poly()
            val ubMonos = listOf(resultMon) +
                poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mD, selectorVars[i])
            allConstraints += MathLinearInequality(
                LinearPolynomial(ubMonos, -poly.constant),
                LinearPolynomial(emptyList(), mD), Comparison.LE)
        }

        // sum(sel_i) = 1 (exactly one selector active)
        val selMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += MathLinearInequality(
            LinearPolynomial(selMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaxFunction<Flt64> = MaxFunction(polynomials, bigM, name, displayName)

        /**
         * Factory: accept List<LinearIntermediateSymbol> for framework compatibility.
         * Each symbol is converted to a single-term polynomial.
         * Parameter named 'polynomials' to match callers using named arguments.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbolF64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MaxFunction(
                polynomials = polynomials.map { it.asMathLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

// ========== Min Function ==========

/**
 * Min function: result = min(polynomials[0], polynomials[1], ...).
 *
 * Creates a continuous result variable and binary selector variables.
 * Uses Big-M method to enforce that result <= each polynomial and
 * result >= polynomial_i - M*(1 - selector_i).
 */
class MinFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val n = polynomials.size

    init {
        require(n >= 1) { "MinFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_min")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_min_sel${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        var minVal: T? = null
        var minValD: Double = Double.POSITIVE_INFINITY
        for (poly in polynomials) {
            val v = poly.evaluate(values) ?: return null
            val vD = v.asFlt64().toDouble()
            if (minVal == null || vD < minValD) {
                minVal = v
                minValD = vD
            }
        }
        return minVal
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val allConstraints = mutableListOf<MathLinearInequality>()

        // For each polynomial: result <= poly (upper bound)
        for (i in polynomials.indices) {
            val poly = polynomials[i].asFlt64Poly()
            val ubMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += MathLinearInequality(
                LinearPolynomial(ubMonos, -poly.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE)
        }

        // For each polynomial: result - poly >= -M*(1 - sel_i)
        // => result - poly + M*sel_i >= 0
        val mD = bigM.asFlt64()
        for (i in polynomials.indices) {
            val poly = polynomials[i].asFlt64Poly()
            val lbMonos = listOf(resultMon) +
                poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mD, selectorVars[i])
            allConstraints += MathLinearInequality(
                LinearPolynomial(lbMonos, -poly.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE)
        }

        // sum(sel_i) = 1 (exactly one selector active)
        val selMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += MathLinearInequality(
            LinearPolynomial(selMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MinFunction<Flt64> = MinFunction(polynomials, bigM, name, displayName)

        /**
         * Factory: accept List<LinearIntermediateSymbol> for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbolF64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MinFunction(
                polynomials = polynomials.map { it.asMathLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
