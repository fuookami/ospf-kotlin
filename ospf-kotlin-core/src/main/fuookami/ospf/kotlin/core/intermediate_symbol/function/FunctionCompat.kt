@file:Suppress("unused", "DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInputV
import fuookami.ospf.kotlin.core.model.mechanism.compare
import fuookami.ospf.kotlin.core.model.mechanism.toTyped
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.VariableTypeKind
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.Flt64Bridge
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Triangle
import fuookami.ospf.kotlin.math.geometry.triangulate
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

internal val compatFlt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

// ========== IfElseFunction — 条件分支取值 ==========
// 原版: condition 选 branch, 1-condition 选 elseBranch
// 当前: 使用 MaskingWithPolyMaskFunction 实现条件掩码

class IfElseFunction<V>(
    val branch: Branch<V>,
    val elseBranch: Branch<V>,
    val condition: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String = "if_else",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    private val branchMasking: MaskingWithPolyMaskFunction<V> by lazy {
        MaskingWithPolyMaskFunction(
            input = branch.polynomial,
            maskPoly = condition,
            converter = converter,
            name = "${name}_branch",
            displayName = null
        )
    }

    private val elseMasking: MaskingWithPolyMaskFunction<V> by lazy {
        val oneMinusCondition = LinearPolynomial(
            condition.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
            converter.one - condition.constant
        )
        MaskingWithPolyMaskFunction(
            input = elseBranch.polynomial,
            maskPoly = oneMinusCondition,
            converter = converter,
            name = "${name}_else",
            displayName = null
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = branchMasking.helperVariables + elseMasking.helperVariables

    val result: LinearPolynomial<V> by lazy {
        val branchResult = branchMasking.resultVar
        val elseResult = elseMasking.resultVar
        LinearPolynomial(
            listOf(LinearMonomial(converter.one, branchResult), LinearMonomial(converter.one, elseResult)),
            converter.zero
        )
    }

    override val resultPolynomial: LinearPolynomial<V> get() = result

    override fun evaluate(values: Map<Symbol, V>): V? {
        val conditionValue = condition.evaluateWith(values) ?: return null
        return if (conditionValue gr converter.zero) {
            branch.polynomial.evaluateWith(values)
        } else {
            elseBranch.polynomial.evaluateWith(values)
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        when (val r1 = branchMasking.registerAuxiliaryTokens(tokens)) {
            is Failed -> return Failed(r1.error)
            is Fatal -> return Fatal(r1.errors)
            else -> {}
        }
        when (val r2 = elseMasking.registerAuxiliaryTokens(tokens)) {
            is Failed -> return Failed(r2.error)
            is Fatal -> return Fatal(r2.errors)
            else -> {}
        }
        return ok
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val r1 = branchMasking.registerConstraints(model)) {
            is Failed -> return Failed(r1.error)
            is Fatal -> return Fatal(r1.errors)
            else -> {}
        }
        when (val r2 = elseMasking.registerConstraints(model)) {
            is Failed -> return Failed(r2.error)
            is Fatal -> return Fatal(r2.errors)
            else -> {}
        }
        val one = converter.one
        val zero = converter.zero
        val sumConditions = LinearPolynomial(
            listOf(LinearMonomial(one, branchMasking.maskVar), LinearMonomial(one, elseMasking.maskVar)),
            zero
        )
        val rhsOne = LinearPolynomial(emptyList(), one)
        when (val r = addConstraints(model, listOf(LinearInequality(sumConditions, rhsOne, Comparison.EQ, "${name}_partition")))) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            else -> {}
        }
        return ok
    }

    data class Branch<V>(
        val polynomial: LinearPolynomial<V>,
        val name: String
    ) where V : RealNumber<V>, V : NumberField<V> {
        companion object {
            operator fun invoke(
                polynomial: ToLinearPolynomial<Flt64>,
                name: String
            ): Branch<Flt64> = Branch(polynomial.toLinearPolynomial(), name)
        }
    }

    companion object {
        operator fun <V> invoke(
            branch: Branch<V>,
            elseBranch: Branch<V>,
            condition: LinearPolynomial<V>,
            converter: IntoValue<V>,
            name: String = "if_else",
            displayName: String? = null
        ): IfElseFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfElseFunction(branch, elseBranch, condition, converter, name, displayName)

        operator fun invoke(
            branch: Branch<Flt64>,
            elseBranch: Branch<Flt64>,
            condition: LinearPolynomial<Flt64>,
            name: String = "if_else",
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            IfElseFunction(branch, elseBranch, condition, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )

        operator fun invoke(
            branch: Branch<Flt64>,
            elseBranch: Branch<Flt64>,
            condition: ToLinearPolynomial<Flt64>,
            name: String = "if_else",
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = invoke(
            branch = branch,
            elseBranch = elseBranch,
            condition = condition.toLinearPolynomial(),
            name = name,
            displayName = displayName
        )
    }
}

// ========== InStepRange — typealias 语义等价 ==========
typealias InStepRange<V> = InStepRangeFunction<V>

// ========== LinearFunction (quadratic) — typealias 语义等价 ==========
typealias LinearFunction<V> = QuadraticLinearFunction<V>

// ========== SatisfiedAmountPolynomialFunction — 多项式非零计数 ==========
// 原版: 判断 poly != 0 (neq zero)，负数多项式也算"满足"
// 当前: 使用 nonzeroIndicatorConstraintsV 编码 poly != 0 (正负都检测)

class SatisfiedAmountPolynomialFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    override var name: String = "satisfied_amount",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    private val indVars: List<AbstractVariableItem<*, *>> by lazy {
        polynomials.mapIndexed { i, _ -> BinVar("${name}_ind_$i") }
    }
    private val sideVars: List<AbstractVariableItem<*, *>> by lazy {
        polynomials.mapIndexed { i, _ -> BinVar("${name}_side_$i") }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = indVars + sideVars

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(
            indVars.map { LinearMonomial(converter.one, it) },
            converter.zero
        )
    }

    override val resultPolynomial: LinearPolynomial<V> get() = result

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v neq converter.zero) count++
        }
        return repeatAdd(converter.one, count)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        when (val r = tokens.add(indVars + sideVars)) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            else -> {}
        }
        return ok
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val bigM = converter.intoValue(Flt64(BIG_M_DEFAULT))
        val tolerance = converter.intoValue(Flt64(NONZERO_TOLERANCE))
        val strictBoundary = converter.intoValue(Flt64(STRICT_BOUNDARY))
        val allConstraints = mutableListOf<LinearInequality<V>>()
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraintsV(
                poly = polynomials[i],
                indVar = indVars[i],
                sideVar = sideVars[i],
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                namePrefix = "${name}_nz_$i"
            )
        }
        when (val r = addConstraints(model, allConstraints)) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            else -> {}
        }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            name: String = "satisfied_amount",
            displayName: String? = null
        ): SatisfiedAmountPolynomialFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SatisfiedAmountPolynomialFunction(polynomials, converter, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            name: String = "satisfied_amount",
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SatisfiedAmountPolynomialFunction(polynomials, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )
    }
}

