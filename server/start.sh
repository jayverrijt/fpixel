#!/bin/bash
if [ -d "/opt/homebrew/opt/openjdk@25" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25"
elif [ -d "/opt/homebrew/opt/openjdk" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk"
fi

export PATH="$JAVA_HOME/bin:$PATH"

java -Xms2G -Xmx4G \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:+AlwaysPreTouch \
  -jar paper.jar --nogui
