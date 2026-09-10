"""Extract the CC0 forearm reference. Blender --python this_file -- base.obj default.mhskel"""
import bpy,json,sys
from mathutils import Vector
from pathlib import Path
source,rig_source=sys.argv[sys.argv.index('--')+1:]
verts=[];faces=[];group=''
for l in open(source):
 if l.startswith('v '):verts.append(Vector(map(float,l.split()[1:])))
 elif l.startswith('g '):group=l.strip()[2:]
 elif l.startswith('f ') and group=='body':faces.append([int(i.split('/')[0])-1 for i in l.split()[1:]])
j=json.load(open(rig_source))['joints']
def joint(name):return sum((verts[i] for i in j[name]),Vector())/len(j[name])
e=joint('lowerarm01.L____head');w=joint('lowerarm02.L____tail');axis=(w-e).normalized();length=(w-e).length
n=Vector((0,0,1));n=(n-axis*n.dot(axis)).normalized();x=n.cross(axis)
coords=[Vector(((v-e).dot(x)/length,(v-e).dot(n)/length,(v-e).dot(axis)/length)) for v in verts]
selected=[f for f in faces if all(verts[i].x>2 and -.25<coords[i].z<1.22 for i in f)]
indices=sorted({i for f in selected for i in f});lookup={old:i for i,old in enumerate(indices)}
data={'source':'https://raw.githubusercontent.com/makehumancommunity/makehuman/master/makehuman/data/3dobjs/base.obj','vertices':[list(coords[i]) for i in indices],'faces':[[lookup[i] for i in f] for f in selected]}
Path(__file__).with_name('makehuman_forearm.json').write_text(json.dumps(data,separators=(',',':'))+'\n')
print(len(indices),len(selected))
