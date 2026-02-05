import os
import time
import glob
from pathlib import Path
from PIL import Image

# ================= 配置区 =================
# 自动获取脚本文件所在的绝对路径
# 这样你把脚本放在哪里，它就监听哪里
WATCH_DIR = os.path.dirname(os.path.abspath(__file__))

TARGET_EXT = ".jpg"   # 目标格式
SOURCE_EXT = ".webp"  # 源格式
CHECK_INTERVAL = 0.2    # 扫描间隔(秒)
# =========================================

def get_next_filename(directory):
    """
    智能计算下一个文件名：
    扫描目录下所有纯数字命名的 jpg (如 1.jpg, 10.jpg)，
    找到最大值并 +1。
    """
    # 查找所有目标格式文件
    existing_files = glob.glob(os.path.join(directory, f"*{TARGET_EXT}"))
    max_num = 0
    for f in existing_files:
        try:
            # 提取文件名（不带后缀）
            name = Path(f).stem
            # 只有当文件名是纯数字时才纳入计算
            if name.isdigit():
                num = int(name)
                if num > max_num:
                    max_num = num
        except ValueError:
            continue
    
    # 返回下一个编号的完整路径
    return os.path.join(directory, f"{max_num + 1}{TARGET_EXT}")

def convert_webp_to_jpg(webp_path):
    try:
        # 1. 稍微等待，防止文件还在下载中被占用
        time.sleep(0.3)
        
        # 2. 生成新文件名 (例如 1.jpg)
        new_filename = get_next_filename(WATCH_DIR)
        
        # 3. 打开并转换
        with Image.open(webp_path) as img:
            # 关键：WebP 若带透明通道(RGBA)，转 JPG 必须先转 RGB
            if img.mode in ("RGBA", "P"):
                img = img.convert("RGB")
            
            # 保存为高质量 JPG
            img.save(new_filename, "JPEG", quality=95)
        
        print(f"✅ [转换成功] {os.path.basename(webp_path)} -> {os.path.basename(new_filename)}")
        
        # 4. 只有转换成功后才删除原文件，防止数据丢失
        os.remove(webp_path)

    except OSError:
        # 文件被占用时的处理（如下载未完成）
        pass 
    except Exception as e:
        print(f"❌ [错误] {os.path.basename(webp_path)}: {e}")

def main():
    print("=" * 40)
    print(f"🚀 监控启动！")
    print(f"📂 正在监听当前文件夹: {WATCH_DIR}")
    print(f"🎯 发现 {SOURCE_EXT} 会自动转为 {TARGET_EXT}")
    print("=" * 40)

    try:
        while True:
            # 扫描当前目录下的 webp 文件
            files = [
                os.path.join(WATCH_DIR, f) 
                for f in os.listdir(WATCH_DIR) 
                if f.lower().endswith(SOURCE_EXT)
            ]
            
            for f in files:
                convert_webp_to_jpg(f)
            
            time.sleep(CHECK_INTERVAL)
            
    except KeyboardInterrupt:
        print("\n👋 停止监控。")

if __name__ == "__main__":
    main()