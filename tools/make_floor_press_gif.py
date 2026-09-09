"""Convert Blender preview frames to a four-second looping GIF using Pillow."""
from pathlib import Path
from PIL import Image

root=Path(__file__).resolve().parents[1]
preview=root/'assets/training3d/previews'
files=sorted((preview/'frames').glob('rep_*.png'))
if len(files)!=60: raise ValueError(f'Expected 60 rendered frames, found {len(files)}')
frames=[Image.open(p).convert('RGB') for p in files]
palette=frames[0].quantize(colors=256)
images=[frame.quantize(palette=palette,dither=Image.Dither.NONE) for frame in frames]
images[0].save(preview/'floor_press.gif',save_all=True,append_images=images[1:],duration=[60,70,70]*20,loop=0,optimize=False,disposal=1)
print(preview/'floor_press.gif')
