/**
 * CSP1D 语义化标识接口 / CSP1D semantic identifier interfaces
 *
 * 以空标记接口（marker interface）替代裸 String id，提供编译期类型区分。
 * 继承关系按 id 派生：ProductionId 为生产物料基类，ProductId / CostarId / MaterialId 派生自它；
 * MachineId 与 CuttingPlanId 为独立标识。
 *
 * Replaces raw String ids with empty marker interfaces for compile-time type discrimination.
 * Inheritance follows id derivation: ProductionId is the base for production materials,
 * with ProductId / CostarId / MaterialId deriving from it;
 * MachineId and CuttingPlanId are standalone identifiers.
 */
package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

/**
 * 生产物料标识基类 / Base identifier for production materials
 *
 * 用于可空场景（如 Production.id）。/ Used for nullable scenarios (e.g. Production.id).
 */
interface ProductionId

/**
 * 产品标识 / Product identifier
 */
interface ProductId : ProductionId

/**
 * 配规标识 / Costar identifier
 */
interface CostarId : ProductionId

/**
 * 物料标识 / Material identifier
 */
interface MaterialId : ProductionId

/**
 * 设备标识 / Machine identifier
 */
interface MachineId

/**
 * 切割方案标识 / Cutting plan identifier
 */
interface CuttingPlanId

/**
 * CuttingPlanId 默认实现，用于拼接生成新方案 id 的场景 /
 * Default CuttingPlanId implementation for scenarios that generate new plan ids by concatenation
 *
 * @property value 字符串值 / String value
 */
data class CuttingPlanIdImpl(
    val value: String
) : CuttingPlanId {
    override fun toString(): String = value
}

/**
 * 工厂函数：从字符串构造 CuttingPlanId / Factory: build CuttingPlanId from a string
 *
 * @param value 字符串值 / String value
 * @return CuttingPlanId 实例 / CuttingPlanId instance
 */
fun cuttingPlanIdOf(value: String): CuttingPlanId = CuttingPlanIdImpl(value)

/**
 * ProductId 默认实现 / Default ProductId implementation
 *
 * @property value 字符串值 / String value
 */
data class ProductIdImpl(
    val value: String
) : ProductId {
    override fun toString(): String = value
}

/**
 * 工厂函数：从字符串构造 ProductId / Factory: build ProductId from a string
 */
fun productIdOf(value: String): ProductId = ProductIdImpl(value)

/**
 * CostarId 默认实现 / Default CostarId implementation
 *
 * @property value 字符串值 / String value
 */
data class CostarIdImpl(
    val value: String
) : CostarId {
    override fun toString(): String = value
}

/**
 * 工厂函数：从字符串构造 CostarId / Factory: build CostarId from a string
 */
fun costarIdOf(value: String): CostarId = CostarIdImpl(value)

/**
 * MaterialId 默认实现 / Default MaterialId implementation
 *
 * @property value 字符串值 / String value
 */
data class MaterialIdImpl(
    val value: String
) : MaterialId {
    override fun toString(): String = value
}

/**
 * 工厂函数：从字符串构造 MaterialId / Factory: build MaterialId from a string
 */
fun materialIdOf(value: String): MaterialId = MaterialIdImpl(value)

/**
 * MachineId 默认实现 / Default MachineId implementation
 *
 * @property value 字符串值 / String value
 */
data class MachineIdImpl(
    val value: String
) : MachineId {
    override fun toString(): String = value
}

/**
 * 工厂函数：从字符串构造 MachineId / Factory: build MachineId from a string
 */
fun machineIdOf(value: String): MachineId = MachineIdImpl(value)
