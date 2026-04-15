@file:Suppress("unused", "DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
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

enum class BinaryzationMethod {
    BigM,
    Threshold,
    Indicator,
    SOS1;

    fun mechanismEquivalent(): BinaryzationMethod = when (this) {
        Threshold -> Threshold
        BigM, Indicator, SOS1 -> BigM
    }
}

/**
 * Binaryzation function: converts a linear expression to binary semantics.
 *
 * When input >= threshold (BigM) or input > 0 (default): result = 1
 * Otherwise: result = 0
 *
 * Uses Big-M method with two constraints:
 * - input - threshold - M*y >= threshold - M (lower bound)
 * - input - threshold - M*y <= 0 (upper bound)
 */
class BinaryzationFunction<T : Field<T>>(
    val input: LinearPolynomial<T>,
    val threshold: T,
    bigM: T? = null,
    val method: BinaryzationMethod = BinaryzationMethod.BigM,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_bin")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, T>): T? {
        val inputVal = evalInput(values) ?: return null
        val thresh = threshold.asFlt64().toDouble()
        val eps = 1e-10
        val isTrue = when (method.mechanismEquivalent()) {
            BinaryzationMethod.Threshold -> inputVal.asFlt64().toDouble() + eps >= thresh
            BinaryzationMethod.BigM -> inputVal.asFlt64().toDouble() > thresh + eps
            BinaryzationMethod.Indicator, BinaryzationMethod.SOS1 -> inputVal.asFlt64().toDouble() > thresh + eps
        }
        return if (isTrue) oneOf() else zeroOf()
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        when (val r = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val yMon = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64().toDouble()
        val thresh = threshold.asFlt64().toDouble()
        val strictEps = 1e-10

        val allConstraints = mutableListOf<MathLinearInequality>()

        // Build monomials from input polynomial
        val inputPoly = input.asFlt64Poly()
        val inputMonos = inputPoly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        val inputConst = inputPoly.constant.toDouble()
        val shiftedConst = inputConst - thresh

        val yCoeff = Flt64(-mD)
        val monosWithY = inputMonos + LinearMonomial(yCoeff, resultVar)

        when (method.mechanismEquivalent()) {
            BinaryzationMethod.Threshold -> {
                // Threshold: result=1 when input >= threshold
                // lb: input - threshold - M*y >= -M
                allConstraints += MathLinearInequality(
                    LinearPolynomial(monosWithY, Flt64(shiftedConst)),
                    LinearPolynomial(emptyList(), Flt64(-mD)), Comparison.GE, "${name}_bin_lb")
                // ub: input - threshold - M*y <= 0
                allConstraints += MathLinearInequality(
                    LinearPolynomial(monosWithY, Flt64(shiftedConst)),
                    LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_bin_ub")
            }
            BinaryzationMethod.BigM -> {
                // BigM: result=1 when input > threshold
                // lb: input - threshold - M*y >= eps - M
                allConstraints += MathLinearInequality(
                    LinearPolynomial(monosWithY, Flt64(shiftedConst)),
                    LinearPolynomial(emptyList(), Flt64(strictEps - mD)), Comparison.GE, "${name}_bin_lb")
                // ub: input - threshold - M*y <= 0
                allConstraints += MathLinearInequality(
                    LinearPolynomial(monosWithY, Flt64(shiftedConst)),
                    LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_bin_ub")
            }
            BinaryzationMethod.Indicator, BinaryzationMethod.SOS1 -> {
                // Currently encoded same as BigM
                allConstraints += MathLinearInequality(
                    LinearPolynomial(monosWithY, Flt64(shiftedConst)),
                    LinearPolynomial(emptyList(), Flt64(strictEps - mD)), Comparison.GE, "${name}_bin_lb")
                allConstraints += MathLinearInequality(
                    LinearPolynomial(monosWithY, Flt64(shiftedConst)),
                    LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${name}_bin_ub")
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    private fun evalInput(values: Map<Symbol, T>): T? {
        return input.evaluate(values)
    }

    companion object {
        private val BINARYZATION_PIECEWISE_THRESHOLD: Flt64 = Flt64(1e-5)

        operator fun invoke(
            input: LinearPolynomial<Flt64>,
            threshold: Flt64 = Flt64.zero,
            bigM: Flt64? = null,
            method: BinaryzationMethod = BinaryzationMethod.BigM,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            BinaryzationFunction(input, threshold, bigM, method, name, displayName)
        )

        fun withBigM(
            input: LinearPolynomial<Flt64>,
            bigM: Flt64,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            BinaryzationFunction(input, Flt64.zero, bigM, BinaryzationMethod.BigM, name, displayName)
        )

        fun withThreshold(
            input: LinearPolynomial<Flt64>,
            threshold: Flt64,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            BinaryzationFunction(input, threshold, null, BinaryzationMethod.Threshold, name, displayName)
        )

        /**
         * Factory: accept core expression AbstractLinearPolynomial for framework compatibility.
         * Maps legacy parameters to new API: extract/threshold/epsilon -> method selection.
         */
        @JvmStatic
        @JvmName("fromCorePolynomial")
        operator fun invoke(
            x: AbstractLinearPolynomial<*>,
            extract: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            piecewise: Boolean = false,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter {
            val input = x.asMathLinearPolynomial()
            val method = if (piecewise || epsilon >= BINARYZATION_PIECEWISE_THRESHOLD) {
                BinaryzationMethod.Threshold
            } else {
                BinaryzationMethod.BigM
            }
            return LinearFunctionSymbolAdapter(
                BinaryzationFunction(input, Flt64.zero, null, method, name, displayName)
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromLinearIntermediateSymbol")
        operator fun invoke(
            x: LinearIntermediateSymbol,
            threshold: Flt64 = Flt64.zero,
            bigM: Flt64? = null,
            method: BinaryzationMethod = BinaryzationMethod.BigM,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            BinaryzationFunction(
                input = x.asMathLinearPolynomial(),
                threshold = threshold,
                bigM = bigM,
                method = method,
                name = name,
                displayName = displayName
            )
        )
    }
}
