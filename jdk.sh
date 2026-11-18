#!/usr/bin/env sh
# Mencari JDK 17+: JAVA_HOME -> JDK extension VS Code -> javac di PATH.
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" -o -x "$JAVA_HOME/bin/javac.exe" ]; then
  JDK_BIN="$JAVA_HOME/bin/"
elif [ -x "$HOME/.vscode/extensions/oracleJdk-25/bin/javac.exe" ]; then
  JDK_BIN="$HOME/.vscode/extensions/oracleJdk-25/bin/"
else
  JDK_BIN=""
fi
JAVAC="${JDK_BIN}javac"
JAVA="${JDK_BIN}java"
