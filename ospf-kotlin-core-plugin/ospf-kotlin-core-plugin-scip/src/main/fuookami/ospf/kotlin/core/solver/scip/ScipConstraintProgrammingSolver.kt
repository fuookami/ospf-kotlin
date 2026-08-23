/**
 * SCIP CP solver adapter。 / SCIP CP solver adapter.
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.scip

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import jscip.Scip
import jscip.SCIP_Status
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.config.SCIPSolverConfig
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflictMinimality
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.output.toCompatibilityFlt64
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressSnapshot
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.progress.SolverStages
import fuookami.ospf.kotlin.core.solver.progress.SolverSubStage
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.redactedValue
import fuookami.ospf.kotlin.core.solver.report.AuditFingerprint
import fuookami.ospf.kotlin.core.solver.report.EvidenceMinimality
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidence
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprints
import fuookami.ospf.kotlin.core.solver.report.SolveFingerprinting
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolveStatistics
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverProvenance
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId

private const val UNKNOWN_RUNTIME_VERSION = "unknown"
private const val BUILD_PLUGIN_VERSION = "1.1.0"

private fun stableRuntimeVersion(value: String?): String? {
    val normalized = value?.trim()?.takeUnless { it.isEmpty() } ?: return null
    return normalized.takeIf { candidate ->
        candidate.length <= 128 && candidate.all { character ->
            character.isLetterOrDigit() || character == '.' || character == '-' || character == '_' || character == '+'
        }
    }
}

private fun runtimeNativeVersion(): String {
    return stableRuntimeVersion(System.getProperty("ospf.scip.native.version"))
        ?: stableRuntimeVersion(System.getProperty("scip.version"))
        ?: stableRuntimeVersion(System.getenv("SCIP_VERSION"))
        ?: UNKNOWN_RUNTIME_VERSION
}

private fun runtimePluginVersion(): String {
    return stableRuntimeVersion(System.getProperty("ospf.scip.plugin.version"))
        ?: stableRuntimeVersion(System.getenv("OSPF_SCIP_PLUGIN_VERSION"))
        ?: stableRuntimeVersion(ScipConstraintProgrammingSolver::class.java.`package`?.implementationVersion)
        ?: BUILD_PLUGIN_VERSION
}

private data class RuntimeLibraryLocation(
    val mode: String,
    val path: Path?
)

private fun runtimeLibraryLocation(): RuntimeLibraryLocation {
    if (ScipSolver.loadedLibrary) {
        return RuntimeLibraryLocation(
            mode = ScipSolver.loadedLibraryMode,
            path = ScipSolver.loadedLibraryPath
        )
    }
    val explicit = System.getProperty("ospf.scip.library")?.takeUnless { it.isBlank() }
    val library = if (explicit != null) {
        runCatching { Path.of(explicit).toAbsolutePath().normalize() }.getOrNull()
    } else {
        val fileName = System.mapLibraryName("jscip")
        System.getProperty("java.library.path")
            ?.split(java.io.File.pathSeparator)
            ?.asSequence()
            ?.mapNotNull { directory -> runCatching { Path.of(directory).resolve(fileName) }.getOrNull() }
            ?.firstOrNull { Files.isRegularFile(it) }
    }
    return RuntimeLibraryLocation(
        mode = if (explicit != null) "explicit" else "system",
        path = library
    )
}

private fun runtimeLibraryLoadMode(): String {
    return runtimeLibraryLocation().mode
}

private fun runtimeSearchDirectories(library: Path): List<Path> {
    val values = buildList {
        library.parent?.let(::add)
        listOf(
            "java.library.path",
            "PATH",
            "LD_LIBRARY_PATH",
            "DYLD_LIBRARY_PATH"
        ).forEach { property ->
            val value = if (property == "java.library.path") {
                System.getProperty(property)
            } else {
                System.getenv(property)
            }
            value
                ?.split(java.io.File.pathSeparator)
                ?.filter { it.isNotBlank() }
                ?.mapNotNullTo(this) { directory ->
                    runCatching { Path.of(directory).toAbsolutePath().normalize() }.getOrNull()
                }
        }
    }
    return values.distinct()
}

private fun runtimeDependencyPath(library: Path, fileName: String): Path? {
    val windows = System.getProperty("os.name").contains("win", ignoreCase = true)
    for (directory in runtimeSearchDirectories(library)) {
        val exact = directory.resolve(fileName)
        if (Files.isRegularFile(exact)) {
            return exact
        }
        if (!windows) {
            val versioned = runCatching {
                Files.list(directory).use { files ->
                    files
                        .filter { candidate ->
                            Files.isRegularFile(candidate) &&
                                candidate.fileName.toString().startsWith(fileName)
                        }
                        .sorted()
                        .findFirst()
                        .orElse(null)
                }
            }.getOrNull()
            if (versioned != null) {
                return versioned
            }
        }
    }
    return null
}

private fun runtimeLibraryIdentity(): String {
    val library = runtimeLibraryLocation().path
    if (library == null) {
        return UNKNOWN_RUNTIME_VERSION
    }
    fun digest(path: Path): String? {
        return runCatching {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(path).use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) {
                        break
                    }
                    messageDigest.update(buffer, 0, count)
                }
            }
            messageDigest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
        }.getOrNull()
    }
    val primary = digest(library) ?: return UNKNOWN_RUNTIME_VERSION
    val dependencyNames = if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        listOf("libscip.dll")
    } else if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
        listOf("libscip.dylib")
    } else {
        listOf("libscip.so")
    }
    val dependencies = dependencyNames.map { name ->
        val dependency = runtimeDependencyPath(library, name)
        "$name:sha256:${dependency?.let(::digest) ?: "unknown"}"
    }
    return buildList {
        add("jscip:sha256:$primary")
        addAll(dependencies)
    }.joinToString("|")
}

private data class LegacyRuntimeLibraryLocation(
    val mode: String,
    val path: Path?,
    val searchPath: String?
)

private fun legacySystemLibrary(searchPath: String?): Path? {
    val fileName = System.mapLibraryName("jscip")
    return searchPath
        ?.split(java.io.File.pathSeparator)
        ?.asSequence()
        ?.mapNotNull { directory -> runCatching { Path.of(directory).resolve(fileName) }.getOrNull() }
        ?.firstOrNull { Files.isRegularFile(it) }
}

private fun legacyRuntimeLibraryLocation(): LegacyRuntimeLibraryLocation {
    val explicit = System.getProperty("ospf.scip.library")?.takeUnless { it.isBlank() }
    val searchPath = System.getProperty("java.library.path")
    val system = legacySystemLibrary(searchPath)
    val primary = if (explicit != null) {
        LegacyRuntimeLibraryLocation(
            mode = "explicit",
            path = runCatching { Path.of(explicit).toAbsolutePath().normalize() }.getOrNull(),
            searchPath = searchPath
        )
    } else {
        LegacyRuntimeLibraryLocation(
            mode = "system",
            path = system,
            searchPath = searchPath
        )
    }
    return primary
}

private fun legacyPathRuntimeNativeVersion(): String {
    return System.getProperty("ospf.scip.native.version")
        ?.takeUnless { it.isBlank() }
        ?: System.getenv("SCIP_VERSION")?.takeUnless { it.isBlank() }
        ?: UNKNOWN_RUNTIME_VERSION
}

private fun legacyPathRuntimePluginVersions(): Set<String> {
    val packageVersion = ScipConstraintProgrammingSolver::class.java.`package`?.implementationVersion
        ?.takeUnless { it.isBlank() }
    val codeSourceVersion = ScipConstraintProgrammingSolver::class.java.protectionDomain?.codeSource?.location
        ?.toExternalForm()
        ?.takeUnless { it.isBlank() }
    return buildSet {
        packageVersion?.let(::add)
        codeSourceVersion?.let(::add)
        if (isEmpty()) {
            add(UNKNOWN_RUNTIME_VERSION)
        }
    }
}

private fun legacyPathRuntimeLibraryIdentity(): String {
    val location = legacyRuntimeLibraryLocation()
    val library = location.path
    if (library == null) {
        return "${location.mode}:${location.searchPath ?: UNKNOWN_RUNTIME_VERSION}"
    }
    return runCatching {
        val attributes = Files.readAttributes(
            library,
            java.nio.file.attribute.BasicFileAttributes::class.java
        )
        "${location.mode}:$library:size=${attributes.size()}:modified=${attributes.lastModifiedTime().toMillis()}"
    }.getOrDefault("${location.mode}:$library")
}

private fun legacyLibraryDigest(path: Path?): String? {
    return path?.let {
        runCatching {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(it).use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) {
                        break
                    }
                    messageDigest.update(buffer, 0, count)
                }
            }
            messageDigest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
        }.getOrNull()
    }
}

private fun legacyContentRuntimeNativeVersion(): String {
    return stableRuntimeVersion(System.getProperty("ospf.scip.native.version"))
        ?: stableRuntimeVersion(System.getProperty("scip.version"))
        ?: stableRuntimeVersion(System.getenv("SCIP_VERSION"))
        ?: UNKNOWN_RUNTIME_VERSION
}

private fun legacyContentRuntimePluginVersion(): String {
    return stableRuntimeVersion(System.getProperty("ospf.scip.plugin.version"))
        ?: stableRuntimeVersion(System.getenv("OSPF_SCIP_PLUGIN_VERSION"))
        ?: stableRuntimeVersion(ScipConstraintProgrammingSolver::class.java.`package`?.implementationVersion)
        ?: BUILD_PLUGIN_VERSION
}

private fun legacyContentRuntimeLibraryIdentities(): Set<String> {
    val primary = legacyRuntimeLibraryLocation()
    val primaryDigest = legacyLibraryDigest(primary.path)
    if (primaryDigest == null) {
        return setOf("${primary.mode}:$UNKNOWN_RUNTIME_VERSION")
    }
    return linkedSetOf(
        "explicit:sha256:$primaryDigest",
        "system:sha256:$primaryDigest"
    )
}

private fun legacyRuntimeFingerprint(
    pluginVersion: String,
    nativeVersion: String,
    bindingVersion: String,
    libraryIdentity: String
): String {
    return "scip-runtime-1:" + SolveFingerprinting.sha256(
        listOf(
            "solverId=scip-cp",
            "backend=SCIP",
            "nativeVersion=$nativeVersion",
            "bindingVersion=$bindingVersion",
            "pluginVersion=$pluginVersion",
            "nativeLibraryIdentity=$libraryIdentity"
        ).joinToString("|"),
        schemaVersion = "scip-runtime-1"
    ).value
}

/**
 * Reconstruct the two historical SCIP runtime fingerprint generations for legacy checkpoint matching.
 * 为 legacy checkpoint 匹配重建两代历史 SCIP 运行时指纹集合。
 *
 * The set includes both the path/size/mtime generation and the content-SHA generation, each with
 * only values that can be reconstructed from the current environment. / 集合同时包含路径/size/mtime
 * 代与内容 SHA 代，并且只加入当前环境可重建的值。
 *
 * For the content generation, both explicit/system modes are included when the currently located
 * library has a readable digest; an unreadable library keeps only its current mode's unknown value.
 * / 对内容代，只要当前定位的库摘要可读就同时加入 explicit/system 两种 mode；摘要不可读时只保留当前
 * mode 的 unknown 值。
 *
 * @return exact legacy runtime fingerprint candidates / 精确的 legacy 运行时指纹候选集合
 */
