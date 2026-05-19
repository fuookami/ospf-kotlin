#!/usr/bin/env pwsh

param(
    [switch]$WithSolverIntegration,
    [switch]$SkipDefaultExampleTest
)

$ErrorActionPreference = "Stop"
$exitCode = 0
$failedSteps = @()

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $repoRoot

$psCommand = if (Get-Command pwsh -ErrorAction SilentlyContinue) {
    "pwsh"
} elseif (Get-Command powershell -ErrorAction SilentlyContinue) {
    "powershell"
} else {
    throw "Neither pwsh nor powershell is available."
}

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host "[RUN ] $Name" -ForegroundColor Cyan
    try {
        & $Action
        Write-Host "[PASS] $Name" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] $Name" -ForegroundColor Red
        Write-Host "      $($_.Exception.Message)" -ForegroundColor DarkGray
        $script:failedSteps += $Name
        $script:exitCode = 1
    }
}

function Invoke-MavenStep {
    param(
        [string]$Name,
        [string[]]$MavenArgs
    )

    Invoke-Step -Name $Name -Action {
        & mvn @MavenArgs
        if ($LASTEXITCODE -ne 0) {
            throw "mvn exited with code $LASTEXITCODE"
        }
    }
}

function Invoke-GuardStep {
    param(
        [string]$Name,
        [string]$GuardMode
    )

    Invoke-Step -Name $Name -Action {
        & $psCommand -NoProfile -ExecutionPolicy Bypass -File ".\ospf-kotlin-core\scripts\check-c8-guards.ps1" -GuardMode $GuardMode
        if ($LASTEXITCODE -ne 0) {
            throw "check-c8-guards.ps1 exited with code $LASTEXITCODE"
        }
    }
}

Invoke-MavenStep -Name "Core source-compat + math bridge/DSL tests" -MavenArgs @(
    "-pl", "ospf-kotlin-core",
    "-am",
    "-Dtest=SourceCompatTest,MathInequalityFlattenTest",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "test"
)

Invoke-MavenStep -Name "Example default compile" -MavenArgs @(
    "-pl", "ospf-kotlin-example",
    "-am",
    "-DskipTests",
    "compile"
)

if (-not $SkipDefaultExampleTest) {
    Invoke-MavenStep -Name "Example default test" -MavenArgs @(
        "-pl", "ospf-kotlin-example",
        "-am",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "test"
    )
}

Invoke-MavenStep -Name "Example profile: core-demo-only" -MavenArgs @(
    "-pl", "ospf-kotlin-example",
    "-am",
    "-Pcore-demo-only",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "test"
)

Invoke-MavenStep -Name "Example profile: build-only-function-tests" -MavenArgs @(
    "-pl", "ospf-kotlin-example",
    "-am",
    "-Pbuild-only-function-tests",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "test"
)

Invoke-MavenStep -Name "Example profile: business-source-compat" -MavenArgs @(
    "-pl", "ospf-kotlin-example",
    "-am",
    "-Pbusiness-source-compat",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "test"
)

Invoke-MavenStep -Name "Example profile: framework-starter-compat" -MavenArgs @(
    "-pl", "ospf-kotlin-example",
    "-am",
    "-Pframework-starter-compat",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "test"
)

if ($WithSolverIntegration) {
    Invoke-MavenStep -Name "Example profile: solver-integration-tests" -MavenArgs @(
        "-pl", "ospf-kotlin-example",
        "-am",
        "-Psolver-integration-tests",
        "-Dsurefire.failIfNoSpecifiedTests=false",
        "test"
    )
}

Invoke-GuardStep -Name "Static guards: P6 baseline mode" -GuardMode "P6"
Invoke-GuardStep -Name "Static guards: P7 whitelist mode (includes P10/P11/P12/P14/P16/P17 guards)" -GuardMode "P7"

Write-Host ""
if ($failedSteps.Count -eq 0) {
    Write-Host "All migration compatibility-free checks passed." -ForegroundColor Green
} else {
    Write-Host "Migration compatibility-free checks failed in steps:" -ForegroundColor Red
    foreach ($step in $failedSteps) {
        Write-Host "  - $step" -ForegroundColor Red
    }
}

exit $exitCode
