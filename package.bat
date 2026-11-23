@echo off
rem ------------------------------------------------------------------
rem Membuat aplikasi Windows (.exe) dengan jpackage dari JDK.
rem Hasil: dist\UangSaku\UangSaku.exe (sudah membawa Java runtime sendiri,
rem jadi komputer lain tidak perlu memasang Java).
rem Folder data\seed ikut disalin ke samping .exe karena aplikasi membaca
rem folder data\ relatif terhadap folder tempat .exe dijalankan.
rem ------------------------------------------------------------------
setlocal
cd /d "%~dp0"
call "%~dp0build.bat"
if errorlevel 1 exit /b 1
call "%~dp0jdk.bat"
if errorlevel 1 exit /b 1

set "JAR=%JDK_DIR%\bin\jar.exe"
set "JPACKAGE=%JDK_DIR%\bin\jpackage.exe"
if not exist "%JPACKAGE%" (
    echo [ERROR] jpackage tidak ditemukan di %JDK_DIR%\bin. Butuh JDK 17+ lengkap.
    exit /b 1
)

if exist out\jar rmdir /s /q out\jar
mkdir out\jar
"%JAR%" --create --file out\jar\UangSaku.jar --main-class Main -C out\main .
if errorlevel 1 (
    echo [GAGAL] Pembuatan JAR gagal.
    exit /b 1
)

if exist dist\UangSaku rmdir /s /q dist\UangSaku
"%JPACKAGE%" --type app-image --name UangSaku --app-version 1.0 ^
    --vendor "Proyek ADVPRO" ^
    --description "Sistem Manajemen Keuangan dan Uang Saku Mahasiswa Rantau" ^
    --input out\jar --main-jar UangSaku.jar --main-class Main ^
    --dest dist
if errorlevel 1 (
    echo [GAGAL] jpackage gagal.
    exit /b 1
)

xcopy /e /i /q /y data\seed dist\UangSaku\data\seed >nul
if errorlevel 1 (
    echo [GAGAL] Gagal menyalin data seed.
    exit /b 1
)

echo [OK] Aplikasi siap: dist\UangSaku\UangSaku.exe
exit /b 0
