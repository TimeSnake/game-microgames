/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.extension;

import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.game.microgames.game.basis.MicroGameExtensionBase;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public interface DeathGame extends MicroGameExtensionBase, Listener {

  @EventHandler
  default void onUserDeath(UserDeathEvent e) {
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
