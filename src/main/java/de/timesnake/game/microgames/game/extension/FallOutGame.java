/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game.extension;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.game.basis.MicroGameExtensionBase;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;

public interface FallOutGame extends MicroGameExtensionBase, Listener {

  @EventHandler
  default void onUserMove(UserMoveEvent e) {
    User user = e.getUser();

    if (!this.isGameRunning()) {
      return;
    }

    if (!user.getStatus().equals(Status.User.IN_GAME)) {
      return;
    }

    if (e.getTo().getY() <= this.getCurrentMap().getLocation(99).getBlockY()) {
      user.showTDTitle("§wYou lose!", "", Duration.ofSeconds(3));
      Server.broadcastSound(Sound.ENTITY_PLAYER_HURT, 2);

      this.addWinner(((MicroGamesUser) user), false);
    }
  }

  @EventHandler
  default void onUserDamage(UserDamageEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setCancelDamage(true);
  }
}
