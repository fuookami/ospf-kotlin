/**
 * 子目标对象
 * Sub-objective object
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 子目标密封基类，持有优化方向、名称、单元格和常数项。
 * Sealed base class for sub-objectives holding category, name, cells, and constant.
 *
 * @param V 数值类型 / The numeric type
 * @property category 目标分类 / The objective category
 * @property name     子目标名称 / The sub-objective name
 */
sealed class SubObject<V : RealNumber<V>>(
    val category: ObjectCategory,
    val name: String = ""
) {
    abstract val cells: List<Cell<V>>
    abstract val constant: V

    abstract fun evaluate(): V?
    abstract fun evaluate(results: List<V>): V?
}

/**
 * 线性子目标，包含线性单元格列表和常数项。
 * Linear sub-objective containing a list of linear cells and a constant.
 */
class LinearSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell<V>>,
    private val _constant: V,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constant: V get() = _constant

    fun linearTerms(): List<Pair<V, AbstractVariableItem<*, *>>> {
        return cells.map { it.coefficient to it.token.variable }
    }

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
        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: LinearFlattenData<V>,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): LinearSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = createLinearCells(
                flattenData.monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
                tokens,
                converter
            )
            return LinearSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }
    }
}

/**
 * 二次子目标，包含二次单元格列表和常数项。
 * Quadratic sub-objective containing a list of quadratic cells and a constant.
 */
class QuadraticSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCell<V>>,
    private val _constant: V,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constant: V get() = _constant

    fun quadraticTerms(): List<Triple<V, AbstractVariableItem<*, *>, AbstractVariableItem<*, *>?>> {
        return cells.map { cell ->
            Triple(cell.coefficient, cell.token1.variable, cell.token2?.variable)
        }
    }

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
        operator fun <V> invoke(
            category: ObjectCategory,
            flattenData: QuadraticFlattenData<V>,
            tokens: AbstractTokenTable<V>,
            name: String = "",
            converter: IntoValue<V>
        ): QuadraticSubObject<V> where V : RealNumber<V>, V : NumberField<V> {
            val cells = createQuadraticCells(
                flattenData.monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
                tokens,
                converter
            )
            return QuadraticSubObject(
                category = category,
                cells = cells,
                _constant = flattenData.constant,
                name = name
            )
        }
    }
}
