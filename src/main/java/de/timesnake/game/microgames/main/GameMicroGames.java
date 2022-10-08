/*
 * game-microgames.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

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