// ========== AtLeastPolynomialFunction — 至少 amount 个多项式非零 ==========
// 原版: 至少 amount 个多项式满足 poly != 0
// 当前: 软约束 — amountFlag=1 表示满足, amountFlag=0 表示不满足, 不强制模型可行

class AtLeastPolynomialFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    val amount: UInt64,
    private val converter: IntoValue<V>,
    override var name: String = "at_least",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    init {
        assert(amount neq UInt64.zero)
        assert(UInt64(polynomials.size) geq amount)
    }

    private val indVars: List<AbstractVariableItem<*, *>> by lazy {
        polynomials.mapIndexed { i, _ -> BinVar("${name}_ind_$i") }
    }
    private val sideVars: List<AbstractVariableItem<*, *>> by lazy {
        polynomials.mapIndexed { i, _ -> BinVar("${name}_side_$i") }
    }

    private val amountFlag: AbstractVariableItem<*, *> by lazy { BinVar("${name}_y") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = indVars + sideVars + amountFlag

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, amountFlag)), converter.zero)
    }

    override val resultPolynomial: LinearPolynomial<V> get() = result

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v neq converter.zero) count++
        }
        return if (count >= amount.toInt()) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        when (val r = tokens.add(indVars + sideVars + amountFlag)) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            else -> {}
        }
        return ok
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val bigM = converter.intoValue(Flt64(BIG_M_DEFAULT))
        val tolerance = converter.intoValue(Flt64(NONZERO_TOLERANCE))
        val strictBoundary = converter.intoValue(Flt64(STRICT_BOUNDARY))
        val n = polynomials.size
        val amountInt = amount.toInt()
        val amountValue = repeatAdd(one, amountInt)

        val nonzeroConstraints = mutableListOf<LinearInequality<V>>()
        for (i in polynomials.indices) {
            nonzeroConstraints += nonzeroIndicatorConstraintsV(
                poly = polynomials[i],
                indVar = indVars[i],
                sideVar = sideVars[i],
                bigM = bigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                namePrefix = "${name}_nz_$i"
            )
        }
        when (val r = addConstraints(model, nonzeroConstraints)) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            else -> {}
        }

        // sumPoly = sum(indVars): indVar=1 means poly!=0, so sum counts nonzero polynomials
        val sumPoly = LinearPolynomial(
            indVars.map { LinearMonomial(one, it) },
            zero
        )

        // Soft constraints for amountFlag:
        //   flag=1 (satisfied): sum >= amount, sum <= n (always true)
        //   flag=0 (not satisfied): sum >= 0 (always true), sum <= amount-1
        val slack = repeatAdd(one, n - amountInt + 1)

        // sum - amount * flag >= 0
        //   flag=1: sum >= amount (satisfied)
        //   flag=0: sum >= 0 (always true)
        val flagLb = LinearInequality(
            LinearPolynomial(sumPoly.monomials + listOf(LinearMonomial(-amountValue, amountFlag)), sumPoly.constant),
            LinearPolynomial(emptyList(), zero),
            Comparison.GE, "${name}_flag_lb"
        )
        // sum - (amount-1) - slack * flag <= 0
        //   flag=1: sum <= amount-1 + slack = n (always true)
        //   flag=0: sum <= amount-1 (not satisfied)
        val flagUb = LinearInequality(
            LinearPolynomial(sumPoly.monomials + listOf(LinearMonomial(-slack, amountFlag)), sumPoly.constant),
            LinearPolynomial(emptyList(), repeatAdd(one, amountInt - 1)),
            Comparison.LE, "${name}_flag_ub"
        )
        when (val r = addConstraints(model, listOf(flagLb, flagUb))) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            else -> {}
        }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            amount: UInt64,
            converter: IntoValue<V>,
            name: String = "at_least",
            displayName: String? = null
        ): AtLeastPolynomialFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AtLeastPolynomialFunction(polynomials, amount, converter, name, displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            amount: UInt64,
            name: String = "at_least",
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            AtLeastPolynomialFunction(polynomials, amount, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )
    }
}

