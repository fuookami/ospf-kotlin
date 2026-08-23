package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates aircraft configuration data including model, fuselage, fuel, decks, and neighbour relationships.
 * 聚合飞机配置数据（包括型号、机身、燃油、甲板和邻接关系）。
 *
 * @property regNo 飞机注册号 / The aircraft registration number.
 * @property aircraftModel 飞机型号规格 / The aircraft model specification.
 * @property formula 用于平衡计算的气动公式 / The aerodynamic formula for balance calculations.
 * @property fuselage 机身属性（包括干操作重量） / The fuselage properties including dry operating weight.
 * @property fuelTanks 飞机上的油箱数量 / The quantity of fuel tanks on the aircraft.
 * @property fuel 按飞行阶段映射的燃油常数 / The fuel constants mapped by flight phase.
 * @property decks 飞机上的甲板列表 / The list of decks on the aircraft.
 * @property neighbours 按类型映射的邻接关系 / The neighbour relationships mapped by type.
*/
data class Aggregation(
    val regNo: RegNo,
    val aircraftModel: AircraftModel,
    val formula: Formula,
    val fuselage: Fuselage,
    val fuelTanks: Quantity<FuelTank>,
    val fuel: Map<FlightPhase, FuelConstant>,
    val decks: List<Deck>,
    val neighbours: HashMap<NeighbourType, List<Neighbour>>
) {
    val positions = decks.flatMap { it.positions }

    val conflictPositions by lazy {
        // Physical overlap is the only conflict relation available in the reduced request schema.
        // 精简请求模型只提供物理坐标，因此冲突关系由实际空间重叠派生。
        positions.withIndex().flatMap { (index, first) ->
            positions.drop(index + 1).mapNotNull { second ->
                if (first.coordinate.withIntersectionWith(second.coordinate)) {
                    first to second
                } else {
                    null
                }
            }
        }
    }
}
