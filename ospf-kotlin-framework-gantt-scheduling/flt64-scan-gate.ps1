# Flt64 扫描门禁脚本 / Flt64 scan gate script
# 用法 / Usage:
#   pwsh.exe -NoLogo -NoProfile -File .\flt64-scan-gate.ps1 [-ProjectRoot <repo-root>]

[CmdletBinding()]
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ganttSchedulingDir = Join-Path $ProjectRoot "ospf-kotlin-framework-gantt-scheduling"
if (-not (Test-Path -LiteralPath $ganttSchedulingDir -PathType Container)) {
    Write-Error "gantt-scheduling directory not found at $ganttSchedulingDir"
    exit 1
}

function Get-KotlinFiles {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("main", "test")]
        [string]$Scope
    )

    Get-ChildItem -LiteralPath $ganttSchedulingDir -Recurse -Filter "*.kt" |
        Where-Object { $_.FullName -like "*\src\$Scope\*" }
}

function Get-Flt64Matches {
    param(
        [AllowEmptyCollection()]
        [System.IO.FileInfo[]]$Files
    )

    if (-not $Files -or $Files.Count -eq 0) {
        return @()
    }
    @(Select-String -LiteralPath $Files.FullName -Pattern "Flt64")
}

function Count-Pattern {
    param(
        [AllowEmptyCollection()]
        [System.IO.FileInfo[]]$Files,
        [Parameter(Mandatory = $true)]
        [string]$Pattern
    )

    if (-not $Files -or $Files.Count -eq 0) {
        return 0
    }
    return @(Select-String -LiteralPath $Files.FullName -Pattern $Pattern).Count
}

function Format-RelativePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return $Path.Substring($ganttSchedulingDir.Length + 1).Replace("\", "/")
}

function Test-NonCodeFlt64Match {
    param(
        [Parameter(Mandatory = $true)]
        $Match
    )

    $line = $Match.Line.Trim()
    if ($line -match "^import\s+") {
        return $true
    }
    if ($line -match "^//") {
        return $true
    }
    if ($line -match "^/\*") {
        return $true
    }
    if ($line -match "^\*") {
        return $true
    }
    if ($line -match "^\*/") {
        return $true
    }
    return $false
}

