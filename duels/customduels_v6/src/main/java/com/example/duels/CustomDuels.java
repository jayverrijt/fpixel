
package com.example.duels;

import org.bukkit.plugin.java.JavaPlugin;

public class CustomDuels extends JavaPlugin {

    public static CustomDuels instance;
    public GameManager gameManager;

    public void onEnable(){
        instance=this;
        gameManager=new GameManager();

        getCommand("queue").setExecutor(new QueueCommand(gameManager));
        getServer().getPluginManager().registerEvents(new GameListener(gameManager),this);
    }
}
