@file:Suppress("unused", "DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
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

// ========== And Function ==========

class AndFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val n = polynomials.size

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_and")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        for (poly in polynomials) {
            val v = poly.evaluate(values) ?: return null
            if (v.isNearZero()) return zeroOf<T>()
        }
        return oneOf<T>()
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val resultIdx = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val allConstraints = mutableListOf<MathLinearInequality>()

        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(
                polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mD, "${name}_and_nz_${i}")
        }

        // Link: result <= ind_i
        for (i in indicatorVars.indices) {
            allConstraints += MathLinearInequality(
                LinearPolynomial(listOf(resultIdx, LinearMonomial(Flt64.one.unaryMinus(), indicatorVars[i])), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_and_link_ub_${i}")
        }

        // result >= sum(ind_i) - (n-1)
        if (indicatorVars.isNotEmpty()) {
            val lbMonos = listOf(resultIdx) + indicatorVars.map { LinearMonomial(Flt64.one.unaryMinus(), it) }
            allConstraints += MathLinearInequality(
                LinearPolynomial(lbMonos, Flt64.zero),
                LinearPolynomial(emptyList(), Flt64(1.0 - n)), Comparison.GE, "${name}_and_link_lb")
        } else {
            allConstraints += MathLinearInequality(
                LinearPolynomial(listOf(resultIdx), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_and_empty")
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): AndFunction<Flt64> = AndFunction(polynomials, bigM, name, displayName)

        /**
         * Factory: accept List<ToLinearPolynomial> for mixed-type inputs.
         */
        @JvmStatic
        @JvmName("fromToLinearPolynomials")
        operator fun invoke(
            polynomials: List<fuookami.ospf.kotlin.core.intermediate_model.ToMathLinearPolynomial>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            AndFunction<Flt64>(
                polynomials = polynomials.map { it.toMathLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

// ========== Or Function ==========

class OrFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val n = polynomials.size

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_or")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        for (poly in polynomials) {
            val v = poly.evaluate(values) ?: return null
            if (v.isNonZero()) return oneOf<T>()
        }
        return zeroOf<T>()
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val resultIdx = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val allConstraints = mutableListOf<MathLinearInequality>()

        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(
                polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mD, "${name}_or_nz_${i}")
        }

        // Link: result >= ind_i
        for (i in indicatorVars.indices) {
            allConstraints += MathLinearInequality(
                LinearPolynomial(listOf(resultIdx, LinearMonomial(Flt64.one.unaryMinus(), indicatorVars[i])), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_or_link_lb_${i}")
        }

        // result <= sum(ind_i)
        if (indicatorVars.isNotEmpty()) {
            val ubMonos = listOf(resultIdx) + indicatorVars.map { LinearMonomial(Flt64.one.unaryMinus(), it) }
            allConstraints += MathLinearInequality(
                LinearPolynomial(ubMonos, Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_or_link_ub")
        } else {
            allConstraints += MathLinearInequality(
                LinearPolynomial(listOf(resultIdx), Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_or_empty")
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): OrFunction<Flt64> = OrFunction(polynomials, bigM, name, displayName)

        /**
         * Factory: accept List<LinearIntermediateSymbol> for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        operator fun invoke(
            polynomials: List<LinearIntermediateSymbol<*>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            OrFunction(
                polynomials = polynomials.map { it.asMathLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )

        /**
         * Factory: accept List<ToLinearPolynomial> for mixed-type inputs.
         */
        @JvmStatic
        @JvmName("fromToLinearPolynomials")
        operator fun invoke(
            polynomials: List<fuookami.ospf.kotlin.core.intermediate_model.ToMathLinearPolynomial>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            OrFunction<Flt64>(
                polynomials = polynomials.map { it.toMathLinearPolynomial() },
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}

// ========== Not Function ==========

class NotFunction<T : Field<T>>(
    val polynomial: LinearPolynomial<T>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_not")
    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_not_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_not_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, indicatorVar, sideVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val v = polynomial.evaluate(values) ?: return null
        return if (v.isNearZero()) oneOf<T>() else zeroOf<T>()
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val resultIdx = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val allConstraints = mutableListOf<MathLinearInequality>()

        allConstraints += nonzeroIndicatorConstraints(polynomial.asFlt64Poly(), indicatorVar, sideVar, mD, "${name}_not_nz")

        // Link: result + indicator = 1
        allConstraints += MathLinearInequality(
            LinearPolynomial(listOf(resultIdx, LinearMonomial(Flt64.one, indicatorVar)), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_not_link")

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomial: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): NotFunction<Flt64> = NotFunction(polynomial, bigM, name, displayName)
    }
}

// ========== Xor Function ==========

class XorFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T
    private val n = polynomials.size

    init {
        require(n >= 2) { "XorFunction requires at least two input polynomials" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_xor")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        var hasZero = false
        var hasNonZero = false
        for (poly in polynomials) {
            val v = poly.evaluate(values) ?: return null
            if (v.isNearZero()) hasZero = true else hasNonZero = true
            if (hasZero && hasNonZero) return oneOf<T>()
        }
        return zeroOf<T>()
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val resultIdx = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val allConstraints = mutableListOf<MathLinearInequality>()

        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(
                polynomials[i].asFlt64Poly(), indicatorVars[i], sideVars[i], mD, "${name}_xor_nz_${i}")
        }

        // result - sum(ind_i) <= 0
        val sumUbMonos = listOf(resultIdx) + indicatorVars.map { LinearMonomial(Flt64.one.unaryMinus(), it) }
        allConstraints += MathLinearInequality(
            LinearPolynomial(sumUbMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_xor_sum_ub")

        // result + sum(ind_i) <= n
        val allOneMonos = listOf(resultIdx) + indicatorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += MathLinearInequality(
            LinearPolynomial(allOneMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64(n)), Comparison.LE, "${name}_xor_all_one_ub")

        // Pairwise: result - ind_i + ind_j >= 0 and result + ind_i - ind_j >= 0
        for (i in indicatorVars.indices) {
            for (j in (i + 1) until n) {
                allConstraints += MathLinearInequality(
                    LinearPolynomial(listOf(
                        resultIdx,
                        LinearMonomial(Flt64.one.unaryMinus(), indicatorVars[i]),
                        LinearMonomial(Flt64.one, indicatorVars[j])
                    ), Flt64.zero),
                    LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_xor_diff_lb_${i}_${j}")

                allConstraints += MathLinearInequality(
                    LinearPolynomial(listOf(
                        resultIdx,
                        LinearMonomial(Flt64.one, indicatorVars[i]),
                        LinearMonomial(Flt64.one.unaryMinus(), indicatorVars[j])
                    ), Flt64.zero),
                    LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${name}_xor_diff_lb_${i}_${j}_rev")
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): XorFunction<Flt64> = XorFunction(polynomials, bigM, name, displayName)
    }
}
