import os, sys, shutil, subprocess, struct, time, re

BACKUP_COMMIT = '311856f'
BASE_DIR = '/home/jayv/fpixel'
SERVER_DIR = os.path.join(BASE_DIR, 'server')
WORLD_DIR = os.path.join(SERVER_DIR, 'world/dimensions/minecraft/survival')
SNAPSHOT_DIR = os.path.join(SERVER_DIR, 'world_safety_snapshots', f'pre_restore_{int(time.time())}')

TARGET_CHUNKS = [(cx, cz) for cx in range(-100, -95) for cz in range(17, 22)]
print(f"Target 5x5 chunk area ({len(TARGET_CHUNKS)} chunks):")
print(f"  Chunk X: -100 to -96 (Blocks { -100*16 } to { -96*16+15 })")
print(f"  Chunk Z: 17 to 21 (Blocks { 17*16 } to { 21*16+15 })")

# 1. Stop Minecraft container gracefully
print(">>> Stopping fpixel-minecraft container...")
subprocess.run(["docker", "stop", "fpixel-minecraft"], check=True)

# 2. Create safety snapshot
print(f">>> Creating safety snapshot in {SNAPSHOT_DIR}...")
os.makedirs(SNAPSHOT_DIR, exist_ok=True)
for sub in ['region', 'entities', 'poi']:
    sub_dir = os.path.join(SNAPSHOT_DIR, sub)
    os.makedirs(sub_dir, exist_ok=True)
    for r in ['r.-4.0.mca', 'r.-3.0.mca']:
        src = os.path.join(WORLD_DIR, sub, r)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(sub_dir, r))

player_yml = os.path.join(SERVER_DIR, 'plugins/FPixelCore/survival_data/2bf5868b-5810-305c-939b-022744b0e1ad.yml')
if os.path.exists(player_yml):
    shutil.copy2(player_yml, os.path.join(SNAPSHOT_DIR, '2bf5868b-5810-305c-939b-022744b0e1ad.yml'))

def read_mca_bytes(data_bytes):
    chunks = {}
    if len(data_bytes) < 8192:
        return chunks
    for i in range(1024):
        rel_x = i % 32
        rel_z = i // 32
        loc = data_bytes[i*4 : (i+1)*4]
        offset = int.from_bytes(loc[:3], 'big')
        count = loc[3]
        timestamp = int.from_bytes(data_bytes[4096 + i*4 : 4096 + (i+1)*4], 'big')
        if offset > 0 and count > 0:
            byte_start = offset * 4096
            length = int.from_bytes(data_bytes[byte_start : byte_start + 4], 'big')
            compression = data_bytes[byte_start + 4]
            compressed_data = data_bytes[byte_start + 5 : byte_start + 4 + length]
            chunks[(rel_x, rel_z)] = {
                'timestamp': timestamp,
                'compression': compression,
                'data': compressed_data
            }
    return chunks

def write_mca(filepath, chunks):
    locations = bytearray(4096)
    timestamps = bytearray(4096)
    sector_data = bytearray()
    
    current_sector = 2
    for i in range(1024):
        rel_x = i % 32
        rel_z = i // 32
        if (rel_x, rel_z) in chunks:
            chunk = chunks[(rel_x, rel_z)]
            c_data = chunk['data']
            comp = chunk['compression']
            ts = chunk['timestamp']
            
            payload_len = 1 + len(c_data)
            chunk_bytes = struct.pack('>IB', payload_len, comp) + c_data
            
            total_len = len(chunk_bytes)
            sector_count = (total_len + 4095) // 4096
            padding_needed = sector_count * 4096 - total_len
            chunk_bytes += b'\x00' * padding_needed
            
            loc_val = (current_sector << 8) | (sector_count & 0xFF)
            locations[i*4 : (i+1)*4] = loc_val.to_bytes(4, 'big')
            timestamps[i*4 : (i+1)*4] = ts.to_bytes(4, 'big')
            
            sector_data += chunk_bytes
            current_sector += sector_count
        else:
            locations[i*4 : (i+1)*4] = b'\x00\x00\x00\x00'
            timestamps[i*4 : (i+1)*4] = b'\x00\x00\x00\x00'
            
    tmp_path = filepath + '.tmp'
    with open(tmp_path, 'wb') as f:
        f.write(locations)
        f.write(timestamps)
        f.write(sector_data)
    os.replace(tmp_path, filepath)

# 3. Perform chunk merge for region, entities, and poi
mca_configs = [
    ('r.-4.0.mca', -4, 0),
    ('r.-3.0.mca', -3, 0)
]

for sub in ['region', 'entities', 'poi']:
    for mca_name, rx, rz in mca_configs:
        git_path = f"dimensions/minecraft/survival/{sub}/{mca_name}"
        live_file = os.path.join(WORLD_DIR, sub, mca_name)
        if not os.path.exists(live_file):
            print(f"Skipping {live_file} (not present)")
            continue
            
        print(f">>> Merging {sub}/{mca_name}...")
        res = subprocess.run(
            ["git", "-C", BASE_DIR, "show", f"{BACKUP_COMMIT}:{git_path}"],
            stdout=subprocess.PIPE, check=True
        )
        backup_bytes = res.stdout
        with open(live_file, 'rb') as f:
            live_bytes = f.read()
            
        b_chunks = read_mca_bytes(backup_bytes)
        l_chunks = read_mca_bytes(live_bytes)
        
        merged = dict(l_chunks)
        restored_count = 0
        
        for cx, cz in TARGET_CHUNKS:
            if (cx >> 5) == rx and (cz >> 5) == rz:
                rel = (cx & 31, cz & 31)
                if rel in b_chunks:
                    merged[rel] = b_chunks[rel]
                    restored_count += 1
                else:
                    if rel in merged:
                        del merged[rel]
                        restored_count += 1
                        
        print(f"  -> Restored {restored_count} chunks in {sub}/{mca_name}")
        write_mca(live_file, merged)

# 4. Fix Thomas's player profile location so he does not spawn in void
if os.path.exists(player_yml):
    with open(player_yml, 'r') as f:
        content = f.read()
    content_fixed = re.sub(r'y:\s*-0\.12325[0-9]*', 'y: 71.25402572284952', content)
    content_fixed = re.sub(r'x:\s*-1575\.5499[0-9]*', 'x: -1574.5248206496879', content_fixed)
    with open(player_yml, 'w') as f:
        f.write(content_fixed)
    print(">>> Fixed Thomas's player profile coordinates to safe ground (y=71.25).")

# 5. Fix permissions
subprocess.run(["chown", "-R", "1000:1000", "/home/jayv/fpixel/server"], check=False)

# 6. Start Minecraft server
print(">>> Starting fpixel-minecraft container...")
subprocess.run(["docker", "start", "fpixel-minecraft"], check=True)

print(">>> SUCCESS! Restore completed.")
