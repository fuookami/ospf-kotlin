package fuookami.ospf.kotlin.core.solver.mindopt

/**
 * MindOpt 求解器配置接口 / MindOpt solver configuration interface.
 */
interface SolverConfig {
  /**
   * 转换为 MindOpt 参数字符串 / Convert to MindOpt parameter string.
   * @return MindOpt 参数字符串 / MindOpt parameter string.
   */
  fun toMindoptParams(): String
}

/**
 * MindOpt 求解器配置数据类 / MindOpt solver configuration data class.
 * @property enableConcurrent 是否启用并发求解 / Whether to enable concurrent solving.
 */
data class MindoptSolverConfig(
  /** 是否启用并发求解 / Whether to enable concurrent solving */
  val enableConcurrent: Boolean = false
) : SolverConfig {
  override fun toMindoptParams(): String {
    return "Presolve_Concurrent $enableConcurrent"
  }
}
