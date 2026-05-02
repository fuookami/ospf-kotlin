@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.AbstractTokenListFlt64
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
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
class MaskingFunction<V>(
    val input: LinearPolynomial<V>,
    val mask: AbstractVariableItem<*, *>,
    bigM: V? = null,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_masking")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = values[mask] ?: return zeroOf<V>()
        val maskD = maskValue.asFlt64().toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return zeroOf<V>()
        return input.evaluate(values)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val resultIdx = LinearMonomial(Flt64.one, resultVar)
        val mD = bigM.asFlt64()
        val inputPoly = input.asFlt64Poly()

        // c1: y - x + M*mask <= M + x_const  =>  y - x <= M*(1 - mask) + x_const
        val c1LhsMonos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar)
        val c1Lhs = LinearPolynomial(c1LhsMonos, -inputPoly.constant)
        val c1RhsPoly = LinearPolynomial(listOf(LinearMonomial(mD, mask)), mD)
        val c1 = Flt64LinearInequality(c1Lhs, c1RhsPoly, Comparison.LE, "${name}_masking_eq_ub")
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
        val c2 = Flt64LinearInequality(c2Lhs, c2RhsPoly, Comparison.GE, "${name}_masking_eq_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: y - M*mask <= 0
        val c3Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(-mD, mask)), Flt64.zero)
        val c3Rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val c3 = Flt64LinearInequality(c3Lhs, c3Rhs, Comparison.LE, "${name}_masking_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: y + M*mask >= 0
        val c4Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(mD, mask)), Flt64.zero)
        val c4Rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val c4 = Flt64LinearInequality(c4Lhs, c4Rhs, Comparison.GE, "${name}_masking_zero_lb")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
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
        val c1 = Flt64LinearInequality(c1Lhs, c1RhsPoly, Comparison.LE, "${name}_masking_eq_ub")
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
        val c2 = Flt64LinearInequality(c2Lhs, c2RhsPoly, Comparison.GE, "${name}_masking_eq_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: y - M*mask <= 0
        val c3Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(-mD, mask)), Flt64.zero)
        val c3Rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val c3 = Flt64LinearInequality(c3Lhs, c3Rhs, Comparison.LE, "${name}_masking_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: y + M*mask >= 0
        val c4Lhs = LinearPolynomial(listOf(resultIdx, LinearMonomial(mD, mask)), Flt64.zero)
        val c4Rhs = LinearPolynomial(emptyList(), Flt64.zero)
        val c4 = Flt64LinearInequality(c4Lhs, c4Rhs, Comparison.GE, "${name}_masking_zero_lb")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        operator fun <V> invoke(
            input: LinearPolynomial<V>,
            mask: AbstractVariableItem<*, *>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): MaskingFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaskingFunction(input, mask, bigM, name, displayName)

        operator fun invoke(
            input: LinearPolynomial<Flt64>,
            mask: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MaskingFunction(input, mask, bigM, name, displayName)
        )

        operator fun invoke(
            input: LinearPolynomial<Flt64>,
            maskVarName: String,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val maskVar = BinVar("${name}_${maskVarName}")
            return LinearFunctionSymbolAdapter<Flt64>(
                MaskingFunction(input, maskVar, bigM, name, displayName)
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for framework compatibility.
         */
        @JvmStatic
        @JvmName("fromLinearIntermediateSymbol")
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            mask: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MaskingFunction(
                input = x.toLinearPolynomial(),
                mask = mask,
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )

        /** Factory: accept ToLinearPolynomial<Flt64> for both x and mask. */
        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun fromLinearPolynomials(
            x: ToLinearPolynomial<Flt64>,
            mask: ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): MaskingWithPolyMaskFunction<Flt64> = MaskingWithPolyMaskFunction(
            input = x.toLinearPolynomial(),
            maskPoly = mask.toLinearPolynomial(),
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
class MaskingWithPolyMaskFunction<V>(
    val input: LinearPolynomial<V>,
    val maskPoly: LinearPolynomial<V>,
    bigM: V? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearIntermediateSymbol<V>, MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V
    val maskVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_var")
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_result")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(maskVar, resultVar)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {}
    override fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null
    override fun toRawString(unfold: UInt64): String = name

    override val flattenedMonomials: LinearFlattenDataFlt64 get() = LinearFlattenDataFlt64(emptyList(), Flt64.zero)

    override val polynomial: LinearPolynomial<V> get() = LinearPolynomial(emptyList(), zeroOf<V>())
    override fun asMutable(): MutableLinearPolynomial<V> = MutableLinearPolynomial(emptyList(), zeroOf<V>())

    override fun toMathLinearInequality(): Flt64LinearInequality {
        return Flt64LinearInequality(LinearPolynomial(emptyList(), Flt64.zero), LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun toMathQuadraticInequality(): QuadraticInequality {
        return QuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.zero), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override fun evaluate(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListFlt64?, zeroIfNone: Boolean): Flt64? {
        @Suppress("UNCHECKED_CAST")
        return delegate().evaluate(values as Map<Symbol, V>)?.asFlt64()
    }

    // V-typed evaluate overrides
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        @Suppress("UNCHECKED_CAST")
        val v = delegate().evaluate(values as Map<Symbol, V>) ?: return null
        return converter.intoValue(v.asFlt64())
    }

    private fun delegate(): MathFunctionSymbol<V> = this

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = maskPoly.evaluateWith(values) ?: return zeroOf<V>()
        val maskD = maskValue.asFlt64().toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return zeroOf<V>()
        return input.evaluateWith(values)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val inputPoly = input.asFlt64Poly()
        val maskPolyF = maskPoly.asFlt64Poly()
        val mF = bigM.asFlt64()

        // maskPoly = maskVar constraint
        val maskMonos = maskPolyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, maskVar)
        val maskConstraint = Flt64LinearInequality(
            LinearPolynomial(monomials = maskMonos, constant = -maskPolyF.constant),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.EQ, "${name}_mask_eq")
        when (val r = model.addConstraint(relation = maskConstraint, name = maskConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c1: result - input <= M*(1 - mask_var_normalized)
        val c1Monos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar) +
            LinearMonomial(mF, maskVar)
        val c1 = Flt64LinearInequality(
            LinearPolynomial(monomials = c1Monos, constant = -inputPoly.constant),
            LinearPolynomial(monomials = emptyList(), constant = mF), Comparison.LE, "${name}_ub")
        when (val r = model.addConstraint(relation = c1, name = c1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c2: result - input >= -M*mask_var
        val c2Monos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar) +
            LinearMonomial(mF, maskVar)
        val c2 = Flt64LinearInequality(
            LinearPolynomial(monomials = c2Monos, constant = -inputPoly.constant),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.GE, "${name}_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: result <= M*mask_var
        val c3Monos = listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-mF, maskVar))
        val c3 = Flt64LinearInequality(
            LinearPolynomial(monomials = c3Monos, constant = Flt64.zero),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.LE, "${name}_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: result >= -M*mask_var
        val c4Monos = listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(mF, maskVar))
        val c4 = Flt64LinearInequality(
            LinearPolynomial(monomials = c4Monos, constant = Flt64.zero),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.GE, "${name}_zero_lb")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val inputPoly = input.asFlt64Poly()
        val maskPolyF = maskPoly.asFlt64Poly()
        val mF = bigM.asFlt64()

        // maskPoly = maskVar constraint
        val maskMonos = maskPolyF.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, maskVar)
        val maskConstraint = Flt64LinearInequality(
            LinearPolynomial(monomials = maskMonos, constant = -maskPolyF.constant),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.EQ, "${name}_mask_eq")
        when (val r = model.addConstraint(relation = maskConstraint, name = maskConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c1: result - input <= M*(1 - mask_var_normalized)
        val c1Monos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar) +
            LinearMonomial(mF, maskVar)
        val c1 = Flt64LinearInequality(
            LinearPolynomial(monomials = c1Monos, constant = -inputPoly.constant),
            LinearPolynomial(monomials = emptyList(), constant = mF), Comparison.LE, "${name}_ub")
        when (val r = model.addConstraint(relation = c1, name = c1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c2: result - input >= -M*mask_var
        val c2Monos = inputPoly.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } +
            LinearMonomial(Flt64.one, resultVar) +
            LinearMonomial(mF, maskVar)
        val c2 = Flt64LinearInequality(
            LinearPolynomial(monomials = c2Monos, constant = -inputPoly.constant),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.GE, "${name}_lb")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c3: result <= M*mask_var
        val c3Monos = listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-mF, maskVar))
        val c3 = Flt64LinearInequality(
            LinearPolynomial(monomials = c3Monos, constant = Flt64.zero),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.LE, "${name}_zero_ub")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // c4: result >= -M*mask_var
        val c4Monos = listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(mF, maskVar))
        val c4 = Flt64LinearInequality(
            LinearPolynomial(monomials = c4Monos, constant = Flt64.zero),
            LinearPolynomial(monomials = emptyList(), constant = Flt64.zero), Comparison.GE, "${name}_zero_lb")
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
class MaskingRangeFunction<V>(
    val mask: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(lower.asFlt64().toDouble() <= upper.asFlt64().toDouble()) {
            "MaskingRange lower bound must be <= upper bound"
        }
    }

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_mask_range")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val maskValue = mask.evaluate(values) ?: return zeroOf<V>()
        val maskD = maskValue.asFlt64().toDouble()
        if (maskD <= 1e-12 && maskD >= -1e-12) return zeroOf<V>()
        val yVal = values[resultVar]?.asFlt64()?.toDouble() ?: return zeroOf<V>()
        val lb = lower.asFlt64().toDouble() * maskD
        val ub = upper.asFlt64().toDouble() * maskD
        @Suppress("UNCHECKED_CAST")
        return Flt64(yVal.coerceIn(lb, ub)) as V
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
        val maskPoly = mask.asFlt64Poly()
        val lowerD = lower.asFlt64().toDouble()
        val upperD = upper.asFlt64().toDouble()

        // Upper: y - upper*mask <= 0
        val upperFlt = Flt64(upperD)
        val upperMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -upperFlt, it.symbol) }
        val upperLhs = LinearPolynomial(upperMonos, maskPoly.constant * -upperFlt)
        val upperRhs = LinearPolynomial(emptyList(), Flt64.zero)
        val upper = Flt64LinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_masking_range_ub")
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
        val lowerC = Flt64LinearInequality(lowerLhs, lowerRhs, Comparison.GE, "${name}_masking_range_lb")
        when (val r = model.addConstraint(relation = lowerC, name = lowerC.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

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
        val maskPoly = mask.asFlt64Poly()
        val lowerD = lower.asFlt64().toDouble()
        val upperD = upper.asFlt64().toDouble()

        // Upper: y - upper*mask <= 0
        val upperFlt = Flt64(upperD)
        val upperMonos = listOf(resultMon) +
            maskPoly.monomials.map { LinearMonomial(it.coefficient * -upperFlt, it.symbol) }
        val upperLhs = LinearPolynomial(upperMonos, maskPoly.constant * -upperFlt)
        val upperRhs = LinearPolynomial(emptyList(), Flt64.zero)
        val upper = Flt64LinearInequality(upperLhs, upperRhs, Comparison.LE, "${name}_masking_range_ub")
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
        val lowerC = Flt64LinearInequality(lowerLhs, lowerRhs, Comparison.GE, "${name}_masking_range_lb")
        when (val r = model.addConstraint(relation = lowerC, name = lowerC.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        operator fun <V> invoke(
            mask: LinearPolynomial<V>,
            lower: V,
            upper: V,
            name: String,
            displayName: String? = null
        ): MaskingRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaskingRangeFunction(mask, lower, upper, name, displayName)

        operator fun invoke(
            mask: LinearPolynomial<Flt64>,
            lower: Flt64,
            upper: Flt64,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MaskingRangeFunction(mask, lower, upper, name, displayName)
        )
    }
}