fun scipRuntimeFingerprintLegacyV1Candidates(): Set<String> {
    val bindingVersion = ScipBindingCapabilityAssessmentProvider.current().bindingVersion
    val fingerprints = linkedSetOf<String>()
    val pathLibraryIdentity = legacyPathRuntimeLibraryIdentity()
    legacyPathRuntimePluginVersions().forEach { pluginVersion ->
        fingerprints += legacyRuntimeFingerprint(
            pluginVersion = pluginVersion,
            nativeVersion = legacyPathRuntimeNativeVersion(),
            bindingVersion = bindingVersion,
            libraryIdentity = pathLibraryIdentity
        )
    }
    val contentLibraryIdentities = legacyContentRuntimeLibraryIdentities()
    contentLibraryIdentities.forEach { libraryIdentity ->
        fingerprints += legacyRuntimeFingerprint(
            pluginVersion = legacyContentRuntimePluginVersion(),
            nativeVersion = legacyContentRuntimeNativeVersion(),
            bindingVersion = bindingVersion,
            libraryIdentity = libraryIdentity
        )
    }
    return fingerprints
}

/**
 * 生成当前 SCIP 运行时身份指纹。 / Generate the current SCIP runtime identity fingerprint.
 *
 * @return 原生库、binding 与插件组合的身份指纹 / Identity fingerprint for the native library, binding, and plugin combination
 */
