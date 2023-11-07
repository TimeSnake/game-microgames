/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserDamageEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ShrinkingPlatformGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;

public class KnockOut extends ShrinkingPlatformGame implements Listener {

  public static final Integer START_RADIUS = 10;
  public static final Integer MIN_RADIUS = 3;
  public static final Integer DECREASE_DELAY = 20;
  protected static final ExItemStack STICK = new ExItemStack(Material.STICK).addExEnchantment(Enchantment.KNOCKBACK, 2);


  public KnockOut() {
    super("knockout",
        "KnockOut",
        Material.STICK,
        "Knock all players from the platform",
        List.of("§hGoal: §plast man standing",
            "Use your knockback stick to punch out all players from the platform.",
            "After a time, the platform becomes smaller."),
        2,
        Duration.ofMinutes(2));
    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void prepare() {
    super.prepare();
    super.currentMap.getWorld().setPVP(false);
  }

  @Override
  protected void loadDelayed() {
    for (User user : Server.getPreGameUsers()) {
      user.teleport(this.getSpawnLocation());
      user.lockInventoryItemMove();
      user.setItem(0, STICK);
    }
  }

  @Override
  public void start() {
    super.start();
    super.currentMap.getWorld().setPVP(true);
  }

  @Override
  public Integer getStartRadius() {
    return START_RADIUS;
  }

  @Override
  public Integer getDelay() {
    return DECREASE_DELAY;
  }

  @Override
  public Integer getMinRadius() {
    return MIN_RADIUS;
  }

  @Override
  public ExLocation getCenterLocation() {
    return this.getSpawnLocation();
  }

  @Override
  public void reset() {
    super.reset();
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
    if (Server.getInGameUsers().size() <= 1) {
      this.stop();
    }
  }

  @EventHandler
  public void onEntityDamage(UserDamageEvent e) {
    if (!this.isGameRunning()) {
      return;
    }
    e.setCancelDamage(true);
  }

  @Override
  public Integer getDeathHeight() {
    return this.getSpawnLocation().getBlockY();
  }

}
