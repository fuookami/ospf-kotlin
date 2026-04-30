package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCellFlt64
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellFlt64
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Generic SubObject<V> with typed public evaluation and Flt64 solver evaluation.
 * 泛型 SubObject<V>：公开求值使用 V，求解器边界求值使用 Flt64。
 */
sealed class SubObject<V : RealNumber<V>>(
    val category: ObjectCategory,
    val name: String = ""
) {
    abstract val cells: List<Cell<V>>

    /** Flt64 constant for solver-boundary callers. / 面向求解器边界的 Flt64 常量。 */
    abstract val constantFlt64: Flt64

    /** V-typed constant for public callers. / 面向调用方的 V 类型常量。 */
    abstract val constant: V

    /** Flt64 evaluation for solver-boundary callers. / 面向求解器边界的 Flt64 求值。 */
    fun evaluateFlt64(): Flt64? {
        var ret = constantFlt64
        for (cell in cells) {
            ret += cell.evaluateFlt64() ?: return null
        }
        return ret
    }

    fun evaluateFlt64(results: Solution): Flt64? {
        var ret = constantFlt64
        for (cell in cells) {
            ret += cell.evaluateFlt64(results) ?: return null
        }
        return ret
    }

    /** V-typed evaluation for public callers. / 面向调用方的 V 类型求值。 */
    abstract fun evaluate(): V?
    abstract fun evaluate(results: List<V>): V?

    /** Explicit conversion helper kept for compatibility. / 保留给兼容路径使用的显式转换工具。 */
    fun evaluateAsV(converter: IntoValue<V>): V? = evaluateFlt64()?.let { converter.intoValue(it) }

    /** Explicit constant conversion helper kept for compatibility. / 保留给兼容路径使用的常量转换工具。 */
    fun constantAsV(converter: IntoValue<V>): V = converter.intoValue(constantFlt64)
}

class LinearSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell<V>>,
    private val _constant: V,
    private val _constantFlt64: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constantFlt64: Flt64 get() = _constantFlt64
    override val constant: V get() = _constant

    override fun evaluate(): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    override fun evaluate(results: List<V>): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }

    companion object {
        /**
         * Create LinearSubObject from LinearFlattenDataFlt64.
         * 从 LinearFlattenDataFlt64 创建 LinearSubObject。
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenDataFlt64,
            tokens: AbstractTokenTableFlt64,
            name: String = ""
        ): LinearSubObjectFlt64 {
            val cells = createLinearCells(flattenData.monomials, tokens)
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                _constantFlt64 = flattenData.constant,
                name = name
            )
        }

        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenDataFlt64,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): LinearSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = ArrayList<LinearCell<V>>(flattenData.monomials.size)
            for (monomial in flattenData.monomials) {
                val variable = monomial.symbol as AbstractVariableItem<*, *>
                val token = tokens.find(variable)
                if (token != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(
                        fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl(
                            tokenTable = tokens,
                            _coefficientFlt64 = monomial.coefficient,
                            token = token,
                            converter = converter
                        )
                    )
                }
            }
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = converter.intoValue(flattenData.constant),
                _constantFlt64 = flattenData.constant,
                name = name
            )
        }
    }
}

class QuadraticSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCell<V>>,
    private val _constant: V,
    private val _constantFlt64: Flt64 = Flt64.zero,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constantFlt64: Flt64 get() = _constantFlt64
    override val constant: V get() = _constant

    override fun evaluate(): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate() ?: return null
        }
        return ret
    }

    override fun evaluate(results: List<V>): V? {
        var ret = constant
        for (cell in cells) {
            ret += cell.evaluate(results) ?: return null
        }
        return ret
    }

    companion object {
        /**
         * Create QuadraticSubObject from QuadraticFlattenDataFlt64.
         * 从 QuadraticFlattenDataFlt64 创建 QuadraticSubObject。
         */
        operator fun invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenDataFlt64,
            tokens: AbstractTokenTableFlt64,
            name: String = ""
        ): QuadraticSubObjectFlt64 {
            val cells = createQuadraticCells(flattenData.monomials, tokens)
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                _constantFlt64 = flattenData.constant,
                name = name
            )
        }

        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenDataFlt64,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): QuadraticSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = ArrayList<QuadraticCell<V>>(flattenData.monomials.size)
            for (monomial in flattenData.monomials) {
                val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                val token1 = tokens.find(variable1)
                val token2 = if (monomial.symbol2 != null) {
                    tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
                } else {
                    null
                }
                if (token1 != null && monomial.coefficient neq Flt64.zero) {
                    cells.add(
                        fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl(
                            tokenTable = tokens,
                            _coefficientFlt64 = monomial.coefficient,
                            token1 = token1,
                            token2 = token2,
                            converter = converter
                        )
                    )
                }
            }
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = converter.intoValue(flattenData.constant),
                _constantFlt64 = flattenData.constant,
                name = name
            )
        }
    }
}

typealias LinearSubObjectFlt64 = LinearSubObject<Flt64>
typealias QuadraticSubObjectFlt64 = QuadraticSubObject<Flt64>
