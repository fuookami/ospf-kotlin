package fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Data transfer object representing a network edge with source/destination nodes, bandwidth, and cost.
 * 表示具有源/目标节点、带宽和成本的网络边数据传输对象。
 *
 * @property fromNodeId the source node ID / 源节点 ID
 * @property toNodeId the destination node ID / 目标节点 ID
 * @property maxBandwidth the maximum bandwidth capacity / 最大带宽容量
 * @property costPerBandwidth the cost per unit of bandwidth / 每单位带宽成本
*/
data class EdgeDTO(
    val fromNodeId: UInt64,
    val toNodeId: UInt64,
    val maxBandwidth: UInt64,
    val costPerBandwidth: UInt64
)

/**
 * Data transfer object representing a client node attached to a normal node with a bandwidth demand.
 * 表示连接到普通节点的具有带宽需求的客户端节点数据传输对象。
 *
 * @property id the client node ID / 客户端节点 ID
 * @property normalNodeId the ID of the connected normal node / 所连接的普通节点 ID
 * @property demand the bandwidth demand / 带宽需求
*/
data class ClientNodeDTO(
    val id: UInt64,
    val normalNodeId: UInt64,
    val demand: UInt64
)

/**
 * Aggregated input data for the SPP problem including service cost, node count, edges, and client nodes.
 * SPP 问题的聚合输入数据，包括服务成本、节点数、边和客户端节点。
 *
 * @property serviceCost the cost per service / 每个服务的成本
 * @property normalNodeAmount the number of normal (transit) nodes / 普通（传输）节点数量
 * @property edges the list of network edges / 网络边列表
 * @property clientNodes the list of client nodes / 客户端节点列表
*/
data class Input(
    val serviceCost: UInt64,
    val normalNodeAmount: UInt64,
    val edges: List<EdgeDTO>,
    val clientNodes: List<ClientNodeDTO>
)

/**
 * Output containing the computed service paths as lists of node IDs.
 * 包含计算的服务路径（节点 ID 列表）的输出。
 *
 * @property links the list of service paths, each represented as a list of node IDs / 服务路径列表，每条路径由节点 ID 列表表示
*/
data class Output(
    val links: List<List<UInt64>>
)
