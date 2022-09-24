package fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure

import fuookami.ospf.kotlin.utils.math.*

data class EdgeDTO(
    val fromNodeId: UInt64,
    val toNodeId: UInt64,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
)

data class ClientNodeDTO(
    val id: UInt64,
    val normalNodeId: UInt64,
    val demand: UInt64
)

data class Input(
    val serviceCost: UInt64,
    val normalNodeAmount: UInt64,
    val edges: List<EdgeDTO>,
    val clientNodes: List<ClientNodeDTO>
)

data class Output(
    val links: List<List<UInt64>>
)
