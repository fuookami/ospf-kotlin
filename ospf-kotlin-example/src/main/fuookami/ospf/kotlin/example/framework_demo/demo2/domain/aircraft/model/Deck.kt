package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

/** 飞机机身内甲板的位置。Location of a deck within the aircraft fuselage. */
enum class DeckLocation {
    Main,
    LowForward,
    LowAft,
}

/**
 * 表示飞机上的物理甲板（具有门、货物位置和门邻近映射）。Represents a physical deck on the aircraft with its doors, cargo positions, and door proximity mappings.
 *
 * @property location 参数。
 * @property doors 参数。
 * @property positions 参数。
 * @property doorUbieties 参数。
 */
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
