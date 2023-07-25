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
import de.timesnake.library.extension.util.chat.Chat;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class LadderKing extends ScoreGame<Integer> implements Listener {

  private static final Integer LADDER_LOCATION_INDEX = 3;
  // spawn locations all above 3

  private static final Integer TIME = 90;
  private static final Integer SCORE_PERIOD = 10;

  private Integer time = TIME;

  private BukkitTask task;
  private BukkitTask scoreTask;

  public LadderKing() {
    super("ladderking", "King of the Ladder", Material.LADDER,
        "Try stand the longest time on top of the ladder",
        2, null);
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
  public void load() {
    super.load();

    super.sideboard.setScore(4, "§9§lTime");
    super.sideboard.setScore(3, Chat.getTimeString(this.time));
    super.sideboard.setScore(2, "§f---------------");
    super.sideboard.setScore(1, "§c§lScore");
    super.sideboard.setScore(0, "0");
  }

  @Override
  protected void loadDelayed() {
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

    this.task = Server.runTaskTimerSynchrony(() -> {
      this.time--;
      super.sideboard.setScore(3, Chat.getTimeString(this.time));
      if (this.time == 0) {
        this.stop();
      }
    }, 0, 20, GameMicroGames.getPlugin());

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

    if (this.task != null) {
      this.task.cancel();
    }

    if (this.scoreTask != null) {
      this.scoreTask.cancel();
    }

    super.stop();
  }

  @Override
  public void reset() {
    super.reset();
    this.time = TIME;
    super.sideboard.setScore(3, Chat.getTimeString(this.time));
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @Override
  public boolean onUserJoin(MicroGamesUser user) {
    return false;
  }

  @Override
  public void onUserQuit(MicroGamesUser user) {
    if (Server.getInGameUsers().size() <= 1) {
      this.stop();
    }
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
