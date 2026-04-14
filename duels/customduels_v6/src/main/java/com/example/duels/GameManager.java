
package com.example.duels;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

public class GameManager {

    private List<Player> queue=new ArrayList<>();
    private Map<Player, Player> fights=new HashMap<>();

    public void joinQueue(Player p){
        queue.add(p);
        if(queue.size()>=2){
            Player p1=queue.remove(0);
            Player p2=queue.remove(0);
            startGame(p1,p2);
        }
    }

    public void startGame(Player p1, Player p2){
        Location loc=Bukkit.getWorlds().get(0).getSpawnLocation();
        p1.teleport(loc);
        p2.teleport(loc);

        fights.put(p1,p2);
        fights.put(p2,p1);

        p1.sendMessage("§aFight started!");
        p2.sendMessage("§aFight started!");
    }

    public void endGame(Player loser){
        Player winner=fights.get(loser);

        if(winner!=null){
            winner.sendMessage("§aYou won!");
            loser.sendMessage("§cYou lost!");

            winner.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            loser.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());

            fights.remove(winner);
            fights.remove(loser);
        }
    }

    public void spectator(Player p, Player target){
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(target);
    }
}
