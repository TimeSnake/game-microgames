/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HotFeet extends FallOutGame {

  private static final Integer FIRST_CORNER_INDEX = 3;
  private static final Integer SECOND_CORNER_INDEX = 4;

  private static final int DESPAWN_TIME = 40;

  private BukkitTask despawnTask;

  private List<Tuple<Block, Integer>> blocksWithDespawnTicks;
  private int despawnBlocks;

  public HotFeet() {
    super("hot_feet",
        "Hot Feet",
        Material.MAGMA_BLOCK,
        "Avoid standing on hot ground",
        List.of("§hGoal: §plast man standing",
            "Blocks getting hotter (more red) and despawn.",
            "Try to be the last standing on white blocks."),
        1,
        Duration.ofSeconds(180));
    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void onMapLoad(Map map) {
    super.onMapLoad(map);

    map.getWorld().setPVP(false);
  }

  @Override
  public void prepare() {
    super.prepare();

    this.blocksWithDespawnTicks = this.currentMap.getWorld().getBlocksWithinCubic(this.getFirstCorner(),
            this.getSecondCorner()).stream()
        .map(b -> new Tuple<>(b, 0))
        .collect(Collectors.toList());
    this.blocksWithDespawnTicks.forEach(b -> b.getA().setType(Material.WHITE_CONCRETE));
  }

  @Override
  public void start() {
    super.start();
    this.despawnBlocks = Server.getInGameUsers().size();

    this.startDespawning();
  }

  private void startDespawning() {
    Collections.shuffle(this.blocksWithDespawnTicks);
    this.despawnTask = Server.runTaskTimerSynchrony(() -> {
      int i = this.despawnBlocks;
      for (Tuple<Block, Integer> tuple : this.blocksWithDespawnTicks) {
        Block block = tuple.getA();
        Integer ticks = tuple.getB();

        if (ticks == 0) {
          switch (block.getType()) {
            case AIR -> {
            }
            case WHITE_CONCRETE -> {
              if (i > 0 && !block.getLocation().getNearbyPlayers(1.5,
                  p -> Server.getUser(p).hasStatus(Status.User.IN_GAME)).isEmpty()) {
                block.setType(Material.YELLOW_CONCRETE);
                tuple.setB(DESPAWN_TIME / 2);
                i--;
              }
            }
            case YELLOW_CONCRETE -> {
              block.setType(Material.ORANGE_CONCRETE);
              tuple.setB(DESPAWN_TIME / 2);
            }
            case ORANGE_CONCRETE -> {
              block.setType(Material.RED_CONCRETE);
              tuple.setB(DESPAWN_TIME / 2);
            }
            case RED_CONCRETE -> {
              block.setType(Material.AIR);
              tuple.setB(DESPAWN_TIME / 2);
            }
          }
        } else {
          tuple.setB(ticks - 1);
        }
      }
    }, 0, 4, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    this.addRemainingAsWinner(false);
    super.stop();

    if (this.despawnTask != null) {
      this.despawnTask.cancel();
    }
  }

  @Override
  public Integer getDeathHeight() {
    return this.getFirstCorner().getBlockY();
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }

  public ExLocation getFirstCorner() {
    return this.currentMap.getLocation(FIRST_CORNER_INDEX);
  }

  public ExLocation getSecondCorner() {
    return this.currentMap.getLocation(SECOND_CORNER_INDEX);
  }
}
