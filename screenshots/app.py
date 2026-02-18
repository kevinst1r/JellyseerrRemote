import os
from PIL import Image

supported_extensions = (".jpg", ".jpeg", ".webp", ".bmp", ".tiff", ".tif")

for filename in os.listdir():
    if filename.lower().endswith(supported_extensions):
        name_without_ext = os.path.splitext(filename)[0]
        output_filename = name_without_ext + ".png"

        try:
            with Image.open(filename) as img:
                img.convert("RGBA").save(output_filename, "PNG")
            print(f"Converted: {filename}")
        except Exception as e:
            print(f"Failed: {filename} — {e}")

print("Done.")
