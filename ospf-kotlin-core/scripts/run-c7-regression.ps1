param(
    [switch]$SkipFramework = $false,
    [switch]$SkipPlugin = $false
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\\..")

function Invoke-MavenStep {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    Write-Host "==> $Name"
    & mvn @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Step failed: $Name"
    }
}

Push-Location $repoRoot
try {
    Invoke-MavenStep -Name "C7 Core Test" -Arguments @(
        "-pl", "ospf-kotlin-core",
        "-am",
        "-q",
        "test"
    )

    if (-not $SkipFramework) {
        Invoke-MavenStep -Name "C7 Framework Compile" -Arguments @(
            "-pl", "ospf-kotlin-framework",
            "-am",
            "-q",
            "-DskipTests",
            "compile"
        )
    }

    if (-not $SkipPlugin) {
        Invoke-MavenStep -Name "C7 Core Plugin Compile" -Arguments @(
            "-pl", "ospf-kotlin-core-plugin",
            "-am",
            "-q",
            "-DskipTests",
            "compile"
        )
    }

    Write-Host "C7 regression checks passed."
}
finally {
    Pop-Location
}
