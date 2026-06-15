package fuookami.ospf.kotlin.framework.persistence

import org.ktorm.dsl.*
import fuookami.ospf.kotlin.math.algebra.number.*

fun Query.limit(offset: UInt64?, limit: UInt64?) = this.limit(offset?.toInt(), limit?.toInt())
fun Query.limit(limit: UInt64) = this.limit(limit.toInt())
