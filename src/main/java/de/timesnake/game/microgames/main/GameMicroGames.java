package de.timesnake.game.microgames.main;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.server.MicroGamesServerManager;
import de.timesnake.game.microgames.user.SkipGameCmd;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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

        Server.getCommandManager().addCommand(this, "microgames", List.of("mg"), new SkipGameCmd(), Plugin.MICRO_GAMES);
    }


    public static GameMicroGames getPlugin() {
        return plugin;
    }

}
