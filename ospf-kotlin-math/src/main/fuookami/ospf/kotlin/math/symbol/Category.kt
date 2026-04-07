/**
 * 表达式类型分类
 * Expression Category
 *
 * 定义符号表达式按次数的分类体系，包括线性、二次、标准和非线性。
 * 用于在编译时和运行时区分表达式的类型，支持优化和类型检查。
 * Defines the classification hierarchy for symbolic expressions by degree,
 * including Linear, Quadratic, Standard, and Nonlinear.
 * Used to distinguish expression types at compile-time and runtime,
 * supporting optimization and type checking.
 */
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 表达式类型分类基类
 * Expression Category Base Class
 *
 * 表示符号表达式按次数分类的密封类层次结构。
 * 子类包括线性(Linear)、二次(Quadratic)、标准(Standard)和非线性(Nonlinear)。
 * Represents the sealed class hierarchy for symbolic expression classification by degree.
 * Subclasses include Linear, Quadratic, Standard, and Nonlinear.
 *
 * @property code 分类代码，用于比较和排序 / Category code used for comparison and ordering
 */
sealed class Category {
    abstract val code: UInt64

    /**
     * 返回两个分类中较高的一个。
     * Returns the higher of two categories.
     *
     * @param rhs 另一个分类 / Another category
     * @return 较高的分类 / The higher category
     */
    infix fun op(rhs: Category) = if (this.code < rhs.code) {
        rhs
    } else {
        this
    }
}

/**
 * 线性表达式类型
 * Linear Expression Category
 *
 * 表示线性表达式（次数为1的多项式）。
 * Represents linear expressions (polynomials of degree 1).
 */
data object Linear : Category() {
    override val code = UInt64.one
}

/**
 * 二次表达式类型
 * Quadratic Expression Category
 *
 * 表示二次表达式（次数为2的多项式）。
 * Represents quadratic expressions (polynomials of degree 2).
 */
data object Quadratic : Category() {
    override val code = UInt64.two
}

/**
 * 标准表达式类型
 * Standard Expression Category
 *
 * 表示标准形式的多项式表达式。
 * Represents polynomial expressions in standard form.
 */
data object Standard : Category() {
    override val code = UInt64.three
}

/**
 * 非线性表达式类型
 * Nonlinear Expression Category
 *
 * 表示非线性表达式（次数大于2或包含非线性运算）。
 * Represents nonlinear expressions (degree greater than 2 or containing nonlinear operations).
 */
data object Nonlinear : Category() {
    override val code = UInt64.ten
}

/**
 * 比较两个分类的大小顺序。
 * Compares the order of two categories.
 *
 * @param rhs 另一个分类 / Another category
 * @return 比较结果（小于、等于、大于）/ Comparison result (less than, equal to, greater than)
 */
infix fun Category.ord(rhs: Category): Order {
    return this.code ord rhs.code
}

/**
 * 返回两个分类中较高的一个。
 * Returns the maximum of two categories.
 *
 * @param lhs 第一个分类 / First category
 * @param rhs 第二个分类 / Second category
 * @return 较高的分类 / The higher category
 */
fun max(lhs: Category, rhs: Category): Category {
    return if (lhs.code > rhs.code) {
        lhs
    } else {
        rhs
    }
}

/**
 * 返回集合中最高的分类。
 * Returns the maximum category in a collection.
 *
 * @return 集合中代码值最大的分类 / Category with the highest code in the collection
 * @throws NoSuchElementException 如果集合为空 / If the collection is empty
 */
fun Collection<Category>.max(): Category {
    return this.maxBy { it.code }
}

/**
 * 返回集合中最高的分类，如果集合为空则返回null。
 * Returns the maximum category in a collection, or null if empty.
 *
 * @return 集合中代码值最大的分类，如果集合为空则返回null / Category with the highest code, or null if the collection is empty
 */
fun Collection<Category>.maxOrNull(): Category? {
    return this.maxByOrNull { it.code }
}







