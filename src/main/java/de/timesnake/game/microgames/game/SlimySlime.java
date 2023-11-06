/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.entities.EntityManager;
import de.timesnake.library.entities.entity.SlimeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Slime;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class SlimySlime extends ScoreGame<Integer> implements Listener {

  private static final int MAIN_SLIME_LOC_INDEX = 3;
  private static final int SLIME_SPAWN_LOC_START_INDEX = 4;

  private static final double SLIME_SPAWN_CHANCE = 0.25;
  private static final int MAX_SLIMES_PER_SPAWN = 3;
  private static final double SLIME_GROWTH = 0.1;
  private static final int MAX_SLIME_BALLS = 5;

  private static final ExItemStack SLIME_BALL = new ExItemStack(Material.SLIME_BALL, "Glibber");

  private Slime mainSlime;
  private double slimeSize;
  private BukkitTask spawnTask;

  public SlimySlime() {
    super("slimy_slime",
        "Slimy Slime",
        Material.SLIME_BALL,
        "Feed the slime at most",
        List.of("§hGoal: §pget most slimeballs",
            "Kill slimes to get slimeballs.",
            "Carry at most 5 balls.",
            "Feed slimy slime to get points."),
        1,
        Duration.ofSeconds(120));
    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void onMapLoad(Map map) {
    super.onMapLoad(map);

    map.getWorld().restrict(ExWorld.Restriction.NO_PLAYER_DAMAGE, true);
    map.getWorld().setPVP(false);
  }

  @Override
  public void prepare() {
    super.prepare();

    this.slimeSize = 1;
    this.mainSlime = new SlimeBuilder(this.currentMap.getWorld().getHandle(), false, false, true)
        .applyOnEntity(e -> {
          e.setInvulnerable(true);
          e.setSize((int) this.slimeSize, true);
          e.setCustomName(Component.literal("Slimy Slime"));
          e.setCustomNameVisible(true);
          ExLocation loc = this.getMotherSlimeLocation();
          e.setPos(loc.getX(), loc.getY(), loc.getZ());
        })
        .build();
    EntityManager.spawnEntity(this.currentMap.getWorld().getBukkitWorld(), this.mainSlime);
  }

  @Override
  public void start() {
    super.start();

    this.spawnTask = Server.runTaskTimerSynchrony(this::spawnSlimes, 0, 20, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    this.calcPlaces(true);
    super.stop();

    if (this.spawnTask != null) {
      this.spawnTask.cancel();
    }
  }

  @Override
  public void reset() {
    super.reset();

    if (this.mainSlime != null) {
      this.mainSlime.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  public ExLocation getMotherSlimeLocation() {
    return this.currentMap.getLocation(MAIN_SLIME_LOC_INDEX);
  }

  private void spawnSlimes() {
    List<ExLocation> locs = this.currentMap.getLocations(SLIME_SPAWN_LOC_START_INDEX);
    for (ExLocation loc : locs) {
      if (Server.getRandom().nextDouble() <= SLIME_SPAWN_CHANCE * Math.sqrt(Server.getInGameUsers().size()) / locs.size()) {
        int max = Server.getRandom().nextInt(1, MAX_SLIMES_PER_SPAWN + 1);
        for (int i = 0; i < max; i++) {
          Slime slime = new SlimeBuilder(loc.getExWorld().getHandle(), true, false, true)
              .applyOnEntity(e -> {
                e.setSize(1, true);
                e.setPos(loc.getX(), loc.getY(), loc.getZ());
              })
              .build();
          EntityManager.spawnEntity(loc.getWorld(), slime);
        }
      }
    }
  }

  @Override
  public boolean hasSideboard() {
    return true;
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
  public void onEntityInteract(PlayerInteractEntityEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    Entity entity = e.getRightClicked();

    if (!entity.equals(this.mainSlime.getBukkitEntity())) {
      return;
    }

    User user = Server.getUser(e.getPlayer());

    if (user == null || user.isService()) {
      return;
    }

    feedByUser(user);
  }

  private void feedByUser(User user) {
    boolean success = user.removeCertainItemStack(SLIME_BALL.asOne());

    if (!success) {
      return;
    }

    this.slimeSize += SLIME_GROWTH;
    this.mainSlime.setSize((int) this.slimeSize, false);
    this.updateUserScore(user, (u, s) -> s + 1);
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (!e.getEntityType().equals(EntityType.SLIME)) {
      return;
    }

    e.setDroppedExp(0);
    e.getDrops().clear();

    if (e.getEntity().getKiller() == null) {
      return;
    }

    User user = Server.getUser(e.getEntity().getKiller());

    if (user == null || user.isService()) {
      return;
    }

    boolean full = user.containsAtLeast(SLIME_BALL.asOne(), MAX_SLIME_BALLS, true) >= 0;

    if (!full) {
      user.addItem(SLIME_BALL.cloneWithId().asOne());
    }
  }

}
