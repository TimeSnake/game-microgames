/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserBlockBreakEvent;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.game.extension.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HotFeet extends MicroGame implements ArenaGame, FallOutGame {

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
  }

  @Override
  public void onMapInit(Map map) {
    super.onMapInit(map);

    map.getWorld().setPVP(false);
    map.getWorld().restrict(ExWorld.Restriction.AUTO_PRIME_TNT, false);
  }

  @Override
  public void prepare() {
    super.prepare();

    this.blocksWithDespawnTicks = this.getArena().getHighestBlocksInside().stream()
        .map(b -> new Tuple<>(b.getBlock(), 0))
        .collect(Collectors.toList());
    this.blocksWithDespawnTicks.forEach(b -> b.getA().setType(Material.WHITE_CONCRETE));
  }

  @Override
  public void start() {
    super.start();
    this.despawnBlocks = Server.getInGameUsers().size();

    for (User user : Server.getInGameUsers()) {
      user.addItem(new ItemStack(Material.TNT, 3));
      user.setGameMode(GameMode.SURVIVAL);
    }

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
    return 0;
  }

  @EventHandler
  public void onBlockBreak(UserBlockBreakEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setCancelled(true);
  }

  @EventHandler
  public void onTntPrime(TNTPrimeEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (e.getPrimingEntity() instanceof TNTPrimed tnt) {
      tnt.setFuseTicks(20);
    }
  }

  @EventHandler
  public void onExplode(EntityExplodeEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setYield(0);
  }
}
