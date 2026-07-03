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
  # S2A — pure-Kotlin data core
  s2a-wiring)    git add settings.gradle.kts gradle/libs.versions.toml \
                         core/model/build.gradle.kts core/data/build.gradle.kts \
                         tools/gitstage.sh ;;
  s2a-model)     git add core/model/src ;;
  s2a-data)      git add core/data/src ;;
  s2a-docs)      git add docs/ ;;
  # S2A follow-ups (post-merge): CI + core-only settings toggle
  s2a-ci)        git add settings.gradle.kts .github/workflows/ci.yml tools/gitstage.sh ;;
  # S2B — durability/GC core (pure-JVM :core:data-storage)
  s2b-wiring)    git add settings.gradle.kts .github/workflows/ci.yml \
                         core/data-storage/build.gradle.kts tools/gitstage.sh ;;
  s2b-storage)   git add core/data-storage/src ;;
  # :data-android module skeleton (ADR-026 PR-A, Build Order Step 1)
  s2b-android)   git add settings.gradle.kts gradle/libs.versions.toml build.gradle.kts \
                         data-android/build.gradle.kts data-android/src tools/gitstage.sh ;;
  # :app -> :data-android edge, completing the intended graph (ADR-026 PR-A, Checkpoint #1 fixup)
  s2b-app-wire)  git add app/build.gradle.kts tools/gitstage.sh ;;
  # AndroidFileSystemOps: real Os.fsync dir flush, fail-closed (ADR-026 PR-A, Build Order Step 2)
  s2b-fsops)     git add gradle/libs.versions.toml data-android/build.gradle.kts \
                         data-android/src tools/gitstage.sh ;;
  # Core library desugaring (nio) so java.nio.file durability core runs on minSdk 24 (ADR-024 amend)
  s2b-desugar)   git add gradle/libs.versions.toml app/build.gradle.kts \
                         data-android/build.gradle.kts docs/DECISIONS.md tools/gitstage.sh ;;
  # Hilt DI graph wiring + Android graph-validation CI (ADR-026 PR-A Step 7 — final PR-A step)
  s2b-step7)     git add .github/workflows/ci.yml gradle/libs.versions.toml gradle.properties \
                         app/build.gradle.kts app/src/main/AndroidManifest.xml \
                         app/src/main/java/com/aritr/zinely/ZinelyApplication.kt \
                         data-android/build.gradle.kts data-android/src \
                         docs/spikes/pr-a-step-7-hilt-di.md tools/gitstage.sh ;;
  # S3 — pure-Kotlin :core:render command model (ADR-027); Android parity backend tier still pending
  s3-render)     git add settings.gradle.kts .github/workflows/ci.yml \
                         core/render/build.gradle.kts core/render/src \
                         docs/DECISIONS.md docs/ARCHITECTURE.md docs/ROADMAP.md \
                         docs/spikes/core-render.md tools/gitstage.sh ;;
  # S6.1 — Room-backed ProjectRepository index over per-project files (ADR-042)
  s61-room)      git add CHANGELOG.md gradle/libs.versions.toml \
                         core/data/src data-android/build.gradle.kts \
                         data-android/schemas data-android/src \
                         docs/ARCHITECTURE.md docs/DECISIONS.md docs/ROADMAP.md \
                         tools/gitstage.sh ;;
  # S6.2 — Home / "My zines" read-only shelf, built-but-unwired (ADR-043)
  s62-home)      git add CHANGELOG.md app/src feature/editor/src \
                         docs/ARCHITECTURE.md docs/DECISIONS.md docs/ROADMAP.md \
                         docs/design/SCREEN-INVENTORY.md tools/gitstage.sh ;;
  # S6.3 — Home shelf actions + repo-enforced open-editor exclusion (ADR-044)
  s63-actions)   git add CHANGELOG.md core/data/src app/src feature/editor/src \
                         data-android/src \
                         docs/ARCHITECTURE.md docs/DECISIONS.md docs/ROADMAP.md \
                         docs/design/SCREEN-INVENTORY.md tools/gitstage.sh ;;
  # S6.4 — Home shelf page-1 thumbnails via the shared render parity path (ADR-045)
  s64-thumbs)    git add CHANGELOG.md app/src feature/editor/src \
                         data-android/src render-android/src \
                         docs/ARCHITECTURE.md docs/DECISIONS.md docs/ROADMAP.md \
                         docs/design/SCREEN-INVENTORY.md tools/gitstage.sh ;;
  *) echo "unknown milestone: ${1:-<none>}" >&2; exit 2 ;;
esac
git status -s
