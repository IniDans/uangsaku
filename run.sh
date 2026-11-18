#!/usr/bin/env sh
cd "$(dirname "$0")" || exit 1
./build.sh || exit 1
. ./jdk.sh || exit 1
"$JAVA" -cp out/main Main
