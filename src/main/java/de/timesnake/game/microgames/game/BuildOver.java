/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.event.UserBlockPlaceEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDeathEvent;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.user.event.UserRespawnEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExPolygon;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.basis.LocationFinishGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class BuildOver extends LocationFinishGame implements Listener {

  private static final Material BUILDING_BLOCKS = Material.WHITE_WOOL;
  private static final ExItemStack BUILDING_ITEMS = new ExItemStack(BUILDING_BLOCKS, 64);

  private HashMap<Map, ExPolygon> polygonByMap;

  public BuildOver() {
    super("build_over", "Build Over",
        Material.GOLDEN_BOOTS,
        "Beat the parkour as fast you can",
        List.of("§hGoal: §pfirst at finish", "Reach the finish at first."),
        1,
        Duration.ofMinutes(4));

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void beforeMapLoad() {
    super.beforeMapLoad();

    this.polygonByMap = new HashMap<>();
  }

  @Override
  public void onMapLoad(Map map) {
    super.onMapLoad(map);

    this.polygonByMap.put(map, new ExPolygon(map.getWorld(), map.getLocationsSorted(10, 100)));
  }

  @Override
  public void load() {
    super.load();

    this.currentMap.getWorld().setPVP(true);
  }

  @Override
  protected void applyBeforeStart() {
    super.applyBeforeStart();

    Server.getPreGameUsers().forEach(u -> {
      u.addItem(BUILDING_ITEMS.cloneWithId());
      u.addItem(BUILDING_ITEMS.cloneWithId());
      u.addItem(new ExItemStack(Material.SHEARS).addExEnchantment(Enchantment.EFFICIENCY, 3));
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
    e.getUser().addItem(BUILDING_ITEMS.cloneWithId());
    e.getUser().addItem(BUILDING_ITEMS.cloneWithId());
    e.getUser().addItem(new ExItemStack(Material.SHEARS).addExEnchantment(Enchantment.EFFICIENCY, 3));
  }

  @Override
  public boolean hasSideboard() {
    return false;
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

    if (!this.polygonByMap.get(this.currentMap).contains(ExLocation.fromLocation(e.getBlock().getLocation()))) {
      e.getUser().sendPluginTDMessage(Plugin.MICRO_GAMES, "§wYou can not build here");
      e.setCancelled(true);
    }
  }
}
