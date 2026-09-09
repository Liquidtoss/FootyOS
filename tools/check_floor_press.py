import sys,struct,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from verify_training3d import validate_glb
p=ROOT/'app/src/main/assets/training3d/floor_press.glb';data=p.read_bytes();validate_glb(data,['rep'])
n=struct.unpack_from('<I',data,12)[0];doc=json.loads(data[20:20+n]);buffers=data[28+n:]
triangles=sum(doc['accessors'][p['indices']]['count']//3 for m in doc['meshes'] for p in m['primitives'] if 'indices' in p)
errors=[];lengths=[]
for a in doc['animations']:
 for c in a['channels']:
  sm=a['samplers'][c['sampler']]; acc=doc['accessors'][sm['input']];lengths+=acc.get('max',[])
  acc=doc['accessors'][sm['output']];view=doc['bufferViews'][acc['bufferView']];num={'VEC3':3,'VEC4':4}[acc['type']];offset=view.get('byteOffset',0)+acc.get('byteOffset',0);stride=view.get('byteStride',4*num)
  first=struct.unpack_from('<'+'f'*num,buffers,offset);last=struct.unpack_from('<'+'f'*num,buffers,offset+(acc['count']-1)*stride)
  delta=max(abs(x-y) for x,y in zip(first,last))
  if c['target']['path']=='rotation':delta=min(delta,max(abs(x+y) for x,y in zip(first,last)))
  if delta>1e-5:errors.append((c['target'],delta))
result={'bytes':len(data),'triangles':triangles,'sha256':hashlib.sha256(data).hexdigest(),'skins':len(doc['skins']),'clips':[a['name'] for a in doc['animations']],'animationChannels':len(doc['animations'][0]['channels']),'durationSeconds':max(lengths),'loopEndpointErrors':errors}
print(json.dumps(result,indent=2));(ROOT/'assets/training3d/previews/floor_press_validation.json').write_text(json.dumps(result,indent=2)+'\n')
assert not errors
