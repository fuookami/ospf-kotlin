/**
 * 约束与表达式分类
 * Constraint and expression category types
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/** 符号线性不等式包装 / Symbolic linear inequality wrapper */
class SymbolicLinearInequality<V : Ring<V>>(val inequality: LinearInequality<V>)

/** 符号二次不等式包装 / Symbolic quadratic inequality wrapper */
class SymbolicQuadraticInequality<V : Ring<V>>(val inequality: QuadraticInequalityOf<V>)

// ========== Constraint<V, P> ==========

/**
 * 泛型约束接口
 * Generic constraint interface
 *
 * 包含 V 类型值的约束。Flt64 求值由求解器适配器处理。
 * Constraint with V-typed values. Flt64 evaluation is handled by the solver adapter.
 *
 * @param V 数值类型 / The number type
 * @param P 多项式类别（线性/二次）/ Polynomial category (linear/quadratic)
 * @property lhs 左侧单元列表 / Left-hand side cell list
 * @property sign 约束关系符号 / Constraint relation sign
 * @property rhs 右侧常量 / Right-hand side constant
 * @property lazy 是否延迟求值 / Whether lazy evaluation
 * @property name 约束名称 / Constraint name
 * @property origin 来源约束 / Origin constraint
 * @property from 来源符号和标志 / Source symbol and flag
 */
interface Constraint<V, P> where V : RealNumber<V>, V : NumberField<V>, P : Category {
    val lhs: List<Cell<V>>
    val sign: ConstraintRelation
    val rhs: V
    val lazy: Boolean
    val name: String
    val origin: MathConstraint?
    val from: Pair<IntermediateSymbol<*>, Boolean>?

    fun isTrue(): Boolean?
    fun isTrue(results: List<V>): Boolean?
}

/**
 * 元对偶解，将约束和符号映射到对偶值。
 * Meta dual solution mapping constraints and symbols to dual values.
 *
 * @property constraints 约束到对偶值的映射 / Mapping from constraints to dual values
 * @property symbols     符号到对偶值的映射 / Mapping from symbols to dual values
 */
data class MetaDualSolution(
    val constraints: Map<MathConstraint, Flt64>,
    val symbols: Map<IntermediateSymbol<*>, List<Pair<Constraint<Flt64, *>, Flt64>>>
)

@JvmName("linearDualSolutionToMetaDualSolution")
fun kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>.toMeta(): MetaDualSolution {
    return MetaDualSolution(
        constraints = this
            .filterKeys { it.origin != null }
            .mapKeys { it.key.origin!! },
        symbols = this
            .filterKeys { it.from != null }
            .entries
            .groupBy { it.key.from!!.first }
            .mapValues { prices -> prices.value.map { it.toPair() } }
    )
}

@JvmName("quadraticDualSolutionToMetaDualSolution")
fun kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>.toMeta(): MetaDualSolution {
    return MetaDualSolution(
        constraints = this
            .filterKeys { it.origin != null }
            .mapKeys { it.key.origin!! },
        symbols = this
            .filterKeys { it.from != null }
            .entries
            .groupBy { it.key.from!!.first }
            .mapValues { prices -> prices.value.map { it.toPair() } }
    )
}

/**
 * 约束实现密封基类
 * Sealed constraint implementation base class
 *
 * 提供 LHS 求值和约束判断能力。
 * Provides LHS evaluation and constraint checking.
 *
 * @param V 数值类型 / The number type
 * @param P 多项式类别 / Polynomial category
 * @property lhs 左侧单元列表 / Left-hand side cell list
 * @property sign 约束关系符号 / Constraint relation sign
 * @property lazy 是否延迟求值 / Whether lazy evaluation
 * @property name 约束名称 / Constraint name
 * @property origin 来源约束 / Origin constraint
 * @property from 来源符号和标志 / Source symbol and flag
 */
sealed class ConstraintImpl<V, P : Category>(
    override val lhs: List<Cell<V>>,
    override val sign: ConstraintRelation,
    private val _rhs: V,
    override val lazy: Boolean,
    override val name: String = "",
    override val origin: MathConstraint? = null,
    override val from: Pair<IntermediateSymbol<*>, Boolean>? = null
) : Constraint<V, P> where V : RealNumber<V>, V : NumberField<V> {
    override val rhs: V get() = _rhs

    override fun isTrue(): Boolean? {
        var lhsValue = _rhs - _rhs
        for (cell in lhs) {
            lhsValue += cell.evaluate() ?: return null
        }
        return sign(lhsValue, _rhs)
    }

    override fun isTrue(results: List<V>): Boolean? {
        var lhsValue = _rhs - _rhs
        for (cell in lhs) {
            lhsValue += cell.evaluate(results) ?: return null
        }
        return sign(lhsValue, _rhs)
    }
}

