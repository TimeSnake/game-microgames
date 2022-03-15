package de.timesnake.game.microgames.server;

import de.timesnake.basic.game.util.GameServer;
import de.timesnake.game.microgames.game.MicroGame;
import de.timesnake.game.microgames.user.TablistManager;

import java.util.Collection;

public class MicroGamesServer extends GameServer {

    private static final MicroGamesServerManager server = MicroGamesServerManager.getInstance();

    public static void nextGame() {
        server.nextGame();
    }

    public static boolean isPaused() {
        return server.isPaused();
    }

    public static MicroGame getCurrentGame() {
        return server.getCurrentGame();
    }

    public static void broadcastMicroGamesMessage(String message) {
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
}
