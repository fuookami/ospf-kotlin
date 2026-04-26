package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

enum class DeckLocation {
    Main,
    LowForward,
    LowAft,
}

data class Deck(
    val location: DeckLocation,
    val doors: List<HatchDoor>,
    val positions: List<Position>,
    val doorUbieties: Map<HatchDoor, List<DoorUbiety>>
) {
    fun ubieties(position: Position): List<DoorUbiety> {
        return doorUbieties.values.mapNotNull { ubieties ->
            ubieties.find { it.position == position }
        }
    }

    override fun toString(): String {
        return location.toString()
    }
}
