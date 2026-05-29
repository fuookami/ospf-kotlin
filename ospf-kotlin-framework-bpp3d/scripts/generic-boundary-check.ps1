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

$checks = @(
    @{
        Name = "LegacyUpperToken"
        Pattern = "\bLegacy[A-Za-z0-9_]*\b"
    },
    @{
        Name = "LegacyLowerToken"
        Pattern = "\blegacy[A-Za-z0-9_]*\b"
    },
    @{
        Name = "LegacyQuantity"
        Pattern = "\bLegacyQuantity\b"
    },
    @{
        Name = "toLegacyModel"
        Pattern = "toLegacyModel\("
    },
    @{
        Name = "toLegacyItems"
        Pattern = "toLegacyItems\("
    },
    @{
        Name = "toLegacyLayers"
        Pattern = "toLegacyLayers\("
    },
    @{
        Name = "toLegacyPlacement3"
        Pattern = "toLegacyPlacement3\("
    },
    @{
        Name = "toLegacy"
        Pattern = "toLegacy\("
    },
    @{
        Name = "toFlt64Quantity"
        Pattern = "toFlt64Quantity\("
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
    },
    @{
        Name = "InfraScalarToken"
        Pattern = "\bInfraScalar\b"
    },
    @{
        Name = "ItemModelScalarToken"
        Pattern = "\bItemModelScalar\b"
    },
    @{
        Name = "LayerAssignmentScalarToken"
        Pattern = "\bLayerAssignmentScalar\b"
    },
    @{
        Name = "ApplicationScalarToken"
        Pattern = "\bApplicationScalar\b"
    },
    @{
        Name = "PackingScalarToken"
        Pattern = "\bPackingScalar\b"
    },
    @{
        Name = "BlockLoadingScalarToken"
        Pattern = "\bBlockLoadingScalar\b"
    },
    @{
        Name = "Bpp3dBlaScalarToken"
        Pattern = "\bBpp3dBlaScalar\b"
    },
    @{
        Name = "LegacyDemandSlicesToken"
        Pattern = "\bLegacyDemandSlices\b"
    },
    @{
        Name = "LegacyCuboidToken"
        Pattern = "\bLegacyCuboid\b"
    },
    @{
        Name = "LegacyCuboidGenericAdapterToken"
        Pattern = "\bLegacyCuboidGenericAdapter\b"
    },
    @{
        Name = "legacyZeroToken"
        Pattern = "\blegacyZero\b"
    },
    @{
        Name = "legacyOneToken"
        Pattern = "\blegacyOne\b"
    },
    @{
        Name = "legacyTwoToken"
        Pattern = "\blegacyTwo\b"
    },
    @{
        Name = "legacyInfinityToken"
        Pattern = "\blegacyInfinity\b"
    },
    @{
        Name = "legacyNegativeInfinityToken"
        Pattern = "\blegacyNegativeInfinity\b"
    },
    @{
        Name = "legacyScalarToken"
        Pattern = "\blegacyScalar\b"
    }
)

$violations = @()

$compatDirectories = @(
    "api/compat",
    "model/compat",
    "service/compat"
)

foreach ($dir in $compatDirectories) {
    $dirs = Get-ChildItem -Path $scanRoot -Recurse -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName.Replace("\", "/") -match "/src/main/.*/$([regex]::Escape($dir))$" }
    foreach ($d in $dirs) {
        $violations += [PSCustomObject]@{
            Check = "CompatDirectory"
            File = $d.FullName.Replace("\", "/")
            Line = 1
            Text = "compat directory is forbidden in strict mode"
        }
    }
}

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

        $violations += [PSCustomObject]@{
            Check = $check.Name
            File = $filePath
            Line = $lineNumber
            Text = $text
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Host "STRICT_GENERIC_BOUNDARY_PASS"
    exit 0
}

Write-Host "STRICT_GENERIC_BOUNDARY_FAIL: $($violations.Count)"
$violations |
    Sort-Object Check, File, Line |
    ForEach-Object {
        Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text)"
    }
exit 1
