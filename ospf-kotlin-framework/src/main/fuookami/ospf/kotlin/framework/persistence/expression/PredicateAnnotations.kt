/**
 * 谓词 schema 生成注解
 * Predicate schema generation annotations
*/
package fuookami.ospf.kotlin.framework.persistence.expression

/**
 * 列命名策略
 * Column naming strategy
 *
 * 控制 KSP 生成 columnMapping 时如何从 Kotlin 属性名推导后端列名。
 * Controls how KSP derives backend column names from Kotlin property names when generating columnMapping.
*/
enum class ColumnNamingStrategy {
    /**
     * 恒等映射：propertyName -> propertyName
     * Identity mapping: propertyName -> propertyName
    */
    Identity,

    /**
     * 驼峰转蛇形：userName -> user_name
     * Camel case to snake case: userName -> user_name
    */
    SnakeCase
}

/**
 * 标记实体用于生成谓词 schema
 * Marks an entity for predicate schema generation
 *
 * @param schemaName 生成的 schema 类名，默认为 ${EntityName}Schema / Generated schema class name, defaults to ${EntityName}Schema
 * @param generateResolver 是否生成 resolver 字段 / Whether to generate resolver field
 * @param generateColumnMapping 是否生成 columnMapping 和 createBinder / Whether to generate columnMapping and createBinder
 * @param namingStrategy 列命名策略，影响 columnMapping 的值 / Column naming strategy, affects columnMapping values
*/
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PredicateEntity(
    val schemaName: String = "",
    val generateResolver: Boolean = true,
    val generateColumnMapping: Boolean = false,
    val namingStrategy: ColumnNamingStrategy = ColumnNamingStrategy.Identity
)

/**
 * 标记属性对应的后端字段名
 * Marks the backend field name for a property
*/
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class PredicateField(
    val name: String
)
