/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.event.UserInteractEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.extension.util.chat.Chat;
import java.time.Duration;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitTask;

public class EggHunter extends ScoreGame<Integer> {

  private static final Integer START_LOCATION_INDEX = 0;
  private static final Integer SPEC_LOCATION_INDEX = 1;

  private static final Duration DURATION = Duration.ofSeconds(45);

  private BukkitTask timeTask;

  public EggHunter() {
    super("egg_hunter", "EggHunter", Material.DRAGON_EGG,
        "Click the egg most often", 2, -1);
  }

  @Override
  public Integer getLocationAmount() {
    return 2;
  }

  @Override
  public void load() {
    super.load();

    super.sideboard.setScore(4, "§9§lTime");
    super.sideboard.setScore(3, "§f" + DURATION + "s");
    super.sideboard.setScore(2, "§f-------------------");
    super.sideboard.setScore(1, "§c§lClicked Eggs");
    super.sideboard.setScore(0, "§f0");
  }

  @Override
  protected void loadDelayed() {

  }

  @Override
  public void start() {
    super.start();

    this.getStartLocation().getBlock().setType(Material.DRAGON_EGG);

    this.timeTask = Server.runTaskTimerSynchrony((time) -> {
      super.sideboard.setScore(3, Chat.getTimeString(time));

      if (time == 0) {
        this.stop();
      }
    }, ((int) DURATION.toSeconds()), true, 0, 20, GameMicroGames.getPlugin());
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
      int number = this.scores.compute(user, (u, v) -> v + 1);
      user.setSideboardScore(0, "§f" + number);

    }
  }
}
