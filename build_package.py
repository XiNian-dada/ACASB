import os
import shutil
import subprocess
import zipfile
from datetime import datetime

def run_command(cmd, cwd=None):
    print(f"执行命令: {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"错误: {result.stderr}")
        return False
    print(result.stdout)
    return True

def build_java_project(project_root):
    print("\n=== 开始打包 Java 项目 ===")
    
    java_home = "D:\\Zulu17"
    os.environ["JAVA_HOME"] = java_home
    os.environ["PATH"] = f"{java_home}\\bin;{os.environ['PATH']}"
    
    print(f"设置 JAVA_HOME: {java_home}")
    
    if not os.path.exists(java_home):
        print(f"错误: Java 路径不存在: {java_home}")
        return False
    
    mvn_cmd = ".\\mvnw.cmd clean package -DskipTests"
    if not run_command(mvn_cmd, cwd=project_root):
        print("Java 项目打包失败")
        return False
    
    jar_path = os.path.join(project_root, "target", "ACASB-0.0.1-SNAPSHOT.jar")
    if not os.path.exists(jar_path):
        print(f"错误: JAR 文件未找到: {jar_path}")
        return False
    
    print(f"Java 项目打包成功: {jar_path}")
    return True

def create_package_structure(project_root, temp_dir):
    print("\n=== 创建打包目录结构 ===")
    
    os.makedirs(temp_dir, exist_ok=True)
    
    jar_source = os.path.join(project_root, "target", "ACASB-0.0.1-SNAPSHOT.jar")
    jar_dest = os.path.join(temp_dir, "ACASB-0.0.1-SNAPSHOT.jar")
    shutil.copy2(jar_source, jar_dest)
    print(f"复制 JAR 文件: {jar_dest}")
    
    analysis_source = os.path.join(project_root, "acasb-analysis")
    analysis_dest = os.path.join(temp_dir, "acasb-analysis")
    if os.path.exists(analysis_dest):
        shutil.rmtree(analysis_dest)
    shutil.copytree(analysis_source, analysis_dest)
    print(f"复制 acasb-analysis 目录: {analysis_dest}")
    
    start_java_source = os.path.join(project_root, "start_java.bat")
    start_java_dest = os.path.join(temp_dir, "start_java.bat")
    shutil.copy2(start_java_source, start_java_dest)
    print(f"复制 Java 启动脚本: {start_java_dest}")
    
    start_python_source = os.path.join(project_root, "start_python.bat")
    start_python_dest = os.path.join(temp_dir, "start_python.bat")
    shutil.copy2(start_python_source, start_python_dest)
    print(f"复制 Python 启动脚本: {start_python_dest}")
    
    readme_content = """ACASB - Ancient Chinese Architecture in Spring Boot
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
- 确保已安装 Java 17 (路径: D:\\Zulu17)
- 确保已安装 Python 3.11+
- 确保 Python 环境已安装所需依赖 (见 acasb-analysis/requirements.txt)
- 需要先启动 Python 服务，再启动 Java 服务

打包时间: {timestamp}
"""
    
    readme_dest = os.path.join(temp_dir, "README.txt")
    with open(readme_dest, "w", encoding="utf-8") as f:
        f.write(readme_content.format(timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    print(f"创建 README 文件: {readme_dest}")
    
    return True

def create_zip_package(temp_dir, output_dir, project_root):
    print("\n=== 创建 ZIP 压缩包 ===")
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    zip_name = f"ACASB_Package_{timestamp}.zip"
    zip_path = os.path.join(output_dir, zip_name)
    
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_dir)
                zipf.write(file_path, arcname)
                print(f"添加文件: {arcname}")
    
    print(f"ZIP 压缩包创建成功: {zip_path}")
    
    file_size = os.path.getsize(zip_path) / (1024 * 1024)
    print(f"文件大小: {file_size:.2f} MB")
    
    return zip_path

def cleanup_temp_dir(temp_dir):
    print("\n=== 清理临时目录 ===")
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
        print(f"临时目录已删除: {temp_dir}")

def main():
    project_root = os.path.dirname(os.path.abspath(__file__))
    output_dir = project_root
    temp_dir = os.path.join(project_root, "temp_package")
    
    print("=" * 60)
    print("ACASB 一键打包脚本")
    print("=" * 60)
    print(f"项目根目录: {project_root}")
    print(f"输出目录: {output_dir}")
    
    try:
        if not build_java_project(project_root):
            print("\n打包失败: Java 项目构建失败")
            return False
        
        if not create_package_structure(project_root, temp_dir):
            print("\n打包失败: 创建目录结构失败")
            return False
        
        zip_path = create_zip_package(temp_dir, output_dir, project_root)
        
        cleanup_temp_dir(temp_dir)
        
        print("\n" + "=" * 60)
        print("打包完成!")
        print("=" * 60)
        print(f"输出文件: {zip_path}")
        print("\n使用说明:")
        print("1. 解压 ZIP 文件")
        print("2. 双击 start_python.bat 启动 Python 服务")
        print("3. 双击 start_java.bat 启动 Java 服务")
        print("=" * 60)
        
        return True
        
    except Exception as e:
        print(f"\n打包过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = main()
    input("\n按回车键退出...")
