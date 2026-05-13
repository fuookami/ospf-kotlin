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
#   P5-1-1: Zero-tolerance (must be 0)
#   P5-3-1/P5-4-1: Baseline-count (must not exceed frozen baseline)
#   P6-0-1~6: Baseline-count (must not exceed frozen baseline)

param(
    [ValidateSet("P6", "P7")]
    [string]$GuardMode = "P6",
    [string]$BaseRef = "",
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

function Get-RelativePath {
    param([string]$Root, [string]$FilePath)
    $rootPath = (Resolve-Path $Root).Path
    $fullPath = (Resolve-Path $FilePath).Path
    if ($fullPath.StartsWith($rootPath)) {
        return $fullPath.Substring($rootPath.Length + 1).Replace("\", "/")
    }
    return $fullPath.Replace("\", "/")
}

function Get-KtMatchCountMap {
    param([string]$Root, [string]$Pattern)
    $result = @{}
    if (-not (Test-Path $Root)) {
        return $result
    }
    Get-ChildItem -Path $Root -Recurse -Filter "*.kt" | ForEach-Object {
        $count = (Select-String -Path $_.FullName -Pattern $Pattern | Where-Object { $_.Line -notmatch "^\s*//" }).Count
        if ($count -gt 0) {
            $relPath = Get-RelativePath -Root $Root -FilePath $_.FullName
            $result[$relPath] = $count
        }
    }
    return $result
}

function Get-MapSum {
    param([hashtable]$Map)
    if ($null -eq $Map -or $Map.Count -eq 0) {
        return 0
    }
    $sum = 0
    foreach ($value in $Map.Values) {
        $sum += [int]$value
    }
    return $sum
}

function Format-Delta {
    param([int]$Delta)
    if ($Delta -gt 0) {
        return "+$Delta"
    } elseif ($Delta -lt 0) {
        return "$Delta"
    } else {
        return "0"
    }
}

function Write-P7Whitelist {
    param(
        [string]$Label,
        [string]$Root,
        [string]$Pattern,
        [hashtable]$WhitelistMap
    )
    if (-not (Test-Path $Root)) {
        Write-Host "[SKIP] ${Label}: root not found ($Root)" -ForegroundColor Yellow
        return
    }

    $actualMap = Get-KtMatchCountMap -Root $Root -Pattern $Pattern
    $baseline = Get-MapSum -Map $WhitelistMap
    $current = Get-MapSum -Map $actualMap
    $deltaStr = Format-Delta -Delta ($current - $baseline)

    $nonWhitelistHits = @()
    $whitelistOverflows = @()
    foreach ($entry in $actualMap.GetEnumerator()) {
        if (-not $WhitelistMap.ContainsKey($entry.Key)) {
            $nonWhitelistHits += "$($entry.Key)=$($entry.Value)"
            continue
        }
        $allowed = [int]$WhitelistMap[$entry.Key]
        if ([int]$entry.Value -gt $allowed) {
            $whitelistOverflows += "$($entry.Key)=$($entry.Value)>$allowed"
        }
    }

    $pass = ($nonWhitelistHits.Count -eq 0 -and $whitelistOverflows.Count -eq 0)
    $icon = if ($pass) { "PASS" } else { "FAIL" }
    $color = if ($pass) { "Green" } else { "Red" }
    Write-Host "[$icon] $Label (baseline=$baseline, current=$current, delta=$deltaStr)" -ForegroundColor $color
    if (($Verbose -or -not $pass) -and $nonWhitelistHits.Count -gt 0) {
        $preview = ($nonWhitelistHits | Select-Object -First 8) -join "; "
        Write-Host "      Non-whitelist hits: $preview" -ForegroundColor DarkGray
    }
    if (($Verbose -or -not $pass) -and $whitelistOverflows.Count -gt 0) {
        $preview = ($whitelistOverflows | Select-Object -First 8) -join "; "
        Write-Host "      Whitelist overflow: $preview" -ForegroundColor DarkGray
    }
    if (-not $pass) {
        $script:exitCode = 1
    }
}

function ConvertTo-HashtableRecursive {
    param([object]$InputObject)
    if ($null -eq $InputObject) {
        return $null
    }
    if ($InputObject -is [System.Collections.IDictionary]) {
        $ht = @{}
        foreach ($key in $InputObject.Keys) {
            $ht[$key] = ConvertTo-HashtableRecursive $InputObject[$key]
        }
        return $ht
    }
    if ($InputObject -is [System.Collections.IEnumerable] -and -not ($InputObject -is [string])) {
        $list = @()
        foreach ($item in $InputObject) {
            $list += ,(ConvertTo-HashtableRecursive $item)
        }
        return $list
    }
    if ($InputObject.PSObject -and $InputObject.PSObject.Properties.Count -gt 0) {
        $ht = @{}
        foreach ($prop in $InputObject.PSObject.Properties) {
            $ht[$prop.Name] = ConvertTo-HashtableRecursive $prop.Value
        }
        return $ht
    }
    return $InputObject
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

# --- P5-1 Guards (new) ---

# Guard 11: No usages of deprecated typealias in core/src/main (definitions exempt)
$deprecatedAliasNames = @()
$ktFiles = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt"
foreach ($file in $ktFiles) {
    $fileLines = Get-Content $file.FullName
    for ($i = 0; $i -lt $fileLines.Count; $i++) {
        if ($fileLines[$i] -match "^\s*typealias\s+([A-Za-z_][A-Za-z0-9_]*)\b") {
            $aliasName = $Matches[1]
            $isDeprecatedAlias = $false
            for ($j = [Math]::Max(0, $i - 3); $j -lt $i; $j++) {
                if ($fileLines[$j] -match "@Deprecated") {
                    $isDeprecatedAlias = $true
                    break
                }
            }
            if ($isDeprecatedAlias) {
                $deprecatedAliasNames += $aliasName
            }
        }
    }
}
$deprecatedAliasNames = $deprecatedAliasNames | Sort-Object -Unique

$deprecatedAliasUsages = @()
foreach ($aliasName in $deprecatedAliasNames) {
    $hits = $ktFiles |
        Select-String -Pattern ("\b" + [Regex]::Escape($aliasName) + "\b") |
        Where-Object {
            $_.Line -notmatch ("^\s*typealias\s+" + [Regex]::Escape($aliasName) + "\b") -and
            $_.Line -notmatch "^\s*@Deprecated" -and
            $_.Line -notmatch "^\s*//"
        }
    $deprecatedAliasUsages += $hits
}
Write-Result "P5-1-1: No deprecated typealias usages in core/src/main (definitions exempt)" ($deprecatedAliasUsages.Count -eq 0) "Found $($deprecatedAliasUsages.Count) usages across $($deprecatedAliasNames.Count) aliases"

# --- P5-3/P5-4 Guards (new) ---

# Guard 12: No increase in Flt64-fixed token-table signatures under core/model.
# Current remaining items are solver-boundary / compatibility shims and are
# tracked by frozen baseline to prevent regressions.
$modelMain = "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model"
$flt64TokenSigMatches = Get-ChildItem -Path $modelMain -Recurse -Filter "*.kt" |
    Select-String -Pattern "AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>" |
    Where-Object { $_.Line -notmatch "^\s*//" }
$flt64TokenSigBaseline = 0  # Updated 2026-04-29 after P6-2b (was 18 after P5 full closure)
Write-Baseline "P5-3-1: No new Flt64-fixed token-table signatures under core/model" $flt64TokenSigMatches.Count $flt64TokenSigBaseline "Found $($flt64TokenSigMatches.Count) total (baseline=$flt64TokenSigBaseline)"

# Guard 13: No increase in deprecated ToMathLinearPolynomial bridge references
# across core/framework during migration window.
$toMathLinearPolyMatches = @()
$bridgeRoots = @("ospf-kotlin-core/src/main", "ospf-kotlin-framework/src/main")
foreach ($root in $bridgeRoots) {
    if (Test-Path $root) {
        $toMathLinearPolyMatches += Get-ChildItem -Path $root -Recurse -Filter "*.kt" |
            Select-String -Pattern "\bToMathLinearPolynomial\b" |
            Where-Object { $_.Line -notmatch "^\s*//" }
    }
}
$toMathLinearPolyBaseline = 13  # Updated 2026-04-29 after P6-2a (was 23 after P5 full closure)
Write-Baseline "P5-4-1: No new ToMathLinearPolynomial bridge references (core/framework)" $toMathLinearPolyMatches.Count $toMathLinearPolyBaseline "Found $($toMathLinearPolyMatches.Count) total (baseline=$toMathLinearPolyBaseline)"

# Guard 14: No new .toDouble() bridge outside SolveValueConversionContext allowlist in core/src/main.
$toDoubleAllowedPath = "fuookami/ospf/kotlin/core/solver/value/SolveValueConversionContext.kt"
$toDoubleAllowedLine = "val converted = this.toDouble()"
$toDoubleViolations = @()
$toDoubleAllowedHits = @()
Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" | ForEach-Object {
    $relativePath = Get-RelativePath -Root $coreMain -FilePath $_.FullName
    $lineNumber = 0
    Get-Content $_.FullName | ForEach-Object {
        $lineNumber++
        $trimmed = $_.Trim()
        if ($trimmed -notmatch "^(//|\*|/\*\*)") {
            if ($trimmed -match "\.toDouble\(\)") {
                $location = "${relativePath}:$lineNumber"
                $isAllowed = $relativePath -eq $toDoubleAllowedPath -and $trimmed -eq $toDoubleAllowedLine
                if ($isAllowed) {
                    $toDoubleAllowedHits += $location
                } else {
                    $toDoubleViolations += "${location}: $trimmed"
                }
            }
        }
    }
}
Write-Result "P5-5-1: No .toDouble() usage outside SolveValueConversionContext allowlist in core/src/main" ($toDoubleViolations.Count -eq 0) "Found $($toDoubleViolations.Count) violations"
if (($Verbose -or $toDoubleViolations.Count -gt 0) -and $toDoubleViolations.Count -gt 0) {
    $preview = ($toDoubleViolations | Select-Object -First 8) -join "; "
    Write-Host "      Violations: $preview" -ForegroundColor DarkGray
}
Write-Result "P5-5-2: Exactly one allowlisted .toDouble() bridge in SolveValueConversionContext" ($toDoubleAllowedHits.Count -eq 1) "Found $($toDoubleAllowedHits.Count) allowlisted hits"

# Guard 15: No isClosedForSend usage in primary modules (zero-tolerance)
$coroutineGuardRoots = @(
    "ospf-kotlin-core/src/main",
    "ospf-kotlin-framework/src/main",
    "ospf-kotlin-framework-plugin",
    "ospf-kotlin-math/src/main",
    "ospf-kotlin-framework-bpp3d"
)
$isClosedForSendMatches = @()
$closedSendExceptionMatches = @()
$globalScopeMatches = @()
$delicateApiMatches = @()
foreach ($root in $coroutineGuardRoots) {
    if (-not (Test-Path $root)) {
        continue
    }
    $isClosedForSendMatches += Get-ChildItem -Path $root -Recurse -Filter "*.kt" |
        Select-String -Pattern "\bisClosedForSend\b" |
        Where-Object { $_.Line -notmatch "^\s*//" }
    $closedSendExceptionMatches += Get-ChildItem -Path $root -Recurse -Filter "*.kt" |
        Select-String -Pattern "\bClosedSendChannelException\b" |
        Where-Object { $_.Line -notmatch "^\s*//" }
    $globalScopeMatches += Get-ChildItem -Path $root -Recurse -Filter "*.kt" |
        Select-String -Pattern "\bGlobalScope\b" |
        Where-Object { $_.Line -notmatch "^\s*//" }
    $delicateApiMatches += Get-ChildItem -Path $root -Recurse -Filter "*.kt" |
        Select-String -Pattern "\bDelicateCoroutinesApi\b" |
        Where-Object { $_.Line -notmatch "^\s*//" }
}
Write-Result "P5-6-1: No isClosedForSend usage in primary modules" ($isClosedForSendMatches.Count -eq 0) "Found $($isClosedForSendMatches.Count) violations"
Write-Result "P5-6-2: No ClosedSendChannelException usage in primary modules" ($closedSendExceptionMatches.Count -eq 0) "Found $($closedSendExceptionMatches.Count) violations"
Write-Result "P5-6-3: No GlobalScope usage in primary modules" ($globalScopeMatches.Count -eq 0) "Found $($globalScopeMatches.Count) violations"
Write-Result "P5-6-4: No DelicateCoroutinesApi usage in primary modules" ($delicateApiMatches.Count -eq 0) "Found $($delicateApiMatches.Count) violations"

# --- P6/P7 Metric Guards ---

$mathMain = "ospf-kotlin-math/src/main"

if ($GuardMode -eq "P6") {
    $coreFlt64Matches = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
        Select-String -Pattern "<Flt64>" |
        Where-Object { $_.Line -notmatch "^\s*//" }
    $coreFlt64Baseline = 606  # Updated 2026-04-30 after P7-7
    Write-Baseline "P6-0-1: core/src/main <Flt64> baseline freeze" $coreFlt64Matches.Count $coreFlt64Baseline "Found $($coreFlt64Matches.Count) total (baseline=$coreFlt64Baseline)"

    $coreStarMatches = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
        Select-String -Pattern "<\*>" |
        Where-Object { $_.Line -notmatch "^\s*//" }
    $coreStarBaseline = 264  # Updated 2026-04-30 after P7-7
    Write-Baseline "P6-0-2: core/src/main <*> baseline freeze" $coreStarMatches.Count $coreStarBaseline "Found $($coreStarMatches.Count) total (baseline=$coreStarBaseline)"

    $coreDeprecatedMatches = Get-ChildItem -Path $coreMain -Recurse -Filter "*.kt" |
        Select-String -Pattern "@Deprecated" |
        Where-Object { $_.Line -notmatch "^\s*//" }
    $coreDeprecatedBaseline = 0
    Write-Baseline "P6-0-3: core/src/main @Deprecated baseline freeze" $coreDeprecatedMatches.Count $coreDeprecatedBaseline "Found $($coreDeprecatedMatches.Count) total (baseline=$coreDeprecatedBaseline)"

    if (Test-Path $mathMain) {
        $mathFlt64Matches = Get-ChildItem -Path $mathMain -Recurse -Filter "*.kt" |
            Select-String -Pattern "<Flt64>" |
            Where-Object { $_.Line -notmatch "^\s*//" }
        $mathFlt64Baseline = 247
        Write-Baseline "P6-0-4: math/src/main <Flt64> baseline freeze" $mathFlt64Matches.Count $mathFlt64Baseline "Found $($mathFlt64Matches.Count) total (baseline=$mathFlt64Baseline)"
    } else {
        Write-Host "[SKIP] P6-0-4: math/src/main not found" -ForegroundColor Yellow
    }

    if (Test-Path $mathMain) {
        $mathStarMatches = Get-ChildItem -Path $mathMain -Recurse -Filter "*.kt" |
            Select-String -Pattern "<\*>" |
            Where-Object { $_.Line -notmatch "^\s*//" }
        $mathStarBaseline = 218  # Updated 2026-04-30 after P7-7
        Write-Baseline "P6-0-5: math/src/main <*> baseline freeze" $mathStarMatches.Count $mathStarBaseline "Found $($mathStarMatches.Count) total (baseline=$mathStarBaseline)"
    } else {
        Write-Host "[SKIP] P6-0-5: math/src/main not found" -ForegroundColor Yellow
    }

    if (Test-Path $mathMain) {
        $mathDeprecatedMatches = Get-ChildItem -Path $mathMain -Recurse -Filter "*.kt" |
            Select-String -Pattern "@Deprecated" |
            Where-Object { $_.Line -notmatch "^\s*//" }
        $mathDeprecatedBaseline = 0
        Write-Baseline "P6-0-6: math/src/main @Deprecated baseline freeze" $mathDeprecatedMatches.Count $mathDeprecatedBaseline "Found $($mathDeprecatedMatches.Count) total (baseline=$mathDeprecatedBaseline)"
    } else {
        Write-Host "[SKIP] P6-0-6: math/src/main not found" -ForegroundColor Yellow
    }

    # P6-0-7 removed after P7-7: <F64> is now zero across all modules
} else {
    $whitelistPath = "ospf-kotlin-core/scripts/p7-whitelist.json"
    if (-not (Test-Path $whitelistPath)) {
        Write-Result "P7-0-0: whitelist file exists ($whitelistPath)" $false "Missing whitelist file."
    } else {
        $p7WhitelistRaw = Get-Content -Raw $whitelistPath | ConvertFrom-Json
        $p7Whitelist = ConvertTo-HashtableRecursive $p7WhitelistRaw
        Write-P7Whitelist "P7-0-1: core/src/main <Flt64> whitelist freeze" $coreMain "<Flt64>" $p7Whitelist.core.flt64
        Write-P7Whitelist "P7-0-2: core/src/main <*> whitelist freeze" $coreMain "<\*>" $p7Whitelist.core.star
        Write-P7Whitelist "P7-0-3: core/src/main @Deprecated whitelist freeze" $coreMain "@Deprecated" $p7Whitelist.core.deprecated
        Write-P7Whitelist "P7-0-4: math/src/main <Flt64> whitelist freeze" $mathMain "<Flt64>" $p7Whitelist.math.flt64
        Write-P7Whitelist "P7-0-5: math/src/main <*> whitelist freeze" $mathMain "<\*>" $p7Whitelist.math.star
        Write-P7Whitelist "P7-0-6: math/src/main @Deprecated whitelist freeze" $mathMain "@Deprecated" $p7Whitelist.math.deprecated
        # P7-0-7 removed after P7-7: <F64> is now zero across all modules
    }
}

# --- Summary ---
Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "All guards passed." -ForegroundColor Green
} else {
    Write-Host "Some guards failed. See above for details." -ForegroundColor Red
}
exit $exitCode
