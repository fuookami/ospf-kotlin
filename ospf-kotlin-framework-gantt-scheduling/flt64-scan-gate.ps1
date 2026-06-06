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

$mainFiles = @(Get-KotlinFiles -Scope "main")
$testFiles = @(Get-KotlinFiles -Scope "test")
$mainMatches = @(Get-Flt64Matches -Files $mainFiles)
$testMatches = @(Get-Flt64Matches -Files $testFiles)
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
$wrapperCount = Count-Pattern -Files $mainFiles -Pattern "^typealias.*Flt64"
$solverBoundary = Count-Pattern -Files $mainFiles -Pattern "SchedulingSolverValueAdapter\.Flt64|SchedulingSolverValueAdapter\.create|LinearMetaModel<Flt64>\("
$solverModel = Count-Pattern -Files $mainFiles -Pattern "AbstractLinearMetaModel<Flt64>|LinearMetaModel<Flt64>|MetaModel<Flt64>|AbstractMetaModel<Flt64>"
$solverSymbols = Count-Pattern -Files $mainFiles -Pattern "LinearIntermediateSymbol[s]?[0-9]?<Flt64>|LinearExpressionSymbol[s]?[0-9]?<Flt64>|LinearFunctionSymbolAdapter<Flt64>|LinearPolynomial<Flt64>"
$flt64Constants = Count-Pattern -Files $mainFiles -Pattern "Flt64\.zero|Flt64\.one|Flt64\([0-9]"
$toFlt64 = Count-Pattern -Files $mainFiles -Pattern "\.toFlt64\(\)"

Write-Output "| 类别 / Category | 使用行 / Lines | 说明 / Description |"
Write-Output "|------|--------|------|"
Write-Output "| Compat Wrapper (typealias) | $wrapperCount | 向后兼容 typealias 声明 / backward-compatible typealiases |"
Write-Output "| Solver Boundary (adapter/model) | $solverBoundary | solver adapter / model 构造 / construction |"
Write-Output "| Solver Model Types | $solverModel | MetaModel/LinearMetaModel<Flt64> |"
Write-Output "| Solver Symbols | $solverSymbols | LinearIntermediate/Expression symbols |"
Write-Output "| Flt64 Constants | $flt64Constants | Flt64.zero/one/Flt64(number) |"
Write-Output "| .toFlt64() Conversion | $toFlt64 | V 到 Flt64 边界转换 / V to Flt64 boundary conversion |"
Write-Output "| Test | $($testMatches.Count) | 测试文件中的 Flt64 / Flt64 in tests |"
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

$unclassified = $mainMatches |
    Where-Object { $_.Line -notmatch "import " } |
    Where-Object { $_.Line -notmatch "//.*Flt64" } |
    Where-Object { $_.Line -notmatch "\*.*Flt64" } |
    Where-Object { $_.Line -notmatch "typealias.*=.*Flt64" } |
    Where-Object { $_.Line -notmatch "MetaModel<Flt64>|AbstractLinearMetaModel<Flt64>|LinearMetaModel<Flt64>|AbstractMetaModel<Flt64>" } |
    Where-Object { $_.Line -notmatch "LinearIntermediateSymbol|LinearExpressionSymbol|LinearFunctionSymbolAdapter|LinearPolynomial<Flt64>" } |
    Where-Object { $_.Line -notmatch "Flt64\.zero|Flt64\.one|Flt64\([0-9]" } |
    Where-Object { $_.Line -notmatch "\.toFlt64\(\)" } |
    Where-Object { $_.Line -notmatch "SchedulingSolverValueAdapter" } |
    Select-Object -First 50

foreach ($match in $unclassified) {
    Write-Output "$(Format-RelativePath -Path $match.Path):$($match.LineNumber):$($match.Line.Trim())"
}

Write-Output ""
Write-Output "============================================="
Write-Output " 扫描完成 / Scan complete."
Write-Output " 基线值 / Baseline: main=$($mainMatches.Count), test=$($testMatches.Count), total=$($mainMatches.Count + $testMatches.Count)"
Write-Output "============================================="
