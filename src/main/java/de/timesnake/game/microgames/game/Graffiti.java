/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.*;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.user.inventory.UserInventoryInteractEvent;
import de.timesnake.basic.bukkit.util.user.inventory.UserInventoryInteractListener;
import de.timesnake.game.microgames.chat.Plugin;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Chat;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class Graffiti extends MicroGame implements Listener, UserInventoryInteractListener {

  private static final Integer BLUE_SPAWN_LOCATION_INDEX = 3;
  private static final Integer RED_SPAWN_LOCATION_INDEX = 4;

  private static final int MAP_RADIUS = 100;

  private static final int TIME = 180;
  private static final int RADIUS = 5;
  private static final int COOLDOWN = 4;
  private static final double DAMAGE = 6;
  private static final ExItemStack PAINT_GUN =
      new ExItemStack(Material.SEA_PICKLE).setMoveable(false).setDropable(false).setSlot(0);


  private static final HashMap<Material, Material> BLUE_PAINT_MAP = new HashMap<>();
  private static final HashMap<Material, Material> RED_PAINT_MAP = new HashMap<>();

  private static final List<BlockFace> BLOCK_FACES = List.of(BlockFace.NORTH, BlockFace.EAST,
      BlockFace.SOUTH,
      BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

  private static final List<BlockFace> JUMP_BLOCK_FACES = List.of(BlockFace.NORTH, BlockFace.EAST,
      BlockFace.SOUTH,
      BlockFace.WEST);
  private static final List<Material> JUMP_BLOCK_TYPES = List.of(Material.WHITE_STAINED_GLASS,
      Material.BLUE_STAINED_GLASS,
      Material.RED_STAINED_GLASS);


  static {
    BLUE_PAINT_MAP.put(Material.WHITE_WOOL, Material.BLUE_WOOL);
    BLUE_PAINT_MAP.put(Material.RED_WOOL, Material.BLUE_WOOL);

    RED_PAINT_MAP.put(Material.WHITE_WOOL, Material.RED_WOOL);
    RED_PAINT_MAP.put(Material.BLUE_WOOL, Material.RED_WOOL);

    BLUE_PAINT_MAP.put(Material.GLASS, Material.BLUE_STAINED_GLASS);
    BLUE_PAINT_MAP.put(Material.RED_STAINED_GLASS, Material.BLUE_STAINED_GLASS);

    RED_PAINT_MAP.put(Material.GLASS, Material.RED_STAINED_GLASS);
    RED_PAINT_MAP.put(Material.BLUE_STAINED_GLASS, Material.RED_STAINED_GLASS);

    BLUE_PAINT_MAP.put(Material.GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);
    BLUE_PAINT_MAP.put(Material.RED_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);

    RED_PAINT_MAP.put(Material.GLASS_PANE, Material.RED_STAINED_GLASS_PANE);
    RED_PAINT_MAP.put(Material.BLUE_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE);

    BLUE_PAINT_MAP.put(Material.WHITE_STAINED_GLASS, Material.BLUE_STAINED_GLASS);
    BLUE_PAINT_MAP.put(Material.RED_STAINED_GLASS, Material.BLUE_STAINED_GLASS);

    RED_PAINT_MAP.put(Material.WHITE_STAINED_GLASS, Material.RED_STAINED_GLASS);
    RED_PAINT_MAP.put(Material.BLUE_STAINED_GLASS, Material.RED_STAINED_GLASS);

    BLUE_PAINT_MAP.put(Material.WHITE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);
    BLUE_PAINT_MAP.put(Material.RED_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);

    RED_PAINT_MAP.put(Material.WHITE_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE);
    RED_PAINT_MAP.put(Material.BLUE_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE);

    BLUE_PAINT_MAP.put(Material.WHITE_TERRACOTTA, Material.BLUE_TERRACOTTA);
    BLUE_PAINT_MAP.put(Material.RED_TERRACOTTA, Material.BLUE_TERRACOTTA);

    RED_PAINT_MAP.put(Material.WHITE_TERRACOTTA, Material.RED_TERRACOTTA);
    RED_PAINT_MAP.put(Material.BLUE_TERRACOTTA, Material.RED_TERRACOTTA);

    BLUE_PAINT_MAP.put(Material.WHITE_CONCRETE, Material.BLUE_CONCRETE);
    BLUE_PAINT_MAP.put(Material.RED_CONCRETE, Material.BLUE_CONCRETE);

    RED_PAINT_MAP.put(Material.WHITE_CONCRETE, Material.RED_CONCRETE);
    RED_PAINT_MAP.put(Material.BLUE_CONCRETE, Material.RED_CONCRETE);

    BLUE_PAINT_MAP.put(Material.WHITE_CONCRETE_POWDER, Material.BLUE_CONCRETE_POWDER);
    BLUE_PAINT_MAP.put(Material.RED_CONCRETE_POWDER, Material.BLUE_CONCRETE_POWDER);

    RED_PAINT_MAP.put(Material.WHITE_CONCRETE_POWDER, Material.RED_CONCRETE_POWDER);
    RED_PAINT_MAP.put(Material.BLUE_CONCRETE_POWDER, Material.RED_CONCRETE_POWDER);
  }

  private final Set<User> blueTeam = new HashSet<>();
  private final Set<User> redTeam = new HashSet<>();
  private final Set<User> cooldownUsers = new HashSet<>();
  private final Map<User, BukkitTask> jumpTasksByUser = new HashMap<>();
  private Integer time = TIME;
  private BukkitTask task;

  public Graffiti() {
    super("graffiti", "Graffiti", Material.SEA_PICKLE,
        "Paint the most walls", List.of(), 2, null);

    Server.registerListener(this, GameMicroGames.getPlugin());
    Server.getInventoryEventManager().addInteractListener(this, PAINT_GUN);
  }

  @Override
  public Integer getLocationAmount() {
    return 5;
  }

  @Override
  public void load() {
    super.load();

  }

  @Override
  public void onMapLoad(de.timesnake.basic.game.util.game.Map map) {
    super.onMapLoad(map);
    map.getWorld().setGameRule(GameRule.NATURAL_REGENERATION, false);
  }

  @Override
  protected void loadDelayed() {

    List<User> users = new ArrayList<>(Server.getPreGameUsers());
    Collections.shuffle(users);

    int teamSize = (int) Math.ceil(users.size() / 2d);

    for (User user : users) {
      user.lockInventoryItemMove();
      user.lockInventory();

      if (this.blueTeam.size() < teamSize) {
        this.blueTeam.add(user);
        user.sendPluginMessage(Plugin.MICRO_GAMES,
            Component.text("You joined", ExTextColor.PERSONAL)
                .append(Component.text(" " + "blue", ExTextColor.VALUE)));
        user.teleport(this.currentMap.getLocation(BLUE_SPAWN_LOCATION_INDEX));
      } else {
        this.redTeam.add(user);
        user.sendPluginMessage(Plugin.MICRO_GAMES,
            Component.text("You joined", ExTextColor.PERSONAL)
                .append(Component.text(" " + "red", ExTextColor.VALUE)));
        user.teleport(this.currentMap.getLocation(RED_SPAWN_LOCATION_INDEX));
      }

      this.setItems(user);
    }
  }

  private String getTimeString(Integer time) {
    if (time >= 60) {
      return time / 60 + "min " + time % 60 + "s";
    } else {
      return time % 60 + "s";
    }
  }

  @Override
  public void start() {
    super.start();

    this.task = Server.runTaskTimerSynchrony(() -> {
      this.time--;
      super.sideboard.setScore(5, this.getTimeString(time));

      if (this.time == 0) {
        this.stop();
      }
    }, 0, 20, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {

    int blueBlocks = 0;
    int redBlocks = 0;

    Location middle = this.getStartLocation();

    for (int x = -MAP_RADIUS; x <= MAP_RADIUS; x++) {
      for (int y = -MAP_RADIUS; y <= MAP_RADIUS; y++) {
        for (int z = -MAP_RADIUS; z <= MAP_RADIUS; z++) {
          Block block = middle.getBlock().getRelative(x, y, z);

          if (BLUE_PAINT_MAP.containsValue(block.getType())) {
            blueBlocks++;
          } else if (RED_PAINT_MAP.containsValue(block.getType())) {
            redBlocks++;
          }
        }
      }
    }

    MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

    if (blueBlocks > redBlocks) {
      MicroGamesServer.broadcastTitle(Component.text("Blue", ExTextColor.BLUE)
              .append(Component.text(" wins", ExTextColor.WHITE)),
          Component.text(blueBlocks, ExTextColor.BLUE)
              .append(Component.text(" - ", ExTextColor.PUBLIC))
              .append(Component.text(redBlocks, ExTextColor.RED)),
          Duration.ofSeconds(3));
      MicroGamesServer.broadcastMicroGamesMessage(Component.text("Blue", ExTextColor.BLUE)
          .append(Component.text(" wins!", ExTextColor.WHITE)));
    } else if (redBlocks > blueBlocks) {
      MicroGamesServer.broadcastTitle(Component.text("Red", ExTextColor.RED)
              .append(Component.text(" wins", ExTextColor.WHITE)),
          Component.text(blueBlocks, ExTextColor.BLUE)
              .append(Component.text(" - ", ExTextColor.PUBLIC))
              .append(Component.text(redBlocks, ExTextColor.RED)),
          Duration.ofSeconds(3));
      MicroGamesServer.broadcastMicroGamesMessage(Component.text("Red", ExTextColor.RED)
          .append(Component.text(" wins!", ExTextColor.WHITE)));
    } else {
      MicroGamesServer.broadcastTitle(Component.text("Draw", ExTextColor.WHITE),
          Component.text(blueBlocks, ExTextColor.BLUE)
              .append(Component.text(" - ", ExTextColor.PUBLIC))
              .append(Component.text(redBlocks, ExTextColor.RED)),
          Duration.ofSeconds(3));
      MicroGamesServer.broadcastMicroGamesMessage(Component.text("Draw", ExTextColor.WHITE));
    }

    MicroGamesServer.broadcastMicroGamesMessage(Component.text("Blocks: ", ExTextColor.WHITE)
        .append(Component.text(blueBlocks, ExTextColor.BLUE))
        .append(Component.text(" - ", ExTextColor.PUBLIC))
        .append(Component.text(redBlocks, ExTextColor.RED)));
    MicroGamesServer.broadcastMicroGamesMessage(Chat.getLineSeparator());

    if (this.task != null) {
      this.task.cancel();
    }

    super.stop();
  }

  @Override
  public void reset() {
    super.reset();
    this.time = TIME;
    this.cooldownUsers.clear();
    this.jumpTasksByUser.clear();

    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
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
    if (Server.getInGameUsers().size() <= 1) {
      this.stop();
    }
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (e.getDamageCause().equals(EntityDamageEvent.DamageCause.FALL)) {
      e.setCancelled(true);
      e.setCancelDamage(true);
    }
  }

  @EventHandler
  public void onItemDrop(UserDropItemEvent e) {
    e.setCancelled(true);
  }

  @EventHandler
  public void onJump(PlayerJumpEvent e) {

    if (!this.isGameRunning()) {
      return;
    }

    User user = Server.getUser(e.getPlayer());
    Block userBlock = user.getLocation().getBlock().getRelative(0, 1, 0);

    if (user.getLocation().getPitch() > -45) {
      return;
    }

    for (BlockFace face : JUMP_BLOCK_FACES) {
      Block block = userBlock.getRelative(face);

      if (JUMP_BLOCK_TYPES.contains(block.getType())) {
        user.setVelocity(new Vector(0, 0.9, 0));
        this.runJump(user);
        break;
      }
    }

  }

  private void runJump(User user) {
    this.jumpTasksByUser.put(user, Server.runTaskTimerAsynchrony(() -> {
      if (user.getLocation().getPitch() > -45) {
        if (this.jumpTasksByUser.get(user) != null) {
          this.jumpTasksByUser.get(user).cancel();
        }
        this.jumpTasksByUser.remove(user);
        return;
      }

      boolean found = false;
      for (BlockFace face : JUMP_BLOCK_FACES) {
        Block block = user.getLocation().getBlock().getRelative(face);

        if (JUMP_BLOCK_TYPES.contains(block.getType())) {
          user.setVelocity(new Vector(0, 0.9, 0));
          found = true;
          break;
        }
      }

      if (!found) {
        this.jumpTasksByUser.get(user).cancel();
        this.jumpTasksByUser.remove(user);
      }
    }, 5, 10, GameMicroGames.getPlugin()));
  }

  @EventHandler
  public void onUserDamageByUser(UserDamageByUserEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (!e.getUserDamager().getStatus().equals(Status.User.IN_GAME)) {
      e.setCancelDamage(true);
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onUserDeath(UserDeathEvent event) {
    if (!this.isGameRunning()) {
      return;
    }

    event.getDrops().clear();
    event.setAutoRespawn(true);
    event.setKeepInventory(true);
    event.setBroadcastDeathMessage(false);
  }

  @EventHandler
  public void onUserRespawn(UserRespawnEvent event) {
    if (!this.isGameRunning()) {
      return;
    }

    User user = event.getUser();

    if (this.blueTeam.contains(user)) {
      event.setRespawnLocation(this.currentMap.getLocation(BLUE_SPAWN_LOCATION_INDEX));
    } else {
      event.setRespawnLocation(this.currentMap.getLocation(RED_SPAWN_LOCATION_INDEX));
    }
  }

  @Override
  public void onUserInventoryInteract(UserInventoryInteractEvent event) {
    if (!this.isGameRunning()) {
      return;
    }

    User user = event.getUser();

    if (this.cooldownUsers.contains(user)) {
      return;
    }

    this.cooldownUsers.add(user);

    Snowball paintball = user.getWorld()
        .spawn(user.getEyeLocation().add(0, -0.5, 0), Snowball.class);
    paintball.setShooter(user.getPlayer());
    paintball.setVelocity(user.getLocation().getDirection().normalize().multiply(1.5));

    Server.runTaskLaterSynchrony(() -> this.cooldownUsers.remove(user), COOLDOWN,
        GameMicroGames.getPlugin());
  }

  @EventHandler
  public void onProjectileHit(ProjectileHitEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (!e.getEntity().getType().equals(EntityType.SNOWBALL)) {
      return;
    }

    if (!(e.getEntity().getShooter() instanceof Player p)) {
      return;
    }

    User user = Server.getUser(p);

    if (e.getHitBlock() != null) {
      Server.runTaskAsynchrony(() -> {
        Set<Block> blocksToPaint = new HashSet<>();

        Block hitBlock = e.getHitBlock();

        Location middle = hitBlock.getLocation().add(0.5, 0.5, 0.5);

        Map<Material, Material> paintMap =
            this.blueTeam.contains(user) ? BLUE_PAINT_MAP : RED_PAINT_MAP;

        Projectile projectile = e.getEntity();

        Location origin = projectile.getOrigin();

        for (int x = -RADIUS; x <= RADIUS; x++) {
          for (int y = -RADIUS; y <= RADIUS; y++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

              Block block = hitBlock.getRelative(x, y, z);
              Location blockMiddle = block.getLocation().add(0.5, 0.5, 0.5);

              if (middle.distanceSquared(blockMiddle) > RADIUS) {
                continue;
              }

              if (paintMap.get(block.getType()) == null) {
                continue;
              }

              for (BlockFace face : BLOCK_FACES) {
                RayTraceResult res = origin.getBlock().getWorld()
                    .rayTraceBlocks(origin,
                        blockMiddle.add(face.getDirection().multiply(0.5))
                            .toVector().subtract(origin.toVector()), 32,
                        FluidCollisionMode.NEVER, true);

                if (res == null) {
                  continue;
                }

                if (block.equals(res.getHitBlock())) {
                  blocksToPaint.add(block);
                  break;
                }
              }

            }
          }
        }

        Server.runTaskSynchrony(() -> {
          for (Block block : blocksToPaint) {
            block.setType(paintMap.get(block.getType()));
          }
        }, GameMicroGames.getPlugin());
      }, GameMicroGames.getPlugin());
    }

    if (e.getHitEntity() != null && e.getHitEntity() instanceof Player hitPlayer) {
      User hitUser = Server.getUser(hitPlayer);

      if ((this.blueTeam.contains(user) && !this.blueTeam.contains(hitUser))
          || (!this.blueTeam.contains(user) && this.blueTeam.contains(hitUser))) {
        hitUser.damage(DAMAGE, p);
        user.playSound(Sound.ENTITY_PLAYER_LEVELUP, 2);
      } else {
        e.setCancelled(true);
      }
    }
  }

  private void setItems(User user) {
    user.setItem(PAINT_GUN);

    if (this.blueTeam.contains(user)) {
      user.setItem(EquipmentSlot.HEAD,
          ExItemStack.getLeatherArmor(Material.LEATHER_HELMET, Color.BLUE));
      user.setItem(EquipmentSlot.CHEST,
          ExItemStack.getLeatherArmor(Material.LEATHER_CHESTPLATE, Color.BLUE));
      user.setItem(EquipmentSlot.LEGS,
          ExItemStack.getLeatherArmor(Material.LEATHER_LEGGINGS, Color.BLUE));
      user.setItem(EquipmentSlot.FEET,
          ExItemStack.getLeatherArmor(Material.LEATHER_BOOTS, Color.BLUE));
    } else {
      user.setItem(EquipmentSlot.HEAD,
          ExItemStack.getLeatherArmor(Material.LEATHER_HELMET, Color.RED));
      user.setItem(EquipmentSlot.CHEST,
          ExItemStack.getLeatherArmor(Material.LEATHER_CHESTPLATE, Color.RED));
      user.setItem(EquipmentSlot.LEGS,
          ExItemStack.getLeatherArmor(Material.LEATHER_LEGGINGS, Color.RED));
      user.setItem(EquipmentSlot.FEET,
          ExItemStack.getLeatherArmor(Material.LEATHER_BOOTS, Color.RED));
    }
  }
}
