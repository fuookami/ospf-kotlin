package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates aircraft configuration data including model, fuselage, fuel, decks, and neighbour relationships.
 * 聚合飞机配置数据（包括型号、机身、燃油、甲板和邻接关系）。
 *
 * @property regNo The aircraft registration number. / 飞机注册号
 * @property aircraftModel The aircraft model specification. / 飞机型号规格
 * @property formula The aerodynamic formula for balance calculations. / 用于平衡计算的气动公式
 * @property fuselage The fuselage properties including dry operating weight. / 机身属性（包括干操作重量）
 * @property fuelTanks The quantity of fuel tanks on the aircraft. / 飞机上的油箱数量
 * @property fuel The fuel constants mapped by flight phase. / 按飞行阶段映射的燃油常数
 * @property decks The list of decks on the aircraft. / 飞机上的甲板列表
 * @property neighbours The neighbour relationships mapped by type. / 按类型映射的邻接关系
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
        val conflictPositions = ArrayList<PositionPair>()
        // todo
        conflictPositions
    }
}
