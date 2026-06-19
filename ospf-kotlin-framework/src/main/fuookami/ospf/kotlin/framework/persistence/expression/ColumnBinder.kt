/**
 * 列绑定器接口
 * Column binder interface
 *
 * 提供强类型列绑定能力，作为现有字符串 resolver 的补充选项。
 * Provides strong-typed column binding capability as a complement to the existing string resolver.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

/**
 * 列映射接口（KSP 生成的 schema 实现此接口）
 * Column mapping interface (KSP generated schema implements this)
 */
interface HasColumnMapping {
    val columnMapping: Map<String, String>
}

/**
 * 列绑定器接口
 * Column binder interface
 *
 * @param C 列类型 / Column type
 */
interface ColumnBinder<C> {
    /**
     * 解析属性路径为列
     * Resolve property path to column
     *
     * @param path 属性路径 / Property path
     * @return 对应的列，未找到时返回 null / Corresponding column, or null if not found
     */
    fun resolve(path: String): C?
}

/**
 * 列绑定上下文
 * Column binding context
 *
 * @param E 实体类型 / Entity type
 * @param C 列类型 / Column type
 * @property schema 谓词 schema / Predicate schema
 * @property binder 列绑定器 / Column binder
 */
class ColumnBindingContext<E, C>(
    private val schema: PredicateSchema<E>,
    private val binder: ColumnBinder<C>
) {
    /**
     * 解析属性路径
     * Resolve property path
     *
     * @param path 属性路径 / Property path
     * @return 对应的列，未找到时返回 null / Corresponding column, or null if not found
     */
    fun resolveColumn(path: String): C? = binder.resolve(path)
}

/**
 * 带列绑定的 predicate DSL
 * Predicate DSL with column binding
 *
 * @param E 实体类型 / Entity type
 * @param C 列类型 / Column type
 * @param binder 列绑定器 / Column binder
 * @param block DSL 块 / DSL block
 * @return 布尔表达式 / Boolean expression
 */
fun <E, C> PredicateSchema<E>.predicateWith(
    binder: ColumnBinder<C>,
    block: ColumnBindingContext<E, C>.() -> BooleanExpression
): BooleanExpression {
    val context = ColumnBindingContext<E, C>(this, binder)
    return block(context)
}