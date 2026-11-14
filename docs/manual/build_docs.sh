#!/usr/bin/env bash
# Regenerate the module reference fragments of the manual from the module
# annotations (docs/manual/modules/generated and modules/undocumented.md),
# so that TestModuleDocs passes again.
#
# Usage: ./build_docs.sh [--no-install] [--maps] [--test]
#   --no-install  skip the 'mvn install' of modules and its dependencies
#                 (only if the current code is already installed)
#   --maps        also regenerate the module maps in images/modules
#                 (slow; runs the example scripts; optimizes with optipng if present)
#   --test        run TestModuleDocs at the end
#
# This does not build the Sphinx html: use ./build_locally.sh for that.
set -euo pipefail

INSTALL=1
MAPS=0
TEST=0
for arg in "$@"; do
  case "$arg" in
    --no-install) INSTALL=0 ;;
    --maps) MAPS=1 ;;
    --test) TEST=1 ;;
    -h|--help) sed -n '2,13p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

# the maven commands run from the repository root
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

if [ "$INSTALL" -eq 1 ]; then
  echo "=== Installing modules and its dependencies ..."
  mvn -o -q install -pl modules -am -DskipTests
fi

echo "=== Generating the module reference fragments ..."
mvn -o -q exec:java -pl modules \
    -Dexec.mainClass=org.hortonmachine.docs.ModuleDocsGenerator \
    -Dexec.args="docs/manual/modules/modules.txt docs/manual/modules/generated docs/manual/modules/undocumented.md"

if [ "$MAPS" -eq 1 ]; then
  echo "=== Generating the module maps ..."
  mvn -o -q exec:java -pl modules -Dexec.classpathScope=test \
      -Dexec.mainClass=org.hortonmachine.docs.ModuleMapsGenerator \
      -Dexec.args="docs/manual/modules/maps/maps.txt docs/manual/images/modules modules/target/module-maps"
  if command -v optipng >/dev/null 2>&1; then
    optipng -quiet -o2 docs/manual/images/modules/*.png
  else
    echo "optipng not found: maps left unoptimized." >&2
  fi
fi

if [ "$TEST" -eq 1 ]; then
  echo "=== Running TestModuleDocs ..."
  mvn -o -q test -pl modules -am -Dtest=TestModuleDocs \
      -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false
fi

echo ""
echo "Done. Changed docs:"
git status --short docs/manual
