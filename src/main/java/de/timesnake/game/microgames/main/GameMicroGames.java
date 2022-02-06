package de.timesnake.game.microgames.main;

import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.game.microgames.server.MicroGamesServerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GameMicroGames extends JavaPlugin {

    private static GameMicroGames plugin;

    @Override
    public void onLoad() {
        ServerManager.setInstance(new MicroGamesServerManager());
    }

    @Override
    public void onEnable() {
        GameMicroGames.plugin = this;
        PluginManager pm = Bukkit.getPluginManager();

        MicroGamesServerManager.getInstance().onMicroGamesEnable();
    }


    public static GameMicroGames getPlugin() {
        return plugin;
    }

}
