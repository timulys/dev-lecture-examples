#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
lesson_java=java
lesson_javac=javac
if [[ -n "${JAVA_HOME:-}" ]]; then
  lesson_java="$JAVA_HOME/bin/java"
  lesson_javac="$JAVA_HOME/bin/javac"
fi
mkdir -p build/classes
"$lesson_javac" -encoding UTF-8 --release 21 -d build/classes src/*.java
if [[ "${1:-demo}" == "test" ]]; then
  "$lesson_java" -cp build/classes LessonChecks
else
  "$lesson_java" -cp build/classes TreeMapLesson "${1:-demo}"
fi
