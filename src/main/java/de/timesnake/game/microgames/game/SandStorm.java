/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.BlockPolygon;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.UserMap;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class SandStorm extends MicroGame implements ArenaGame {

  private static final int SAND_SPAWN_RATE = 30;
  private static final double SAND_CHANCE = 0.1;
  private static final int HEIGHT_DIFF = 20;

  private BlockPolygon arena;

  private BukkitTask moveTask;
  private BukkitTask sandTask;

  private int deathHeight;

  private final UserMap<User, Tuple<Integer, Block>> lastBlockCounter = new UserMap<>();

  public SandStorm() {
    super("sand_storm", "Sand Storm",
        Material.SAND,
        "Do not stop",
        List.of("§hGoal: §plast man standing",
            "Avoid falling and red sand blocks.",
            "If you stand still or touch a red sand block, you lose."),
        1,
        Duration.ofSeconds(180));
  }

  @Override
  public void onMapInit(Map map) {
    super.onMapInit(map);
  }

  @Override
  public void prepare() {
    super.prepare();

    this.arena = this.getArena();
    this.currentMap.getWorld().setPVP(false);
  }

  @Override
  public void start() {
    super.start();

    this.deathHeight = 0;

    this.moveTask = Server.runTaskTimerAsynchrony(this::checkMovements, 0, 10, GameMicroGames.getPlugin());
    this.sandTask = Server.runTaskTimerSynchrony(this::spawnSand, 120 * 10, true, 0, SAND_SPAWN_RATE,
        GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    this.addRemainingAsWinner(false);
    super.stop();

    if (this.sandTask != null) {
      this.sandTask.cancel();
    }

    if (this.moveTask != null) {
      this.moveTask.cancel();
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

      if (user.getLocation().add(0, -1, 0).getBlock().getType().equals(Material.RED_SAND)) {
        Server.runTaskSynchrony(() -> this.addWinner((MicroGamesUser) user, false), GameMicroGames.getPlugin());
      }
    }
  }

  private void spawnSand(int time) {
    for (Block block : this.arena.getBlocksInside(b -> this.random.nextDouble() <= SAND_CHANCE)) {
      this.currentMap.getWorld().spawnFallingBlock(block.getLocation().add(0.5, HEIGHT_DIFF, 0.5),
          Material.SAND.createBlockData());
    }

    if (time % 8 == 0 && time <= 117 * 10) {
      for (Block block : this.arena.getBlocksInsideOnHeight(this.deathHeight, b -> b.getType().equals(Material.SAND))) {
        block.setType(Material.RED_SAND);
      }
      this.deathHeight++;
    }
  }
}
