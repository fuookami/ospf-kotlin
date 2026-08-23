package fuookami.ospf.kotlin.framework.persistence

import org.ktorm.dsl.*
import fuookami.ospf.kotlin.math.algebra.number.*
/**
 * Query.
 * Query。
 * @param offset 跳过的行数 / Number of rows to skip
 * @param limit 返回的最大行数 / Maximum number of rows to return
*/
fun Query.limit(offset: UInt64?, limit: UInt64?) = this.limit(offset?.toInt(), limit?.toInt())

/**
 * Query.
 * Query。
 * @param limit 返回的最大行数 / Maximum number of rows to return
*/
fun Query.limit(limit: UInt64) = this.limit(limit.toInt())
