param(
    [string]$BaseRef = "HEAD~1",
    [string]$TargetPath = "ospf-kotlin-core/src/main"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\\..")
$emptyTreeHash = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"

function Resolve-BaseRef {
    param([string]$Ref)

    & git rev-parse --verify $Ref *> $null
    if ($LASTEXITCODE -eq 0) {
        return $Ref
    }
    return $emptyTreeHash
}

function Get-AddedLines {
    param(
        [string]$ResolvedBaseRef,
        [string]$ScopePath
    )

    $diff = & git diff --unified=0 --no-color $ResolvedBaseRef -- $ScopePath
    if ($LASTEXITCODE -ne 0) {
        throw "Cannot read git diff from base ref: $ResolvedBaseRef"
    }

    $rows = New-Object System.Collections.Generic.List[object]
    $currentPath = ""
    foreach ($line in $diff) {
        if ($line.StartsWith("+++ b/")) {
            $currentPath = $line.Substring(6)
            continue
        }
        if (
            $line.StartsWith("diff --git") -or
            $line.StartsWith("index ") -or
            $line.StartsWith("@@") -or
            $line.StartsWith("--- ")
        ) {
            continue
        }
        if ($line.StartsWith("+") -and -not $line.StartsWith("+++")) {
            $rows.Add(
                [PSCustomObject]@{
                    Path = $currentPath
                    Line = $line.Substring(1)
                }
            ) | Out-Null
        }
    }
    return $rows
}

function Show-Violations {
    param(
        [string]$Title,
        [System.Collections.Generic.List[object]]$Violations
    )

    if ($Violations.Count -eq 0) {
        return $false
    }

    Write-Host "[C8] $Title"
    foreach ($violation in $Violations) {
        Write-Host "  - $($violation.Path): $($violation.Line.Trim())"
    }
    return $true
}

Push-Location $repoRoot
try {
    $resolvedBaseRef = Resolve-BaseRef -Ref $BaseRef
    $added = Get-AddedLines -ResolvedBaseRef $resolvedBaseRef -ScopePath $TargetPath

    $apiFiles = @(
        "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/Model.kt",
        "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_model/MetaModel.kt",
        "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_model/MathInequalityDsl.kt",
        "ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Bridge.kt"
    )

    $legacyTypeViolations = New-Object System.Collections.Generic.List[object]
    $cellsViolations = New-Object System.Collections.Generic.List[object]
    $doubleViolations = New-Object System.Collections.Generic.List[object]

    foreach ($row in $added) {
        if ($apiFiles -contains $row.Path -and $row.Line -match "AbstractLinearPolynomial|AbstractQuadraticPolynomial") {
            $legacyTypeViolations.Add($row) | Out-Null
        }

        if (
            $row.Line -match "\.cells\b" -and
            $row.Path -notmatch "^ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_model/Polynomial\.kt$"
        ) {
            $cellsViolations.Add($row) | Out-Null
        }

        if ($row.Line -match "\bDouble\b") {
            $doubleViolations.Add($row) | Out-Null
        }
    }

    $hasViolation = $false
    $hasViolation = (Show-Violations -Title "Forbidden Abstract*Polynomial exposure in API files." -Violations $legacyTypeViolations) -or $hasViolation
    $hasViolation = (Show-Violations -Title "Forbidden .cells usage outside legacy Polynomial.kt." -Violations $cellsViolations) -or $hasViolation
    $hasViolation = (Show-Violations -Title "Forbidden Double concretization in core main path." -Violations $doubleViolations) -or $hasViolation

    if ($hasViolation) {
        throw "C8 guard failed."
    }

    Write-Host "C8 guard checks passed."
}
finally {
    Pop-Location
}
