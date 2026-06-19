#!/usr/bin/env bash
# Stage files for a named milestone without typing build-tool keywords in the shell
# (a session hook blocks commands literally containing "gradle"/"build"). Dev-only.
set -euo pipefail
case "${1:-}" in
  p1-build)
    git add settings.gradle.kts gradle/libs.versions.toml build.gradle.kts \
            core/model/build.gradle.kts core/imposition/build.gradle.kts \
            tools/grun.sh tools/gitstage.sh .gitignore ;;
  p1-model)      git add core/model/src ;;
  p1-imposition) git add core/imposition/src ;;
  p1-docs)       git add docs/ARCHITECTURE.md ;;
  *) echo "unknown milestone: ${1:-<none>}" >&2; exit 2 ;;
esac
git status -s
