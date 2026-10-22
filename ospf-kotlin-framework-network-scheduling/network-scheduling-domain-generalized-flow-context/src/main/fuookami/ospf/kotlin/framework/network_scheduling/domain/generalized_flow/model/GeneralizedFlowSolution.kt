package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

/** 单弧投入、产出与计费结果。 / Per-arc input, output, and tariff result.
 *
 * @property input 起点投入量，容量与资费按此计 / Origin input; capacity and tariff use this amount
 * @property unitConvertedOutput 仅应用单位换算、产率取 1 时的产出 / Output after unit conversion only, with yield set to one
 * @property output 应用单位换算及业务产率后的到达产出 / Arrival output after unit conversion and business yield
 * @property lossOrGain 正值为损耗，负值为增益，按终点单位计 / Positive is loss, negative is gain, measured in destination units
 * @property cost 按投入量计费的弧费用 / Arc tariff charged on input
 */
data class GeneralizedFlowArcResult<V : RealNumber<V>>(
    val input: Quantity<V>,
    val unitConvertedOutput: Quantity<V>,
    val output: Quantity<V>,
    val lossOrGain: Quantity<V>,
    val cost: Quantity<V>
)

/** 可复核的节点广义守恒结果。 / Auditable generalized node-balance result.
 *
 * @property declaredNetSupply 节点声明净供需 / Declared node net supply
 * @property outgoingInput 流出弧投入量之和 / Sum of outgoing arc inputs
 * @property incomingOutput 流入弧到达产出之和 / Sum of incoming arc outputs
 * @property realizedNetSupply 流出投入减流入产出 / Outgoing input minus incoming output
 * @property residual 实际净供需减声明净供需，应为零 / Realized minus declared net supply; should be zero
 */
data class GeneralizedFlowNodeResult<V : RealNumber<V>>(
    val declaredNetSupply: Quantity<V>,
    val outgoingInput: Quantity<V>,
    val incomingOutput: Quantity<V>,
    val realizedNetSupply: Quantity<V>,
    val residual: Quantity<V>
)

/** 广义流的领域解，保留弧级输入/产出及节点守恒核验。 / Domain solution with per-arc inputs/outputs and node-balance checks.
 *
 * @property arcs 按稳定弧标识索引的结果 / Results indexed by stable arc identity
 * @property nodes 按稳定节点标识索引的结果 / Results indexed by stable node identity
 * @property totalCost 总费用 / Total cost
 */
data class GeneralizedFlowSolution<V : RealNumber<V>>(
    val arcs: Map<NetworkArcId, GeneralizedFlowArcResult<V>>,
    val nodes: Map<NetworkNodeId, GeneralizedFlowNodeResult<V>>,
    val totalCost: Quantity<V>
)