function Get-Flt64Category {
    param(
        [Parameter(Mandatory = $true)]
        $Match,
        [Parameter(Mandatory = $true)]
        [ValidateSet("main", "test")]
        [string]$Scope
    )

    if ($Scope -eq "test") {
        return "Test"
    }

    $line = $Match.Line.Trim()
    $relativePath = Format-RelativePath -Path $Match.Path

    if (Test-NonCodeFlt64Match -Match $Match) {
        return "Import/Comment"
    }

    if ($line -match "^(typealias\s+.*Flt64|@Deprecated.*typealias\s+.*Flt64)") {
        return "Compat Wrapper"
    }

    if ($line -match "Use .*Flt64.* directly|ReplaceWith\(.*Flt64") {
        return "Compat Wrapper"
    }

    if ($relativePath -match "^gantt-scheduling-infrastructure/src/main/.*/infrastructure/TimeWindow\.kt$") {
        if ($line -match "toFlt64Boundary") {
            return "Time/Calendar Adapter"
        }
        if ($line -match "TimeWindow\.(seconds|minutes|hours).*Flt64|TimeWindow<Flt64>|Flt64\(it\)") {
            return "Compat Wrapper"
        }
    }

    if ($relativePath -match "^gantt-scheduling-infrastructure/src/main/.*/infrastructure/WorkingCalendar\.kt$") {
        if ($line -match "calendarValueOf|productivityRateOf") {
            return "Time/Calendar Adapter"
        }
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-compilation-context/src/main/.*/domain/task_compilation/model/SolverTimeWindowBoundary\.kt$") {
        return "Compilation Solver Time Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-compilation-context/src/main/.*/domain/task_compilation/service/limits/ThresholdSlack\.kt$") {
        return "Solver Boundary"
    }

    if ($line -match "SchedulingSolverValueAdapter|\.toFlt64\(\)") {
        return "Adapter Conversion"
    }

    if ($relativePath -match "^gantt-scheduling-domain-capacity-scheduling-context/src/main/.*/domain/capacity_scheduling/model/CapacitySolverValue\.kt$") {
        return "Adapter Conversion"
    }

    if ($relativePath -match "^gantt-scheduling-domain-produce-context/src/main/.*/domain/produce/model/ProduceSolverValue\.kt$") {
        return "Adapter Conversion"
    }

    if ($relativePath -match "^gantt-scheduling-domain-resource-context/src/main/.*/domain/resource/model/ResourceSolverValue\.kt$") {
        return "Adapter Conversion"
    }

    if ($relativePath -match "^gantt-scheduling-domain-bunch-compilation-context/src/main/.*/domain/bunch_compilation/model/SlotBasedCapacityResult\.kt$") {
        if ($line -match "toGeneric|Quantity<Flt64>") {
            return "Adapter Conversion"
        }
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-context/src/main/.*/domain/task/model/SchedulingSolverValueAdapter\.kt$") {
        return "Adapter Conversion"
    }

    if ($line -match "AbstractLinearMetaModel<Flt64>|LinearMetaModel<Flt64>|MetaModel<Flt64>|AbstractMetaModel<Flt64>") {
        return "Solver Boundary"
    }

    if ($line -match "FeasibleSolverOutput<Flt64>|SolverOutput<Flt64>|SolverInput<Flt64>") {
        return "Solver Boundary"
    }

    if ($line -match "LinearIntermediateSymbol[s]?[0-9]?<Flt64>|LinearExpressionSymbol[s]?[0-9]?<Flt64>|LinearFunctionSymbolAdapter<Flt64>|LinearPolynomial<Flt64>|MutableLinearPolynomial|LinearMonomial\(Flt64|LinearPolynomial\(.*Flt64|LinearExpressionSymbol\(Flt64") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-bunch-compilation-context/src/main/.*/domain/bunch_compilation/service/SlotBasedCapacityPreSolver\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-bunch-compilation-context/src/main/.*/domain/bunch_compilation/(BunchCompilationContext|model/(TaskTime|Compilation)|service/(SlotBasedBunchCompilationContext|TaskSolutionAnalyzer|BunchSolutionAnalyzer|limits/BunchCostMinimization))\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-capacity-scheduling-context/src/main/.*/domain/capacity_scheduling/(model/(CapacityCompilation|CapacityOrderCompilation|IterativeCapacityCompilation|ProductionAction)|service/limits/(ExecutorCapacityConstraint|OrderConstraint))\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(resource|produce)-context/src/main/.*/domain/(resource|produce)/service/limits/.*QuantityConstraint\.kt$") {
        return "Shadow Price Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(resource|produce)-context/src/main/.*/domain/(resource|produce)/service/limits/.*(Constraint|Minimization|Maximization)\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(resource|produce)-context/src/main/.*/domain/(resource|produce)/model/.*(Slack|Usage|Produce|Consumption)\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-resource-context/src/main/.*/domain/resource/model/(Resource|ExecutionResource|ConnectionResource|StorageResource)\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-produce-context/src/main/.*/domain/produce/model/ProductionTask\.kt$") {
        if ($line -match "flt64|Flt64|nonZeroFlt64|AbstractTaskBunch<.*Flt64") {
            return "Compat Wrapper"
        }
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-compilation-context/src/main/.*/domain/task_compilation/service/limits/.*Constraint\.kt$") {
        if ($line -match "ShadowPrice|shadowPrice") {
            return "Shadow Price Boundary"
        }
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-compilation-context/src/main/.*/domain/task_compilation/service/limits/.*(Minimization|Maximization)\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-compilation-context/src/main/.*/domain/task_compilation/service/(SolutionAnalyzer|limits/ThresholdSlack)\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-compilation-context/src/main/.*/domain/task_compilation/model/(TaskTime|Switch|Makespan|Compilation)\.kt$") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-context/src/main/.*/domain/task/model/Cost\.kt$") {
        if ($line -match "solverCost|\.toFlt64\(\)") {
            return "Adapter Conversion"
        }
    }

    if ($relativePath -match "^gantt-scheduling-application/src/main/.*/application/model/(task|bunch)/Iteration\.kt$") {
        if ($line -match "snapshot|quantity\(|value:\s+Flt64") {
            return "Application Result Boundary"
        }
        return "Application Algorithm Internal"
    }

    if ($relativePath -match "^gantt-scheduling-application/src/main/.*/application/service/(task|bunch)/BranchAndPriceAlgorithm\.kt$") {
        return "Application Algorithm Internal"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(task|bunch)-compilation-context/src/main/.*/domain/(task|bunch)_compilation/(IterativeAggregation|IterativeContext|Aggregation)\.kt$") {
        if ($line -match "TimeWindow<Flt64>") {
            return "Compilation Solver Time Boundary"
        }
        if ($line -match "solution:\s+List<Flt64>|Ret<Map<.*Flt64|HashMap<.*Flt64|predicate:\s*\(Flt64\)") {
            return "Compilation Solver Result Boundary"
        }
        if ($line -match "maximumReducedCost|reducedCost|bar|bestValue|Ret<Flt64>|Flt64\.(zero|one)|Flt64\([0-9.+-]") {
            return "Compilation Algorithm Scalar"
        }
        return "Compilation Algorithm Internal"
    }

    if ($relativePath -match "^gantt-scheduling-domain-task-context/src/main/.*/domain/task/model/ShadowPriceMap\.kt$") {
        return "Shadow Price Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-(infrastructure|domain-task-compilation-context)/src/main/") {
        return "Time/Calendar Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(capacity-scheduling|resource|produce|bunch-compilation)-context/src/main/") {
        return "Domain DTO Pending"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(task|bunch)-generation-context/src/main/") {
        return "Generation Boundary"
    }

    if ($line -match "Flt64\.(zero|one|two|maximum|minimum|infinity)|Flt64\.Companion\.(zero|one|two|maximum|minimum|infinity)|Flt64\([0-9.+-]") {
        return "Numeric Constant/Internal"
    }

    return "Unclassified"
}

