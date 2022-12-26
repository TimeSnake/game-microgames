/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.chat.Chat;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.user.scoreboard.TeamTablist;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.server.MicroGamesServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorManager extends de.timesnake.basic.game.util.user.SpectatorManager {

    @Override
    public @NotNull TeamTablist getGameTablist() {
        return MicroGamesServer.getTablistManager().getTablist();
    }

    @Override
    public @Nullable Sideboard getGameSideboard() {
        return MicroGamesServer.getCurrentGame().getSideboard();
    }

    @Override
    public @Nullable Sideboard getSpectatorSideboard() {
        return null;
    }

    @Override
    public @Nullable Chat getSpectatorChat() {
        return null;
    }

    @Override
    public ExLocation getSpectatorSpawn() {
        return MicroGamesServer.getCurrentGame().getSpecLocation();
    }

    @Override
    public boolean loadTools() {
        return true;
    }
}
