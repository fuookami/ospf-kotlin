/**
 * 实体元数据
 * Entity Metadata
 *
 * 管理 PO 字段到数据库列的映射。
 * Manages mapping between PO fields and database columns.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import fuookami.ospf.kotlin.math.symbol.expression.PropertyPath

/**
 * 实体元数据
 * Entity Metadata
 *
 * 管理 PO 字段到数据库列的映射，支持路径解析和类型转换。
 * Manages mapping between PO fields and database columns, supporting path resolution and type transformation.
 *
 * @param E 实体类型 / Entity type
 * @property entityClass 实体类 / Entity class
 * @property tableName 表名 / Table name
 * @property bindings 字段绑定映射 / Field binding map
 */
class EntityMeta<E : Any>(
    val entityClass: KClass<E>,
    val tableName: String,
    private val bindings: Map<PropertyPath, FieldBinding<*>>
) {
    /**
     * 获取字段绑定
     * Get field binding
     */
    operator fun get(path: PropertyPath): FieldBinding<*>? = bindings[path]

    /**
     * 获取字段绑定（字符串路径）
     * Get field binding (string path)
     */
    operator fun get(path: String): FieldBinding<*>? = bindings[PropertyPath.parse(path)]

    /**
     * 解析路径为列
     * Resolve path to column
     */
    fun resolveColumn(path: PropertyPath): ColumnDeclaring<*>? = bindings[path]?.column

    /**
     * 解析路径为列（字符串路径）
     * Resolve path to column (string path)
     */
    fun resolveColumn(path: String): ColumnDeclaring<*>? = resolveColumn(PropertyPath.parse(path))

    /**
     * 获取所有路径
     * Get all paths
     */
    val paths: Set<PropertyPath> get() = bindings.keys

    /**
     * 获取所有绑定
     * Get all bindings
     */
    val allBindings: Collection<FieldBinding<*>> get() = bindings.values

    companion object {
        /**
         * 创建实体元数据
         * Create entity metadata
         */
        inline fun <reified E : Any> from(
            tableName: String,
            block: EntityMetaBuilder<E>.() -> Unit
        ): EntityMeta<E> {
            return EntityMetaBuilder<E>(E::class, tableName).apply(block).build()
        }
    }
}

/**
 * 实体元数据构建器
 * Entity Metadata Builder
 */
class EntityMetaBuilder<E : Any>(
    private val entityClass: KClass<E>,
    private val tableName: String
) {
    private val bindings = mutableMapOf<PropertyPath, FieldBinding<*>>()

    /**
     * 添加字段绑定
     * Add field binding
     */
    fun <T : Any> binding(
        path: String,
        property: KProperty1<E, *>,
        column: ColumnDeclaring<T>
    ) {
        bindings[PropertyPath.parse(path)] = FieldBinding(PropertyPath.parse(path), column)
    }

    /**
     * 添加字段绑定（带转换器）
     * Add field binding with transformer
     */
    fun <T : Any> binding(
        path: String,
        property: KProperty1<E, *>,
        column: ColumnDeclaring<T>,
        transformer: ValueTransformer<T>
    ) {
        bindings[PropertyPath.parse(path)] = FieldBinding(
            PropertyPath.parse(path),
            column,
            transformer
        )
    }

    /**
     * 添加字段绑定（使用 PropertyPath）
     * Add field binding using PropertyPath
     */
    fun <T : Any> binding(
        path: PropertyPath,
        column: ColumnDeclaring<T>
    ) {
        bindings[path] = FieldBinding(path, column)
    }

    /**
     * 添加字段绑定（使用 Table 的 Column）
     * Add field binding using Table's Column
     */
    fun <T : Any> binding(
        path: String,
        column: ColumnDeclaring<T>
    ) {
        val propertyPath = PropertyPath.parse(path)
        bindings[propertyPath] = FieldBinding(propertyPath, column)
    }

    /**
     * 构建实体元数据
     * Build entity metadata
     */
    fun build(): EntityMeta<E> {
        return EntityMeta(entityClass, tableName, bindings.toMap())
    }
}

/**
 * 表元数据扩展
 * Table Metadata Extension
 *
 * 从 Ktorm Table 自动创建 EntityMeta。
 * Automatically create EntityMeta from Ktorm Table.
 */
fun <E : Any> Table<E>.toEntityMeta(
    entityClass: KClass<E>,
    pathMapping: Map<String, String> = emptyMap()
): EntityMeta<E> {
    val bindings = mutableMapOf<PropertyPath, FieldBinding<*>>()

    for (column in this.columns) {
        val path = pathMapping[column.name] ?: column.name
        bindings[PropertyPath.parse(path)] = FieldBinding(
            PropertyPath.parse(path),
            column as ColumnDeclaring<*>
        )
    }

    return EntityMeta(entityClass, this.tableName, bindings)
}