/**
 * 子目标对象
 * Sub-objective object
*/
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
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

    /** The list of cells (coefficient-token pairs) that compose this sub-objective. 中文构成此子目标的单元格（系数-标记对）列表。 */
    abstract val cells: List<Cell<V>>

    /** The constant term of this sub-objective. 中文此子目标的常数项。 */
    abstract val constant: V

    /**
     * 使用标记的已知结果求值。
     * Evaluate using the tokens' known results.
     *
     * @return 求值结果（含常数项），若任一单元格结果未知则返回 null / The evaluation result (including the constant), or null if any cell result is unknown
    */
    abstract fun evaluate(): V?

    /**
     * 使用解向量按索引求值。
     * Evaluate using a solution vector by index lookup.
     *
     * @param results 解向量，索引对应标记在标记表中的位置 / The solution vector whose indices correspond to token positions in the token table
     * @return 求值结果（含常数项），若任一单元格结果未知则返回 null / The evaluation result (including the constant), or null if any cell result is unknown
    */
    abstract fun evaluate(results: List<V>): V?
}

/**
 * 线性子目标，包含线性单元格列表和常数项。
 * Linear sub-objective containing a list of linear cells and a constant.
 *
 * @param category 目标分类 / The objective category
 * @property cells 线性单元格列表 / List of linear cells
 * @param _constant 常数项 / Constant term
 * @param name 子目标名称 / Sub-objective name
*/
class LinearSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<LinearCell<V>>,
    private val _constant: V,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constant: V get() = _constant

    /**
     * 提取所有线性项为（系数, 变量）对列表。
     * Extract all linear terms as a list of (coefficient, variable) pairs.
     *
     * @return 线性项列表，每项包含系数和对应的变量 / A list of linear terms, each containing a coefficient and its corresponding variable
    */
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
        /**
         * Creates a linear sub-objective from flatten data and a token table.
         * 从展平数据和符号表创建线性子目标。
         *
         * @param category The objective category / 目标分类
         * @param flattenData The flattened linear data / 展平的线性数据
         * @param tokens The token table for cell lookup / 用于单元格查找的符号表
         * @param name The sub-objective name / 子目标名称
         * @param converter The value converter / 值转换器
         * @return A new LinearSubObject / 新的线性子目标
        */
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
 *
 * @param category 目标分类 / The objective category
 * @property cells 二次单元格列表 / List of quadratic cells
 * @param _constant 常数项 / Constant term
 * @param name 子目标名称 / Sub-objective name
*/
class QuadraticSubObject<V : RealNumber<V>>(
    category: ObjectCategory,
    override val cells: ArrayList<QuadraticCell<V>>,
    private val _constant: V,
    name: String = ""
) : SubObject<V>(category, name) {
    override val constant: V get() = _constant

    /**
     * 提取所有二次项为（系数, 变量1, 变量2）三元组列表。
     * Extract all quadratic terms as a list of (coefficient, variable1, variable2) triples.
     *
     * @return 二次项列表，每项包含系数和两个变量（第二个可能为 null） / A list of quadratic terms, each containing a coefficient and two variables (the second may be null)
    */
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
