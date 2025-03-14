/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserBlockBreakEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld.Restriction;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.game.extension.FallOutGame;
import de.timesnake.library.basic.util.Status;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class Spleef extends MicroGame implements FallOutGame {

  private static final ExItemStack SHOVEL = new ExItemStack(Material.GOLDEN_SHOVEL)
      .addExEnchantment(Enchantment.EFFICIENCY, 10)
      .setUnbreakable(true).setDropable(false).setMoveable(false).immutable();

  protected static final Integer DEATH_HEIGHT_LOCATION_INDEX = 3;

  private final Random random = new Random();

  public Spleef() {
    super("spleef",
        "Spleef",
        Material.SNOWBALL,
        "Spleef other players",
        List.of("§hGoal: §plast man standing",
            "Break snow blocks to get snowballs and blocks.",
            "Throw snowballs to knock back other players.",
            "Use the shovel to spleef other players."),
        2,
        Duration.ofMinutes(3));
  }

  @Override
  public void prepare() {
    super.prepare();
    super.currentMap.getWorld().setPVP(true);
    super.currentMap.getWorld().restrict(Restriction.NO_PLAYER_DAMAGE, false);
  }

  @Override
  public void applyBeforeStart() {
    for (User user : Server.getPreGameUsers()) {
      user.teleport(this.getSpawnLocation());
      user.addItem(SHOVEL);
      user.setCooldown(Material.SNOWBALL, 2);
    }
  }

  @Override
  public void start() {
    super.start();

    for (User user : Server.getInGameUsers()) {
      user.setInvulnerable(false);
      user.setGameMode(GameMode.SURVIVAL);
    }
  }

  @Override
  public boolean hasSideboard() {
    return false;
  }

  public ExLocation getDeathLocation() {
    return super.currentMap.getLocation(DEATH_HEIGHT_LOCATION_INDEX);
  }

  @Override
  public Integer getDeathHeight() {
    return this.getDeathLocation().getBlockY();
  }

  @EventHandler
  public void onBlockBreak(UserBlockBreakEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    User user = e.getUser();

    if (user.isService()) {
      return;
    }

    if (user.getStatus().equals(Status.User.IN_GAME)) {
      user.addItem(new ItemStack(Material.SNOWBALL, 2));
      if (this.random.nextInt(4) == 0) {
        user.addItem(new ItemStack(Material.SNOW_BLOCK, 1));
      }
      e.setDropItems(false);
    }
  }

  @EventHandler
  public void onProjectileHit(ProjectileHitEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (e.getHitBlock() != null) {
      e.getHitBlock().setType(Material.VOID_AIR);
    }

    if (e.getHitEntity() != null && e.getHitEntity() instanceof Player) {
      ((Player) e.getHitEntity()).damage(0.01, e.getEntity());
      ((Player) e.getHitEntity()).setHealth(((Player) e.getHitEntity()).getAttribute(
          Attribute.GENERIC_MAX_HEALTH).getBaseValue());
      e.getHitEntity().setVelocity(e.getEntity().getVelocity().normalize());
    }
  }
}
