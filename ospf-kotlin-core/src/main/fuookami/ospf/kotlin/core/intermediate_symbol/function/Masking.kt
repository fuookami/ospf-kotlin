@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
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

/**
 * Masking function: y = x * mask where mask is binary.
 * When mask=1, y=x. When mask=0, y=0.
 */
class MaskingFunction<T : Field<T>>(
    val input: LinearPolynomial<T>,
    val mask: AbstractVariableItem<*, *>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_masking")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, T>): T? {
        val maskValue = values[mask] ?: return zeroOf<T>()
        val maskD = maskValue.asFlt64().toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return zeroOf<T>()
        return input.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        val vars = listOf(resultVar)
        when (val result = model.add(vars)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val resultIdx = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val inputPoly = input.asFlt64Poly()

        // c1: y - x + M*mask <= M + x_const  =>  y - x <= M*(1 - mask) + x_const
        val c1LhsMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar)
        val c1Lhs = LinearPolynomial(c1LhsMonos, -inputPoly.constant)
        val c1RhsPoly = LinearPolynomial(listOf(LinearMonomial(mD, mask)), mD)
        val c1 = MathLinearInequality(c1Lhs, c1RhsPoly, Comparison.LE, "${name}_masking_eq_ub")
        when (val r = model.addConstraint(relation = c1, name = c1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c2: y - x >= -M*mask + x_const  =>  y - x >= -M*mask + x_const
        val c2LhsMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar)
        val c2Lhs = LinearPolynomial(c2LhsMonos, -inputPoly.constant)
        val c2RhsPoly = LinearPolynomial(listOf(LinearMonomial(-mD, mask)), Flt64.zero)
        val c2 = MathLinearInequality(c2Lhs, c2RhsPoly, Comparison.GE, "${name}_masking_eq_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: y - M*mask <= 0
        val c3Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(-mD, mask)), Flt64.zero)
        val c3Rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val c3 = MathLinearInequality(c3Lhs, c3Rhs, Comparison.LE, "${name}_masking_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: y + M*mask >= 0
        val c4Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(mD, mask)), Flt64.zero)
        val c4Rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val c4 = MathLinearInequality(c4Lhs, c4Rhs, Comparison.GE, "${name}_masking_zero_lb")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        /**
         * Factory: accept core expression AbstractLinearPolynomial for framework compatibility.
         */
        @JvmStatic
        operator fun invoke(
            x: fuookami.ospf.kotlin.core.expression.polynomial.AbstractLinearPolynomial<*>,
            mask: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction<Flt64> = MaskingFunction(
            input = x.asMathLinearPolynomial(),
            mask = mask,
            bigM = bigM,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            input: LinearPolynomial<Flt64>,
            mask: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction<Flt64> = MaskingFunction(input, mask, bigM, name, displayName)

        operator fun invoke(
            input: LinearPolynomial<Flt64>,
            maskVarName: String,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction<Flt64> {
            val maskVar = BinVar("${name}_${maskVarName}")
            return MaskingFunction(input, maskVar, bigM, name, displayName)
        }
    }
}

/**
 * Masking range function: y in [lower*mask, upper*mask].
 * When mask=0, y=0. When mask=1, y in [lower, upper].
 */
class MaskingRangeFunction<T : Field<T>>(
    val mask: LinearPolynomial<T>,
    val lower: T,
    val upper: T,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    init {
        require(lower.asFlt64().toDouble() <= upper.asFlt64().toDouble()) {
            "MaskingRange lower bound must be <= upper bound"
        }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_range")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, T>): T? {
        val maskValue = mask.evaluate(values) ?: return zeroOf<T>()
        val maskD = maskValue.asFlt64().toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return zeroOf<T>()
        val yVal = values[resultVar]?.asFlt64()?.toDouble() ?: return zeroOf<T>()
        val lb = lower.asFlt64().toDouble() * maskD
        val ub = upper.asFlt64().toDouble() * maskD
        @Suppress("UNCHECKED_CAST")
        return Flt64(yVal.coerceIn(lb, ub)) as T
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        val vars = listOf(resultVar)
        when (val result = model.add(vars)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val resultMon = LinearMonomial(Flt64.one, resultVar)
        val maskPoly = mask.asFlt64Poly()
        val lowerD = lower.asFlt64().toDouble()
        val upperD = upper.asFlt64().toDouble()

        // Upper: y - upper*mask <= 0
        val upperFlt = Flt64(upperD)
        val upperMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -upperFlt, it.symbol) }
        val upperLhs = LinearPolynomial(upperMonos, maskPoly.constant * -upperFlt)
        val upperRhs = LinearPolynomial(emptyList(), Flt64.zero)
        val upper = MathLinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_masking_range_ub")
        when (val r = model.addConstraint(relation = upper, name = upper.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // Lower: y - lower*mask >= 0
        val lowerFlt = Flt64(lowerD)
        val lowerMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -lowerFlt, it.symbol) }
        val lowerLhs = LinearPolynomial(lowerMonos, maskPoly.constant * -lowerFlt)
        val lowerRhs = LinearPolynomial(emptyList(), Flt64.zero)
        val lowerC = MathLinearInequality(lowerLhs, lowerRhs, Comparison.GE, "${name}_masking_range_lb")
        when (val r = model.addConstraint(relation = lowerC, name = lowerC.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        operator fun invoke(
            mask: LinearPolynomial<Flt64>,
            lower: Flt64,
            upper: Flt64,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction<Flt64> = MaskingRangeFunction(mask, lower, upper, name, displayName)
    }
}
