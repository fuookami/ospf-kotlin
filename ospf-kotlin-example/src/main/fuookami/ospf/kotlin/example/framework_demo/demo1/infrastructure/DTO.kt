package fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Data transfer object representing a network edge with source/destination nodes, bandwidth, and cost. */
data class EdgeDTO(
    val fromNodeId: UInt64,
    val toNodeId: UInt64,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
)

/** Data transfer object representing a client node attached to a normal node with a bandwidth demand. */
data class ClientNodeDTO(
    val id: UInt64,
    val normalNodeId: UInt64,
    val demand: UInt64
)

/** Aggregated input data for the SPP problem including service cost, node count, edges, and client nodes. */
data class Input(
    val serviceCost: UInt64,
    val normalNodeAmount: UInt64,
    val edges: List<EdgeDTO>,
    val clientNodes: List<ClientNodeDTO>
)

/** Output containing the computed service paths as lists of node IDs. */
data class Output(
    val links: List<List<UInt64>>
)
