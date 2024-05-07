/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;
import java.util.List;

public class EggHunter extends ScoreGame<Integer> {

  public EggHunter() {
    super("egghunter", "EggHunter", Material.DRAGON_EGG,
        "Click the egg most often",
        List.of("§hGoal: §pmost egg clicks", "Find the egg and click it to get points."),
        2,
        Duration.ofSeconds(45));
  }

  @Override
  public void load() {
    super.load();
  }

  @Override
  protected void applyBeforeStart() {
    super.applyBeforeStart();
  }

  @Override
  public void start() {
    super.start();

    this.getStartLocation().getBlock().setType(Material.DRAGON_EGG);
  }

  @Override
  public void stop() {
    this.calcPlaces(true);
    super.stop();
  }

  @Override
  public void reset() {
    super.reset();
    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public String getScoreName() {
    return "Clicked Eggs";
  }

  @Override
  public boolean onUserJoin(MicroGamesUser user) {
    return false;
  }

  @Override
  public void onUserQuit(MicroGamesUser user) {

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

    if (e.getClickedBlock().getType().equals(Material.DRAGON_EGG)) {
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

    this.spawnEgg(e.getBlock().getLocation());
  }

  private void spawnEgg(Location oldLocation) {
    Block block;
    do {
      block = this.currentMap.getWorld().getHighestBlockAt(ExLocation.fromLocation(oldLocation).getRandomNearbyLocation(10).getBlock().getLocation());
    } while (block.getY() == this.currentMap.getWorld().getMinHeight() || !this.isValidSpawnBlock(block));

    block.getLocation().add(0, 1, 0).getBlock().setType(Material.DRAGON_EGG);
  }

  private boolean isValidSpawnBlock(Block block) {
    return block.getType().equals(Material.END_STONE) || block.getType().equals(Material.OBSIDIAN);
  }
}
