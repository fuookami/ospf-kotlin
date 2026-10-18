/**
 * 二维装箱标识类型。 / 2D packing identifier types.
 *
 * 与 BPP3D 的 `Id.kt` 对齐：板材类型编码、物品编号、批次号与物料编号。
 * / Mirrors BPP3D `Id.kt`: sheet type codes, item identifiers, batch numbers and material numbers.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

/**
 * 板材类型编码。 / Sheet type code.
 *
 * @property value 编码文本 / code text
*/
data class SheetTypeId(val value: String) : Comparable<SheetTypeId> {
    override fun compareTo(other: SheetTypeId): Int = value.compareTo(other.value)

    override fun toString(): String = value
}

/**
 * 物品编号。 / Item identifier.
 *
 * @property value 编号文本 / identifier text
*/
data class ItemId(val value: String) : Comparable<ItemId> {
    override fun compareTo(other: ItemId): Int = value.compareTo(other.value)

    override fun toString(): String = value
}

/**
 * 物料编号。 / Material number.
 *
 * @property value 编号文本 / number text
*/
data class MaterialNo(val value: String) : Comparable<MaterialNo> {
    override fun compareTo(other: MaterialNo): Int = value.compareTo(other.value)

    override fun toString(): String = value
}

/**
 * 批次号。 / Batch number.
 *
 * @property value 批次文本 / batch text
*/
data class BatchNo(val value: String) : Comparable<BatchNo> {
    override fun compareTo(other: BatchNo): Int = value.compareTo(other.value)

    override fun toString(): String = value
}

/**
 * 单批次号标记：用于表示不区分批次的场景。 / Single-batch marker used when batches are not distinguished.
*/
val SingleBatchNo = BatchNo("single")