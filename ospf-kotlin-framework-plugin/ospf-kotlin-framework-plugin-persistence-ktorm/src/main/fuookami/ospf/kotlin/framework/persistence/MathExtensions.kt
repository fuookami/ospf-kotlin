package fuookami.ospf.kotlin.framework.persistence

import org.ktorm.dsl.*
import fuookami.ospf.kotlin.math.algebra.number.*
/**
 * Query.
 * Query。
 * @param offset Number of rows to skip / 跳过的行数
 * @param limit Maximum number of rows to return / 返回的最大行数
*/
fun Query.limit(offset: UInt64?, limit: UInt64?) = this.limit(offset?.toInt(), limit?.toInt())

/**
 * Query.
 * Query。
 * @param limit Maximum number of rows to return / 返回的最大行数
*/
fun Query.limit(limit: UInt64) = this.limit(limit.toInt())
