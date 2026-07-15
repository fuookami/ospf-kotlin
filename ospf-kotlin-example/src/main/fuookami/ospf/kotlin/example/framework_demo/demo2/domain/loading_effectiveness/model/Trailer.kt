package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Enumerates the types of trailers used in cargo loading.
 * 枚举货物装载中使用的拖车类型。
*/
enum class TrailerType {
    Hardstand,
    Transit,
    Warehouse
}

/**
 * Represents a trailer with its type, loading order, and associated cargo items.
 * 表示一个拖车，包含其类型、装载顺序和关联的货物项。
 *
 * @property type The type of the trailer. / 拖车类型
 * @property order The loading order of the trailer. / 拖车的装载顺序
 * @property name The name identifier of the trailer. / 拖车的名称标识
 * @property items The cargo items currently on the trailer. / 拖车上的货物项
*/
data class Trailer(
    val type: TrailerType,
    val order: UInt8,
    val name: String,
    val items: List<Item>
) {
    override fun toString(): String {
        return name
    }
}
