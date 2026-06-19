/**
 * MyBatis 列绑定器
 * MyBatis column binder
 *
 * 提供基于 MyBatis 的强类型列绑定能力。
 * Provides strong-typed column binding based on MyBatis.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

/**
 * MyBatis 列绑定器
 * MyBatis column binder
 *
 * @property columnMapping 属性名到后端列名的映射 / Property name to backend column name mapping
 */
class MybatisColumnBinder(
    private val columnMapping: Map<String, String> = emptyMap()
) : ColumnBinder<String> {
    override fun resolve(path: String): String? {
        return columnMapping[path] ?: path.toSnakeCase()
    }

    companion object {
        /**
         * 驼峰转蛇形命名
         * Camel case to snake case
         */
        private fun String.toSnakeCase(): String {
            return replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
        }
    }
}

/**
 * 使用 MyBatis 映射的 predicate DSL 扩展（带列映射）
 * Predicate DSL extension with MyBatis mapping (with column mapping)
 *
 * @param E 实体类型 / Entity type
 * @param columnMapping 属性名到后端列名的映射 / Property name to backend column name mapping
 * @param block DSL 块 / DSL block
 * @return 布尔表达式 / Boolean expression
 */
fun <E : Any> PredicateSchema<E>.withMybatisMapping(
    columnMapping: Map<String, String>,
    block: ColumnBindingContext<E, String>.() -> BooleanExpression
): BooleanExpression {
    val binder = MybatisColumnBinder(columnMapping)
    return predicateWith(binder, block)
}

/**
 * 使用 MyBatis 映射的 predicate DSL 扩展（使用 KSP 生成的列映射）
 * Predicate DSL extension with MyBatis mapping (using KSP generated column mapping)
 *
 * @param E 实体类型 / Entity type
 * @param block DSL 块 / DSL block
 * @return 布尔表达式 / Boolean expression
 */
fun <E : Any> PredicateSchema<E>.withMybatisMapping(
    block: ColumnBindingContext<E, String>.() -> BooleanExpression
): BooleanExpression {
    val columnMapping = (this as? HasColumnMapping)?.columnMapping ?: emptyMap()
    return withMybatisMapping(columnMapping, block)
}
