#!/bin/bash
modules=("ospf-kotlin-core" "ospf-kotlin-core-plugin" "ospf-kotlin-utils" "ospf-kotlin-math" "ospf-kotlin-multiarray" "ospf-kotlin-quantities" "ospf-kotlin-framework" "ospf-kotlin-framework-bpp1d" "ospf-kotlin-framework-bpp2d" "ospf-kotlin-framework-bpp3d" "ospf-kotlin-framework-csp1d" "ospf-kotlin-framework-csp2d" "ospf-kotlin-framework-gantt-scheduling" "ospf-kotlin-framework-network-scheduling" "ospf-kotlin-framework-plugin" "ospf-kotlin-starters" "ospf-kotlin-dependencies" "ospf-kotlin-example" "ospf-kotlin-benchmark")

for mod in "${modules[@]}"; do
    echo "=== $mod ==="
    if [ -d "$mod/src/main" ]; then
        src_files=$(find "$mod/src/main" -name '*.kt' | wc -l)
        total_loc=$(find "$mod/src/main" -name '*.kt' -exec wc -l {} + | tail -1 | awk '{print $1}')
        echo "  Source files: $src_files"
        echo "  Total LOC: $total_loc"
        
        # Show main packages
        echo "  Main packages:"
        find "$mod/src/main/kotlin" -type d 2>/dev/null || find "$mod/src/main/fuookami" -type d 2>/dev/null | head -5
    else
        echo "  Source directory not found"
    fi
done
