package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

data class Appointment(
    val appointment: Map<Item, Position>
) {
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
