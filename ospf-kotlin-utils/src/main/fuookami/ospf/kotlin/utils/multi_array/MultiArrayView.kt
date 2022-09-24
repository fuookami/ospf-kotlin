package fuookami.ospf.kotlin.utils.multi_array

class MultiArrayView<T : Any, S : Shape>(
    private val list: List<T>,
    private val shape: S
) {
    operator fun iterator(): Iterator<T?> {
        return list.iterator()
    }
}
