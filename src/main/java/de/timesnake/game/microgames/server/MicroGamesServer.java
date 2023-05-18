/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.server;

import de.timesnake.basic.game.util.server.GameServer;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.user.TablistManager;
import java.util.Collection;
import net.kyori.adventure.text.Component;

public class MicroGamesServer extends GameServer {

  public static final float GAME_COINS = 3;

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

  public static void broadcastMicroGamesTDMessage(String message) {
    server.broadcastMicroGamesTDMessage(message);
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
