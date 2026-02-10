import os
import sys
import io
import shutil
import zipfile
from datetime import datetime

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def create_package_structure(project_root, temp_dir):
    print("\n=== 创建打包目录结构 ===")
    
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
    os.makedirs(temp_dir, exist_ok=True)
    
    jar_source = os.path.join(project_root, "target", "ACASB-0.0.1-SNAPSHOT.jar")
    if not os.path.exists(jar_source):
        print(f"错误: 找不到 JAR 文件: {jar_source}")
        print("请先执行 Maven 构建命令 (mvn package)")
        return False
    
    jar_dest = os.path.join(temp_dir, "ACASB.jar")
    shutil.copy2(jar_source, jar_dest)
    print(f"复制 JAR 文件: {jar_dest}")
    
    analysis_source = os.path.join(project_root, "acasb-analysis")
    analysis_dest = os.path.join(temp_dir, "acasb-analysis")
    shutil.copytree(analysis_source, analysis_dest, ignore=shutil.ignore_patterns('__pycache__'))
    print(f"复制 acasb-analysis 目录: {analysis_dest}")
    
    for script in ["start_java.bat", "start_python.bat"]:
        src = os.path.join(project_root, script)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(temp_dir, script))
            print(f"复制启动脚本: {script}")
        else:
            print(f"警告: 找不到脚本 {script}，跳过")
    
    frontend_source = os.path.join(project_root, "acasb-frontend", "index.html")
    if os.path.exists(frontend_source):
        frontend_dest = os.path.join(temp_dir, "index.html")
        shutil.copy2(frontend_source, frontend_dest)
        print(f"复制前端文件: {frontend_dest}")
    else:
        print("警告: 找不到前端文件 index.html")
    
    config_source = os.path.join(project_root, "config.properties")
    if os.path.exists(config_source):
        config_dest = os.path.join(temp_dir, "config.properties")
        shutil.copy2(config_source, config_dest)
        print(f"复制配置文件: {config_dest}")
    else:
        print("警告: 找不到配置文件 config.properties")
    
    create_readme(temp_dir)
    
    return True

def create_readme(temp_dir):
    readme_content = """ACASB - Ancient Chinese Architecture Style Recognition System
========================================================
打包时间: {timestamp}

## 使用说明

### 1. 配置数据库
编辑 config.properties 文件，修改数据库配置：
- db.host: 数据库地址
- db.port: 数据库端口
- db.name: 数据库名称
- db.username: 数据库用户名
- db.password: 数据库密码

### 2. 启动后端服务
双击 start.bat 启动后端服务

### 3. 启动 Python 服务
进入 acasb-analysis 目录，运行：
python api_server.py

### 4. 访问前端
双击 index.html 在浏览器中打开

### 5. API 文档
查看 API_DOCUMENTATION.md 了解接口使用方法

## 注意事项
- 确保 Java 17+ 已安装
- 确保 Python 3.8+ 已安装
- 确保数据库服务已启动
- 确保端口 8080 和 5000 未被占用

## 目录结构
- ACASB.jar (后端 JAR 包)
- config.properties (配置文件)
- start.bat (启动脚本)
- index.html (前端页面)
- acasb-analysis/ (Python 分析代码)
- datasets/ (数据集)
- README.txt (本文件)
"""
    readme_dest = os.path.join(temp_dir, "README.txt")
    with open(readme_dest, "w", encoding="utf-8") as f:
        f.write(readme_content.format(timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    print(f"创建 README 文件: {readme_dest}")

def copy_test_images(project_root, temp_dir):
    print("\n=== 复制测试图片 ===")
    
    image_count = 0
    for i in range(1, 11):
        image_name = f"{i}.jpg"
        image_source = os.path.join(project_root, image_name)
        if os.path.exists(image_source):
            image_dest = os.path.join(temp_dir, image_name)
            shutil.copy2(image_source, image_dest)
            print(f"复制测试图片: {image_name}")
            image_count += 1
        else:
            print(f"警告: 找不到测试图片 {image_name}")
    
    print(f"共复制 {image_count} 张测试图片")
    return image_count

def copy_datasets(project_root, temp_dir):
    print("\n=== 复制数据集 ===")
    
    datasets_source = os.path.join(project_root, "datasets")
    if os.path.exists(datasets_source):
        datasets_dest = os.path.join(temp_dir, "datasets")
        shutil.copytree(datasets_source, datasets_dest, ignore=shutil.ignore_patterns('__pycache__'))
        print(f"复制数据集目录: {datasets_dest}")
    else:
        print("警告: 找不到数据集目录")

def copy_api_documentation(project_root, temp_dir):
    print("\n=== 复制 API 文档 ===")
    
    api_doc_source = os.path.join(project_root, "API_DOCUMENTATION.md")
    if os.path.exists(api_doc_source):
        api_doc_dest = os.path.join(temp_dir, "API_DOCUMENTATION.md")
        shutil.copy2(api_doc_source, api_doc_dest)
        print(f"复制 API 文档: {api_doc_dest}")
    else:
        print("警告: 找不到 API 文档")

def cleanup_old_packages(project_root):
    print("\n=== 清理旧的打包文件 ===")
    
    package_dirs = []
    for item in os.listdir(project_root):
        if item.startswith("ACASB_Package_") and os.path.isdir(os.path.join(project_root, item)):
            package_dirs.append(os.path.join(project_root, item))
    
    if package_dirs:
        for package_dir in package_dirs:
            print(f"删除旧打包目录: {package_dir}")
            shutil.rmtree(package_dir)
        print(f"共删除 {len(package_dirs)} 个旧打包目录")
    else:
        print("没有找到旧的打包目录")

def create_zip_package(temp_dir, output_dir):
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
    
    print(f"ZIP 创建成功: {zip_path}")
    return zip_path

def main():
    project_root = os.getcwd()
    temp_dir = os.path.join(project_root, "temp_package")
    
    print("=" * 60)
    print("ACASB 打包工具")
    print("=" * 60)
    
    try:
        cleanup_old_packages(project_root)
        
        if not create_package_structure(project_root, temp_dir):
            return False
        
        copy_test_images(project_root, temp_dir)
        copy_datasets(project_root, temp_dir)
        copy_api_documentation(project_root, temp_dir)
        
        zip_path = create_zip_package(temp_dir, project_root)
        
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
        
        print(f"\n打包完成: {zip_path}")
        print(f"文件大小: {os.path.getsize(zip_path) / 1024 / 1024:.2f} MB")
        return True
        
    except Exception as e:
        print(f"\n错误: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    if not main():
        sys.exit(1)
