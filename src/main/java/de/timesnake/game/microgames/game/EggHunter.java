/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.core.world.DelegatedBlock;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.world.BlockPolygon;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.RandomList;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;
import java.util.List;

public class EggHunter extends ScoreGame<Integer> implements ArenaGame {

  private BlockPolygon arena;

  public EggHunter() {
    super("egg_hunter", "Egg Hunter", Material.DRAGON_EGG,
        "Find the egg most often",
        List.of("§hGoal: §pmost egg clicks", "Find the egg and click it to get points."),
        2,
        Duration.ofSeconds(45));
  }

  @Override
  public void load() {
    super.load();
  }

  @Override
  public void prepare() {
    super.prepare();

    this.arena = this.getArena();
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();
  }

  @Override
  public void start() {
    super.start();

    this.getSpawnLocation().getBlock().setType(Material.DRAGON_EGG);
  }

  @Override
  public void stop() {
    this.calcPlaces(true);
    super.stop();
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public String getScoreName() {
    return "Found Eggs";
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @EventHandler
  public void onEntityDamage(PlayerInteractEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    MicroGamesUser user = (MicroGamesUser) Server.getUser(e.getPlayer());

    if (e.getClickedBlock() != null && e.getClickedBlock().getType().equals(Material.DRAGON_EGG)) {
      this.updateUserScore(user, (u, v) -> v + 1);
    }
  }

  @EventHandler
  public void onInteract(BlockFromToEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setCancelled(true);
    e.getBlock().setType(Material.AIR);

    this.spawnEgg();
  }

  private void spawnEgg() {
    RandomList.anyOf(this.arena.getHighestBlocksInside(DelegatedBlock::isSolid)).getLocation().add(0, 5, 0).getBlock().setType(Material.DRAGON_EGG);
  }
}
