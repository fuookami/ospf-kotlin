package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

/**
 * Appointment mapping between items and positions, representing pre-assigned
 * stowage relationships that must be respected during optimization.
 * 货物与舱位之间的预约映射，表示优化过程中必须遵守的预分配装载关系。
 *
 * @property appointment the mapping from items to their appointed positions / 货物到其预约舱位的映射
*/
data class Appointment(
    val appointment: Map<Item, Position>
) {

    /** Reverse mapping from positions to their appointed items / 从舱位到其预约货物的反向映射 */
    val reverse: Map<Position, List<Item>> by lazy {
        appointment.entries
            .groupBy { it.value }
            .mapValues { items -> items.value.map { it.key } }
    }

    operator fun get(item: Item): Position? {
        return appointment[item]
    }

    operator fun get(position: Position): List<Item> {
        return reverse[position] ?: emptyList()
    }
}