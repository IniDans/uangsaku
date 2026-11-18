@echo off
rem ------------------------------------------------------------------
rem Mencari JDK 17+ untuk build/run/test.
rem Urutan: JAVA_HOME -> JDK bawaan extension VS Code (oracleJdk-25).
rem Jika JDK ada di lokasi lain, set JAVA_HOME ke folder JDK tersebut.
rem ------------------------------------------------------------------
set "JDK_DIR="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_DIR=%JAVA_HOME%"
if not defined JDK_DIR if exist "%USERPROFILE%\.vscode\extensions\oracleJdk-25\bin\javac.exe" set "JDK_DIR=%USERPROFILE%\.vscode\extensions\oracleJdk-25"
if not defined JDK_DIR (
    echo [ERROR] JDK 17+ tidak ditemukan. Set JAVA_HOME ke folder JDK 17 atau lebih baru.
    exit /b 1
)
set "JAVAC=%JDK_DIR%\bin\javac.exe"
set "JAVA=%JDK_DIR%\bin\java.exe"
exit /b 0
