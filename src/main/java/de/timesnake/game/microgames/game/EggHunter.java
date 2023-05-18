/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.user.event.UserInteractEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

public class EggHunter extends ScoreGame<Integer> {

  private static final Integer START_LOCATION_INDEX = 0;
  private static final Integer SPEC_LOCATION_INDEX = 1;

  public EggHunter() {
    super("egg_hunter", "EggHunter", Material.DRAGON_EGG,
        "Click the egg most often", 2, 120);
  }

  @Override
  public Integer getLocationAmount() {
    return 2;
  }

  @Override
  protected void loadDelayed() {

  }

  @Override
  public void start() {
    super.start();

    this.getStartLocation().getBlock().setType(Material.DRAGON_EGG);
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public boolean onUserJoin(MicroGamesUser user) {
    return false;
  }

  @Override
  public void onUserQuit(MicroGamesUser user) {

  }

  @Override
  public ExLocation getSpecLocation() {
    return super.currentMap.getLocation(SPEC_LOCATION_INDEX);
  }

  @Override
  public ExLocation getStartLocation() {
    return super.currentMap.getLocation(START_LOCATION_INDEX);
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @EventHandler
  public void onInteract(UserInteractEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    MicroGamesUser user = ((MicroGamesUser) e.getUser());

    if (e.getClickedBlock() != null
        && e.getClickedBlock().getType().equals(Material.DRAGON_EGG)) {
      this.scores.compute(user, (u, v) -> v + 1);
    }
  }
}
