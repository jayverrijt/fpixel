#!/bin/bash
set -e

cd /data

# Accept EULA
echo "eula=true" > eula.txt

# Ensure paper.jar exists
if [ ! -f paper.jar ] || [ $(stat -c%s paper.jar 2>/dev/null || echo 0) -lt 40000000 ]; then
  if [ -f "/shared/fpixel/survival/paper.jar" ]; then
    echo "Copying bootstrap paper.jar from /shared/fpixel..."
    cp "/shared/fpixel/survival/paper.jar" paper.jar
  elif [ -f "/home/jayv/fpixel-af2727b702412a9bbd1b5ac3af6817f9107ac91b/survival/paper.jar" ]; then
    echo "Copying bootstrap paper.jar from fpixel-af2727..."
    cp "/home/jayv/fpixel-af2727b702412a9bbd1b5ac3af6817f9107ac91b/survival/paper.jar" paper.jar
  fi
fi

# Clean up stale session.locks if container previously stopped unexpectedly
rm -f /data/world/session.lock /data/Urban/session.lock /data/Galaxy/session.lock /data/arenas/session.lock /data/survival/session.lock 2>/dev/null || true

JAVA_MEMORY="${JAVA_MEMORY:-4G}"
JAVA_INIT_MEMORY="${JAVA_INIT_MEMORY:-2G}"

echo "=========================================="
echo " Starting FPixel Minecraft Server (Docker)"
echo " Java Version: $(java -version 2>&1 | head -n 1)"
echo " Memory:       ${JAVA_INIT_MEMORY} -> ${JAVA_MEMORY}"
echo " Enforced Host: mc.fpixel.nl"
echo " Ports:        25565 (Java), 19132/udp (Bedrock)"
echo "=========================================="

exec java -Dterminal.jline=false -Dterminal.ansi=true \
  -Xms"${JAVA_INIT_MEMORY}" -Xmx"${JAVA_MEMORY}" \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:+AlwaysPreTouch \
  -jar paper.jar --nogui
