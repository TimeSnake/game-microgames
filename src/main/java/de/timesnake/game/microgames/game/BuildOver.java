/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.user.event.UserRespawnEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.game.microgames.game.basis.LocationFinishGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;

import java.time.Duration;
import java.util.List;

public class BuildOver extends LocationFinishGame {

  private static final Material BUILDING_BLOCKS = Material.WHITE_WOOL;
  private static final ExItemStack BUILDING_ITEMS = new ExItemStack(BUILDING_BLOCKS, 64);

  public BuildOver() {
    super("build_over", "Build Over",
        Material.GOLDEN_BOOTS,
        "Beat the parkour as fast you can",
        List.of("§hGoal: §pfirst at finish", "Reach the finish (beacon) at first."),
        1,
        Duration.ofMinutes(3));
  }

  @Override
  protected void applyBeforeStart() {
    super.applyBeforeStart();

    Server.getPreGameUsers().forEach(u -> u.addItem(BUILDING_ITEMS.cloneWithId()));
  }

  @Override
  protected void onUserMove(UserMoveEvent e) {
    if (e.getUser().getLocation().getBlock().equals(this.getFinishLocation().getBlock())) {
      super.addWinner(((MicroGamesUser) e.getUser()), true);
    }
  }

  @Override
  protected void onUserRepsawn(UserRespawnEvent e) {
    super.onUserRepsawn(e);
    e.getUser().addItem(BUILDING_ITEMS.cloneWithId());
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }
}