fun scipRuntimeFingerprint(): String {
    val binding = ScipBindingCapabilityAssessmentProvider.current()
    return "scip-runtime-2:" + SolveFingerprinting.sha256(
        listOf(
            "solverId=scip-cp",
            "backend=SCIP",
            "nativeVersion=${runtimeNativeVersion()}",
            "bindingVersion=${binding.bindingVersion}",
            "pluginVersion=${runtimePluginVersion()}",
            "nativeLibraryIdentity=${runtimeLibraryIdentity()}"
        ).joinToString("|"),
        schemaVersion = "scip-runtime-2"
    ).value
}

/** SCIP CP 求解器。 / SCIP CP solver.
 *
 * @property sparseDomainLimit Maximum sparse-domain expansion size. / 稀疏值域展开规模上限。
 * @property decompositionLimit Maximum decomposition size for global constraints. / 全局约束分解规模上限。
 */
class ScipConstraintProgrammingSolver(
    private val sparseDomainLimit: Int = 128,
    private val decompositionLimit: Int = 256
) : ConstraintProgrammingSolver {
    override val descriptor: SolverDescriptor = SolverDescriptor(
        solverId = "scip-cp",
        backendName = "SCIP",
        backendVersion = runtimeNativeVersion().takeUnless { it == UNKNOWN_RUNTIME_VERSION },
        pluginVersion = runtimePluginVersion().takeUnless { it == UNKNOWN_RUNTIME_VERSION },
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.CP),
            interrupt = true,
            constraintProgrammingFeatures = mapOf(
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.BooleanLogic to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.Native,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Reification to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.SparseDomain to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.AllDifferent to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Element to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Table to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Interval to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.NoOverlap to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Cumulative to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.Native,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Circuit to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Automaton to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Reservoir to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.Assumption to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.ConflictCore to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.ExactLowering,
                fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.IncrementalSolve to fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.Unsupported
            )
        )
    )

    override suspend fun solve(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSolverOutput> {
        val session = createSession(model, options)
        if (session.failed) {
            return propagate(session)
        }
        return try {
            session.value!!.solve()
        } finally {
            session.value!!.close()
        }
    }

    override fun createSession(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions
    ): Ret<ConstraintProgrammingSession> {
        if (sparseDomainLimit <= 0 || decompositionLimit <= 0) {
            return Failed(ErrorCode.IllegalArgument, "SCIP CP 编译规模上限必须为正 / SCIP CP compilation limits must be positive")
        }
        validateOptions(options)?.let {
            return Failed(ErrorCode.IllegalArgument, it)
        }
        val snapshot = model.snapshot()
        if (snapshot.failed) {
            return propagate(snapshot)
        }
        return ok(
            ScipConstraintProgrammingSession(
                model = model,
                snapshot = snapshot.value!!,
                options = options,
                descriptor = descriptor,
                sparseDomainLimit = sparseDomainLimit,
                decompositionLimit = decompositionLimit
            )
        )
    }

    private fun validateOptions(options: ConstraintProgrammingSolveOptions): String? {
        val timeLimit = options.timeLimit
        if (timeLimit != null && timeLimit.isNegative()) {
            return "SCIP CP timeLimit 不得为负 / SCIP CP timeLimit must not be negative"
        }
        if (options.nodeLimit != null && options.nodeLimit == fuookami.ospf.kotlin.math.algebra.number.UInt64.zero) {
            return "SCIP CP nodeLimit 必须为正 / SCIP CP nodeLimit must be positive"
        }
        if (options.solutionLimit != null && options.solutionLimit == fuookami.ospf.kotlin.math.algebra.number.UInt64.zero) {
            return "SCIP CP solutionLimit 必须为正 / SCIP CP solutionLimit must be positive"
        }
        options.threadCount?.let { threadCount ->
            if (threadCount <= 0) {
                return "SCIP CP threadCount 必须为正 / SCIP CP threadCount must be positive"
            }
        }
        if (options.deterministic && options.threadCount != null && options.threadCount != 1) {
            return "SCIP CP deterministic 模式必须使用单线程 / SCIP CP deterministic mode requires a single thread"
        }
        if (options.conflictShrinkLimit != null &&
            options.conflictShrinkLimit == fuookami.ospf.kotlin.math.algebra.number.UInt64.zero
        ) {
            return "SCIP CP conflictShrinkLimit 必须为正 / SCIP CP conflictShrinkLimit must be positive"
        }
        if (options.conflictActivationIds?.any { it.isBlank() } == true) {
            return "SCIP CP conflictActivationIds 不得包含空 ID / SCIP CP conflictActivationIds must not contain blank IDs"
        }
        options.relativeObjectiveGap?.toDouble()?.let { gap ->
            if (!gap.isFinite() || gap < 0.0 || gap > 1.0) {
                return "SCIP CP relativeObjectiveGap 必须位于 [0, 1] / SCIP CP relativeObjectiveGap must be in [0, 1]"
            }
        }
        options.absoluteObjectiveGap?.toDouble()?.let { gap ->
            if (!gap.isFinite() || gap < 0.0) {
                return "SCIP CP absoluteObjectiveGap 必须为非负有限值 / SCIP CP absoluteObjectiveGap must be finite and non-negative"
            }
        }
        options.randomSeed?.let { seed ->
            if (seed < Int.MIN_VALUE || seed > Int.MAX_VALUE) {
                return "SCIP CP randomSeed 超出 JSCIP 整数参数范围 / SCIP CP randomSeed exceeds the JSCIP integer parameter range"
            }
        }
        return null
    }
}

