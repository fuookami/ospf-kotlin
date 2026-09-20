package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble

/**
 * 有限二次输入的函数组合基类。二次输入通过精确等式绑定，仿射输入直接复用。
 * / Function composition with bounded quadratic inputs; exact equalities bind quadratic inputs, affine inputs pass through.
 *
 * @param inputs 输入多项式；构造时复制 / input polynomials, copied at construction
 * @property converter 数值转换器 / value converter
 * @property name 符号名称 / symbol name
 * @property displayName 显示名称 / display name
 */
abstract class QuadraticFunctionSymbol<V> protected constructor(
    inputs: List<QuadraticPolynomial<V>>,
    protected val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V>
        where V : RealNumber<V>, V : NumberField<V> {
    private val inputs = inputs.map { QuadraticPolynomial(it.monomials.toList(), it.constant) }
    private val bounds by lazy { this.inputs.map { it.finiteBounds(converter) } }
    private val bridges by lazy {
        this.inputs.mapIndexed { index, input ->
            if (input.monomials.any { it.isQuadratic || it.symbol1 is QuadraticIntermediateSymbol<*> }) {
                RealVar("${name}_input_$index").also { variable ->
                    bounds[index]?.let { range ->
                        val lower = converter.fromValue(range.lower)
                        val upper = converter.fromValue(range.upper)
                        if (lower.isFinite() && upper.isFinite()) {
                            variable.range.geq(if (converter.intoValue(lower) > range.lower) {
                                Flt64(Math.nextDown(lower.toSolverDouble("${name}.input.$index.lower")))
                            } else lower)
                            variable.range.leq(if (converter.intoValue(upper) < range.upper) {
                                Flt64(Math.nextUp(upper.toSolverDouble("${name}.input.$index.upper")))
                            } else upper)
                        }
                    }
                }
            } else null
        }
    }
    private val linearInputs by lazy {
        this.inputs.mapIndexed { index, input ->
            bridges[index]?.let {
                LinearPolynomial(listOf(LinearMonomial(converter.one, it)), converter.zero)
            } ?: LinearPolynomial(
                input.monomials.map { LinearMonomial(it.coefficient, it.symbol1) },
                input.constant
            )
        }
    }
    private val function by lazy { createFunction(linearInputs) }
    private val resultAdapter by lazy { LinearFunctionSymbolAdapter(function, converter) }

    /**
     * 为已绑定的输入构造对应线性函数。 / Construct the linear function for bound inputs.
     * @param inputs 已绑定的仿射输入 / bound affine inputs
     * @return 线性语义实现 / linear semantic implementation
     */
    protected abstract fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V>

    /** 辅助变量包含输入绑定变量与函数变量。 / Helpers include input bindings and function variables. */
    val helperVariables: List<AbstractVariableItem<*, *>>
        get() = bridges.filterNotNull() + function.helperVariables

    override val identifier: UInt64 by lazy { IdentifierGenerator.gen() }
    override val index: Int = 0
    override val category: Category = Linear
    override val operationCategory: Category = Quadratic
    override val cached: Boolean = false
    override val dependencies: Set<IntermediateSymbol<*>>
        get() = inputs.flatMap { input ->
            input.monomials.flatMap { listOfNotNull(it.symbol1, it.symbol2) }
        }.filterIsInstance<IntermediateSymbol<*>>().toSet()
    override val range: ExpressionRange<V>
        get() = ExpressionRange(converter.zero.constants).also { range ->
            resultAdapter.polynomial.finiteBounds(converter)?.let { bounds ->
                range.geq(bounds.lower)
                range.leq(bounds.upper)
            }
        }
    override val polynomial: QuadraticPolynomial<V>
        get() = resultAdapter.polynomial.let { result ->
            QuadraticPolynomial(
                result.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) },
                result.constant
            )
        }

    override fun asMutable(): MutableQuadraticPolynomial<V> =
        MutableQuadraticPolynomial(polynomial.monomials, polynomial.constant)

    override fun flush(force: Boolean) {
        dependencies.forEach { it.flush(force) }
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    private fun validate(): Try {
        if (inputs.isEmpty()) {
            return Failed(ErrorCode.IllegalArgument, "输入不得为空 / Inputs must not be empty")
        }
        for ((index, input) in inputs.withIndex()) {
            val captured = bounds[index]
            val current = input.finiteBounds(converter)
            if (captured == null || current == null || current.lower < captured.lower || current.upper > captured.upper ||
                linearInputs[index].finiteBounds(converter) == null) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "$name 输入 $index 必须具有有限且未扩大的范围 / Input $index must have finite, unexpanded bounds"
                )
            }
        }
        return ok
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = validate()) {
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
            is Ok -> {}
        }
        val preparation = AutoTokenTable<V>(Quadratic, false)
        try {
            when (val result = function.registerAuxiliaryTokens(preparation)) {
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
                is Ok -> {}
            }
        } finally {
            preparation.close()
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        when (val validation = validate()) {
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
            is Ok -> {}
        }
        val pending = mutableListOf<QuadraticInequalityOf<V>>()
        for ((index, bridge) in bridges.withIndex()) {
            if (bridge != null) {
                pending += QuadraticInequalityOf(
                    lhs = inputs[index],
                    rhs = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, bridge)), converter.zero),
                    comparison = Comparison.EQ,
                    name = "${name}_input_$index"
                )
            }
        }
        val target = object : AbstractLinearMechanismModel<V> by model {
            override fun rollbackConstraintsTo(size: Int): Try {
                pending.clear()
                return ok
            }

            override fun addConstraint(
                relation: LinearInequality<V>,
                name: String?,
                from: Pair<IntermediateSymbol<out V>, Boolean>?
            ): Try {
                fun promote(input: LinearPolynomial<V>) = QuadraticPolynomial(
                    input.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) },
                    input.constant
                )
                pending += QuadraticInequalityOf(
                    lhs = promote(relation.lhs),
                    rhs = promote(relation.rhs),
                    comparison = relation.comparison,
                    name = name ?: relation.name
                )
                return ok
            }
        }
        when (val result = function.registerConstraints(target)) {
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Ok -> {}
        }
        return addQuadraticConstraints(model, pending) ?: ok
    }

    private fun evaluateWithResolver(resolve: (Symbol) -> V?): V? {
        if (inputs.isEmpty()) return null
        val values = mutableMapOf<Symbol, V>()
        for ((index, input) in inputs.withIndex()) {
            var total = input.constant
            for (monomial in input.monomials) {
                val first = resolve(monomial.symbol1) ?: return null
                values[monomial.symbol1] = first
                var term = monomial.coefficient * first
                monomial.symbol2?.let { symbol ->
                    val second = resolve(symbol) ?: return null
                    values[symbol] = second
                    term *= second
                }
                total += term
            }
            bridges[index]?.let { values[it] = total }
        }
        return function.evaluate(values)
    }

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? =
        evaluate(values ?: emptyMap(), tokenTable, converter, false)

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? =
        evaluate(emptyMap(), tokenTable, converter, zeroIfNone)

    override fun evaluate(
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = evaluateWithResolver { symbol ->
        when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable.indexOf(symbol)?.let { results.getOrNull(it) }
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol)
                .evaluate(results, tokenTable, converter, zeroIfNone)
            else -> null
        } ?: if (zeroIfNone) converter.zero else null
    }

    override fun evaluate(
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = evaluateWithResolver { symbol ->
        values[symbol] ?: when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable?.find(symbol)?.result
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol)
                .evaluate(values, tokenTable, converter, zeroIfNone)
            else -> null
        } ?: if (zeroIfNone) converter.zero else null
    }
}
