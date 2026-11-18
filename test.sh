#!/usr/bin/env sh
cd "$(dirname "$0")" || exit 1
./build.sh || exit 1
. ./jdk.sh || exit 1
rm -rf out/test && mkdir -p out/test
"$JAVAC" --release 17 -encoding UTF-8 -Xlint:unchecked -cp out/main \
  -sourcepath src/test/java -d out/test src/test/java/TestRunner.java || { echo "[GAGAL] Kompilasi tes gagal."; exit 1; }
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
"$JAVA" -cp "out/main${SEP}out/test" TestRunner