private class ScipConstraintProgrammingSession(
    override val model: ConstraintProgrammingModel,
    private val snapshot: ConstraintProgrammingModelSnapshot,
    override val options: ConstraintProgrammingSolveOptions,
    private val descriptor: SolverDescriptor,
    private val sparseDomainLimit: Int,
    private val decompositionLimit: Int
) : ScipSolver(), ConstraintProgrammingSession {

    private var closed = false
    private var nativeInitialized = false
    private var compiled: ScipConstraintProgrammingCompiledModel? = null
    private var configuredThreadCount: Int? = null
    private var configuredRandomSeed: Long? = null

    override val isClosed: Boolean
        get() = closed

    override suspend fun solve(
        assumptions: List<BooleanLiteral>,
        fixedValues: Map<VariableId, Int64>,
        hints: ConstraintProgrammingSolution?
    ): Ret<ConstraintProgrammingSolverOutput> {
        if (closed) {
            return Failed(ErrorCode.ApplicationStopped, "SCIP CP session 已关闭 / SCIP CP session is closed")
        }
        if (options.cancellationToken?.isCancellationRequested == true) {
            return ok(ConstraintProgrammingUnknownOutput(TerminationReason.Cancelled))
        }
        val started = System.nanoTime()
        closeCurrentNativeModel()
        val initialized = initNative()
        if (initialized.failed) {
            return propagate(initialized)
        }
        val configured = configure()
        if (configured.failed) {
            closeCurrentNativeModel()
            return propagate(configured)
        }
        val buildProgress = reportProgress(
            stage = SolverStages.ModelBuilding,
            progressInStage = 0,
            overallProgress = 0,
            subStage = SolverSubStage(
                key = "compile",
                defaultTemplate = "编译 CP 模型",
                messageKey = "i18n.ospf.substage.cp_compile"
            )
        )
        if (buildProgress.failed) {
            closeCurrentNativeModel()
            return propagate(buildProgress)
        }
        val compiler = ScipConstraintProgrammingCompiler(
            scip = scip,
            snapshot = snapshot,
            sparseDomainLimit = sparseDomainLimit,
            decompositionLimit = decompositionLimit
        )
        val compiledResult = compiler.compile(
            assumptions = assumptions,
            fixedValues = fixedValues,
            diagnosticMode = options.collectConflict || options.conflictActivationIds != null,
            activeActivationIds = options.conflictActivationIds
        )
        if (compiledResult.failed) {
            compiler.cleanup()
            closeCurrentNativeModel()
            return propagate(compiledResult)
        }
        compiled = compiledResult.value
        val compiledProgress = reportProgress(
            stage = SolverStages.ModelBuilding,
            progressInStage = 100,
            overallProgress = 30,
            subStage = SolverSubStage(
                key = "compiled",
                defaultTemplate = "CP 模型编译完成",
                messageKey = "i18n.ospf.substage.cp_compiled"
            )
        )
        if (compiledProgress.failed) {
            closeCurrentNativeModel()
            return propagate(compiledProgress)
        }
        val hintResult = applyHint(hints)
        if (hintResult.failed) {
            closeCurrentNativeModel()
            return propagate(hintResult)
        }
        val cancellationWatcher = startCancellationWatcher()
        return try {
            val solvingProgress = reportProgress(
                stage = SolverStages.MILP,
                progressInStage = 0,
                overallProgress = 30,
                subStage = SolverSubStage(
                    key = "search",
                    defaultTemplate = "SCIP 搜索中",
                    messageKey = "i18n.ospf.substage.cp_search"
                )
            )
            if (solvingProgress.failed) {
                propagate(solvingProgress)
            } else if (options.cancellationToken?.isCancellationRequested == true) {
                unknown(started, TerminationReason.Cancelled)
            } else {
                scip.solve()
                val extracted = extract(started, assumptions)
                if (extracted.failed) {
                    propagate(extracted)
                } else {
                    val result = extracted.value!!
                    val completedProgress = reportProgress(
                        stage = SolverStages.PostProcessing,
                        progressInStage = 100,
                        overallProgress = 100,
                        subStage = SolverSubStage(
                            key = "completed",
                            defaultTemplate = "SCIP 求解完成",
                            messageKey = "i18n.ospf.substage.cp_completed"
                        )
                    )
                    if (completedProgress.failed) {
                        propagate(completedProgress)
                    } else if (result is ConstraintProgrammingInfeasibleOutput &&
                        options.shrinkConflict &&
                        (assumptions.isNotEmpty() || result.conflict?.activationIds?.isNotEmpty() == true)
                    ) {
                        closeCurrentNativeModel()
                        shrinkConflict(result, assumptions)
                    } else {
                        ok(result)
                    }
                }
            }
        } catch (_: CancellationException) {
            unknown(started, TerminationReason.Cancelled)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "SCIP CP 求解失败：${error.message ?: error::class.simpleName} / " +
                    "SCIP CP solve failed: ${error.message ?: error::class.simpleName}"
            )
        } finally {
            stopCancellationWatcher(cancellationWatcher)
            closeCurrentNativeModel()
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        closeCurrentNativeModel()
        if (nativeInitialized) {
            super.close()
            nativeInitialized = false
        }
    }

    private suspend fun initNative(): Ret<Unit> {
        if (nativeInitialized) {
            return ok(Unit)
        }
        val explicit = System.getProperty("ospf.scip.library")
        if (ScipSolver.loadedLibrary && !explicit.isNullOrBlank()) {
            val requested = runCatching { Path.of(explicit).toAbsolutePath().normalize() }.getOrNull()
            val loaded = ScipSolver.loadedLibraryPath
            val sameLibrary = requested != null && loaded != null && runCatching {
                Files.isSameFile(requested, loaded)
            }.getOrDefault(requested == loaded)
            if (!sameLibrary) {
                return Failed(
                    ErrorCode.SolverNotFound,
                    "SCIP 已加载的原生库与显式配置不一致：loaded=${loaded?.fileName ?: "unknown"}, requested=${requested?.fileName ?: "invalid"} / " +
                        "The loaded SCIP native library differs from the explicitly configured library"
                )
            }
        }
        if (!ScipSolver.loadedLibrary) {
            try {
                if (!explicit.isNullOrBlank()) {
                    val path = Path.of(explicit).toAbsolutePath().normalize()
                    System.load(path.toString())
                    ScipSolver.loadedLibraryPath = path
                    ScipSolver.loadedLibraryMode = "explicit"
                } else {
                    System.loadLibrary("jscip")
                    ScipSolver.loadedLibraryPath = System.getProperty("java.library.path")
                        ?.split(java.io.File.pathSeparator)
                        ?.asSequence()
                        ?.mapNotNull { directory ->
                            runCatching {
                                Path.of(directory).resolve(System.mapLibraryName("jscip"))
                            }.getOrNull()
                        }
                        ?.firstOrNull { Files.isRegularFile(it) }
                    ScipSolver.loadedLibraryMode = "system"
                }
                ScipSolver.loadedLibrary = true
            } catch (error: Throwable) {
                return Failed(
                    ErrorCode.SolverNotFound,
                    "无法加载 JSCIP 原生库：${error.message ?: error::class.simpleName} / " +
                        "Unable to load the JSCIP native library: ${error.message ?: error::class.simpleName}"
                )
            }
        }
        val result = init(snapshot.name)
        if (result.failed) {
            return propagate(result)
        }
        nativeInitialized = true
        return ok(Unit)
    }

    private fun configure(): Ret<Unit> {
        return try {
            scip.hideOutput(!options.logEnabled)
            options.timeLimit?.let {
                scip.setRealParam(
                    "limits/time",
                    it.inWholeNanoseconds.toDouble() / 1_000_000_000.0
                )
            }
            options.nodeLimit?.let { scip.setLongintParam("limits/nodes", it.toLong()) }
            options.solutionLimit?.let { scip.setLongintParam("limits/solutions", it.toLong()) }
            val backendDeterministic = (options.backendConfiguration as? SCIPSolverConfig)?.deterministic == true
            val requestedThreadCount = options.threadCount ?: if (options.deterministic || backendDeterministic) 1 else null
            when (val backendConfiguration = applyBackendConfiguration(options.backendConfiguration, requestedThreadCount)) {
                is Failed -> return Failed(backendConfiguration.error)
                is Fatal -> return Fatal(backendConfiguration.errors)
                else -> Unit
            }
            requestedThreadCount?.let { threadCount ->
                scip.setIntParam("parallel/maxnthreads", threadCount)
            }
            options.randomSeed?.let { seed ->
                scip.setIntParam("randomization/randomseedshift", seed.toInt())
            }
            if (options.deterministic) {
                scip.setBoolParam("randomization/permutevars", false)
                scip.setBoolParam("randomization/permuteconss", false)
            }
            options.relativeObjectiveGap?.let { scip.setRealParam("limits/gap", it.toDouble()) }
            options.absoluteObjectiveGap?.let { scip.setRealParam("limits/absgap", it.toDouble()) }
            configuredThreadCount = scip.getIntParam("parallel/maxnthreads")
            configuredRandomSeed = scip.getIntParam("randomization/randomseedshift").toLong()
            ok(Unit)
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineModelingException,
                "SCIP CP 参数配置失败：${error.message ?: error::class.simpleName} / " +
                    "SCIP CP parameter configuration failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }

    private fun extract(
        started: Long,
        assumptions: List<BooleanLiteral>
    ): Ret<ConstraintProgrammingSolverOutput> {
        val status = scip.status
        val reason = ScipConstraintProgrammingStatusMapper.terminationReason(status)
        val solution = scip.bestSol
        if (status == SCIP_Status.SCIP_STATUS_INFEASIBLE) {
            val conflict = if (options.collectConflict) {
                val activationEntries = compiled?.activations.orEmpty()
                val members = activationEntries.values.mapTo(linkedSetOf()) { it.member }
                val constraintIds = members.mapNotNullTo(linkedSetOf()) {
                    (it as? InfeasibilityMember.Constraint)?.id
                }
                ConstraintProgrammingConflict(
                    constraintIds = if (constraintIds.isEmpty() && assumptions.isEmpty()) {
                        snapshot.constraints.mapTo(linkedSetOf()) { it.id }
                    } else constraintIds,
                    variableIds = assumptions.mapNotNullTo(linkedSetOf()) { it.variableId },
                    assumptions = assumptions,
                    members = members,
                    activationIds = activationEntries.keys,
                    activationMembers = activationEntries.mapValues { it.value.member },
                    validity = EvidenceValidity.Verified,
                    message = if (assumptions.isEmpty()) {
                        "SCIP returned a proven infeasible model / SCIP 已证明模型不可行"
                    } else {
                        "All active assumptions are a proven conflict seed / 所有活动 assumption 构成已证明冲突种子"
                    }
                )
            } else {
                null
            }
            return ok(
                ConstraintProgrammingInfeasibleOutput(
                    conflict = conflict,
                    proofStatus = ProofStatus.Verified,
                    report = report(
                        started = started,
                        status = ProblemStatus.Infeasible,
                        reason = reason,
                        presence = SolutionPresence.None,
                        solution = null,
                        proof = ProofStatus.Verified,
                        diagnostics = conflict?.let(::conflictDiagnostics) ?: SolveDiagnostics()
                    )
                )
            )
        }
        if (solution == null) {
            val problemStatus = when (status) {
                SCIP_Status.SCIP_STATUS_UNBOUNDED -> ProblemStatus.Unbounded
                SCIP_Status.SCIP_STATUS_INFORUNBD -> ProblemStatus.InfeasibleOrUnbounded
                else -> ProblemStatus.Unknown
            }
            return ok(ConstraintProgrammingUnknownOutput(reason, report(started, problemStatus, reason, SolutionPresence.None, null, ProofStatus.None)))
        }
        val extracted = extractSolution(solution)
        if (extracted.failed) {
            return propagate(extracted)
        }
        val optimal = status == SCIP_Status.SCIP_STATUS_OPTIMAL
        val exactObjective = if (snapshot.objectives.isEmpty()) {
            null
        } else {
            when (val evaluated = snapshot.objectives.first().expression.evaluate(extracted.value!!.values)) {
                is Failed -> return propagate(evaluated)
                is Fatal -> return propagate(evaluated)
                else -> evaluated.value
            }
        }
        val objective = exactObjective?.toCompatibilityFlt64()
        val bound = if (snapshot.objectives.isEmpty()) null else Flt64(scip.dualbound)
        val relativeGap = if (snapshot.objectives.isEmpty()) null else {
            scip.getGap().takeIf { it.isFinite() && it >= 0.0 }?.let(::Flt64)
        }
        return ok(
            ConstraintProgrammingFeasibleOutput(
                solution = extracted.value!!,
                objective = objective,
                bestBound = bound,
                status = if (optimal) SolverStatus.Optimal else SolverStatus.Feasible,
                proofStatus = if (optimal) ProofStatus.Verified else ProofStatus.None,
                exactObjective = exactObjective,
                report = report(
                    started,
                    ProblemStatus.Feasible,
                    reason,
                    if (optimal) SolutionPresence.Optimal else SolutionPresence.Incumbent,
                    extracted.value,
                    if (optimal) ProofStatus.Verified else ProofStatus.None,
                    exactObjective,
                    bestBound = bound,
                    gap = relativeGap
                )
            )
        )
    }

    private fun extractSolution(solution: jscip.Solution): Ret<ConstraintProgrammingSolution> {
        val values = LinkedHashMap<VariableId, Int64>()
        val compiledModel = compiled
            ?: return Failed(ErrorCode.ApplicationError, "SCIP CP 编译模型缺失 / Compiled SCIP CP model is missing")
        for ((id, variable) in compiledModel.variables) {
            val raw = scip.getSolVal(solution, variable)
            val rounded = raw.roundToLong()
            if (abs(raw - rounded.toDouble()) > 1e-6) {
                return Failed(ErrorCode.ORSolutionInvalid, "SCIP CP 返回非整数解 / SCIP CP returned a non-integer value")
            }
            values[id] = Int64(rounded)
        }
        val intervals = LinkedHashMap<fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId, IntervalValue>()
        for ((id, interval) in compiledModel.intervals) {
            val start = scip.getSolVal(solution, interval.start).roundToLong()
            val end = scip.getSolVal(solution, interval.end).roundToLong()
            intervals[id] = IntervalValue(Int64(start), Int64(interval.size.toLong()), Int64(end))
        }
        return ok(ConstraintProgrammingSolution(values = values, intervals = intervals))
    }

    private fun unknown(started: Long, reason: TerminationReason): Ret<ConstraintProgrammingSolverOutput> {
        return ok(ConstraintProgrammingUnknownOutput(reason, report(started, ProblemStatus.Unknown, reason, SolutionPresence.None, null, ProofStatus.None)))
    }

    private fun report(
        started: Long,
        status: ProblemStatus,
        reason: TerminationReason,
        presence: SolutionPresence,
        solution: ConstraintProgrammingSolution?,
        proof: ProofStatus,
        objective: Int64? = null,
        bestBound: Flt64? = null,
        gap: Flt64? = null,
        diagnostics: SolveDiagnostics<Int64> = SolveDiagnostics()
    ): SolveReport<Int64> {
        return SolveReport(
            problemStatus = status,
            terminationReason = reason,
            solutionPresence = presence,
            solution = solution?.let {
                SolveSolution(
                    it.asList(snapshot.variables.map { variable -> variable.id }),
                    objective = objective
                )
            },
            proof = SolveProof(status = proof, kind = "scip-cp"),
            statistics = SolveStatistics(
                solveTime = (scip.solvingTime).seconds,
                bestBound = bestBound,
                gap = gap
            ),
            diagnostics = diagnostics,
            provenance = SolverProvenance(
                descriptor = descriptor,
                nativeVersion = runtimeNativeVersion(),
                effectiveParameters = effectiveParameters,
                threadCount = effectiveThreadCount,
                randomSeed = effectiveRandomSeed,
                deterministic = options.deterministic,
                environmentSummary = mapOf(
                    "nativeLibraryLoadMode" to runtimeLibraryLoadMode(),
                    "nativeLibraryIdentity" to runtimeLibraryIdentity(),
                    "bindingVersion" to ScipBindingCapabilityAssessmentProvider.current().bindingVersion,
                    "pluginVersion" to runtimePluginVersion()
                )
            ),
            fingerprints = reportFingerprints()
        )
    }

    private fun conflictDiagnostics(conflict: ConstraintProgrammingConflict): SolveDiagnostics<Int64> {
        val minimality = when (conflict.minimality) {
            ConstraintProgrammingConflictMinimality.Irreducible -> EvidenceMinimality.Irreducible
            ConstraintProgrammingConflictMinimality.Partial -> EvidenceMinimality.Partial
            ConstraintProgrammingConflictMinimality.NotChecked -> EvidenceMinimality.NotChecked
        }
        val completeness = when (conflict.minimality) {
            ConstraintProgrammingConflictMinimality.Partial ->
                fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness.Partial

            ConstraintProgrammingConflictMinimality.Irreducible,
            ConstraintProgrammingConflictMinimality.NotChecked ->
                fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness.Complete
        }
        val members = linkedSetOf<InfeasibilityMember>().apply {
            addAll(conflict.members)
            conflict.constraintIds.forEach { add(InfeasibilityMember.Constraint(it)) }
        }
        val boundRefs = members.mapNotNullTo(linkedSetOf()) {
            (it as? InfeasibilityMember.VariableBound)?.ref
        }
        val domainRefs = members.mapNotNullTo(linkedSetOf()) {
            (it as? InfeasibilityMember.VariableDomain)?.ref
        }
        return SolveDiagnostics(
            infeasibilityEvidence = InfeasibilityEvidence(
                source = InfeasibilityEvidenceSource.ConstraintConflict,
                exactness = when {
                    conflict.validity == EvidenceValidity.Unknown ->
                        fuookami.ospf.kotlin.core.solver.report.EvidenceExactness.Unknown

                    conflict.minimality == ConstraintProgrammingConflictMinimality.Irreducible ->
                        fuookami.ospf.kotlin.core.solver.report.EvidenceExactness.Irreducible

                    else -> fuookami.ospf.kotlin.core.solver.report.EvidenceExactness.Exact
                },
                completeness = if (conflict.validity == EvidenceValidity.Unknown) {
                    fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness.Partial
                } else {
                    completeness
                },
                constraintIds = conflict.constraintIds,
                variableBoundIds = boundRefs.mapTo(linkedSetOf()) { it.variableId },
                variableBoundRefs = boundRefs,
                variableDomainRefs = domainRefs,
                validity = conflict.validity,
                minimality = minimality,
                members = members,
                assumptionIds = conflict.variableIds,
                verificationChecks = conflict.verificationChecks,
                terminationReason = conflict.terminationReason
            )
        )
    }

    private val effectiveThreadCount: Int?
        get() = configuredThreadCount ?: options.threadCount ?: if (options.deterministic) 1 else null

    private val effectiveRandomSeed: Long?
        get() = configuredRandomSeed ?: options.randomSeed

    private val effectiveParameters: Map<String, String>
        get() = buildMap {
            effectiveThreadCount?.let { put("parallel.maxnthreads", it.toString()) }
            effectiveRandomSeed?.let { put("randomization.randomseedshift", it.toString()) }
            put("deterministic", options.deterministic.toString())
            put("limits.time", options.timeLimit?.toString() ?: "unlimited")
            put("limits.nodes", options.nodeLimit?.toString() ?: "unlimited")
            put("limits.solutions", options.solutionLimit?.toString() ?: "unlimited")
            put("limits.gap", options.relativeObjectiveGap?.toString() ?: "unlimited")
            put("limits.absgap", options.absoluteObjectiveGap?.toString() ?: "unlimited")
            options.backendConfiguration?.let { configuration ->
                put("backendConfiguration.type", configuration.type)
                configuration.parameters().forEach { parameter ->
                    put("backendConfiguration.${parameter.name}", parameter.redactedValue())
                }
            }
        }

    private fun reportFingerprints(): SolveFingerprints {
        val model = ConstraintProgrammingSnapshotCodec.encode(snapshot).value?.let {
            SolveFingerprinting.sha256(it)
        }
        val configuration = options.configurationFingerprint?.let {
            AuditFingerprint(
                schemaVersion = SolveReport.CURRENT_SCHEMA_VERSION,
                algorithm = "SHA-256",
                value = it
            )
        } ?: SolveFingerprinting.configuration(effectiveParameters)
        return SolveFingerprints(
            model = model,
            configuration = configuration,
            solver = AuditFingerprint(
                schemaVersion = "scip-runtime-2",
                algorithm = "SHA-256",
                value = runtimeFingerprint()
            )
        )
    }

    private fun applyHint(hints: ConstraintProgrammingSolution?): Ret<Unit> {
        if (hints == null || (hints.values.isEmpty() && hints.intervals.isEmpty())) {
            return ok(Unit)
        }
        val compiledModel = compiled
            ?: return Failed(ErrorCode.ApplicationError, "SCIP CP 编译模型缺失 / Compiled SCIP CP model is missing")
        val definitions = snapshot.variables.associateBy { it.id }
        for ((id, value) in hints.values) {
            val definition = definitions[id]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "SCIP CP hint 引用了未知变量：$id / SCIP CP hint references an unknown variable: $id"
                )
            if (!definition.domain.contains(value)) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "SCIP CP hint 超出变量值域：$id=$value / SCIP CP hint is outside the variable domain: $id=$value"
                )
            }
            if (abs(value.toLong().toDouble()) > MAX_EXACT_DOUBLE_INTEGER) {
                return Failed(
                    ErrorCode.ORSolutionInvalid,
                    "SCIP CP hint 超出 double 精确整数范围 / SCIP CP hint exceeds SCIP's exact integer range"
                )
            }
        }
        for (id in hints.intervals.keys) {
            if (id !in compiledModel.intervals) {
                return Failed(
                    ErrorCode.DataNotFound,
                    "SCIP CP hint 引用了未知 interval：$id / SCIP CP hint references an unknown interval: $id"
                )
            }
        }
        return try {
            val solution = scip.createPartialSol()
            for ((id, value) in hints.values) {
                scip.setSolVal(solution, compiledModel.variables[id]!!, value.toLong().toDouble())
            }
            for ((id, intervalValue) in hints.intervals) {
                val interval = compiledModel.intervals[id]!!
                scip.setSolVal(solution, interval.start, intervalValue.start.toLong().toDouble())
                scip.setSolVal(solution, interval.end, intervalValue.end.toLong().toDouble())
            }
            if (!scip.addSolFree(solution)) {
                Failed(
                    ErrorCode.ORSolutionInvalid,
                    "SCIP 拒绝 CP hint / SCIP rejected the CP hint"
                )
            } else {
                ok(Unit)
            }
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineModelingException,
                "SCIP CP hint 注入失败：${error.message ?: error::class.simpleName} / " +
                    "SCIP CP hint injection failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }

    private fun reportProgress(
        stage: fuookami.ospf.kotlin.core.solver.progress.SolverStage,
        progressInStage: Int,
        overallProgress: Int,
        subStage: SolverSubStage
    ): Ret<Unit> {
        val context: SolverProgressContext = options.progressContext ?: return ok(Unit)
        val result = context.report(
            SolverProgressSnapshot(
                stage = stage,
                subStage = subStage,
                progressInStage = progressInStage,
                overallProgress = overallProgress
            )
        )
        return if (result.failed) {
            propagate(result)
        } else {
            ok(Unit)
        }
    }

    private fun startCancellationWatcher(): Thread? {
        val token = options.cancellationToken ?: return null
        val watcher = Thread {
            try {
                while (!Thread.currentThread().isInterrupted && nativeInitialized) {
                    if (token.isCancellationRequested) {
                        try {
                            scip.interruptSolve()
                        } catch (_: Throwable) {
                            // Native interruption is best effort; SCIP status remains authoritative.
                        }
                        return@Thread
                    }
                    Thread.sleep(CANCELLATION_POLL_MILLIS)
                }
            } catch (_: InterruptedException) {
                // Normal watcher shutdown after solve completion.
            }
        }.apply {
            isDaemon = true
            name = "ospf-scip-cp-cancellation"
            start()
        }
        return watcher
    }

    private fun stopCancellationWatcher(watcher: Thread?) {
        watcher ?: return
        watcher.interrupt()
        try {
            watcher.join(CANCELLATION_JOIN_MILLIS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private suspend fun shrinkConflict(
        initial: ConstraintProgrammingInfeasibleOutput,
        assumptions: List<BooleanLiteral>
    ): Ret<ConstraintProgrammingSolverOutput> {
        val initialConflict = initial.conflict ?: return ok(initial)
        val initialActivationIds = initialConflict.activationIds.toList()
        val maximumChecks = options.conflictShrinkLimit?.toLong()
            ?.takeIf { it > 0L }
            ?: (initialActivationIds.size + assumptions.size).toLong()
        val active = initialActivationIds.toMutableList()
        val activeAssumptions = assumptions.toMutableList()
        var checks = 0L
        var partial = false
        var lastReason: TerminationReason? = null
        for (candidate in initialActivationIds) {
            if (checks >= maximumChecks) {
                partial = true
                break
            }
            val trial = active.filterNot { it == candidate }
            val result = checkInfeasible(trial, assumptions)
            ++checks
            when (result) {
                is Failed,
                is Fatal -> partial = true
                is fuookami.ospf.kotlin.utils.functional.Ok -> when (val value = result.value) {
                    is ConstraintProgrammingInfeasibleOutput -> {
                        if (value.proofStatus == ProofStatus.Verified) {
                            active.remove(candidate)
                            lastReason = value.report?.terminationReason
                        } else {
                            // A non-verified deletion result cannot justify shrinking the conflict.
                            // 未经证明的删除结果不能作为缩减 conflict 的依据。
                            partial = true
                            lastReason = value.report?.terminationReason ?: TerminationReason.BackendFailure
                        }
                    }
                    is ConstraintProgrammingUnknownOutput -> {
                        partial = true
                        lastReason = value.terminationReason
                    }
                    is ConstraintProgrammingFeasibleOutput -> {
                        lastReason = value.report?.terminationReason
                    }
                    else -> {}
                }
            }
            if (partial) {
                break
            }
        }
        if (!partial) {
            for (candidate in assumptions) {
                if (checks >= maximumChecks) {
                    partial = true
                    break
                }
                val trial = activeAssumptions.filterNot { it == candidate }
                val result = checkInfeasible(active, trial)
                ++checks
                when (result) {
                    is Failed,
                    is Fatal -> partial = true
                    is fuookami.ospf.kotlin.utils.functional.Ok -> when (val value = result.value) {
                        is ConstraintProgrammingInfeasibleOutput -> {
                            if (value.proofStatus == ProofStatus.Verified) {
                                activeAssumptions.remove(candidate)
                                lastReason = value.report?.terminationReason
                            } else {
                                // A non-verified deletion result cannot justify shrinking the conflict.
                                // 未经证明的删除结果不能作为缩减 conflict 的依据。
                                partial = true
                                lastReason = value.report?.terminationReason ?: TerminationReason.BackendFailure
                            }
                        }
                        is ConstraintProgrammingUnknownOutput -> {
                            partial = true
                            lastReason = value.terminationReason
                        }
                        is ConstraintProgrammingFeasibleOutput -> {
                            lastReason = value.report?.terminationReason
                        }
                        else -> {}
                    }
                }
                if (partial) {
                    break
                }
            }
        }
        val minimality = if (partial) {
            ConstraintProgrammingConflictMinimality.Partial
        } else {
            ConstraintProgrammingConflictMinimality.Irreducible
        }
        val conflict = initialConflict.copy(
            assumptions = activeAssumptions,
            variableIds = activeAssumptions.mapNotNullTo(linkedSetOf()) { it.variableId },
            activationIds = active.toSet(),
            members = active.mapNotNullTo(linkedSetOf()) { initialConflict.activationMembers[it] },
            constraintIds = active.mapNotNullTo(linkedSetOf()) {
                (initialConflict.activationMembers[it] as? InfeasibilityMember.Constraint)?.id
            },
            minimality = minimality,
            verificationChecks = Flt64(checks.toDouble()).toUInt64(),
            terminationReason = lastReason ?: TerminationReason.Completed
        )
        val finalVerification = checkInfeasible(active, activeAssumptions)
        ++checks
        val (verified, finalReason) = when (finalVerification) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> when (val value = finalVerification.value) {
                is ConstraintProgrammingInfeasibleOutput -> {
                    (value.proofStatus == ProofStatus.Verified) to
                        (value.report?.terminationReason ?: TerminationReason.Completed)
                }

                is ConstraintProgrammingUnknownOutput -> false to value.terminationReason
                is ConstraintProgrammingFeasibleOutput -> false to
                    (value.report?.terminationReason ?: TerminationReason.Completed)

                else -> false to TerminationReason.BackendFailure
            }

            is Failed,
            is Fatal -> false to TerminationReason.BackendFailure
        }
        if (!verified) {
            partial = true
        }
        val finalConflict = conflict.copy(
            minimality = if (partial || !verified) {
                ConstraintProgrammingConflictMinimality.Partial
            } else {
                ConstraintProgrammingConflictMinimality.Irreducible
            },
            validity = if (verified) EvidenceValidity.Verified else EvidenceValidity.Unknown,
            verificationChecks = Flt64(checks.toDouble()).toUInt64(),
            terminationReason = finalReason
        )
        return ok(
            initial.copy(
                conflict = finalConflict,
                report = initial.report?.copy(diagnostics = conflictDiagnostics(finalConflict))
            )
        )
    }

    private suspend fun checkInfeasible(
        activeActivationIds: List<String>,
        assumptions: List<BooleanLiteral>
    ): Ret<ConstraintProgrammingSolverOutput> {
        val solver = ScipConstraintProgrammingSolver(
            sparseDomainLimit = sparseDomainLimit,
            decompositionLimit = decompositionLimit
        )
        val sessionResult = solver.createSession(
            model,
            options.copy(
                collectConflict = false,
                shrinkConflict = false,
                conflictShrinkLimit = null,
                conflictActivationIds = activeActivationIds.toSet(),
                progressContext = null
            )
        )
        if (sessionResult.failed) {
            return propagate(sessionResult)
        }
        val session = sessionResult.value!!
        return try {
            session.solve(assumptions = assumptions)
        } finally {
            session.close()
        }
    }

    private fun closeCurrentNativeModel() {
        // JSCIP 求解结束后不允许释放原始变量和约束；直接释放整个 problem，下一轮重新创建实例。
        // JSCIP does not permit releasing original variables and constraints after solve; free the whole problem and rebuild it next round.
        if (nativeInitialized) {
            try {
                super.close()
            } catch (_: Throwable) {
                // Native cleanup is best effort at the adapter boundary.
            }
            nativeInitialized = false
        }
        compiled = null
    }

    companion object {
        /**
         * 生成当前 SCIP 原生库、binding 与插件身份指纹。 / Generate the current SCIP native-library, binding, and plugin identity fingerprint.
         *
         * 配置参数由独立 configuration fingerprint 表达，避免调度量子改变 solver 身份。 /
         * Configuration parameters are represented by a separate configuration fingerprint so scheduling quantum does not change solver identity.
         *
         * @return 运行时求解器身份指纹 / Runtime solver identity fingerprint
         */
        fun runtimeFingerprint(): String {
            return scipRuntimeFingerprint()
        }

        const val CANCELLATION_POLL_MILLIS = 10L
        const val CANCELLATION_JOIN_MILLIS = 100L
        const val MAX_EXACT_DOUBLE_INTEGER = 9_007_199_254_740_991.0
    }
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "SCIP CP 结果状态无效 / Invalid SCIP CP result state")
    }
}