// ========== UIntegerSlackFunction — 整数松弛变量 ==========
// 原版: UIntegerSlackFunction(x, y, withNegative, withPositive, threshold, constraint, name, displayName)
// 当前: SlackFunction<V> 带 type = UInteger, constraint=false 时跳过约束注册

class UIntegerSlackFunction<V>(
    private val delegate: SlackFunction<V>,
    private val registerConstraints_: Boolean,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    constructor(
        x: LinearPolynomial<V>,
        y: LinearPolynomial<V>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        converter: IntoValue<V>,
        name: String,
        displayName: String? = null
    ) : this(
        delegate = SlackFunction(
            x = x, y = y, type = UInteger,
            withNegative = withNegative, withPositive = withPositive,
            threshold = threshold, converter = converter, name = name, displayName = displayName
        ),
        registerConstraints_ = constraint,
        name = name, displayName = displayName
    )

    val neg: LinearPolynomial<V>? get() = delegate.neg
    val pos: LinearPolynomial<V>? get() = delegate.pos
    val polyX: LinearPolynomial<V> get() = delegate.polyX

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    override val resultPolynomial: LinearPolynomial<V> get() = polyX

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try =
        delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        if (registerConstraints_) delegate.registerConstraints(model) else ok

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): UIntegerSlackFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            UIntegerSlackFunction(x, y, withNegative, withPositive, threshold, constraint, converter, name, displayName)

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            y: LinearPolynomial<Flt64>,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            UIntegerSlackFunction(x, y, withNegative, withPositive, threshold, constraint, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )
    }
}

// ========== URealSlackFunction — 实数松弛变量 ==========
// 原版: URealSlackFunction(x, y, withNegative, withPositive, threshold, constraint, name, displayName)
// 当前: SlackFunction<V> 带 type = UContinuous, constraint=false 时跳过约束注册

