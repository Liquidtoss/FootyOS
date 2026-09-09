"""Combine Blender's per-object animation clips into one synchronized exercise clip."""
import json
from pathlib import Path
import struct


def merge_animations(path, name='rep'):
    path=Path(path);data=path.read_bytes()
    magic,version,length=struct.unpack_from('<4sII',data)
    if magic!=b'glTF' or version!=2 or length!=len(data):raise ValueError('Invalid GLB')
    size,kind=struct.unpack_from('<II',data,12)
    if kind!=0x4E4F534A:raise ValueError('Missing JSON chunk')
    doc=json.loads(data[20:20+size]);merged={'name':name,'channels':[],'samplers':[]}
    targets=set()
    for clip in doc.get('animations',[]):
        offset=len(merged['samplers'])
        for channel in clip['channels']:
            target=(channel['target'].get('node'),channel['target'].get('path'))
            if target in targets:raise ValueError('Conflicting animation tracks')
            targets.add(target)
            merged['channels'].append({**channel,'sampler':channel['sampler']+offset})
        merged['samplers'].extend(clip['samplers'])
    if not merged['channels']:raise ValueError('No animation tracks')
    doc['animations']=[merged]
    encoded=json.dumps(doc,separators=(',',':')).encode()
    encoded+=b' '*((-len(encoded))%4)
    tail=data[20+size:]
    result=struct.pack('<4sII',b'glTF',2,20+len(encoded)+len(tail))+struct.pack('<II',len(encoded),kind)+encoded+tail
    path.write_bytes(result)


if __name__=='__main__':
    import sys
    merge_animations(sys.argv[1])
