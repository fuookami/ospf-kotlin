/**
 * 物料、设备与副产物。 / Materials, machines and by-products.
 *
 * 物料是**双模式**的核心：`height` 为空表示卷材（幅宽固定、长度待定，对应条带排样），否则为定尺板材。
 * / The material is the core of the **dual mode**: a null `height` means a coil (fixed width, length to be determined,
 * i.e. strip packing), otherwise it is a fixed-size sheet.
 *
 * @property V 数值类型 / numeric type
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.material.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Region2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.region2Of

/**
 * 物料形态。 / Material shape kind.
*/
enum class Material2Kind {
    /** 卷材：幅宽固定，长度待定 / coil: fixed width, length to be determined */
    Coil,

    /** 定尺板材：宽高固定 / fixed-size sheet: both width and height fixed */
    Sheet
}

/**
 * 物料。 / A material.
 *
 * @property V 数值类型 / numeric type
 * @property id 物料编号 / material identifier
 * @property name 物料名称 / material name
 * @property width 幅宽（卷材）或板宽（定尺） / coil width or sheet width
 * @property height 板高；卷材为 null / sheet height; null for a coil
 * @property length 卷长；null 表示待推导 / coil length; null means to be derived
 * @property unitWeight 单位重量 / unit weight
 * @property machineId 所属设备 / owning machine
 * @property availableBatches 可用卷数/张数 / available coil or sheet count
 * @property minRemnantArea 最小可用余料面积，低于该值的余料视为废料 / minimum usable remnant area; anything below it counts as scrap
*/
data class Material2<V : FloatingNumber<V>>(
    override val id: MaterialId2,
    override val name: String,
    override val width: Quantity<V>,
    override val height: Quantity<V>? = null,
    val length: Quantity<V>? = null,
    override val unitWeight: Quantity<V>? = null,
    val machineId: MachineId2? = null,
    val availableBatches: UInt64 = UInt64.maximum,
    val minRemnantArea: Quantity<V>? = null
) : Production2<V> {

    /** 物料形态 / material kind */
    val kind: Material2Kind get() = if (height == null) Material2Kind.Coil else Material2Kind.Sheet

    /** 是否为卷材 / whether this is a coil */
    val isCoil: Boolean get() = kind == Material2Kind.Coil

    /**
     * 定尺板材的面积；卷材返回 null。 / The area of a fixed-size sheet, or null for a coil.
     *
     * @return 面积 / area
    */
    fun sheetArea(): Quantity<V>? {
        val actualHeight = height ?: return null
        return width * actualHeight
    }

    /**
     * 构造一块料域。 / Build a region of material.
     *
     * 定尺模式用整块板材；卷材模式用「幅宽 × 给定段高」，段高就是这一段消耗的卷长。
     * / In sheet mode this is the whole sheet; in coil mode it is "width by the given segment height", where the segment
     * height is the coil length consumed by that segment.
     *
     * @param segmentHeight 段高（卷材模式必填；定尺模式忽略并取板高）/ segment height, required for coils and ignored for sheets
     * @return 料域，参数缺失时返回 null / the region, or null when a required parameter is missing
    */
    fun region(segmentHeight: Quantity<V>? = null): Region2<V>? {
        val actualHeight = height ?: segmentHeight ?: return null
        return region2Of(
            x = quantityZeroOf(width),
            y = quantityZeroOf(width),
            width = width,
            height = actualHeight
        )
    }

    /**
     * 校验物料几何与配置。 / Validate the material geometry and configuration.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        if (!quantityIsPositive(width)) {
            return Failed(
                Csp2dValidationError(
                    detail = "物料 $id 的幅宽必须为正：$width / The material width must be positive"
                )
            )
        }
        val actualHeight = height
        if (actualHeight != null && !quantityIsPositive(actualHeight)) {
            return Failed(
                Csp2dValidationError(
                    detail = "物料 $id 的板高必须为正：$actualHeight / The material height must be positive"
                )
            )
        }
        val actualLength = length
        if (actualLength != null && !quantityIsPositive(actualLength)) {
            return Failed(
                Csp2dValidationError(
                    detail = "物料 $id 的卷长必须为正：$actualLength / The material length must be positive"
                )
            )
        }
        if (isCoil && length != null) {
            // 卷长给定即退化为「按固定长度切段的定尺问题」，仍允许，但需在文档中说明语义。
            // A given coil length degrades the problem into fixed-length segments; still allowed, but the semantics must
            // be documented.
        }
        return ok
    }

    /**
     * 判断某块余料是否仍可复用。 / Decide whether a remnant is still reusable.
     *
     * @param remnantArea 余料面积 / remnant area
     * @return 是否可用 / whether usable
    */
    fun isUsableRemnant(remnantArea: Quantity<V>): Boolean {
        val threshold = minRemnantArea ?: return quantityIsPositive(remnantArea)
        return quantityGreaterOrEqual(remnantArea, threshold)
    }

    /**
     * 判断设备是否能加工本物料。 / Decide whether a machine can process this material.
     *
     * @param machine 设备 / machine
     * @return 是否可加工 / whether processable
    */
    fun enabledBy(machine: Machine2<V>): Boolean {
        val limit = machine.maxWidth ?: return true
        return quantityLessOrEqual(width, limit)
    }
}

