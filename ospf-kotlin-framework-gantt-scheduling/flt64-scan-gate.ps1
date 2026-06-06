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

    if ($line -match "^typealias\s+.*Flt64") {
        return "Compat Wrapper"
    }

    if ($line -match "SchedulingSolverValueAdapter|\.toFlt64\(\)") {
        return "Adapter Conversion"
    }

    if ($line -match "AbstractLinearMetaModel<Flt64>|LinearMetaModel<Flt64>|MetaModel<Flt64>|AbstractMetaModel<Flt64>") {
        return "Solver Boundary"
    }

    if ($line -match "FeasibleSolverOutput<Flt64>|SolverOutput<Flt64>|SolverInput<Flt64>") {
        return "Solver Boundary"
    }

    if ($line -match "LinearIntermediateSymbol[s]?[0-9]?<Flt64>|LinearExpressionSymbol[s]?[0-9]?<Flt64>|LinearFunctionSymbolAdapter<Flt64>|LinearPolynomial<Flt64>") {
        return "Solver Boundary"
    }

    if ($relativePath -match "^gantt-scheduling-application/src/main/.*/application/(model|service)/(task|bunch)/(Iteration|BranchAndPriceAlgorithm)\.kt$") {
        return "Algorithm Internal"
    }

    if ($relativePath -match "^gantt-scheduling-domain-(task|bunch)-compilation-context/src/main/.*/domain/(task|bunch)_compilation/(IterativeAggregation|IterativeContext|Aggregation)\.kt$") {
        return "Algorithm Internal"
    }

    if ($line -match "Flt64\.(zero|one|two|maximum|minimum|infinity)|Flt64\.Companion\.(zero|one|two|maximum|minimum|infinity)|Flt64\([0-9.+-]") {
        return "Algorithm Internal"
    }

    if ($relativePath -match "^gantt-scheduling-(infrastructure|domain-capacity-scheduling-context|domain-resource-context|domain-produce-context|domain-task-context|domain-task-compilation-context|domain-bunch-compilation-context|domain-bunch-generation-context)/") {
        return "Documented Pending"
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
    "Legacy API" = "保留的旧 Flt64 入口 / retained legacy Flt64 API"
    "Algorithm Internal" = "算法内部无量纲值、目标值与阈值 / algorithm-local dimensionless values, objectives, and thresholds"
    "Documented Pending" = "已记录待迁移领域边界 / documented pending domain boundary"
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
