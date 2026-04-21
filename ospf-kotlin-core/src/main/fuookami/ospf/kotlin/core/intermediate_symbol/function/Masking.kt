@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
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
    val input: MathLinearPolynomial<T>,
    val mask: AbstractVariableItem<*, *>,
    bigM: T? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {
    private val bigM: T = bigM ?: Flt64(BIG_M_DEFAULT) as T

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_masking")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val maskValue = values[mask] ?: return zeroOf<T>()
        val maskD = maskValue.asFlt64().toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return zeroOf<T>()
        return input.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val result = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val resultIdx = MathLinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val inputPoly = input.asFlt64Poly()

        // c1: y - x + M*mask <= M + x_const  =>  y - x <= M*(1 - mask) + x_const
        val c1LhsMonos = inputPoly.monomials.map { MathLinearMonomial(-it.coefficient, it.symbol) } +
            MathLinearMonomial(Flt64.one, resultVar)
        val c1Lhs = MathLinearPolynomial(c1LhsMonos, -inputPoly.constant)
        val c1RhsPoly = MathLinearPolynomial(listOf(MathLinearMonomial(mD, mask)), mD)
        val c1 = MathLinearInequality(c1Lhs, c1RhsPoly, Comparison.LE, "${name}_masking_eq_ub")
        when (val r = model.addConstraint(relation = c1, name = c1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c2: y - x >= -M*mask + x_const  =>  y - x >= -M*mask + x_const
        val c2LhsMonos = inputPoly.monomials.map { MathLinearMonomial(-it.coefficient, it.symbol) } +
            MathLinearMonomial(Flt64.one, resultVar)
        val c2Lhs = MathLinearPolynomial(c2LhsMonos, -inputPoly.constant)
        val c2RhsPoly = MathLinearPolynomial(listOf(MathLinearMonomial(-mD, mask)), Flt64.zero)
        val c2 = MathLinearInequality(c2Lhs, c2RhsPoly, Comparison.GE, "${name}_masking_eq_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: y - M*mask <= 0
        val c3Lhs = MathLinearPolynomial(listOf(resultIdx, MathLinearMonomial(-mD, mask)), Flt64.zero)
        val c3Rhs = MathLinearPolynomial(emptyList(), Flt64.zero)
        val c3 = MathLinearInequality(c3Lhs, c3Rhs, Comparison.LE, "${name}_masking_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: y + M*mask >= 0
        val c4Lhs = MathLinearPolynomial(listOf(resultIdx, MathLinearMonomial(mD, mask)), Flt64.zero)
        val c4Rhs = MathLinearPolynomial(emptyList(), Flt64.zero)
        val c4 = MathLinearInequality(c4Lhs, c4Rhs, Comparison.GE, "${name}_masking_zero_lb")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        operator fun invoke(
            input: MathLinearPolynomial<Flt64>,
            mask: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MaskingFunction(input, mask, bigM, name, displayName)
        )

        operator fun invoke(
            input: MathLinearPolynomial<Flt64>,
            maskVarName: String,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter {
            val maskVar = BinVar("${name}_${maskVarName}")
            return LinearFunctionSymbolAdapter(
                MaskingFunction(input, maskVar, bigM, name, displayName)
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromLinearIntermediateSymbol")
        operator fun invoke(
            x: LinearIntermediateSymbol<*>,
            mask: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MaskingFunction(
                input = x.asMathLinearPolynomial(),
                mask = mask,
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )

        /**
         * Factory: accept ToLinearPolynomial for both x and mask.
         * Fully generic factory for mixed framework types.
         */
        @JvmStatic
        @JvmName("fromToLinearPolynomials")
        operator fun invoke(
            x: fuookami.ospf.kotlin.core.intermediate_model.ToMathLinearPolynomial,
            mask: fuookami.ospf.kotlin.core.intermediate_model.ToMathLinearPolynomial,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaskingWithPolyMaskFunction = MaskingWithPolyMaskFunction(
            input = x.toMathLinearPolynomial(),
            maskPoly = mask.toMathLinearPolynomial(),
            bigM = bigM,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * Masking function variant where mask is a polynomial instead of a single variable.
 * y = x * mask where mask is a polynomial expression.
 * Creates an internal variable `m` with constraint m = maskPoly, then applies
 * standard Big-M masking constraints with m as the binary mask.
 */
class MaskingWithPolyMaskFunction(
    val input: MathLinearPolynomial<Flt64>,
    val maskPoly: MathLinearPolynomial<Flt64>,
    bigM: Flt64? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearIntermediateSymbol<Flt64>, MathFunctionSymbol<Flt64> {
    private val bigM: Flt64 = bigM ?: Flt64(BIG_M_DEFAULT)
    val maskVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_var")
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_result")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(maskVar, resultVar)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        val vars = helperVariables
        return if (vars.isNotEmpty()) tokens.add(vars) else ok
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {}
    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: LegacyAbstractTokenTable): Flt64? = null
    override fun toRawString(unfold: UInt64): String = name

    override val flattenedMonomials: LinearFlattenDataF64 get() = LinearFlattenDataF64(emptyList(), Flt64.zero)

    override fun toMathLinearInequality(): MathLinearInequality {
        return MathLinearInequality(MathLinearPolynomial(emptyList(), Flt64.zero), MathLinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        return MathQuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.zero), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun evaluate(tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListF64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListF64?, zeroIfNone: Boolean): Flt64? =
        (this as MathFunctionSymbol<Flt64>).evaluate(values)

    override fun evaluate(values: Map<Symbol, Flt64>): Flt64? {
        val maskValue = maskPoly.evaluate(values) ?: return Flt64.zero
        val maskD = maskValue.toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return Flt64.zero
        return input.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val result = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // maskPoly = maskVar constraint
        val maskMonos = maskPoly.monomials.map { MathLinearMonomial(-it.coefficient, it.symbol) } +
            MathLinearMonomial(Flt64.one, maskVar)
        val maskConstraint = MathLinearInequality(
            MathLinearPolynomial(monomials = maskMonos, constant = -maskPoly.constant),
            MathLinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.EQ, "${name}_mask_eq")
        when (val r = model.addConstraint(relation = maskConstraint, name = maskConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val mD = bigM.toDouble()

        // c1: result - input <= M*(1 - mask_var_normalized)
        val c1Monos = input.monomials.map { MathLinearMonomial(-it.coefficient, it.symbol) } +
            MathLinearMonomial(Flt64.one, resultVar) +
            MathLinearMonomial(Flt64(mD), maskVar)
        val c1 = MathLinearInequality(
            MathLinearPolynomial(monomials = c1Monos, constant = -input.constant),
            MathLinearPolynomial(monomials = emptyList(), constant = Flt64(mD)), Comparison.LE, "${name}_ub")
        when (val r = model.addConstraint(relation = c1, name = c1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c2: result - input >= -M*mask_var
        val c2Monos = input.monomials.map { MathLinearMonomial(-it.coefficient, it.symbol) } +
            MathLinearMonomial(Flt64.one, resultVar) +
            MathLinearMonomial(Flt64(mD), maskVar)
        val c2 = MathLinearInequality(
            MathLinearPolynomial(monomials = c2Monos, constant = -input.constant),
            MathLinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.GE, "${name}_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: result <= M*mask_var
        val c3Monos = listOf(MathLinearMonomial(Flt64.one, resultVar), MathLinearMonomial(-Flt64(mD), maskVar))
        val c3 = MathLinearInequality(
            MathLinearPolynomial(monomials = c3Monos, constant = Flt64.zero),
            MathLinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.LE, "${name}_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: result >= -M*mask_var
        val c4Monos = listOf(MathLinearMonomial(Flt64.one, resultVar), MathLinearMonomial(Flt64(mD), maskVar))
        val c4 = MathLinearInequality(
            MathLinearPolynomial(monomials = c4Monos, constant = Flt64.zero),
            MathLinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.GE, "${name}_zero_lb")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }
}

/**
 * Masking range function: y in [lower*mask, upper*mask].
 * When mask=0, y=0. When mask=1, y in [lower, upper].
 */
class MaskingRangeFunction<T : Field<T>>(
    val mask: MathLinearPolynomial<T>,
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

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

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

    override fun register(model: AbstractLinearMetaModelF64): Try {
        when (val result = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val resultMon = MathLinearMonomial(Flt64.one, resultVar)
        val maskPoly = mask.asFlt64Poly()
        val lowerD = lower.asFlt64().toDouble()
        val upperD = upper.asFlt64().toDouble()

        // Upper: y - upper*mask <= 0
        val upperFlt = Flt64(upperD)
        val upperMonos = listOf(resultMon) +
            maskPoly.monomials.map { MathLinearMonomial(it.coefficient * -upperFlt, it.symbol) }
        val upperLhs = MathLinearPolynomial(upperMonos, maskPoly.constant * -upperFlt)
        val upperRhs = MathLinearPolynomial(emptyList(), Flt64.zero)
        val upper = MathLinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_masking_range_ub")
        when (val r = model.addConstraint(relation = upper, name = upper.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // Lower: y - lower*mask >= 0
        val lowerFlt = Flt64(lowerD)
        val lowerMonos = listOf(resultMon) +
            maskPoly.monomials.map { MathLinearMonomial(it.coefficient * -lowerFlt, it.symbol) }
        val lowerLhs = MathLinearPolynomial(lowerMonos, maskPoly.constant * -lowerFlt)
        val lowerRhs = MathLinearPolynomial(emptyList(), Flt64.zero)
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
            mask: MathLinearPolynomial<Flt64>,
            lower: Flt64,
            upper: Flt64,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter = LinearFunctionSymbolAdapter(
            MaskingRangeFunction(mask, lower, upper, name, displayName)
        )
    }
}
