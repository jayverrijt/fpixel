
package com.example.duels;

import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

public class GameListener implements Listener {

    private GameManager gm;

    public GameListener(GameManager gm){this.gm=gm;}

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Player dead=e.getEntity();
        gm.endGame(dead);
    }
}
