/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.player.UserMap;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class SandStorm extends MicroGame {

  private static final int SAND_SPAWN_RATE = 30;
  private static final double SAND_CHANCE = 0.1;
  private static final int HEIGHT_DIFF = 20;

  private static final Integer FIRST_CORNER_INDEX = 3;
  private static final Integer SECOND_CORNER_INDEX = 4;

  private BukkitTask moveTask;
  private BukkitTask sandTask;

  private final UserMap<User, Tuple<Integer, Block>> lastBlockCounter = new UserMap<>();

  public SandStorm() {
    super("sand_storm", "Sand Storm",
        Material.SAND,
        "Do not stop",
        List.of("§hGoal: §plast man standing", "Avoid falling sand blocks.", "If you stand still, you lose."),
        1,
        Duration.ofSeconds(180));
  }

  @Override
  public void onMapLoad(Map map) {
    super.onMapLoad(map);
  }

  @Override
  public void start() {
    super.start();

    this.moveTask = Server.runTaskTimerAsynchrony(this::checkMovements, 0, 10, GameMicroGames.getPlugin());
    this.sandTask = Server.runTaskTimerSynchrony(this::spawnSand, 120 * 10, true, 0, SAND_SPAWN_RATE,
        GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    super.stop();

    if (this.sandTask != null) {
      this.sandTask.cancel();
    }

    if (this.moveTask != null) {
      this.moveTask.cancel();
    }
  }

  @Override
  public void reset() {
    super.reset();

    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  private void checkMovements() {
    for (User user : Server.getInGameUsers()) {
      Tuple<Integer, Block> tuple = this.lastBlockCounter.get(user);
      if (tuple != null && user.getLocation().getBlock().getX() == tuple.getB().getX()
          && user.getLocation().getBlock().getZ() == tuple.getB().getZ()) {
        int count = tuple.getA();
        if (count >= 3) {
          Server.runTaskSynchrony(() -> this.addWinner((MicroGamesUser) user, false), GameMicroGames.getPlugin());
          this.lastBlockCounter.remove(user);
        } else {
          tuple.setA(count + 1);
        }
      } else {
        this.lastBlockCounter.put(user, new Tuple<>(0, user.getLocation().getBlock()));
      }
    }
  }

  private void spawnSand(int time) {
    for (Block block : this.currentMap.getWorld().getBlocksWithinCubic(this.getFirstCorner(), this.getSecondCorner())) {
      if (this.random.nextDouble() <= SAND_CHANCE) {
        this.currentMap.getWorld().spawnFallingBlock(block.getLocation().add(0.5, HEIGHT_DIFF, 0.5),
            Material.SAND.createBlockData());
      }
    }
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }

  @Override
  public boolean onUserJoin(MicroGamesUser user) {
    return false;
  }

  @Override
  public void onUserQuit(MicroGamesUser user) {

  }

  public ExLocation getFirstCorner() {
    return this.currentMap.getLocation(FIRST_CORNER_INDEX);
  }

  public ExLocation getSecondCorner() {
    return this.currentMap.getLocation(SECOND_CORNER_INDEX);
  }
}
