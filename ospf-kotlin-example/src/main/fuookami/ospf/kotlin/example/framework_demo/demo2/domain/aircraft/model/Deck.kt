package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

/**
 * Location of a deck within the aircraft fuselage.
 * 飞机机身内甲板的位置。
*/
enum class DeckLocation {
    Main,
    LowForward,
    LowAft,
}

/**
 * Represents a physical deck on the aircraft with its doors, cargo positions, and door proximity mappings.
 * 表示飞机上的物理甲板（具有门、货物位置和门邻近映射）。
 *
 * @property location 甲板在机身内的位置 / The location of the deck within the fuselage.
 * @property doors 该甲板上的舱门列表 / The list of hatch doors on this deck.
 * @property positions 该甲板上的货物位置列表 / The list of cargo positions on this deck.
 * @property doorUbieties 舱门到其邻近信息的映射 / The mapping of hatch doors to their proximity information.
*/
data class Deck(
    val location: DeckLocation,
    val doors: List<HatchDoor>,
    val positions: List<Position>,
    val doorUbieties: Map<HatchDoor, List<DoorUbiety>>
) {

    /**
     * Get the door ubieties for a given position across all doors.
     * 获取指定位置在所有舱门下的邻近信息。
     *
     * @param position 要查询的位置 / The position to query.
     * @return 该位置对应的所有舱门邻近信息 / All door ubieties for the given position.
    */
    fun ubieties(position: Position): List<DoorUbiety> {
        return doorUbieties.values.mapNotNull { ubieties ->
            ubieties.find { it.position == position }
        }
    }

    override fun toString(): String {
        return location.toString()
    }
}
