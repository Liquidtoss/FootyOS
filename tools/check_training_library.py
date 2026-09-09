"""Validate every bundled animation's structure, motion, and seamless endpoint samples."""
import hashlib,json,struct,sys
from pathlib import Path
from verify_training3d import validate_glb
ROOT=Path(__file__).resolve().parents[1]
manifest=json.loads((ROOT/'assets/training3d/manifest.json').read_text())
reports=[]
for entry in manifest['exercises']:
    path=ROOT/entry['runtimePath'];data=path.read_bytes();validate_glb(data,['rep'])
    n=struct.unpack_from('<I',data,12)[0];doc=json.loads(data[20:20+n]);binary=data[28+n:]
    def values(index):
        acc=doc['accessors'][index];view=doc['bufferViews'][acc['bufferView']]
        width={'SCALAR':1,'VEC3':3,'VEC4':4}[acc['type']];start=view.get('byteOffset',0)+acc.get('byteOffset',0);stride=view.get('byteStride',width*4)
        return [struct.unpack_from('<'+'f'*width,binary,start+i*stride) for i in range(acc['count'])]
    motion=0;duration=0
    for clip in doc['animations']:
        for channel in clip['channels']:
            sampler=clip['samplers'][channel['sampler']];v=values(sampler['output']);duration=max(duration,values(sampler['input'])[-1][0])
            delta=max(abs(a-b) for a,b in zip(v[0],v[-1]))
            if channel['target']['path']=='rotation':delta=min(delta,max(abs(a+b) for a,b in zip(v[0],v[-1])))
            assert delta<1e-4,(entry['id'],'loop seam',channel['target'],delta)
            if any(max(abs(a-b) for a,b in zip(v[0],sample))>.0001 for sample in v[1:]):motion+=1
    assert motion>0,(entry['id'],'static animation')
    assert len(data)<=manifest['maxBytesPerAsset']
    assert hashlib.sha256(data).hexdigest()==entry['sha256']
    reports.append({'exercise':entry['id'],'bytes':len(data),'durationSeconds':duration,'movingChannels':motion,'loopSeamPass':True})
(ROOT/'assets/training3d/previews/library_validation.json').write_text(json.dumps(reports,indent=2)+'\n')
print(f'All {len(reports)} assets have real motion, matching loop endpoints, embedded resources and verified checksums.')
