/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.basis;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;

public abstract class DeathGame extends MicroGame implements Listener {

  public DeathGame(String name, String displayName, Material material, String headLine, List<String> description,
                   Integer minPlayers, Duration maxDuration) {
    super(name, displayName, material, headLine, description, minPlayers, maxDuration);
    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @EventHandler
  public void onUserDeath(UserDeathEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    User user = e.getUser();

    if (user.isService() || !user.hasStatus(Status.User.IN_GAME)) {
      return;
    }

    this.addWinner(((MicroGamesUser) user), false);
  }
}
