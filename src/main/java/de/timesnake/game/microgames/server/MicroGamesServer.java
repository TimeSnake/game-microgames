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

package de.timesnake.game.microgames.server;

import de.timesnake.basic.game.util.GameServer;
import de.timesnake.game.microgames.game.MicroGame;
import de.timesnake.game.microgames.user.TablistManager;
import net.kyori.adventure.text.Component;

import java.util.Collection;

public class MicroGamesServer extends GameServer {

    public static void nextGame() {
        server.nextGame();
    }

    public static boolean isPaused() {
        return server.isPaused();
    }

    public static MicroGame getCurrentGame() {
        return server.getCurrentGame();
    }

    public static void broadcastMicroGamesMessage(Component message) {
        server.broadcastMicroGamesMessage(message);
    }

    public static Collection<MicroGame> getGames() {
        return server.getGames();
    }

    public static boolean isPartyMode() {
        return server.isPartyMode();
    }

    public static void startParty() {
        server.startParty();
    }

    public static TablistManager getTablistManager() {
        return server.getTablistManager();
    }

    public static void skipGame() {
        server.skipGame();
    }

    private static final MicroGamesServerManager server = MicroGamesServerManager.getInstance();
}
