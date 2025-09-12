/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserBlockPlaceEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.user.event.UserRespawnEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.BlockPolygon;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.LocationFinishGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.RandomList;
import de.timesnake.library.chat.Plugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;

public class BuildOver extends LocationFinishGame implements ArenaGame, Listener {

  private static final ExItemStack SHEARS = new ExItemStack(Material.SHEARS)
      .setDropable(false);
  private static final RandomList<Material> BUILDING_BLOCKS = new RandomList<>(Tag.WOOL.getValues());

  private BlockPolygon arena;

  public BuildOver() {
    super("build_over", "Build Over",
        Material.GOLDEN_BOOTS,
        "Beat the parkour as fast you can",
        List.of("§hGoal: §pfirst at finish", "Reach the finish at first."),
        1,
        Duration.ofMinutes(4));
  }

  @Override
  public void prepare() {
    super.prepare();
    this.arena = this.getArena();
  }

  @Override
  public void load() {
    super.load();

    this.currentMap.getWorld().setPVP(true);
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();

    Server.getPreGameUsers().forEach(u -> {
      ItemStack blocks = new ItemStack(BUILDING_BLOCKS.getAny()).asQuantity(64);
      u.addItem(blocks);
      u.addItem(blocks);
      u.addItem(SHEARS);
    });
  }

  @Override
  public void start() {
    super.start();

    Server.getInGameUsers().forEach(u -> u.setGameMode(GameMode.SURVIVAL));
    this.currentMap.getWorld().setPVP(true);
  }

  @Override
  protected void onUserMove(UserMoveEvent e) {
    if (e.getUser().getLocation().getBlock().equals(this.getFinishLocation().getBlock())) {
      super.addWinner(((MicroGamesUser) e.getUser()), true);
    }
  }

  @Override
  protected void onUserRepsawn(UserRespawnEvent e) {
    super.onUserRepsawn(e);
    User user = e.getUser();
    user.clearInventory();
    ItemStack blocks = new ItemStack(BUILDING_BLOCKS.getAny()).asQuantity(64);
    user.addItem(blocks);
    user.addItem(blocks);
    user.addItem(SHEARS);
  }

  @Override
  protected void onUserFallIntoVoid(User user) {
    user.kill();
  }

  @EventHandler
  public void onUserDeath(UserDeathEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setDrops(List.of());
  }

  @EventHandler
  public void onUserBlockPlace(UserBlockPlaceEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (!this.arena.contains(ExLocation.fromLocation(e.getBlock().getLocation()))) {
      e.getUser().sendPluginTDMessage(Plugin.GAME, "§wYou can not build here");
      e.setCancelled(true);
    }
  }
}
