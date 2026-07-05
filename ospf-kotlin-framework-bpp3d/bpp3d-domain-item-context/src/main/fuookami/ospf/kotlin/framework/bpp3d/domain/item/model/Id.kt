/**
 * 物品域 id 语义标记接口 / Item domain id semantic marker interfaces
 *
 * 将原 String id 改为语义化标记接口，业务侧实现接口、子类协变 override，
 * 以保留类型安全（参考 gantt-scheduling 的标记接口方案）。
 *
 * 注：MaterialNo / PackageCode 是 infrastructure 模块的 @JvmInline value class，
 * 由于 infrastructure 不依赖 item-context（item-context 依赖 infrastructure），
 * 无法令 value class 实现此处的接口。因此 Material.no / Package.code 仍保留
 * value class 类型（本身已是语义化类型，满足类型安全意图）；
 * 仅将原 String id（ActualItem.id / BinType.typeCode / QuantityItem.id /
 * MaterialPackingProgramCandidate.id）改为标记接口。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

/**
 * 货物 id 标记接口 / Item id marker interface
 */
interface ItemId

/**
 * 箱型 id 标记接口 / Bin type id marker interface
 */
interface BinTypeId

/**
 * 物料 id 标记接口 / Material id marker interface
 *
 * 供未来 infrastructure 侧 value class（MaterialNo）实现接口后使用。
 * 当前 Material.no 仍为 MaterialNo 类型（已是语义化 value class）。
 */
interface MaterialId

/**
 * 包装 id 标记接口 / Package id marker interface
 *
 * 供未来 infrastructure 侧 value class（PackageCode）实现接口后使用。
 * 当前 Package.code 仍为 PackageCode? 类型（已是语义化 value class）。
 */
interface PackageId

/**
 * ItemId 的默认实现：承载 String 裸值 / Default ItemId impl wrapping a raw String value
 *
 * 用于拼接生成 ActualItem.id（如 MaterialPacker / LayerGenerationProgramCandidateAdapters），
 * 以及测试构造 ActualItem 实例。
 * value class 仅允许单字段，此处为简单 data class 承载单字段并 override toString 以保持原拼接行为。
 */
data class ItemIdImpl(
    val value: String
) : ItemId {
    override fun toString(): String = value
}

/**
 * ItemId 工厂函数：由 String 裸值构造 [ItemId] / Factory constructing an [ItemId] from a raw String value
 *
 * 供测试构造 ActualItem 实例使用，避免直接暴露 [ItemIdImpl] 构造器。
 */
fun itemIdOf(value: String): ItemId = ItemIdImpl(value)

/**
 * BinTypeId 的默认实现：承载 String 裸值 / Default BinTypeId impl wrapping a raw String value
 *
 * 用于构造 BinType.typeCode，以及测试构造 BinType 实例。
 */
data class BinTypeIdImpl(
    val value: String
) : BinTypeId {
    override fun toString(): String = value
}

/**
 * BinTypeId 工厂函数：由 String 裸值构造 [BinTypeId] / Factory constructing a [BinTypeId] from a raw String value
 */
fun binTypeIdOf(value: String): BinTypeId = BinTypeIdImpl(value)
