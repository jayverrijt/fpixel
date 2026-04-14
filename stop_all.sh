#!/bin/bash

echo "Stopping all servers..."

screen -S proxy -X stuff "stop$(printf \\r)"
screen -S lobby -X stuff "stop$(printf \\r)"
screen -S duels -X stuff "stop$(printf \\r)"
screen -S skywars -X stuff "stop$(printf \\r)"
screen -S bedwars -X stuff "stop$(printf \\r)"

echo "Stop commands sent!"