function New-ClassifiedMatch {
    param(
        [Parameter(Mandatory = $true)]
        $Match,
        [Parameter(Mandatory = $true)]
        [ValidateSet("main", "test")]
        [string]$Scope
    )

    return [PSCustomObject]@{
        Match = $Match
        Scope = $Scope
        Category = Get-Flt64Category -Match $Match -Scope $Scope
    }
}

$mainFiles = @(Get-KotlinFiles -Scope "main")
$testFiles = @(Get-KotlinFiles -Scope "test")
$mainMatches = @(Get-Flt64Matches -Files $mainFiles)
$testMatches = @(Get-Flt64Matches -Files $testFiles)
$classifiedMainMatches = @($mainMatches | ForEach-Object { New-ClassifiedMatch -Match $_ -Scope "main" })
$classifiedTestMatches = @($testMatches | ForEach-Object { New-ClassifiedMatch -Match $_ -Scope "test" })
$classifiedMatches = @($classifiedMainMatches + $classifiedTestMatches)
$mainFilesWithFlt64 = @($mainMatches | Select-Object -ExpandProperty Path -Unique)
$testFilesWithFlt64 = @($testMatches | Select-Object -ExpandProperty Path -Unique)

Write-Output "============================================="
Write-Output " Flt64 扫描门禁报告 / Flt64 Scan Gate Report"
Write-Output " 扫描目录 / Scan directory: $ganttSchedulingDir"
Write-Output " 日期 / Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Output "============================================="
Write-Output ""

Write-Output "## 1. 总量统计 / Totals"
Write-Output ""
Write-Output "| 指标 / Metric | 数值 / Value |"
Write-Output "|------|------|"
Write-Output "| main Kotlin 文件总数 / main Kotlin files | $($mainFiles.Count) |"
Write-Output "| 含 Flt64 的 main 文件数 / main files with Flt64 | $($mainFilesWithFlt64.Count) |"
Write-Output "| main Flt64 使用行 / main Flt64 lines | $($mainMatches.Count) |"
Write-Output "| test Flt64 使用行 / test Flt64 lines | $($testMatches.Count) |"
Write-Output "| 总计 / Total | $($mainMatches.Count + $testMatches.Count) |"
Write-Output ""

Write-Output "## 2. 按子模块统计 / By Submodule"
Write-Output ""
Write-Output "| 子模块 / Submodule | main Flt64 | test Flt64 | 合计 / Total |"
Write-Output "|--------|-----------|-----------|------|"

$totalMain = 0
$totalTest = 0
Get-ChildItem -LiteralPath $ganttSchedulingDir -Directory -Filter "gantt-scheduling-*" |
    Sort-Object Name |
    ForEach-Object {
        $moduleFiles = @(Get-ChildItem -LiteralPath $_.FullName -Recurse -Filter "*.kt")
        $moduleMainFiles = @($moduleFiles | Where-Object { $_.FullName -like "*\src\main\*" })
        $moduleTestFiles = @($moduleFiles | Where-Object { $_.FullName -like "*\src\test\*" })
        $moduleMainCount = @(Get-Flt64Matches -Files $moduleMainFiles).Count
        $moduleTestCount = @(Get-Flt64Matches -Files $moduleTestFiles).Count
        $moduleTotal = $moduleMainCount + $moduleTestCount
        $totalMain += $moduleMainCount
        $totalTest += $moduleTestCount
        Write-Output "| $($_.Name) | $moduleMainCount | $moduleTestCount | $moduleTotal |"
    }

