#!/usr/bin/env python3
import urllib.request
import json
import os
import sys

SERVER_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "server"))
PLUGINS_DIR = os.path.join(SERVER_DIR, "plugins")

os.makedirs(SERVER_DIR, exist_ok=True)
os.makedirs(PLUGINS_DIR, exist_ok=True)

USER_AGENT = "FPixel-Server-Installer/1.0.0 (contact@fpixel.net)"

def download_file(url, target_path):
    print(f"Downloading {url} -> {target_path}...")
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req) as resp, open(target_path, "wb") as f:
        total = int(resp.headers.get("Content-Length", 0))
        downloaded = 0
        chunk_size = 1024 * 1024
        while True:
            chunk = resp.read(chunk_size)
            if not chunk:
                break
            f.write(chunk)
            downloaded += len(chunk)
            if total > 0:
                percent = (downloaded / total) * 100
                sys.stdout.write(f"\rProgress: {percent:.1f}% ({downloaded // (1024*1024)}MB / {total // (1024*1024)}MB)")
                sys.stdout.flush()
            else:
                sys.stdout.write(f"\rProgress: {downloaded // (1024*1024)}MB downloaded")
                sys.stdout.flush()
    print("\nDownload complete!")

def fetch_paper():
    print("Fetching Paper 1.21.4 (build 232)...")
    paper_url = "https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar"
    paper_jar = os.path.join(SERVER_DIR, "paper.jar")
    download_file(paper_url, paper_jar)

def fetch_geyser():
    print("Fetching Geyser-Spigot...")
    geyser_url = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
    geyser_jar = os.path.join(PLUGINS_DIR, "Geyser-Spigot.jar")
    download_file(geyser_url, geyser_jar)

def fetch_floodgate():
    print("Fetching Floodgate-Spigot...")
    floodgate_url = "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"
    floodgate_jar = os.path.join(PLUGINS_DIR, "floodgate-spigot.jar")
    download_file(floodgate_url, floodgate_jar)

def setup_server_files():
    print("Configuring server files (EULA, properties, start.sh)...")
    # EULA
    with open(os.path.join(SERVER_DIR, "eula.txt"), "w") as f:
        f.write("eula=true\n")
    
    # server.properties
    props = """# Minecraft Server Properties for FPixel
server-port=25565
server-ip=
online-mode=false
motd=\\u00a76\\u00a7lFPIXEL NETWORK \\u00a77\\u00bb \\u00a7eMinigames & BedWars\\n\\u00a7b100% Server Sided \\u00a77| \\u00a7aCrossplay Bedrock+Java
difficulty=normal
spawn-protection=0
allow-flight=true
gamemode=adventure
force-gamemode=true
pvp=true
max-players=100
view-distance=10
simulation-distance=8
enable-command-block=true
level-name=world
"""
    with open(os.path.join(SERVER_DIR, "server.properties"), "w") as f:
        f.write(props)

    # start.sh
    start_sh = """#!/bin/bash
export JAVA_HOME="/opt/homebrew/opt/openjdk"
export PATH="$JAVA_HOME/bin:$PATH"

java -Xms2G -Xmx4G \\
  -XX:+UseG1GC \\
  -XX:+ParallelRefProcEnabled \\
  -XX:MaxGCPauseMillis=200 \\
  -XX:+UnlockExperimentalVMOptions \\
  -XX:+DisableExplicitGC \\
  -XX:+AlwaysPreTouch \\
  -jar paper.jar --nogui
"""
    start_path = os.path.join(SERVER_DIR, "start.sh")
    with open(start_path, "w") as f:
        f.write(start_sh)
    os.chmod(start_path, 0o755)

if __name__ == "__main__":
    fetch_paper()
    fetch_geyser()
    fetch_floodgate()
    setup_server_files()
    print("Server installation completed successfully!")
