/*
 * workspace.game-microgames.main
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
