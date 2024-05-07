/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.game.basis.LocationFinishGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;

public class Parkour extends LocationFinishGame implements Listener {

  public Parkour() {
    super("parkour", "Parkour",
        Material.GOLDEN_BOOTS,
        "Beat the parkour as fast you can",
        List.of("§hGoal: §pfirst at finish", "Jump and run the parkour.", "Reach the finish (beacon) at first."),
        1,
        Duration.ofMinutes(3));
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }

  @Override
  public void onUserMove(UserMoveEvent e) {
    if (e.getUser().getLocation().getBlock().equals(this.getFinishLocation().getBlock())) {
      super.addWinner(((MicroGamesUser) e.getUser()), true);
    }
  }
}
