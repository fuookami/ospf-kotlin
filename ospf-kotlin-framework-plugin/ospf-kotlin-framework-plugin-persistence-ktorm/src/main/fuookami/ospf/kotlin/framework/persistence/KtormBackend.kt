/**
 * Ktorm backend bundle / Ktorm 后端配置
 *
 * Couples a Ktorm database with the framework dialect used to compile repository expressions.
 * 将 Ktorm 数据库与仓储表达式编译所需的框架方言绑定。
 */
package fuookami.ospf.kotlin.framework.persistence

import org.ktorm.database.Database
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect

/**
 * Database and matching framework dialect returned by persistence plugins.
 * 持久化插件返回的数据库实例及其对应的框架方言。
 *
 * @property database Ktorm database instance / Ktorm 数据库实例
 * @property dialect Framework relational dialect / 框架关系数据库方言
 */
data class KtormBackend(
    val database: Database,
    val dialect: RelationalQueryDialect
)
