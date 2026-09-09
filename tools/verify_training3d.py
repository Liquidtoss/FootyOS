#!/usr/bin/env python3
"""Check the intake inventory and reviewed GLB assets; does not certify exercise form."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]


def local_file(relative):
    path = (ROOT / relative).resolve()
    if not path.is_relative_to(ROOT) or not path.is_file():
        raise ValueError(f"Missing or out-of-repository file: {relative}")
    return path


def validate_glb(data, required_clips):
    if len(data) < 20:
        raise ValueError("Truncated GLB")
    magic, version, length = struct.unpack_from('<4sII', data)
    if magic != b'glTF' or version != 2 or length != len(data):
        raise ValueError("Invalid GLB v2 header")
    offset, chunks = 12, []
    while offset < len(data):
        if offset + 8 > len(data):
            raise ValueError("Truncated GLB chunk header")
        size, kind = struct.unpack_from('<II', data, offset)
        offset += 8
        if size % 4 or offset + size > len(data):
            raise ValueError("Invalid GLB chunk length")
        chunks.append((kind, data[offset:offset + size]))
        offset += size
    if not chunks or chunks[0][0] != 0x4E4F534A:
        raise ValueError("GLB must start with JSON")
    doc = json.loads(chunks[0][1])
    if doc.get('asset', {}).get('version') != '2.0':
        raise ValueError("Expected glTF 2.0")
    if not doc.get('meshes') or not doc.get('skins'):
        raise ValueError("Expected a skinned mesh, not a static or primitive placeholder")
    for resource in doc.get('buffers', []) + doc.get('images', []):
        uri = resource.get('uri', '')
        if uri and not uri.startswith('data:'):
            raise ValueError("External resources are not allowed; embed them in the GLB")
    clips = {clip.get('name'): clip for clip in doc.get('animations', [])}
    for name in required_clips:
        clip = clips.get(name)
        if not clip or not clip.get('channels') or not clip.get('samplers'):
            raise ValueError(f"Missing animated clip: {name}")
    # Structural intake checks only. Use the Khronos validator plus visual review
    # for accessor correctness, actual motion, contact, skinning and loop quality.


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--require-ready', action='store_true', help='Fail until every exercise is reviewed')
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'assets/training3d/manifest.json').read_text())
    entries = manifest['exercises']
    catalog = (ROOT / 'app/src/main/java/app/footyos/domain/ExerciseCatalog.kt').read_text()
    expected = set(re.findall(r'id = "([^"]+)"', catalog))
    ids = [entry['id'] for entry in entries]
    if len(ids) != len(set(ids)) or set(ids) != expected:
        raise ValueError("Manifest must contain each catalog exercise exactly once")
    ready = 0
    prototypes = 0
    for entry in entries:
        if entry['status'] == 'pending':
            continue
        if entry['status'] not in {'reviewed', 'prototype'}:
            raise ValueError(f"Unknown status for {entry['id']}")
        if not entry.get('sourceUrl') or not entry.get('reviewEvidence'):
            raise ValueError(f"Missing provenance/review evidence for {entry['id']}")
        local_file(entry['licensePath'])
        local_file(entry['reviewEvidence'])
        data = local_file(entry['runtimePath']).read_bytes()
        if len(data) > manifest['maxBytesPerAsset']:
            raise ValueError(f"Asset exceeds size budget: {entry['id']}")
        if hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise ValueError(f"Checksum mismatch: {entry['id']}")
        validate_glb(data, entry['requiredClips'])
        if entry['status'] == 'reviewed': ready += 1
        else: prototypes += 1
    print(f"Inventory valid: {len(entries)} exercises; {ready} reviewed GLBs; {prototypes} prototypes; {len(entries) - ready - prototypes} pending.")
    if args.require_ready and ready != len(entries):
        raise ValueError("Library is not ready for full 3D rollout")


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)
