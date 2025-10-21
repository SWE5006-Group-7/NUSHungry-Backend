@echo off
REM NUSHungry Kubernetes 部署脚本 (Windows)
REM 用法: deploy.bat [namespace] [method]
REM 示例: deploy.bat nushungry helm

setlocal enabledelayedexpansion

REM 默认值
set NAMESPACE=%1
if "%NAMESPACE%"=="" set NAMESPACE=nushungry

set METHOD=%2
if "%METHOD%"=="" set METHOD=helm

set BASE_DIR=%~dp0
set BASE_DIR=%BASE_DIR:~0,-1%

echo ==========================================
echo NUSHungry Kubernetes 部署脚本 (Windows)
echo ==========================================
echo 命名空间: %NAMESPACE%
echo 部署方法: %METHOD%
echo ==========================================

REM 检查 kubectl
where kubectl >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: kubectl 未安装或不在 PATH 中
    exit /b 1
)

REM 检查集群连接
kubectl cluster-info >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 无法连接到 Kubernetes 集群
    exit /b 1
)

echo ✓ Kubernetes 集群连接正常

if /i "%METHOD%"=="helm" goto deploy_helm
if /i "%METHOD%"=="kubectl" goto deploy_kubectl

echo 错误: 不支持的部署方法: %METHOD%
echo 支持的方法: helm, kubectl
exit /b 1

:deploy_helm
echo.
echo 使用 Helm 部署...
echo ==========================================

where helm >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: Helm 未安装或不在 PATH 中
    exit /b 1
)

echo ✓ Helm 已安装

set CHART_DIR=%BASE_DIR%\charts\nushungry
set VALUES_FILE=%CHART_DIR%\values.yaml

if not exist "%VALUES_FILE%" (
    echo 错误: 找不到 values.yaml 文件: %VALUES_FILE%
    exit /b 1
)

echo.
echo 创建命名空间: %NAMESPACE%
kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -

echo.
echo 检查是否已安装...
helm list -n %NAMESPACE% | findstr /i "nushungry" >nul 2>nul
if %errorlevel% equ 0 (
    echo 检测到现有部署,执行升级...
    helm upgrade nushungry "%CHART_DIR%" -n %NAMESPACE% -f "%VALUES_FILE%" --wait --timeout 10m
    echo ✓ Helm 升级完成
) else (
    echo 执行全新安装...
    helm install nushungry "%CHART_DIR%" -n %NAMESPACE% -f "%VALUES_FILE%" --wait --timeout 10m
    echo ✓ Helm 安装完成
)

goto show_status

:deploy_kubectl
echo.
echo 使用 kubectl 部署...
echo ==========================================

set BASE_CONFIG_DIR=%BASE_DIR%\base

if not exist "%BASE_CONFIG_DIR%\secrets.yaml" (
    echo ⚠ 警告: secrets.yaml 不存在
    echo 请复制 secrets.yaml.example 并填写实际值:
    echo   copy "%BASE_CONFIG_DIR%\secrets.yaml.example" "%BASE_CONFIG_DIR%\secrets.yaml"
    echo.
    set /p CONTINUE="是否继续? (y/N): "
    if /i not "!CONTINUE!"=="y" exit /b 1
)

echo.
echo [1/8] 创建命名空间...
kubectl apply -f "%BASE_CONFIG_DIR%\namespace.yaml"

echo.
echo [2/8] 创建 ConfigMap 和 Secret...
kubectl apply -f "%BASE_CONFIG_DIR%\configmaps.yaml"
if exist "%BASE_CONFIG_DIR%\secrets.yaml" (
    kubectl apply -f "%BASE_CONFIG_DIR%\secrets.yaml"
)

echo.
echo [3/8] 创建 PersistentVolume...
kubectl apply -f "%BASE_CONFIG_DIR%\persistent-volumes.yaml"

echo.
echo [4/8] 部署数据库 StatefulSet...
kubectl apply -f "%BASE_CONFIG_DIR%\postgres-statefulset.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\mongodb-statefulset.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\redis-statefulset.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\rabbitmq-statefulset.yaml"

echo 等待数据库就绪 (60秒)...
timeout /t 60 /nobreak >nul

echo.
echo [5/8] 部署基础服务 (Eureka, Config Server)...
kubectl apply -f "%BASE_CONFIG_DIR%\eureka-server-deployment.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\config-server-deployment.yaml"

echo 等待基础服务就绪 (60秒)...
timeout /t 60 /nobreak >nul

echo.
echo [6/8] 部署应用服务...
kubectl apply -f "%BASE_CONFIG_DIR%\gateway-service-deployment.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\admin-service-deployment.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\cafeteria-service-deployment.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\review-service-deployment.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\media-service-deployment.yaml"
kubectl apply -f "%BASE_CONFIG_DIR%\preference-service-deployment.yaml"

echo.
echo [7/8] 创建 Service...
kubectl apply -f "%BASE_CONFIG_DIR%\services.yaml"

echo.
echo [8/8] 创建 Ingress...
kubectl apply -f "%BASE_CONFIG_DIR%\ingress.yaml"

echo.
echo ✓ kubectl 部署完成

:show_status
echo.
echo ==========================================
echo 部署状态
echo ==========================================
echo.
echo Pods:
kubectl get pods -n %NAMESPACE%
echo.
echo Services:
kubectl get svc -n %NAMESPACE%
echo.
echo Ingress:
kubectl get ingress -n %NAMESPACE%

echo.
echo ==========================================
echo ✓ 部署完成!
echo ==========================================
echo.
echo 后续步骤:
echo 1. 查看 Pod 状态: kubectl get pods -n %NAMESPACE% -w
echo 2. 查看日志: kubectl logs -f -n %NAMESPACE% -l app=gateway-service
echo 3. 访问应用: 查看 Ingress 获取访问地址
echo.

endlocal
