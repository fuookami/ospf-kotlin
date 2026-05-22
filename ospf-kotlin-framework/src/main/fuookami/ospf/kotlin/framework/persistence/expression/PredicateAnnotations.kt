/**
 * 谓词 schema 生成注解
 * Predicate schema generation annotations
 */
package fuookami.ospf.kotlin.framework.persistence.expression

/**
 * 标记实体用于生成谓词 schema
 * Marks an entity for predicate schema generation
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PredicateEntity(
    val schemaName: String = "",
    val generateResolver: Boolean = true
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
