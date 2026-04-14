
package com.example.duels;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class QueueCommand implements CommandExecutor {

    private GameManager gm;

    public QueueCommand(GameManager gm){this.gm=gm;}

    public boolean onCommand(CommandSender s, Command c, String l, String[] a){
        if(s instanceof Player){
            gm.joinQueue((Player)s);
        }
        return true;
    }
}
