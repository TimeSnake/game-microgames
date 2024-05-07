/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class LadderKing extends ScoreGame<Integer> implements Listener {

  private static final Integer LADDER_LOCATION_INDEX = 3;
  // spawn locations all above 3

  private static final Integer SCORE_PERIOD = 10;

  private BukkitTask scoreTask;

  public LadderKing() {
    super("ladderking",
        "King of the Ladder",
        Material.LADDER,
        "Try stand the longest time on top of the ladder",
        List.of("§hGoal: §plongest time on top",
            "Climb up the ladder to the top.",
            "Knock other players from the ladder.",
            "Be in sum the most time on top."),
        2,
        Duration.ofSeconds(90));
    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public void prepare() {
    super.prepare();
  }

  @Override
  protected void applyBeforeStart() {
    List<ExLocation> spawnLocations = super.currentMap.getLocations(4);
    for (User user : Server.getPreGameUsers()) {
      user.teleport(spawnLocations.get(super.random.nextInt(spawnLocations.size())));
      user.lockLocation();
    }
  }

  @Override
  public void start() {
    super.start();

    for (User user : MicroGamesServer.getInGameUsers()) {
      user.unlockLocation();
    }

    this.scoreTask = Server.runTaskTimerSynchrony(() -> {
      for (User user : Server.getInGameUsers()) {
        if (this.isOnTop(user)) {
          int score = this.scores.compute(((MicroGamesUser) user), (u, v) -> v + 1);
          user.setSideboardScore(0, String.valueOf(score));
        }
      }
    }, 0, SCORE_PERIOD, GameMicroGames.getPlugin());
  }

  private boolean isOnTop(User user) {
    Block userBlock = user.getLocation().getBlock();
    Block topBlock = super.currentMap.getLocation(LADDER_LOCATION_INDEX).getBlock();
    if (userBlock.getY() >= topBlock.getY()) {
      return userBlock.getLocation().distanceSquared(topBlock.getLocation()) <= 1;
    }
    return false;
  }

  @Override
  public void stop() {
    super.calcPlaces(true);

    if (this.scoreTask != null) {
      this.scoreTask.cancel();
    }

    super.stop();
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setCancelDamage(true);
  }

  @EventHandler
  public void onUserDamageByUser(UserDamageByUserEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (!e.getUserDamager().getStatus().equals(Status.User.IN_GAME)) {
      e.setCancelDamage(true);
      e.setCancelled(true);
    }
  }

}
