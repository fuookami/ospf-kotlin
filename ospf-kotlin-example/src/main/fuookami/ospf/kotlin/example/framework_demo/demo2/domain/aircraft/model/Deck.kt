package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

/** Location of a deck within the aircraft fuselage. */
enum class DeckLocation {
    Main,
    LowForward,
    LowAft,
}

/** Represents a physical deck on the aircraft with its doors, cargo positions, and door proximity mappings. */
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
