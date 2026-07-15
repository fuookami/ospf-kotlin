/**
 * 列绑定器接口
 * Column binder interface
 *
 * 提供强类型列绑定能力，作为现有字符串 resolver 的补充选项。
 * Provides strong-typed column binding capability as a complement to the existing string resolver.
 *
 * 设计说明 / Design Notes:
 * - 列绑定发生在 binder → resolver 层，不在 DSL 层。
 *   Column binding occurs at the binder → resolver layer, not at the DSL layer.
 * - 用户通过 PredicateSchema.predicate { ... } 构造 BooleanExpression（字段来自 schema 接收者），
 *   再通过 ColumnBinder.toResolver() 生成 PersistenceFieldResolver 交给仓储。
 *   Users construct BooleanExpression via PredicateSchema.predicate { ... } (fields from schema receiver),
 *   then generate PersistenceFieldResolver via ColumnBinder.toResolver() to hand to repository.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

/**
 * 列映射接口（KSP 生成的 schema 实现此接口）
 * Column mapping interface (KSP generated schema implements this)
*/
interface HasColumnMapping {

    /** Mapping of property paths to column names / 属性路径到列名的映射 */
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
 * 将 ColumnBinder 转为 PersistenceFieldResolver
 * Convert ColumnBinder to PersistenceFieldResolver
 *
 * @param C 列类型 / Column type
 * @return 持久化字段解析器 / Persistence field resolver
*/
fun <C> ColumnBinder<C>.toResolver(): PersistenceFieldResolver<C> = { path -> resolve(path) }