class URealSlackFunction<V>(
    private val delegate: SlackFunction<V>,
    private val registerConstraints_: Boolean,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    constructor(
        x: LinearPolynomial<V>,
        y: LinearPolynomial<V>,
        withNegative: Boolean = true,
        withPositive: Boolean = true,
        threshold: Boolean = false,
        constraint: Boolean = true,
        converter: IntoValue<V>,
        name: String,
        displayName: String? = null
    ) : this(
        delegate = SlackFunction(
            x = x, y = y, type = UContinuous,
            withNegative = withNegative, withPositive = withPositive,
            threshold = threshold, converter = converter, name = name, displayName = displayName
        ),
        registerConstraints_ = constraint,
        name = name, displayName = displayName
    )

    val neg: LinearPolynomial<V>? get() = delegate.neg
    val pos: LinearPolynomial<V>? get() = delegate.pos
    val polyX: LinearPolynomial<V> get() = delegate.polyX

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    override val resultPolynomial: LinearPolynomial<V> get() = polyX

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try =
        delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        if (registerConstraints_) delegate.registerConstraints(model) else ok

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): URealSlackFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            URealSlackFunction(x, y, withNegative, withPositive, threshold, constraint, converter, name, displayName)

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            y: LinearPolynomial<Flt64>,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            URealSlackFunction(x, y, withNegative, withPositive, threshold, constraint, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )
    }
}

// ========== UIntegerSlackRangeFunction — 整数松弛范围变量 ==========
// 原版: UIntegerSlackRangeFunction(x, lb, ub, constraint, name, displayName)
// 当前: SlackRangeFunction<V> 带 type = UInteger

class UIntegerSlackRangeFunction<V>(
    private val delegate: SlackRangeFunction<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    constructor(
        x: LinearPolynomial<V>,
        lb: LinearPolynomial<V>,
        ub: LinearPolynomial<V>,
        constraint: Boolean = true,
        converter: IntoValue<V>,
        name: String,
        displayName: String? = null
    ) : this(
        delegate = SlackRangeFunction(
            x = x, lb = lb, ub = ub, type = UInteger,
            constraint = constraint, converter = converter,
            name = name, displayName = displayName
        ),
        name = name, displayName = displayName
    )

    val neg: LinearPolynomial<V> get() = delegate.neg
    val pos: LinearPolynomial<V> get() = delegate.pos
    val polyX: LinearPolynomial<V> get() = delegate.polyX

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    override val resultPolynomial: LinearPolynomial<V> get() = polyX

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try =
        delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        delegate.registerConstraints(model)

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): UIntegerSlackRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            UIntegerSlackRangeFunction(x, lb, ub, constraint, converter, name, displayName)

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            UIntegerSlackRangeFunction(x, lb, ub, constraint, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )
    }
}

// ========== URealSlackRangeFunction — 实数松弛范围变量 ==========
// 原版: URealSlackRangeFunction(x, lb, ub, constraint, name, displayName)
// 当前: SlackRangeFunction<V> 带 type = UContinuous

class URealSlackRangeFunction<V>(
    private val delegate: SlackRangeFunction<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    constructor(
        x: LinearPolynomial<V>,
        lb: LinearPolynomial<V>,
        ub: LinearPolynomial<V>,
        constraint: Boolean = true,
        converter: IntoValue<V>,
        name: String,
        displayName: String? = null
    ) : this(
        delegate = SlackRangeFunction(
            x = x, lb = lb, ub = ub, type = UContinuous,
            constraint = constraint, converter = converter,
            name = name, displayName = displayName
        ),
        name = name, displayName = displayName
    )

    val neg: LinearPolynomial<V> get() = delegate.neg
    val pos: LinearPolynomial<V> get() = delegate.pos
    val polyX: LinearPolynomial<V> get() = delegate.polyX

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    override val resultPolynomial: LinearPolynomial<V> get() = polyX

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try =
        delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        delegate.registerConstraints(model)

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): URealSlackRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            URealSlackRangeFunction(x, lb, ub, constraint, converter, name, displayName)

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            URealSlackRangeFunction(x, lb, ub, constraint, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )
    }
}

// ========== MonotoneUnivariateLinearPiecewiseFunction — 单调分段线性 ==========
// 原版: 独立子类，断言单调递增，points 构造
// 当前: 独立类（非 typealias），委托给 UnivariateLinearPiecewiseFunction

class MonotoneUnivariateLinearPiecewiseFunction<V>(
    private val delegate: UnivariateLinearPiecewiseFunction<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    override val resultPolynomial: LinearPolynomial<V>
        get() = (delegate as? HasResultPolynomial<V>)?.resultPolynomial
            ?: delegate.result

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try =
        delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        delegate.registerConstraints(model)

    companion object {
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            breakpoints: List<Flt64>,
            slopes: List<Flt64>,
            intercepts: List<Flt64>,
            m: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            MonotoneUnivariateLinearPiecewiseFunction(
                delegate = UnivariateLinearPiecewiseFunction(
                    x = x, breakpoints = breakpoints, slopes = slopes, intercepts = intercepts,
                    m = m, converter = compatFlt64Converter, name = name, displayName = displayName
                ),
                name = name, displayName = displayName
            ),
            converter = compatFlt64Converter
        )

        @JvmStatic
        @JvmName("fromPoints")
        fun fromPoints(
            x: LinearPolynomial<Flt64>,
            points: List<Point<Dim2, Flt64>>,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val delegate = UnivariateLinearPiecewiseFunction.fromPoints(x, points, m, name, displayName)
            return LinearFunctionSymbolAdapter(
                MonotoneUnivariateLinearPiecewiseFunction(delegate, name, displayName),
                converter = compatFlt64Converter
            )
        }
    }
}

