package fuookami.ospf.kotlin.utils.math.geometry

interface Dimension {
    val size: Int
    val indices get() = 0 until size
}

object Dim1: Dimension {
    override val size = 1
}

object Dim2: Dimension {
    override val size = 2
}

object Dim3: Dimension {
    override val size = 3
}
