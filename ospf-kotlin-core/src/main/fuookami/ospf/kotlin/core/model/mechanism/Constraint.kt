package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 符号化线性不等式 / Symbolic linear inequality
 *
 * @property inequality 符号化不等式 / The symbolic inequality
 */
class SymbolicLinearInequality<V : Ring<V>>(val inequality: LinearInequality<V>)

/**
 * 符号化二次不等式 / Symbolic quadratic inequality
 *
 * @property inequality 符号化不等式 / The symbolic inequality
 */
class SymbolicQuadraticInequality<V : Ring<V>>(val inequality: QuadraticInequalityOf<V>)

/**
 * 约束接口，定义约束的基本结构与判定能力。
 * Constraint interface defining the basic structure and evaluation capability of a constraint.
 *
 * @param V 数值类型 / The numeric type
 * @param P 约束类别 / The constraint category
 */
interface Constraint<V, P> where V : RealNumber<V>, V : NumberField<V>, P : Category {
    val lhs: List<Cell<V>>
    val sign: ConstraintRelation
    val rhs: V
    val lazy: Boolean
    val name: String
    val origin: MathConstraint?
    val from: Pair<IntermediateSymbol<*>, Boolean>?

    /**
     * 判定约束是否成立。
     * Checks whether the constraint is satisfied.
     *
     * @return 约束是否成立，若无法求值则返回 null / Whether the constraint holds, or null if evaluation is not possible
     */
    fun isTrue(): Boolean?
    /**
     * 使用给定求解结果判定约束是否成立。
     * Checks whether the constraint is satisfied using the given solution results.
     *
     * @param results 求解结果列表 / The solution result values
     * @return 约束是否成立，若无法求值则返回 null / Whether the constraint holds, or null if evaluation is not possible
     */
    fun isTrue(results: List<V>): Boolean?
}

/**
 * 元对偶解，包含按数学约束和中间符号分组的对偶价格。
 * Meta dual solution containing dual prices grouped by math constraint and intermediate symbol.
 *
 * @property constraints 按数学约束分组的对偶价格 / Dual prices grouped by math constraint
 * @property symbols     按中间符号分组的对偶价格 / Dual prices grouped by intermediate symbol
 */
data class MetaDualSolution(
    val constraints: Map<MathConstraint, Flt64>,
    val symbols: Map<IntermediateSymbol<*>, List<Pair<Constraint<Flt64, *>, Flt64>>>
)

/**
 * 将线性约束对偶解映射转换为元对偶解。
 * Converts a map of linear constraint dual solutions to a meta dual solution.
 *
 * @return 元对偶解 / The meta dual solution
 */
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

/**
 * 将二次约束对偶解映射转换为元对偶解。
 * Converts a map of quadratic constraint dual solutions to a meta dual solution.
 *
 * @return 元对偶解 / The meta dual solution
 */
@JvmName("quadraticDualSolutionToMetaDualSolution")
fun Map<Constraint<Flt64, Quadratic>, Flt64>.toMeta(): MetaDualSolution {
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
 * 约束实现的密封基类，提供约束求值和判定的通用逻辑。
 * Sealed base class for constraint implementations providing common evaluation and checking logic.
 *
 * @param V 数值类型 / The numeric type
 * @param P 约束类别（线性或二次） / The constraint category (linear or quadratic)
 * @property lhs  约束左端项单元列表 / The left-hand side cell list
 * @property sign 约束关系符号 / The constraint relation sign
 * @property lazy 是否为惰性约束 / Whether this is a lazy constraint
 * @property name 约束名称 / The constraint name
 * @property origin 原始数学约束 / The originating math constraint
 * @property from  关联的中间符号及方向 / The associated intermediate symbol and direction
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
 * 线性约束实现，基于线性单元列表表示约束左端项。
 * Linear constraint implementation representing the left-hand side with a list of linear cells.
 *
 * @param V 数值类型 / The numeric type
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
        ): Ret<LinearConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
            val constraintRelation = when (val result = relation.constraintRelation()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val flattenData = relation.flattenData
            val flt64Monomials = flattenData.monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) }
            val lhs = createLinearCells(flt64Monomials, tokens, converter)
            val rhs: V = -flattenData.constant
            return Ok(LinearConstraintImpl(
                lhs = lhs,
                sign = constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            ))
        }
    }
}

/**
 * 二次约束实现，基于二次单元列表表示约束左端项。
 * Quadratic constraint implementation representing the left-hand side with a list of quadratic cells.
 *
 * @param V 数值类型 / The numeric type
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
        ): Ret<QuadraticConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
            val constraintRelation = when (val result = relation.constraintRelation()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val flattenData = relation.flattenData
            val flt64Monomials = flattenData.monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) }
            val lhs = createQuadraticCells(flt64Monomials, tokens, converter)
            val rhs: V = -flattenData.constant
            return Ok(QuadraticConstraintImpl(
                lhs = lhs,
                sign = constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            ))
        }
    }
}

internal fun <V> createLinearCells(
    monomials: List<LinearMonomial<Flt64>>,
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

internal fun <V> createQuadraticCells(
    monomials: List<QuadraticMonomial<Flt64>>,
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