/**
 * 设备。 / A machine.
 *
 * `defaultProcess` 把设备与裁切工艺绑定：剪板机只能是断头台，激光/水刀可以是任意排样。
 * / `defaultProcess` couples a machine to a cutting process: a shear can only do guillotine, while a laser or waterjet
 * can do arbitrary nesting.
 *
 * @property V 数值类型 / numeric type
 * @property id 设备编号 / machine identifier
 * @property name 设备名称 / machine name
 * @property maxBatchCount 最大批次数量 / maximum batch count
 * @property maxSwitchCount 最大换型次数 / maximum switch count
 * @property maxWidth 可加工最大幅宽 / maximum processable width
 * @property capacity 产能，通常为「面积/时间」或「面积」 / capacity, usually area per time or area
 * @property defaultProcess 默认裁切工艺 / default cutting process
*/
data class Machine2<V : FloatingNumber<V>>(
    val id: MachineId2,
    val name: String,
    val maxBatchCount: UInt64? = null,
    val maxSwitchCount: UInt64? = null,
    val maxWidth: Quantity<V>? = null,
    val capacity: Quantity<V>? = null,
    val defaultProcess: CuttingProcess2? = null
)

/**
 * 副产物（配规件）。 / A by-product (filler piece).
 *
 * 副产物用于填充主产品排完之后剩下的料域；它与成品件的区别只是「不是需求驱动的」，但在几何上同样是矩形件。
 * / A by-product fills the material left after the main products are laid out. Its only difference from a finished part
 * is that it is not demand driven; geometrically it is likewise a rectangle.
 *
 * @property V 数值类型 / numeric type
 * @property id 副产物编号 / by-product identifier
 * @property name 副产物名称 / by-product name
 * @property width 宽度 / width
 * @property height 高度 / height
 * @property allowRotate 是否允许旋转 90° / whether a 90-degree rotation is allowed
 * @property unitWeight 单位重量 / unit weight
*/
data class Costar2<V : FloatingNumber<V>>(
    override val id: CostarId2,
    override val name: String,
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    val allowRotate: Boolean = true,
    override val unitWeight: Quantity<V>? = null
) : Production2<V> {

    /** 单件面积 / single-piece area */
    val area: Quantity<V> get() = width * height

    /**
     * 校验副产物几何。 / Validate the by-product geometry.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        if (!quantityIsPositive(width) || !quantityIsPositive(height)) {
            return Failed(
                Csp2dValidationError(
                    detail = "副产物 $id 的宽高必须为正：$width × $height / By-product dimensions must be positive"
                )
            )
        }
        return ok
    }

    /**
     * 允许的摆放方向集合。 / The set of allowed orientations.
     *
     * @return 方向列表 / orientation list
    */
    fun orientations(): List<Boolean> {
        return if (allowRotate) listOf(false, true) else listOf(false)
    }
}