Write-Output "| **合计 / Total** | **$totalMain** | **$totalTest** | **$($totalMain + $totalTest)** |"
Write-Output ""

Write-Output "## 3. 按类别统计 / By Category"
Write-Output ""
Write-Output "| 类别 / Category | 使用行 / Lines | 说明 / Description |"
Write-Output "|------|--------|------|"
$categoryDescriptions = [ordered]@{
    "Compat Wrapper" = "向后兼容 typealias 声明 / backward-compatible typealiases"
    "Solver Boundary" = "solver 模型、符号、求解结果边界 / solver model, symbol, and output boundary"
    "Adapter Conversion" = "V 与 Flt64 的适配转换 / V and Flt64 adapter conversion"
    "Application Algorithm Internal" = "application 分支定价算法内部目标值、阈值与心跳 / application branch-and-price internal objectives, thresholds, and heartbeat"
    "Application Result Boundary" = "application 结果快照与泛型物理量适配边界 / application result snapshot and generic quantity adapter boundary"
    "Compilation Solver Time Boundary" = "task/bunch compilation solver 时间窗口边界 / task/bunch compilation solver time-window boundary"
    "Compilation Solver Result Boundary" = "task/bunch compilation 解值、比例与提取谓词边界 / task/bunch compilation solution value, ratio, and extraction predicate boundary"
    "Compilation Algorithm Scalar" = "task/bunch compilation 约简成本、阈值与固定策略标量 / task/bunch compilation reduced-cost, threshold, and fixing-policy scalar"
    "Compilation Algorithm Internal" = "task/bunch compilation 其它迭代与聚合算法内部值 / other task/bunch compilation iterative and aggregation internals"
    "Time/Calendar Adapter" = "泛型时间窗口到 Flt64 日历边界的集中适配 / centralized generic time-window to Flt64 calendar boundary adapter"
    "Time/Calendar Boundary" = "时间窗口、日历与 task time solver 边界 / time window, calendar, and task-time solver boundary"
    "Domain DTO Pending" = "capacity/resource/produce/bunch 领域 DTO 待迁移边界 / capacity/resource/produce/bunch domain DTO pending boundary"
    "Generation Boundary" = "生成器标签与路径评估边界 / generator label and path-evaluation boundary"
    "Shadow Price Boundary" = "shadow price 基础 API 边界 / shadow-price base API boundary"
    "Numeric Constant/Internal" = "无量纲常量、默认值与内部阈值 / dimensionless constants, defaults, and internal thresholds"
    "Test" = "测试文件中的 Flt64 / Flt64 in tests"
    "Import/Comment" = "import 或注释中的 Flt64 / Flt64 in imports or comments"
    "Unclassified" = "未归类使用点 / unclassified usages"
}

foreach ($category in $categoryDescriptions.Keys) {
    $count = @($classifiedMatches | Where-Object { $_.Category -eq $category }).Count
    Write-Output "| $category | $count | $($categoryDescriptions[$category]) |"
}
Write-Output ""

Write-Output "## 4. 含 Flt64 的 main 文件清单 / main Files with Flt64"
Write-Output ""
Write-Output "| 文件 / File | Flt64 行数 / Lines |"
Write-Output "|------|-----------|"
$mainMatches |
    Group-Object Path |
    Sort-Object Count -Descending |
    ForEach-Object {
        Write-Output "| $(Format-RelativePath -Path $_.Name) | $($_.Count) |"
    }
Write-Output ""

Write-Output "## 5. 未归类检查 / Unclassified Domain API Check"
Write-Output ""
Write-Output "以下行在 main 代码中使用 Flt64，且未匹配常见边界模式 / Lines below use Flt64 in main code and do not match common boundary patterns:"
Write-Output ""

$unclassified = @($classifiedMainMatches | Where-Object { $_.Category -eq "Unclassified" })
Write-Output "未归类项数量 / Unclassified count: $($unclassified.Count)"
Write-Output ""

foreach ($classifiedMatch in ($unclassified | Select-Object -First 50)) {
    $match = $classifiedMatch.Match
    Write-Output "$(Format-RelativePath -Path $match.Path):$($match.LineNumber):$($match.Line.Trim())"
}

Write-Output ""
Write-Output "============================================="
Write-Output " 扫描完成 / Scan complete."
Write-Output " 基线值 / Baseline: main=$($mainMatches.Count), test=$($testMatches.Count), total=$($mainMatches.Count + $testMatches.Count)"
Write-Output " 未归类项 / Unclassified: $($unclassified.Count)"
Write-Output "============================================="

if ($unclassified.Count -gt 0) {
    exit 1
}
