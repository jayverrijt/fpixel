#!/bin/bash

echo "Starting Minecraft Network..."

# Proxy
echo "Starting Proxy..."
cd proxy
screen -dmS proxy java -Xms1G -Xmx1G -jar velocity.jar
cd ..

sleep 2

# Lobby
echo "Starting Lobby..."
cd lobby
screen -dmS lobby java -Xms1G -Xmx2G -jar paper.jar nogui
cd ..

sleep 2

# Duels
echo "Starting Duels..."
cd duels
screen -dmS duels java -Xms2G -Xmx4G -jar paper.jar nogui
cd ..

sleep 2

# SkyWars
echo "Starting SkyWars..."
cd skywars
screen -dmS skywars java -Xms1G -Xmx2G -jar paper.jar nogui
cd ..

sleep 2

# BedWars
echo "Starting BedWars..."
cd bedwars
screen -dmS bedwars java -Xms1G -Xmx2G -jar paper.jar nogui
cd ..

echo "All servers started!"
