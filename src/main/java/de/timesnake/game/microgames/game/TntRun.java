/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.game.extension.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.library.basic.util.Status;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TntRun extends MicroGame implements FallOutGame, Listener {

  protected static final Integer REMOVE_DELAY = 19;

  protected static final Double[][] NEAR_BLOCK_VECTORS = {{0.3, 0.0}, {0.0, 0.3}, {-0.3, 0.0},
      {0.0, -0.3}, {0.3, 0.3}, {0.3, -0.3}, {-0.3, 0.3}, {-0.3, -0.3}};

  private final Set<Block> removedBlocks = new HashSet<>();

  public TntRun() {
    super("tntrun",
        "TNT Run",
        Material.TNT,
        "Try not to fall",
        List.of("§hGoal: §plast man standing.",
            "Blocks your standing on gets tnt and despawn.",
            "Keep running to not fall."),
        1,
        Duration.ofMinutes(5));
  }

  @Override
  public void prepare() {
    super.prepare();
    super.currentMap.getWorld().setPVP(false);
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();
  }

  @Override
  public void start() {
    super.start();

    for (User user : Server.getInGameUsers()) {
      user.getPlayer().setInvulnerable(true);

      Location from = user.getLocation();

      Server.runTaskLaterSynchrony(() -> {
            this.removeBlocks(from.clone().add(0, -1, 0));
            this.removeBlocks(from.clone().add(0, -2, 0));
          }, 40 - REMOVE_DELAY,
          GameMicroGames.getPlugin());
    }
  }

  private void removeBlocks(Location from) {

    Set<Block> blocks = new HashSet<>();

    if (this.isBlockRemoveable(from.getBlock())) {
      from.getBlock().setType(Material.TNT);
      blocks.add(from.getBlock());
      this.removedBlocks.add(from.getBlock());
    }

    for (Double[] vec : NEAR_BLOCK_VECTORS) {
      Location loc = from.clone().add(vec[0], 0, vec[1]);
      if (this.isBlockRemoveable(loc.getBlock())) {
        loc.getBlock().setType(Material.TNT);
        this.removedBlocks.add(loc.getBlock());
        blocks.add(loc.getBlock());
      }
    }

    Server.runTaskLaterSynchrony(() -> {
      for (Block block : blocks) {
        block.setType(Material.AIR);
      }
    }, REMOVE_DELAY, GameMicroGames.getPlugin());

  }

  private boolean isBlockRemoveable(Block block) {
    return !block.getType().equals(Material.AIR)
        && !block.getType().equals(Material.TNT)
        && !this.removedBlocks.contains(block);
  }

  @Override
  public void reset() {
    super.reset();
    this.removedBlocks.clear();
  }

  @EventHandler
  @Override
  public void onUserMove(UserMoveEvent e) {
    User user = e.getUser();

    if (!this.isGameRunning()) {
      return;
    }

    if (!user.getStatus().equals(Status.User.IN_GAME)) {
      return;
    }

    FallOutGame.super.onUserMove(e);

    e.getFrom().getBlock().setType(Material.AIR);
    this.removeBlocks(e.getFrom().clone().add(0, -1, 0));
    this.removeBlocks(e.getFrom().clone().add(0, -2, 0));
  }

}
