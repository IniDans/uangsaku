#!/usr/bin/env sh
# Build untuk Git Bash / Linux / macOS. Butuh JDK 17+ (JAVA_HOME atau javac di PATH).
cd "$(dirname "$0")" || exit 1
. ./jdk.sh || exit 1
rm -rf out/main && mkdir -p out/main
"$JAVAC" --release 17 -encoding UTF-8 -Xlint:unchecked -Xlint:deprecation \
  -sourcepath src/main/java -d out/main src/main/java/Main.java || { echo "[GAGAL] Kompilasi gagal."; exit 1; }
echo "[OK] Kompilasi berhasil ke folder out/main"
