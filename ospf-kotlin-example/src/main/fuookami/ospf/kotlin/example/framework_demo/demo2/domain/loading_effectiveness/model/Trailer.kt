package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

enum class TrailerType {
    Hardstand,
    Transit,
    Warehouse
}

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

