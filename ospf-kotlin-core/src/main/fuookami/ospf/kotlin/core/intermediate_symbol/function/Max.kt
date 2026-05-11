@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

// ========== Max Function ==========

class MaxFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "max",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val n = polynomials.size

    init {
        require(n >= 1) { "MaxFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_max")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_max_sel${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        var maxVal: V? = null
        var maxValD: Double = Double.NEGATIVE_INFINITY
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            val vD = converter.fromValue(v).toDouble()
            if (maxVal == null || vD > maxValD) {
                maxVal = v
                maxValD = vD
            }
        }
        return maxVal
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val resultMon = LinearMonomial(one, resultVar)
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // result >= poly[i] for each i
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val lbMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += LinearInequality(
                LinearPolynomial(lbMonos, -poly.constant),
                LinearPolynomial(emptyList(), zero), Comparison.GE)
        }

        // result - poly[i] + M*sel[i] <= M
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val ubMonos = listOf(resultMon) +
                poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(bigM, selectorVars[i])
            allConstraints += LinearInequality(
                LinearPolynomial(ubMonos, -poly.constant),
                LinearPolynomial(emptyList(), bigM), Comparison.LE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(selMonos, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ)

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "max",
            displayName: String? = null
        ): MaxFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaxFunction(polynomials, bigM, converter, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
            bigM: Flt64? = null,
            name: String = "max",
            displayName: String? = null
        ): MaxFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = MaxFunction(polynomials, bigM, flt64Converter, name, displayName)

        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearFunctionSymbolAdapter(
            MaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}

// ========== Min Function ==========

class MinFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "min",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val n = polynomials.size

    init {
        require(n >= 1) { "MinFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_min")
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_min_sel${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        var minVal: V? = null
        var minValD: Double = Double.POSITIVE_INFINITY
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            val vD = converter.fromValue(v).toDouble()
            if (minVal == null || vD < minValD) {
                minVal = v
                minValD = vD
            }
        }
        return minVal
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val resultMon = LinearMonomial(one, resultVar)
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // result <= poly[i] for each i
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val ubMonos = listOf(resultMon) + poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += LinearInequality(
                LinearPolynomial(ubMonos, -poly.constant),
                LinearPolynomial(emptyList(), zero), Comparison.LE)
        }

        // result - poly[i] + M*sel[i] >= 0
        for (i in polynomials.indices) {
            val poly = polynomials[i]
            val lbMonos = listOf(resultMon) +
                poly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(bigM, selectorVars[i])
            allConstraints += LinearInequality(
                LinearPolynomial(lbMonos, -poly.constant),
                LinearPolynomial(emptyList(), zero), Comparison.GE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(selMonos, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ)

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "min",
            displayName: String? = null
        ): MinFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MinFunction(polynomials, bigM, converter, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
            bigM: Flt64? = null,
            name: String = "min",
            displayName: String? = null
        ): MinFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = MinFunction(polynomials, bigM, flt64Converter, name, displayName)

        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearFunctionSymbolAdapter(
            MinFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}
