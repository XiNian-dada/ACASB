ACASB - Ancient Chinese Architecture in Spring Boot
========================================================

启动说明:
1. 首先启动 Python API 服务:
   双击 start_python.bat
   
2. 然后启动 Java 后端服务:
   双击 start_java.bat

服务端口:
- Java 后端: http://localhost:8080
- Python API: http://localhost:5000

API 端点:
- Java 健康检查: http://localhost:8080/api/health
- Java 预测接口: http://localhost:8080/api/predict (POST)
- Python 健康检查: http://localhost:5000/health
- Python 预测接口: http://localhost:5000/predict (POST)

注意事项:
- 确保已安装 Java 17 (路径: D:\Zulu17)
- 确保已安装 Python 3.11+
- 确保 Python 环境已安装所需依赖 (见 acasb-analysis/requirements.txt)
- 需要先启动 Python 服务，再启动 Java 服务

打包时间: 2026-02-06 01:25:18
