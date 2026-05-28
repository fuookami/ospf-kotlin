param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$projectPath = (Resolve-Path -Path $ProjectRoot).Path

if ((Test-Path (Join-Path $projectPath "pom.xml")) -and (Test-Path (Join-Path $projectPath "bpp3d-application"))) {
    $scanRoot = $projectPath
} elseif (Test-Path (Join-Path $projectPath "ospf-kotlin-framework-bpp3d")) {
    $scanRoot = Join-Path $projectPath "ospf-kotlin-framework-bpp3d"
} else {
    throw "Cannot locate ospf-kotlin-framework-bpp3d root from '$projectPath'."
}

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg not found. Please install ripgrep."
}

$forbiddenPathRules = @(
    "bpp3d-infrastructure/src/main/.*/Cuboid\.kt$",
    "bpp3d-infrastructure/src/main/.*/Projection\.kt$",
    "bpp3d-infrastructure/src/main/.*/Placement\.kt$",
    "bpp3d-infrastructure/src/main/.*/Container\.kt$",
    "bpp3d-domain-item-context/src/main/.*/model/.*\.kt$",
    "bpp3d-domain-item-context/src/main/.*/service/.*\.kt$",
    "bpp3d-domain-.*/src/main/.*/service/.*\.kt$",
    "bpp3d-domain-layer-assignment-context/src/main/.*/model/.*\.kt$",
    "bpp3d-application/src/main/.*/service/.*\.kt$"
)

$excludedPathRules = @(
    "/compat/",
    "LegacyScalars\.kt$",
    "QuantityDomainAliases\.kt$",
    "InfraLegacyAliases\.kt$",
    "ApplicationScalarAliases\.kt$",
    "LayerGenerationScalarAliases\.kt$",
    "PackingScalarAliases\.kt$"
)

$checks = @(
    @{
        Name = "LegacyQuantity"
        Pattern = "\bLegacyQuantity\b"
    },
    @{
        Name = "toLegacy"
        Pattern = "toLegacy\("
    },
    @{
        Name = "asScalarF64"
        Pattern = "asScalarF64\("
    },
    @{
        Name = "toFlt64"
        Pattern = "toFlt64\("
    },
    @{
        Name = "QuantityFlt64"
        Pattern = "QuantityFlt64"
    },
    @{
        Name = "Flt64Token"
        Pattern = "\bFlt64\b"
    },
    @{
        Name = "LegacyScalarToken"
        Pattern = "\bLegacyScalar\b"
    }
)

$violations = @()

foreach ($check in $checks) {
    $lines = rg -n --no-heading --color never $check.Pattern $scanRoot -g "**/src/main/**/*.kt" -S
    foreach ($line in $lines) {
        $match = [regex]::Match($line, "^([A-Za-z]:.*?):([0-9]+):(.*)$")
        if (-not $match.Success) {
            continue
        }

        $filePath = $match.Groups[1].Value.Replace("\", "/")
        $lineNumber = $match.Groups[2].Value
        $text = $match.Groups[3].Value

        $isExcluded = $false
        foreach ($rule in $excludedPathRules) {
            if ($filePath -match $rule) {
                $isExcluded = $true
                break
            }
        }
        if ($isExcluded) {
            continue
        }

        $isForbidden = $false
        foreach ($rule in $forbiddenPathRules) {
            if ($filePath -match $rule) {
                $isForbidden = $true
                break
            }
        }

        if ($isForbidden) {
            $violations += [PSCustomObject]@{
                Check = $check.Name
                File = $filePath
                Line = $lineNumber
                Text = $text
            }
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Host "GENERIC_BOUNDARY_PASS"
    exit 0
}

Write-Host "GENERIC_BOUNDARY_FAIL: $($violations.Count)"
$violations |
    Sort-Object Check, File, Line |
    ForEach-Object {
        Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text)"
    }
exit 1
