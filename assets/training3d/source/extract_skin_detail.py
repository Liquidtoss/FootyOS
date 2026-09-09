"""Extract sculpt deltas from the licensed Blender Studio v1.0 bundle.
Run with Blender --python extract_skin_detail.py -- /path/to/bundle.blend.
The topology must match the original 15,093-vertex character source.
"""
import bpy, struct, sys
from pathlib import Path
source=sys.argv[sys.argv.index('--')+1]
with bpy.data.libraries.load(source) as (s,d):d.objects=['GEO-body_male_realistic']
o=d.objects[0];bpy.context.collection.objects.link(o);m=o.modifiers[0];m.levels=1;m.show_viewport=True
bpy.context.view_layer.update();dep=bpy.context.evaluated_depsgraph_get();hi=o.evaluated_get(dep).to_mesh();coords=[v.co.copy() for v in hi.vertices];o.evaluated_get(dep).to_mesh_clear()
o.modifiers.clear();m=o.modifiers.new('Subdivision','SUBSURF');m.levels=1
bpy.context.view_layer.update();lo=o.evaluated_get(dep).to_mesh();assert len(coords)==len(lo.vertices),(len(coords),len(lo.vertices))
deltas=[a-v.co for a,v in zip(coords,lo.vertices)];print('DETAIL',len(deltas),max(v.length for v in deltas))
p=Path(__file__).with_name('skin_detail.bin');p.write_bytes(struct.pack('<I',len(deltas))+b''.join(struct.pack('<3f',*v) for v in deltas))
