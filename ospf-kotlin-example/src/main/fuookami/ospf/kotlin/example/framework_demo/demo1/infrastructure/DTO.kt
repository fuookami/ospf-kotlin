package fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 数据传输对象，表示具有源/目标节点、带宽和成本的网络边。Data transfer object representing a network edge with source/destination nodes, bandwidth, and cost.
 *
 * @property fromNodeId 参数。
 * @property toNodeId 参数。
 * @property maxBandwidth 参数。
 * @property costPerBandwidth 参数。
 */
data class EdgeDTO(
    val fromNodeId: UInt64,
    val toNodeId: UInt64,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
)

/**
 * 数据传输对象，表示连接到普通节点的具有带宽需求的客户端节点。Data transfer object representing a client node attached to a normal node with a bandwidth demand.
 *
 * @property id 参数。
 * @property normalNodeId 参数。
 * @property demand 参数。
 */
data class ClientNodeDTO(
    val id: UInt64,
    val normalNodeId: UInt64,
    val demand: UInt64
)

/**
 * SPP 问题的聚合输入数据（包括服务成本、节点数、边和客户端节点）。Aggregated input data for the SPP problem including service cost, node count, edges, and client nodes.
 *
 * @property serviceCost 参数。
 * @property normalNodeAmount 参数。
 * @property edges 参数。
 * @property clientNodes 参数。
 */
data class Input(
    val serviceCost: UInt64,
    val normalNodeAmount: UInt64,
    val edges: List<EdgeDTO>,
    val clientNodes: List<ClientNodeDTO>
)

/**
 * 包含计算的服务路径（节点 ID 列表）的输出。Output containing the computed service paths as lists of node IDs.
 *
 * @property links 参数。
 */
data class Output(
    val links: List<List<UInt64>>
)
