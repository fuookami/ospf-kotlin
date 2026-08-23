package fuookami.ospf.kotlin.core.solver.progress

import kotlinx.coroutines.CancellationException
import fuookami.ospf.kotlin.utils.functional.Try

/** 求解阶段标识 / Solver stage identifier */
@JvmInline
value class SolverStageId(val value: String)

/** 求解阶段类型 / Solver stage kind */
enum class StageKind {
    ModelBuilding,
    Registration,
    Presolve,
    ColumnGeneration,
    BranchAndPrice,
    Heuristic,
    MasterLP,
    MILP,
    PostProcessing
}

/**
 * 求解阶段。 / Solver stage.
 *
 * @property id 稳定阶段标识 / Stable stage identifier
 * @property defaultLabel 简体中文默认标签 / Simplified Chinese default label
 * @property messageKey 国际化键 / Localization key
 * @property kind 阶段类型 / Stage kind
 * @property labelOverride 调用方标签覆盖 / Caller-provided label override
 */
data class SolverStage(
    val id: SolverStageId,
    val defaultLabel: String,
    val messageKey: String,
    val kind: StageKind,
    val labelOverride: String? = null
) {
    /** 获取回退标签 / Get fallback label */
    val fallbackLabel: String get() = labelOverride ?: defaultLabel
}

/**
 * 求解子阶段。 / Solver sub-stage.
 *
 * @property key 稳定子阶段键 / Stable sub-stage key
 * @property defaultTemplate 简体中文默认模板 / Simplified Chinese default template
 * @property messageKey 国际化键 / Localization key
 * @property args 模板参数 / Template arguments
 */
data class SolverSubStage(
    val key: String,
    val defaultTemplate: String,
    val messageKey: String,
    val args: Map<String, String> = emptyMap()
) {
    /** 使用参数渲染指定模板 / Render the supplied template with arguments */
    fun render(template: String): String {
        return args.entries.fold(template) { text, (key, value) ->
            text.replace("{$key}", value)
        }
    }

    /** 使用参数渲染默认模板 / Render the default template with arguments */
    fun renderDefault(): String {
        return render(defaultTemplate)
    }
}

/**
 * 求解进度快照。 / Solver progress snapshot.
 *
 * @property stage 当前一级阶段 / Current top-level stage
 * @property subStage 当前二级阶段 / Current sub-stage
 * @property progressInStage 阶段内百分比，范围 0..100 / Percentage within the stage, in 0..100
 * @property overallProgress 总百分比，范围 0..100 / Overall percentage, in 0..100
 * @property diagnostics 结构化诊断字段 / Structured diagnostic fields
 * @property resolvedStageLabel 已解析阶段标签 / Resolved stage label
 * @property resolvedSubStageLabel 已解析子阶段标签 / Resolved sub-stage label
 */
data class SolverProgressSnapshot(
    val stage: SolverStage,
    val subStage: SolverSubStage? = null,
    val progressInStage: Int,
    val overallProgress: Int,
    val diagnostics: Map<String, String> = emptyMap(),
    val resolvedStageLabel: String = stage.fallbackLabel,
    val resolvedSubStageLabel: String? = subStage?.renderDefault()
) {
    /** 将百分比收敛到合法范围 / Clamp percentages to the valid range */
    fun normalized(): SolverProgressSnapshot {
        return copy(
            progressInStage = progressInStage.coerceIn(0, 100),
            overallProgress = overallProgress.coerceIn(0, 100)
        )
    }
}

/** 进度标签解析器，由上层本地化系统实现 / Progress label resolver implemented by the caller localization system */
fun interface ProgressLabelResolver {
    /**
     * 解析指定语言的文本。 / Resolve text for the requested locale.
     *
     * @param key 稳定国际化键 / Stable localization key
     * @param defaultText 简体中文默认文本 / Simplified Chinese default text
     * @param locale 语言标签 / Locale tag
     * @return 已解析文本 / Resolved text
     */
    fun resolve(key: String, defaultText: String, locale: String): String
}

