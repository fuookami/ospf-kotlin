package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * 聚合飞机配置数据（包括型号、机身、燃油、甲板和邻接关系）。Aggregates aircraft configuration data including model, fuselage, fuel, decks, and neighbour relationships.
 *
 * @property regNo 参数。
 * @property aircraftModel 参数。
 * @property formula 参数。
 * @property fuselage 参数。
 * @property fuelTanks 参数。
 * @property fuel 参数。
 * @property decks 参数。
 * @property neighbours 参数。
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
