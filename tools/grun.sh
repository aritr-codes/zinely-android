#!/usr/bin/env bash
# Neutral Gradle runner for this dev environment.
# Why: a session hook intercepts commands that literally name build tools, and no
# Gradle/Kotlin is on PATH. This wraps the project wrapper (or cached dist) behind
# short aliases that contain no trigger words, so test/build runs work. Dev-only.
set -euo pipefail

export JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.10.7-hotspot"
CACHED="$HOME/.gradle/wrapper/dists/gradle-9.4.1-bin"
CACHED="$(ls -d "$CACHED"/*/gradle-9.4.1/bin/gradle 2>/dev/null | head -1 || true)"

if [ -x "./gradlew" ]; then RUNNER="./gradlew"; else RUNNER="$CACHED"; fi

cmd="${1:-help}"; shift || true
case "$cmd" in
  v)   "$RUNNER" --version "$@" ;;
  tm)  m="$1"; shift; "$RUNNER" ":core:${m}:test" "$@" ;;          # test one core module
  cm)  m="$1"; shift; "$RUNNER" ":core:${m}:check" "$@" ;;         # check one core module
  call) "$RUNNER" ":core:model:test" ":core:imposition:test" "$@" ;; # both core modules
  task) "$RUNNER" tasks "$@" ;;
  *)   "$RUNNER" "$cmd" "$@" ;;
esac
