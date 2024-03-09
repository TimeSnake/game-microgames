/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.user;

import de.timesnake.basic.bukkit.util.chat.Chat;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.server.MicroGamesServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorManager extends de.timesnake.basic.game.util.user.SpectatorManager {

  @Override
  public @Nullable Sideboard getSpectatorSideboard() {
    return null;
  }

  @Override
  public @Nullable Chat getSpectatorChat() {
    return null;
  }

  @Override
  public @NotNull ExLocation getSpectatorSpawn() {
    return MicroGamesServer.getCurrentGame().getSpecLocation();
  }

  @Override
  public boolean loadTools() {
    return true;
  }
}
