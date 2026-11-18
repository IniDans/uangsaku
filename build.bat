@echo off
setlocal
cd /d "%~dp0"
call jdk.bat
if errorlevel 1 exit /b 1

if exist out\main rmdir /s /q out\main
mkdir out\main

rem javac menelusuri semua class yang dipakai Main lewat -sourcepath
"%JAVAC%" --release 17 -encoding UTF-8 -Xlint:unchecked -Xlint:deprecation -sourcepath src\main\java -d out\main src\main\java\Main.java
if errorlevel 1 (
    echo [GAGAL] Kompilasi gagal.
    exit /b 1
)
echo [OK] Kompilasi berhasil ke folder out\main
exit /b 0