// ========== IsolineBivariateLinearPiecewiseFunction — 等值线分段线性 ==========
// 原版: 独立子类，isolines 构造
// 当前: 独立类（非 typealias），委托给 BivariateLinearPiecewiseFunction

class IsolineBivariateLinearPiecewiseFunction<V>(
    private val delegate: BivariateLinearPiecewiseFunction<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    override val resultPolynomial: LinearPolynomial<V>
        get() = (delegate as? HasResultPolynomial<V>)?.resultPolynomial
            ?: delegate.result

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try =
        delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        delegate.registerConstraints(model)

    companion object {
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            y: LinearPolynomial<Flt64>,
            triangles: List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            IsolineBivariateLinearPiecewiseFunction(
                delegate = BivariateLinearPiecewiseFunction(
                    x = x, y = y, triangles = triangles,
                    converter = compatFlt64Converter, name = name, displayName = displayName
                ),
                name = name, displayName = displayName
            ),
            converter = compatFlt64Converter
        )

        @JvmStatic
        @JvmName("fromPoints")
        fun fromPoints(
            x: LinearPolynomial<Flt64>,
            y: LinearPolynomial<Flt64>,
            points: List<Point<Dim3, Flt64>>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val triangles = triangulate(points)
            return invoke(x, y, triangles, name, displayName)
        }

        @JvmStatic
        @JvmName("fromIsolines")
        fun fromIsolines(
            x: LinearPolynomial<Flt64>,
            y: LinearPolynomial<Flt64>,
            isolines: List<Pair<Flt64, List<Point<Dim2, Flt64>>>>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val triangles = triangulate(isolines)
            return invoke(x, y, triangles, name, displayName)
        }
    }
}

// ========== InListFunction — 列表包含检查 ==========
// 原版: InListFunction(x, list, name, displayName) — x 等于 list 中某一项
// 当前: 继承 SatisfiedAmountInequalityFunction，等式约束列表

class InListFunction<V>(
    xSymbol: LinearIntermediateSymbol<V>,
    listSymbols: List<LinearIntermediateSymbol<V>>,
    converter: IntoValue<V>,
    name: String = "in_list",
    displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = listSymbols.map { item ->
        LinearConstraintInputV.from(
            relation = LinearInequality(
                xSymbol.toLinearPolynomial(),
                item.toLinearPolynomial(),
                Comparison.EQ
            ),
            converter = converter,
            lhsRange = ValueRange(Flt64.zero, Flt64.zero).value!!,
            rhsConstant = converter.zero
        )
    },
    amount = ValueRange(UInt64.one, UInt64(listSymbols.size)).value!!,
    epsilon = converter.intoValue(Flt64(1e-6)),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            x: LinearIntermediateSymbol<V>,
            list: List<LinearIntermediateSymbol<V>>,
            bridge: Flt64Bridge<V>,
            name: String = "in_list",
            displayName: String? = null
        ): InListFunction<V> where V : RealNumber<V>, V : NumberField<V> = InListFunction(
            xSymbol = x,
            listSymbols = list,
            converter = IntoValue.fromBridge(bridge),
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            list: List<LinearIntermediateSymbol<Flt64>>,
            name: String = "in_list",
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            InListFunction(x, list, compatFlt64Converter, name, displayName),
            converter = compatFlt64Converter
        )

        operator fun invoke(
            x: ToLinearPolynomial<Flt64>,
            list: List<ToLinearPolynomial<Flt64>>,
            name: String = "in_list",
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val xSymbol = LinearExpressionSymbol(
                polynomial = x.toLinearPolynomial(),
                name = "${name}_x"
            )
            val listSymbols = list.mapIndexed { i, item ->
                LinearExpressionSymbol(
                    polynomial = item.toLinearPolynomial(),
                    name = "${name}_item_$i"
                )
            }
            return LinearFunctionSymbolAdapter(
                InListFunction(xSymbol, listSymbols, compatFlt64Converter, name, displayName),
                converter = compatFlt64Converter
            )
        }
    }
}