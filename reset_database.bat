@echo off
REM Set codepage to handle potential output issues, though input is now ASCII.
@chcp 65001 >nul

REM --- Configuration ---
REM Database name
SET DB_NAME=nushungry_db
REM SQL backup filename
SET BACKUP_FILE=nushungry_db_backup.sql
REM --- End of Configuration ---

REM Prompt the user for their MySQL username
set /p DB_USER="Enter your MySQL username (e.g., root): "

REM Check if the backup file exists
if not exist "%BACKUP_FILE%" (
    echo ERROR: Backup file '%BACKUP_FILE%' not found in the current directory.
    pause
    exit /b 1
)

echo --- Starting database reset for: %DB_NAME% ---

REM -p prompts for password securely
echo Step 1/3: Dropping old database (if it exists)...
mysql -u "%DB_USER%" -p -e "DROP DATABASE IF EXISTS `%DB_NAME%`;"

REM Check if the last command was successful
if %errorlevel% neq 0 (
    echo ERROR: Failed to drop the database. Please check your username and password.
    pause
    exit /b 1
)

echo Step 2/3: Creating new database...
mysql -u "%DB_USER%" -p -e "CREATE DATABASE `%DB_NAME%`;"

if %errorlevel% neq 0 (
    echo ERROR: Failed to create the database.
    pause
    exit /b 1
)

echo Step 3/3: Importing data from '%BACKUP_FILE%'...
mysql -u "%DB_USER%" -p "%DB_NAME%" < "%BACKUP_FILE%"

if %errorlevel% neq 0 (
    echo ERROR: Failed to import data.
    pause
    exit /b 1
)

echo --- SUCCESS! Database '%DB_NAME%' has been reset and re-imported. ---
pause

