@echo off
echo 从数据库查询数据...
echo.

echo 前5张图片（皇室建筑）:
echo.

for /L %%i in (3,1,7) do (
    echo 查询类型记录 ID: %%i
    curl -s "http://localhost:8080/data/type/%%i"
    echo.
)

echo.
echo 后5张图片（平民建筑）:
echo.

for /L %%i in (8,1,12) do (
    echo 查询类型记录 ID: %%i
    curl -s "http://localhost:8080/data/type/%%i"
    echo.
)

echo.
echo 完成
pause
