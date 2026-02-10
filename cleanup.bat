@echo off
echo ========================================
echo ACASB 清理脚本
echo ========================================
echo.

echo [1/3] 清理旧的打包文件...
if exist "ACASB_Package_*" (
    echo 正在删除旧的打包文件...
    for /d %%d in (ACASB_Package_*) do (
        echo 删除: %%d
        rd /s /q "%%d"
    )
    echo 完成！
) else (
    echo 没有找到旧的打包文件
)

echo.
echo [2/3] 清理旧的 JAR 文件...
if exist "ACASB-0.0.1-SNAPSHOT.jar" (
    echo 正在删除旧的 JAR 文件...
    del /q "ACASB-0.0.1-SNAPSHOT.jar"
    echo 完成！
) else (
    echo 没有找到旧的 JAR 文件
)

echo.
echo [3/3] 清理旧的启动脚本...
if exist "start_java.bat" (
    echo 正在删除旧的启动脚本...
    del /q "start_java.bat"
    echo 完成！
) else (
    echo 没有找到旧的启动脚本
)

if exist "start_python.bat" (
    echo 正在删除旧的启动脚本...
    del /q "start_python.bat"
    echo 完成！
) else (
    echo 没有找到旧的启动脚本
)

echo.
echo [4/3] 清理旧的 README 文件...
if exist "README.txt" (
    echo 正在删除旧的 README 文件...
    del /q "README.txt"
    echo 完成！
) else (
    echo 没有找到旧的 README 文件
)

echo.
echo ========================================
echo 清理完成！
echo ========================================
echo.
echo 保留的文件：
echo - src/ (源代码)
echo - acasb-frontend/ (前端代码)
echo - acasb-analysis/ (Python 分析代码)
echo - datasets/ (数据集)
echo - config.properties (配置文件)
echo - pom.xml (Maven 配置)
echo - mvnw, mvnw.cmd (Maven Wrapper)
echo.
echo 按任意键退出...
pause > nul