/**
 * 线性约束实现
 * Linear constraint implementation
 *
 * @param V 数值类型 / The number type
 * @property lhs 线性单元列表 / Linear cell list
 */
class LinearConstraintImpl<V>(
    override val lhs: List<LinearCell<V>>,
    sign: ConstraintRelation,
    rhs: V,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
) : ConstraintImpl<V, Linear>(
    lhs = lhs,
    sign = sign,
    _rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            relation: LinearRelation<V>,
            tokens: AbstractTokenTable<V>,
            converter: IntoValue<V>,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): LinearConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
            val flattenData = relation.flattenData
            val flt64Monomials = flattenData.monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) }
            val lhs = createLinearCells(flt64Monomials, tokens, converter)
            val rhs: V = -flattenData.constant
            return LinearConstraintImpl(
                lhs = lhs,
                sign = relation.constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            )
        }
    }
}

/**
 * 二次约束实现
 * Quadratic constraint implementation
 *
 * @param V 数值类型 / The number type
 * @property lhs 二次单元列表 / Quadratic cell list
 */
class QuadraticConstraintImpl<V>(
    override val lhs: List<QuadraticCell<V>>,
    sign: ConstraintRelation,
    rhs: V,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null
) : ConstraintImpl<V, Quadratic>(
    lhs = lhs,
    sign = sign,
    _rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            relation: QuadraticRelation<V>,
            tokens: AbstractTokenTable<V>,
            converter: IntoValue<V>,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): QuadraticConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
            val flattenData = relation.flattenData
            val flt64Monomials = flattenData.monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) }
            val lhs = createQuadraticCells(flt64Monomials, tokens, converter)
            val rhs: V = -flattenData.constant
            return QuadraticConstraintImpl(
                lhs = lhs,
                sign = relation.constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            )
        }
    }
}

// Type aliases for Constraint<V, P> with specific polynomial kinds

/**
 * 从线性单项式和符号表创建线性单元列表
 * Create linear cells from linear monomials and token table
 *
 * @param V 数值类型 / The number type
 * @param monomials Flt64 线性单项式列表 / Flt64 linear monomial list
 * @param tokens 符号表 / Token table
 * @param converter 值转换器 / Value converter
 * @return 线性单元列表 / Linear cell list
 */
internal fun <V> createLinearCells(
    monomials: List<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>
): ArrayList<LinearCell<V>> where V : RealNumber<V>, V : NumberField<V> {
    val cells = ArrayList<LinearCell<V>>()
    for (monomial in monomials) {
        val variable = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokens.find(variable)
        if (token != null && monomial.coefficient neq Flt64.zero) {
            cells.add(LinearCellImpl(tokens, monomial.coefficient, token, converter))
        }
    }
    return cells
}

/**
 * 从二次单项式和符号表创建二次单元列表
 * Create quadratic cells from quadratic monomials and token table
 *
 * @param V 数值类型 / The number type
 * @param monomials Flt64 二次单项式列表 / Flt64 quadratic monomial list
 * @param tokens 符号表 / Token table
 * @param converter 值转换器 / Value converter
 * @return 二次单元列表 / Quadratic cell list
 */
internal fun <V> createQuadraticCells(
    monomials: List<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>
): ArrayList<QuadraticCell<V>> where V : RealNumber<V>, V : NumberField<V> {
    val cells = ArrayList<QuadraticCell<V>>()
    for (monomial in monomials) {
        val variable1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val token1 = tokens.find(variable1)
        val token2 = if (monomial.symbol2 != null) {
            tokens.find(monomial.symbol2 as? AbstractVariableItem<*, *> ?: continue) ?: continue
        } else {
            null
        }
        if (token1 != null && monomial.coefficient neq Flt64.zero) {
            cells.add(QuadraticCellImpl(tokens, monomial.coefficient, token1, token2, converter))
        }
    }
    return cells
}
