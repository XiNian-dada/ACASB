import os
import sys
import io
import shutil
import zipfile
from datetime import datetime

# 设置标准输出编码，防止 Windows 下中文乱码
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def create_package_structure(project_root, temp_dir):
    print("\n=== 创建打包目录结构 ===")
    
    # 1. 确保临时目录存在，如果存在则先清理
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
    os.makedirs(temp_dir, exist_ok=True)
    
    # 2. 检查并复制 JAR 文件 (关键：这里假设外部已经构建好了 JAR)
    jar_source = os.path.join(project_root, "target", "ACASB-0.0.1-SNAPSHOT.jar")
    if not os.path.exists(jar_source):
        print(f"错误: 找不到 JAR 文件: {jar_source}")
        print("请先执行 Maven 构建命令 (mvn package)")
        return False

    jar_dest = os.path.join(temp_dir, "ACASB-0.0.1-SNAPSHOT.jar")
    shutil.copy2(jar_source, jar_dest)
    print(f"复制 JAR 文件: {jar_dest}")
    
    # 3. 复制分析目录
    analysis_source = os.path.join(project_root, "acasb-analysis")
    analysis_dest = os.path.join(temp_dir, "acasb-analysis")
    # 忽略 __pycache__ 文件夹
    shutil.copytree(analysis_source, analysis_dest, ignore=shutil.ignore_patterns('__pycache__'))
    print(f"复制 acasb-analysis 目录: {analysis_dest}")
    
    # 4. 复制启动脚本 (容错处理：如果没有就不复制)
    for script in ["start_java.bat", "start_python.bat"]:
        src = os.path.join(project_root, script)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(temp_dir, script))
            print(f"复制启动脚本: {script}")
        else:
            print(f"警告: 找不到脚本 {script}，跳过")
    
    # 5. 生成 README
    create_readme(temp_dir)
    
    return True

def create_readme(temp_dir):
    readme_content = """ACASB - Ancient Chinese Architecture in Spring Boot
========================================================
打包时间: {timestamp}

启动说明:
1. 启动 Python API: 双击 start_python.bat
2. 启动 Java 后端:  双击 start_java.bat

依赖环境:
- Java 17+
- Python 3.11+
"""
    readme_dest = os.path.join(temp_dir, "README.txt")
    with open(readme_dest, "w", encoding="utf-8") as f:
        f.write(readme_content.format(timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    print(f"创建 README 文件: {readme_dest}")

def create_zip_package(temp_dir, output_dir):
    print("\n=== 创建 ZIP 压缩包 ===")
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    zip_name = f"ACASB_Package_{timestamp}.zip"
    zip_path = os.path.join(output_dir, zip_name)
    
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                file_path = os.path.join(root, file)
                # 保持压缩包内的相对路径结构
                arcname = os.path.relpath(file_path, temp_dir)
                zipf.write(file_path, arcname)
                print(f"添加文件: {arcname}")
    
    print(f"ZIP 创建成功: {zip_path}")
    return zip_path

def main():
    project_root = os.getcwd() # 在 CI 中通常这就是项目根目录
    temp_dir = os.path.join(project_root, "temp_package")
    
    print("=" * 60)
    print("ACASB 打包工具 (CI Mode)")
    print("=" * 60)
    
    try:
        # 不再执行 Maven 构建，直接打包
        if not create_package_structure(project_root, temp_dir):
            return False
        
        zip_path = create_zip_package(temp_dir, project_root)
        
        # 清理临时目录
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
            
        print(f"\n打包完成: {zip_path}")
        return True
        
    except Exception as e:
        print(f"\n错误: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    if not main():
        sys.exit(1) # 显式返回错误码给 CI