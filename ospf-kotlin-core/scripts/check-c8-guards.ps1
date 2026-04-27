# check-c8-guards.ps1
# P4-2 门禁：确保旧类型不再被新代码引入
#
# 模式：
#   - Guard 1 (硬阻断): 禁止任何旧 FunctionSymbol 族类型引用
#   - Guard 2/3 (基线计数): 记录存量，只报告增量变化，不阻断当前基线
#
# 用法: powershell -ExecutionPolicy Bypass -File scripts/check-c8-guards.ps1

param(
    [string]$BaselineFile = ""
)

$ErrorActionPreference = "Stop"
# Resolve root from script location: scripts/ -> ospf-kotlin-core/ -> repo root
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $ScriptDir) { $ScriptDir = Join-Path $PWD.Path "ospf-kotlin-core" | Join-Path -ChildPath "scripts" }
$RootDir = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$CoreMain = Join-Path $RootDir "ospf-kotlin-core" "src" "main"

# Guard 1: 禁止旧 FunctionSymbol 族类型引用（硬阻断）
$forbiddenOldTypes = @(
    "FunctionSymbolRegistrationScope",
    "fuookami\.ospf\.kotlin\.core\.intermediate_symbol\.FunctionSymbol\b",
    "fuookami\.ospf\.kotlin\.core\.intermediate_symbol\.LinearFunctionSymbol\b",
    "fuookami\.ospf\.kotlin\.core\.intermediate_symbol\.QuadraticFunctionSymbol\b",
    "fuookami\.ospf\.kotlin\.core\.intermediate_symbol\.LogicFunctionSymbol\b",
    "fuookami\.ospf\.kotlin\.core\.intermediate_symbol\.LinearLogicFunctionSymbol\b",
    "fuookami\.ospf\.kotlin\.core\.intermediate_symbol\.QuadraticLogicFunctionSymbol\b"
)

# Guard 2/3 白名单（typealias 定义文件）
$tokenF64Whitelist = @(
    "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/Token.kt",
    "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenList.kt",
    "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTable.kt",
    "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenCacheContext.kt"
)

$legacyWhitelist = @(
    "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTable.kt"
)

# Current baseline counts (D0 freeze, 2026-04-27)
$baselineTokenF64 = 34    # TokenF64 occurrences in core/src/main (excluding whitelist files)
$baselineLegacy = 109     # LegacyAbstract*TokenTable occurrences in core/src/main (excluding whitelist files)

function Count-Matches {
    param([string[]]$Patterns, [string[]]$WhitelistFiles, [string]$SearchRoot)

    $total = 0
    $files = @()
    Get-ChildItem -Path $SearchRoot -Recurse -Filter "*.kt" | ForEach-Object {
        $relPath = $_.FullName.Substring($RootDir.Length + 1).Replace("\", "/")
        if ($WhitelistFiles -contains $relPath) { return }

        $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
        if (-not $content) { return }

        $fileCount = 0
        foreach ($pat in $Patterns) {
            $matches = [regex]::Matches($content, $pat)
            $fileCount += $matches.Count
        }
        if ($fileCount -gt 0) {
            $total += $fileCount
            $files += "$relPath ($fileCount)"
        }
    }
    return @{ Total = $total; Files = $files }
}

$hardViolations = @()
$warnings = @()

# Guard 1: Hard block on old FunctionSymbol types
$g1 = Count-Matches -Patterns $forbiddenOldTypes -WhitelistFiles @() -SearchRoot $CoreMain
if ($g1.Total -gt 0) {
    $hardViolations += "[GUARD-1] BLOCKED: Old FunctionSymbol types still referenced ($($g1.Total) occurrences):"
    $hardViolations += $g1.Files
}

# Guard 2: TokenF64 baseline count check
$g2 = Count-Matches -Patterns @("\bTokenF64\b") -WhitelistFiles $tokenF64Whitelist -SearchRoot $CoreMain
$delta2 = $g2.Total - $baselineTokenF64
if ($delta2 -gt 0) {
    $hardViolations += "[GUARD-2] BLOCKED: TokenF64 usage increased by $delta2 (baseline: $baselineTokenF64, current: $($g2.Total))"
} elseif ($delta2 -lt 0) {
    $warnings += "[GUARD-2] GOOD: TokenF64 usage decreased by $(-$delta2) (baseline: $baselineTokenF64, current: $($g2.Total))"
} else {
    $warnings += "[GUARD-2] OK: TokenF64 usage unchanged (baseline: $baselineTokenF64, current: $($g2.Total))"
}

# Guard 3: LegacyAbstract*TokenTable baseline count check
$g3 = Count-Matches -Patterns @("\bLegacyAbstractTokenTable\b", "\bLegacyAbstractMutableTokenTable\b") -WhitelistFiles $legacyWhitelist -SearchRoot $CoreMain
$delta3 = $g3.Total - $baselineLegacy
if ($delta3 -gt 0) {
    $hardViolations += "[GUARD-3] BLOCKED: LegacyAbstract*TokenTable usage increased by $delta3 (baseline: $baselineLegacy, current: $($g3.Total))"
} elseif ($delta3 -lt 0) {
    $warnings += "[GUARD-3] GOOD: LegacyAbstract*TokenTable usage decreased by $(-$delta3) (baseline: $baselineLegacy, current: $($g3.Total))"
} else {
    $warnings += "[GUARD-3] OK: LegacyAbstract*TokenTable usage unchanged (baseline: $baselineLegacy, current: $($g3.Total))"
}

# Report
$warnings | ForEach-Object { Write-Host $_ -ForegroundColor Cyan }
if ($hardViolations.Count -gt 0) {
    Write-Host "C8 GUARD CHECK FAILED" -ForegroundColor Red
    $hardViolations | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    exit 1
} else {
    Write-Host "C8 GUARD CHECK PASSED" -ForegroundColor Green
    exit 0
}