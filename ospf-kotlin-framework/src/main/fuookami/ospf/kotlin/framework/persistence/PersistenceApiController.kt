/**
 * 持久化 API 控制器
 * Persistence API Controller
 *
 * 定义持久化层 API 控制器的通用接口。
 * Defines a common interface for persistence layer API controllers.
*/
package fuookami.ospf.kotlin.framework.persistence

// todo: extract interface function from mongodb.PersistenceApiController

/** 持久化 API 控制器接口 / Persistence API controller interface */
interface PersistenceApiController

/** ORM 持久化 API 控制器 / ORM persistence API controller */
class ORMPersistenceApiController : PersistenceApiController {}