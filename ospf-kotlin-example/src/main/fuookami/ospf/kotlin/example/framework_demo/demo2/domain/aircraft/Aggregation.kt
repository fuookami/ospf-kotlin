package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft

import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

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
