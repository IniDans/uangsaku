@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
call jdk.bat

if exist out\test rmdir /s /q out\test
mkdir out\test
"%JAVAC%" --release 17 -encoding UTF-8 -Xlint:unchecked -cp out\main -sourcepath src\test\java -d out\test src\test\java\TestRunner.java
if errorlevel 1 (
    echo [GAGAL] Kompilasi tes gagal.
    exit /b 1
)
"%JAVA%" -cp out\main;out\test TestRunner
exit /b %errorlevel%
