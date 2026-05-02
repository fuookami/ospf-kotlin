@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

// ========== Max Function ==========

class MaxFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    override var name: String = "max",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
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
            val vD = v.asFlt64().toDouble()
            if (maxVal == null || vD > maxValD) {
                maxVal = v
                maxValD = vD
            }
        }
        return maxVal
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val mF = bigM.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // result >= poly[i] for each i
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val lbMonos = listOf(resultMon) + polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(lbMonos, -polyF.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE)
        }

        // result - poly[i] + M*sel[i] <= M
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val ubMonos = listOf(resultMon) +
                polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mF, selectorVars[i])
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(ubMonos, -polyF.constant),
                LinearPolynomial(emptyList(), mF), Comparison.LE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(selMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)

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

        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val mF = bigM.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // result >= poly[i] for each i
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val lbMonos = listOf(resultMon) + polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(lbMonos, -polyF.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE)
        }

        // result - poly[i] + M*sel[i] <= M
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val ubMonos = listOf(resultMon) +
                polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mF, selectorVars[i])
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(ubMonos, -polyF.constant),
                LinearPolynomial(emptyList(), mF), Comparison.LE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(selMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): MaxFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaxFunction(polynomials, bigM, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaxFunction<Flt64> = MaxFunction(polynomials, bigM, name, displayName)

        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbolFlt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

// ========== Min Function ==========

class MinFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    override var name: String = "min",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
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
            val vD = v.asFlt64().toDouble()
            if (minVal == null || vD < minValD) {
                minVal = v
                minValD = vD
            }
        }
        return minVal
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val mF = bigM.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // result <= poly[i] for each i
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val ubMonos = listOf(resultMon) + polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(ubMonos, -polyF.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE)
        }

        // result - poly[i] + M*sel[i] >= 0
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val lbMonos = listOf(resultMon) +
                polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mF, selectorVars[i])
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(lbMonos, -polyF.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(selMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)

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

        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val mF = bigM.asFlt64()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // result <= poly[i] for each i
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val ubMonos = listOf(resultMon) + polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(ubMonos, -polyF.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE)
        }

        // result - poly[i] + M*sel[i] >= 0
        for (i in polynomials.indices) {
            val polyF = polynomials[i].asFlt64Poly()
            val lbMonos = listOf(resultMon) +
                polyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
                LinearMonomial(mF, selectorVars[i])
            allConstraints += Flt64LinearInequality(
                LinearPolynomial(lbMonos, -polyF.constant),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE)
        }

        // sum(sel[i]) = 1
        val selMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(selMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): MinFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MinFunction(polynomials, bigM, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MinFunction<Flt64> = MinFunction(polynomials, bigM, name, displayName)

        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbolFlt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MinFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
