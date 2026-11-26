#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ -n "${LECTURE_JAVA_HOME:-}" ]; then
  JAVA="$LECTURE_JAVA_HOME/bin/java"
  JAVAC="$LECTURE_JAVA_HOME/bin/javac"
else
  JAVA=java
  JAVAC=javac
fi
mkdir -p build/classes
"$JAVAC" -encoding UTF-8 --release 21 -d build/classes src/*.java test/Checks.java
case "${1:-demo}" in
  demo) "$JAVA" -cp build/classes Demo "${2:-outcomes}" ;;
  test) "$JAVA" -cp build/classes Checks ;;
  compile-demo)
    mkdir -p build/missing build/fixed
    if "$JAVAC" -J-Duser.language=en -encoding UTF-8 --release 21 -d build/missing variants/missing/Expansion.java >build/missing.log 2>&1; then
      echo 'FAIL: missing branch compiled' >&2
      exit 1
    fi
    cat build/missing.log
    if ! grep -q 'does not cover all possible input values' build/missing.log; then
      echo 'FAIL: expected exhaustiveness diagnostic missing' >&2
      exit 1
    fi
    "$JAVAC" -encoding UTF-8 --release 21 -d build/fixed variants/fixed/Expansion.java
    "$JAVA" -cp build/fixed Expansion
    mkdir -p build/default build/unpermitted
    "$JAVAC" -encoding UTF-8 --release 21 -d build/default variants/default/Expansion.java
    "$JAVA" -cp build/default Expansion
    if "$JAVAC" -J-Duser.language=en -encoding UTF-8 --release 21 -cp build/classes -d build/unpermitted variants/unpermitted/Unknown.java >build/unpermitted.log 2>&1; then
      echo 'FAIL: unpermitted implementation compiled' >&2
      exit 1
    fi
    if ! grep -q 'not allowed to extend sealed class' build/unpermitted.log; then
      cat build/unpermitted.log >&2
      exit 1
    fi
    echo 'PASS: missing case and unpermitted type rejected; explicit and default cases ran'
    ;;
  *) echo 'Usage: ./run.sh demo [outcomes|flow|confirm|state|invalid|ignored] | test | compile-demo' >&2; exit 2 ;;
esac
