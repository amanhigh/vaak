#!/bin/bash

set -euo pipefail

XML_FILE="${1:-}"

if [ -z "$XML_FILE" ] || [ ! -f "$XML_FILE" ]; then
    echo "Error: XML coverage report not found: $XML_FILE"
    exit 1
fi

_RED="\033[31m"
_YELLOW="\033[33m"
_BLUE="\033[34m"
_GREEN="\033[32m"
_RESET="\033[0m"

parse_coverage() {
    local xml_file="$1"
    
    awk '
    /<package name=/ {
        match($0, /name="([^"]*)"/, arr)
        current_pkg = arr[1]
        gsub(/\//, ".", current_pkg)
        in_pkg = 1
        pkg_line_counter = ""
        next
    }
    in_pkg && /^<counter type="LINE"/ && !/sourcefile|class|method/ {
        pkg_line_counter = $0
    }
    /<\/package>/ {
        if (pkg_line_counter != "" && current_pkg !~ /^(dagger\.|hilt_aggregated_deps|.*databinding|.*BuildConfig)/) {
            match(pkg_line_counter, /missed="([0-9]+)"/, m)
            match(pkg_line_counter, /covered="([0-9]+)"/, c)
            total = m[1] + c[1]
            if (total > 0) {
                pct = int((c[1] * 100) / total)
                print current_pkg, pct
            }
        }
        in_pkg = 0
        pkg_line_counter = ""
    }
    ' "$xml_file"
}

overall_coverage=$(tail -10 "$XML_FILE" | grep '<counter type="LINE"' | tail -1 | grep -o 'missed="[0-9]*" covered="[0-9]*"' | \
    awk -F'"' '{
        missed = $2
        covered = $4
        total = missed + covered
        if (total > 0) {
            printf "%.2f%%", (covered * 100.0) / total
        } else {
            print "0.00%"
        }
    }')

echo "Overall Coverage: $overall_coverage"
echo ""
echo "Packages by Coverage (Lowest to Highest)"

parse_coverage "$XML_FILE" | sort -k2 -n | \
awk -v red="$_RED" -v yellow="$_YELLOW" -v blue="$_BLUE" -v green="$_GREEN" -v reset="$_RESET" '
BEGIN {
    critical = 0
    low = 0
    medium = 0
    good = 0
}
{
    pkg = $1
    pct = int($2)
    
    if (pct == 0) {
        printf "%s🔴 %-50s %6s%%%s ← NEEDS TESTS!\n", red, pkg, pct, reset
        critical++
    } else if (pct < 25) {
        printf "%s🟠 %-50s %6s%%%s ← CRITICAL\n", red, pkg, pct, reset
        low++
    } else if (pct < 50) {
        printf "%s🟡 %-50s %6s%%%s ← LOW\n", yellow, pkg, pct, reset
        low++
    } else if (pct < 75) {
        printf "%s🔵 %-50s %6s%%%s ← MEDIUM\n", blue, pkg, pct, reset
        medium++
    } else {
        printf "%s🟢 %-50s %6s%%%s ← GOOD\n", green, pkg, pct, reset
        good++
    }
}
END {
    print ""
    printf "%s[Summary]%s\n", green, reset
    print "🔴 Critical (0%): " critical " packages"
    print "🟠 Low (<50%): " low " packages"
    print "🔵 Medium (50-75%): " medium " packages"
    print "🟢 Good (≥75%): " good " packages"
}'
