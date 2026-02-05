import os
import shutil
from PIL import Image
import re

# ================= 配置区 =================
# 目标尺寸
TARGET_SIZE = (400, 400) 
# 输出文件夹名（自动创建，防止覆盖原图造成事故）
OUTPUT_DIR = "dataset_fixed"
# 支持的图片格式
VALID_EXTS = ('.jpg', '.jpeg', '.png', '.webp', '.bmp')
# =========================================

def natural_sort_key(s):
    """
    自然排序算法：让 1.jpg, 2.jpg, 10.jpg 按数字顺序排，而不是 1, 10, 2
    """
    return [int(text) if text.isdigit() else text.lower()
            for text in re.split(r'(\d+)', s)]

def process_images():
    # 1. 获取当前目录下所有图片
    current_dir = os.path.dirname(os.path.abspath(__file__))
    images = [
        f for f in os.listdir(current_dir) 
        if f.lower().endswith(VALID_EXTS)
    ]
    
    # 2. 关键步骤：按自然顺序排序
    # 这样能保证如果你原图是按顺序拍的，重命名后依然保持那个顺序
    images.sort(key=natural_sort_key)
    
    if not images:
        print("❌ 当前目录下没有找到图片！")
        return

    # 3. 创建输出目录
    output_path = os.path.join(current_dir, OUTPUT_DIR)
    if not os.path.exists(output_path):
        os.makedirs(output_path)
        print(f"📁 创建输出目录: {OUTPUT_DIR}")
    else:
        print(f"⚠️ 输出目录 {OUTPUT_DIR} 已存在，新图片将存入其中...")

    print(f"🔍 找到 {len(images)} 张图片，准备处理...")
    print("-" * 30)

    # 4. 循环处理
    success_count = 0
    # enumerate(images, 1) 让编号从 1 开始 (1, 2, 3...)
    # 这步操作自动解决了“缺失编号”的问题，因为它是强制连续计数的
    for new_index, filename in enumerate(images, 1):
        try:
            file_path = os.path.join(current_dir, filename)
            
            with Image.open(file_path) as img:
                # --- 转换颜色模式 ---
                # 如果是 .png (RGBA) 转 .jpg (RGB)，必须丢弃透明通道
                if img.mode in ("RGBA", "P"):
                    img = img.convert("RGB")
                
                # --- 强制缩放 ---
                # 使用 LANCZOS 滤镜保证缩放后的清晰度
                img_resized = img.resize(TARGET_SIZE, Image.Resampling.LANCZOS)
                
                # --- 生成新文件名 ---
                # 格式：1.jpg, 2.jpg ...
                new_name = f"{new_index}.jpg"
                save_path = os.path.join(output_path, new_name)
                
                # --- 保存 ---
                # quality=95 保证训练数据质量
                img_resized.save(save_path, "JPEG", quality=95)
                
                print(f"✅ [{new_index}] {filename} -> {TARGET_SIZE} -> {new_name}")
                success_count += 1
                
        except Exception as e:
            print(f"❌ 处理 {filename} 失败: {e}")

    print("-" * 30)
    print(f"🎉 处理完成！")
    print(f"📊 共处理: {len(images)} 张")
    print(f"✅ 成功: {success_count} 张")
    print(f"📂 新图片保存在: ./{OUTPUT_DIR}/")
    print("💡 建议检查新文件夹，确认无误后可替换原图。")

if __name__ == "__main__":
    process_images()
