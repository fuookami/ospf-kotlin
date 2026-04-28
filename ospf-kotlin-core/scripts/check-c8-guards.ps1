#!/usr/bin/env pwsh
# P4-2 Guard Script — Static checks for core refactor integrity
# Runs against staged changes (git diff --cached) or full src/main if no staging.
#
# Guard categories:
#   C8-1: Zero-tolerance (must be 0)
#   C8-2/C8-3: Baseline-count (must not exceed frozen baseline)
#   P4-2-1/2/3: Zero-tolerance (must be 0)
#   P4-3-1: Zero-tolerance (must be 0)
#   P4-4-1/2: Baseline-count (must not exceed frozen baseline)

param(
    [switch]$Full,
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"
$exitCode = 0

function Write-Result {
    param([string]$Label, [bool]$Pass, [string]$Detail = "")
    $icon = if ($Pass) { "PASS" } else { "FAIL" }
    $color = if ($Pass) { "Green" } else { "Red" }
    Write-Host "[$icon] $Label" -ForegroundColor $color
    if ($Detail -and ($Verbose -or -not $Pass)) {
        Write-Host "      $Detail" -ForegroundColor DarkGray
    }
    if (-not $Pass) { $script:exitCode = 1 }
}

function Write-Baseline {
    param([string]$Label, [int]$Current, [int]$Baseline, [string]$Detail = "")
    $increased = $Current -gt $Baseline
    $icon = if ($increased) { "FAIL" } else { "PASS" }
    $color = if ($increased) { "Red" } else { "Green" }
    $delta = $Current - $Baseline
    $deltaStr = if ($delta -gt 0) { "+$delta" } elseif ($delta -lt 0) { "$delta" } else { "0" }
    Write-Host "[$icon] $Label (baseline=$Baseline, current=$Current, delta=$deltaStr)" -ForegroundColor $color
    if ($Detail -and ($Verbose -or $increased)) {
        Write-Host "      $Detail" -ForegroundColor DarkGray
    }
    if ($increased) { $script:exitCode = 1 }
}

$coreMain = "ospf-kotlin-core/src/main"

# --- C8 Guards (original) ---

# Guard 1: No new old-polynomial imports in core/src/main
$oldPoly = @(
    "fuookami.ospf.kotlin.core.intermediate_model.monomial",
    "fuookami.ospf.kotlin.core.intermediate_model.Expression",
    "fuookami.ospf.kotlin.core.intermediate_model.ToPolynomial"
)
$found = @()
foreach ($pat in $oldPoly) {
    $matches = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
        Select-String -Pattern $pat -SimpleMatch |
        Where-Object { $_.Line -match "^\s*import" }
    $found += $matches
}
Write-Result "C8-1: No old polynomial imports in core/src/main" ($found.Count -eq 0) "Found $($found.Count) violations"

# Guard 2: No new .cells direct access in core/src/main (baseline-count)
# Historical baseline: existing violations are grandfathered; only new ones fail.
$cellsMatches = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "\.cells\b" |
    Where-Object { $_.Path -notmatch "intermediate[/\\]" -and $_.Path -notmatch "model[/\\]intermediate[/\\]" -and $_.Line -notmatch "^\s*//" }
$cellsBaseline = 2  # Frozen 2026-04-28 at P4-2 D7 close
Write-Baseline "C8-2: No new .cells direct access outside intermediate model" $cellsMatches.Count $cellsBaseline "Found $($cellsMatches.Count) total (baseline=$cellsBaseline)"

# Guard 3: No new Double-typed constraint/variable in core/src/main (baseline-count)
# Historical baseline: existing violations are grandfathered; only new ones fail.
$doubleMatches = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "\bDouble\b" |
    Where-Object { $_.Line -notmatch "^\s*//" -and $_.Line -notmatch "import " -and $_.Line -notmatch "kotlin\." -and $_.Line -notmatch "java\." }
$doubleBaseline = 31  # Frozen 2026-04-28 at P4-2 D7 close
Write-Baseline "C8-3: No bare Double type in core/src/main" $doubleMatches.Count $doubleBaseline "Found $($doubleMatches.Count) total (baseline=$doubleBaseline)"

# --- P4-2 Guards (new) ---

# Guard 4: No LegacyAbstractTokenTable* in core/src/main (zero-tolerance after P4-3B deletion)
$legacyTT = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "LegacyAbstract(TokenTable|MutableTokenTable)" |
    Where-Object { $_.Line -notmatch "^\s*//" }
Write-Result "P4-2-1: No LegacyAbstractTokenTable* in core/src/main" ($legacyTT.Count -eq 0) "Found $($legacyTT.Count) violations"

# Guard 5: No new old FunctionSymbol naming (FunctionSymbol/LinearFunctionSymbol/QuadraticFunctionSymbol)
# in core/src/main (excluding deprecated typealias and MathFunctionSymbolAdapter)
$oldFuncSym = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "\b(LinearFunctionSymbol|QuadraticFunctionSymbol)\b" |
    Where-Object { $_.Line -notmatch "^\s*//" -and $_.Line -notmatch "@Deprecated" -and $_.Line -notmatch "typealias" -and $_.Line -notmatch "Adapter" }
Write-Result "P4-2-2: No old FunctionSymbol naming in core/src/main" ($oldFuncSym.Count -eq 0) "Found $($oldFuncSym.Count) violations"

# Guard 6: No FunctionSymbolRegistrationScope in core/src/main
$fsrScope = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "FunctionSymbolRegistrationScope" |
    Where-Object { $_.Line -notmatch "^\s*//" }
Write-Result "P4-2-3: No FunctionSymbolRegistrationScope in core/src/main" ($fsrScope.Count -eq 0) "Found $($fsrScope.Count) violations"

# --- P4-3 Guards (new) ---

# Guard 7: No TokenF64 in core/src/main (zero-tolerance after P4-3B deletion)
$tokenF64 = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "\bTokenF64\b" |
    Where-Object { $_.Line -notmatch "^\s*//" }
Write-Result "P4-3-1: No TokenF64 in core/src/main" ($tokenF64.Count -eq 0) "Found $($tokenF64.Count) violations"

# --- P4-4 Guards (new) ---

# Guard 8: No AbstractTokenTable<*> in intermediate_symbol (zero-tolerance after P4-5)
# After P4-5 D3, star-projected AbstractTokenTable is fully eliminated from
# intermediate_symbol. The only remaining AbstractTokenTable<*> are in
# TokenCacheContext.kt (cache infrastructure, outside intermediate_symbol).
$starProjDir = "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol"
$starProjMatches = Get-ChildItem -Path $starProjDir -Recurse -Filter "*.kt" |
    Select-String -Pattern "AbstractTokenTable<\*>" |
    Where-Object { $_.Line -notmatch "^\s*//" }
Write-Result "P4-4-1: No AbstractTokenTable<*> in intermediate_symbol (zero-tolerance)" ($starProjMatches.Count -eq 0) "Found $($starProjMatches.Count) violations"

# Guard 9: No `as AbstractTokenTable<Flt64>` casts in intermediate_symbol (zero-tolerance after P4-5)
# After P4-5 D3, unchecked casts to AbstractTokenTable<Flt64> are eliminated from
# intermediate_symbol. Product.kt uses `tokenTable as AbstractTokenTable<Flt64>` but
# these are now in the V-typed override path and cast from AbstractTokenTable<V>,
# which is the expected solver-boundary pattern.
$castMatches = Get-ChildItem -Path $starProjDir -Recurse -Filter "*.kt" |
    Select-String -Pattern "as AbstractTokenTable<Flt64>" |
    Where-Object { $_.Line -notmatch "^\s*//" }
# Exclude expected solver-boundary casts in Product.kt (V-typed override path)
$castViolations = $castMatches | Where-Object { $_.Filename -ne "Product.kt" }
Write-Result "P4-4-2: No as AbstractTokenTable<Flt64> casts in intermediate_symbol (zero-tolerance, Product.kt exempt)" ($castViolations.Count -eq 0) "Found $($castViolations.Count) violations (total $($castMatches.Count) including Product.kt)"

# --- P4-5 Guards (new) ---

# Guard 10: No new Flt64-returning evaluate/prepare/evaluateFromTokens in IntermediateSymbol
# interface (excluding @Deprecated extension functions and solver-boundary convenience).
# After P4-5 D1+D2, the V-typed primary path is the interface member, and Flt64
# convenience methods are @Deprecated extension functions only.
# Solver-boundary convenience methods (AbstractTokenListF64 parameter) are exempt.
$interfaceFile = "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/IntermediateSymbol.kt"
$p4_5_violations = 0
$lines = Get-Content $interfaceFile
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    # Match Flt64?-returning prepare/evaluate/evaluateFromTokens/prepareAndCache
    if ($line -match "^\s*fun\s+(prepare|evaluate|evaluateFromTokens|prepareAndCache)" -and
        $line -match ":\s*Flt64\?" -and
        $line -notmatch "@Deprecated" -and
        ($i -eq 0 -or $lines[$i-1] -notmatch "@Deprecated") -and
        # Exempt solver-boundary convenience (AbstractTokenListF64 parameter)
        $line -notmatch "AbstractTokenListF64") {
        $p4_5_violations++
    }
}
Write-Result "P4-5-1: No non-deprecated Flt64-returning prepare/evaluate in IntermediateSymbol interface (solver-boundary exempt)" ($p4_5_violations -eq 0) "Found $($p4_5_violations) violations"

# --- Summary ---
Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "All guards passed." -ForegroundColor Green
} else {
    Write-Host "Some guards failed. See above for details." -ForegroundColor Red
}
exit $exitCode