/** 求解进度报告器 / Solver progress reporter */
fun interface ProgressReporter {
    /** 上报进度快照 / Report a progress snapshot */
    fun report(snapshot: SolverProgressSnapshot): Try
}

/**
 * 进度上报上下文。 / Progress reporting context.
 *
 * @property reporter 报告器 / Reporter
 * @property labelResolver 标签解析器 / Label resolver
 * @property locale 语言标签 / Locale tag
 * @property isCancelled 协作式取消判定 / Cooperative cancellation predicate
 */
data class SolverProgressContext(
    val reporter: ProgressReporter,
    val labelResolver: ProgressLabelResolver? = null,
    val locale: String = "zh-CN",
    val isCancelled: () -> Boolean = { false }
) {
    private val monotonicState = MonotonicProgressState()

    /** 在可中断边界检查取消状态 / Check cancellation state at an interruptible boundary */
    fun throwIfCancelled() {
        if (isCancelled()) {
            throw CancellationException("Solver execution cancelled")
        }
    }

    /** 解析文本并上报快照 / Resolve labels and report the snapshot */
    fun report(snapshot: SolverProgressSnapshot): Try {
        throwIfCancelled()
        val resolver = labelResolver
        val resolved = if (resolver == null) {
            snapshot.normalized()
        } else {
            snapshot.copy(
                resolvedStageLabel = resolver.resolve(
                    snapshot.stage.messageKey,
                    snapshot.stage.fallbackLabel,
                    locale
                ),
                resolvedSubStageLabel = snapshot.subStage?.let { subStage ->
                    subStage.render(
                        resolver.resolve(
                        subStage.messageKey,
                        subStage.defaultTemplate,
                        locale
                        )
                    )
                }
            ).normalized()
        }
        return reporter.report(monotonicState.accept(resolved))
    }
}

/** 在同一求解上下文内保持阶段内及总体进度单调。 */
private class MonotonicProgressState {
    private var lastOverallProgress = 0
    private val stageProgress = mutableMapOf<String, Int>()

    @Synchronized
    fun accept(snapshot: SolverProgressSnapshot): SolverProgressSnapshot {
        val stageId = snapshot.stage.id.value
        val progressInStage = maxOf(
            stageProgress[stageId] ?: 0,
            snapshot.progressInStage
        ).coerceIn(0, 100)
        val overallProgress = maxOf(lastOverallProgress, snapshot.overallProgress).coerceIn(0, 100)
        stageProgress[stageId] = progressInStage
        lastOverallProgress = overallProgress
        return snapshot.copy(
            progressInStage = progressInStage,
            overallProgress = overallProgress
        )
    }
}

/** OSPF 内置求解阶段目录 / Built-in OSPF solver stage catalog */
object SolverStages {
    val ModelBuilding = stage("MODEL_BUILDING", "模型构建", StageKind.ModelBuilding)
    val Registration = stage("REGISTRATION", "模型注册", StageKind.Registration)
    val Presolve = stage("PRESOLVE", "预求解", StageKind.Presolve)
    val ColumnGeneration = stage("COLUMN_GENERATION", "列生成", StageKind.ColumnGeneration)
    val BranchAndPrice = stage("BRANCH_AND_PRICE", "分支定价", StageKind.BranchAndPrice)
    val Heuristic = stage("HEURISTIC", "启发式求解", StageKind.Heuristic)
    val MasterLP = stage("MASTER_LP", "主问题求解", StageKind.MasterLP)
    val MILP = stage("MILP", "整数求解", StageKind.MILP)
    val PostProcessing = stage("POST_PROCESSING", "结果处理", StageKind.PostProcessing)

    /** 全部内置阶段 / All built-in stages */
    val entries: List<SolverStage> = listOf(
        ModelBuilding,
        Registration,
        Presolve,
        ColumnGeneration,
        BranchAndPrice,
        Heuristic,
        MasterLP,
        MILP,
        PostProcessing
    )

    private fun stage(id: String, label: String, kind: StageKind): SolverStage {
        return SolverStage(
            id = SolverStageId(id),
            defaultLabel = label,
            messageKey = "i18n.ospf.stage.$id",
            kind = kind
        )
    }
}
