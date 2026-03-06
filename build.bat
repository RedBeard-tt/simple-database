@echo off
if not exist out mkdir out
javac -d out -encoding UTF-8 --release 8 src\kvstore\*.java
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
echo Build OK. Run with: java -cp out kvstore.Main
