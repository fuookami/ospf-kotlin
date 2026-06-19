/**
 * Ktorm 列绑定器
 * Ktorm column binder
 *
 * 提供基于 Ktorm 表的强类型列绑定能力。
 * Provides strong-typed column binding based on Ktorm tables.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

/**
 * Ktorm 列绑定器
 * Ktorm column binder
 *
 * @param T Ktorm 表类型 / Ktorm table type
 * @property table Ktorm 表定义 / Ktorm table definition
 * @property columnMapping 属性名到后端列名的映射 / Property name to backend column name mapping
 */
class KtormColumnBinder<T : Table<*>>(
    val table: T,
    private val columnMapping: Map<String, String> = emptyMap()
) : ColumnBinder<ColumnDeclaring<*>> {
    override fun resolve(path: String): ColumnDeclaring<*>? {
        val backendName = columnMapping[path] ?: path
        return table.columns.find { it.name == backendName } as? ColumnDeclaring<*>
    }
}

/**
 * 使用 Ktorm 表的 predicate DSL 扩展（带列映射）
 * Predicate DSL extension with Ktorm table (with column mapping)
 *
 * @param E 实体类型 / Entity type
 * @param T Ktorm 表类型 / Ktorm table type
 * @param table Ktorm 表定义 / Ktorm table definition
 * @param columnMapping 属性名到后端列名的映射 / Property name to backend column name mapping
 * @param block DSL 块 / DSL block
 * @return 布尔表达式 / Boolean expression
 */
fun <E : Any, T : Table<*>> PredicateSchema<E>.withKtormTable(
    table: T,
    columnMapping: Map<String, String>,
    block: ColumnBindingContext<E, ColumnDeclaring<*>>.() -> BooleanExpression
): BooleanExpression {
    val binder = KtormColumnBinder(table, columnMapping)
    return predicateWith(binder, block)
}

/**
 * 使用 Ktorm 表的 predicate DSL 扩展（使用 KSP 生成的列映射）
 * Predicate DSL extension with Ktorm table (using KSP generated column mapping)
 *
 * @param E 实体类型 / Entity type
 * @param T Ktorm 表类型 / Ktorm table type
 * @param table Ktorm 表定义 / Ktorm table definition
 * @param block DSL 块 / DSL block
 * @return 布尔表达式 / Boolean expression
 */
fun <E : Any, T : Table<*>> PredicateSchema<E>.withKtormTable(
    table: T,
    block: ColumnBindingContext<E, ColumnDeclaring<*>>.() -> BooleanExpression
): BooleanExpression {
    val columnMapping = (this as? HasColumnMapping)?.columnMapping ?: emptyMap()
    return withKtormTable(table, columnMapping, block)
}
