/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.game.basis.LocationFinishGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;

public class Dropper extends LocationFinishGame implements Listener {

  public Dropper() {
    super("dropper",
        "Dropper",
        Material.ANVIL,
        "Try to reach the ground without hitting any block",
        List.of("§hGoal: §preach ground first", "Jump off the platform an reach the ground without taking damage"),
        1,
        Duration.ofMinutes(3));
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.getUser().teleport(this.getSpawnLocation());
    e.setCancelDamage(true);
  }

  @Override
  public void onUserMove(UserMoveEvent e) {
    if (e.getUser().getLocation().getY() <= this.getFinishLocation().getY()) {
      super.addWinner(((MicroGamesUser) e.getUser()), true);
    }
  }
}
