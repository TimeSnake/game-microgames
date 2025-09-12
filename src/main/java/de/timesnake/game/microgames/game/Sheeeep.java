/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.game.microgames.game.basis.ScoreGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.RandomList;
import de.timesnake.library.entities.EntityManager;
import de.timesnake.library.entities.entity.SheepBuilder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftSheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import java.time.Duration;
import java.util.*;

public class Sheeeep extends ScoreGame<Integer> implements ArenaGame, Listener {

  protected static final int COLOR_AMOUNT = 10;
  private static final ExItemStack SHEARS = new ExItemStack(Material.SHEARS).setSlot(4)
      .setMoveable(false).setDropable(false).unbreakable().immutable();
  protected static final List<DyeColor> COLORS = List.of(DyeColor.values());

  protected LinkedList<DyeColor> colors = new LinkedList<>();
  protected Map<User, LinkedList<DyeColor>> colorsByUser = new HashMap<>();

  protected Set<Sheep> sheep = new HashSet<>();

  public Sheeeep() {
    super("sheep",
        "Sheeeep",
        Material.SHEEP_SPAWN_EGG,
        "Shear sheep in given order",
        List.of("§hGoal: §pshear 10 sheep", "Shear sheep with color given in your hotbar.",
            "Be the first who sheared 10 sheep"),
        1,
        Duration.ofMinutes(3));
  }

  @Override
  public void onMapInit(de.timesnake.basic.game.util.game.Map map) {
    super.onMapInit(map);

    map.getWorld().setPVP(false);
  }

  @Override
  public void prepare() {
    super.prepare();

    List<DyeColor> availableColors = new ArrayList<>(COLORS);
    this.colors.clear();

    for (int i = 0; i < COLOR_AMOUNT; i++) {
      this.colors.addLast(availableColors.remove(this.random.nextInt(availableColors.size())));
    }
  }

  @Override
  public void applyBeforeStart() {
    super.applyBeforeStart();

    RandomList<ExBlock> blocks = new RandomList<>(this.getArena().getHighestBlocksInside(this::isValidSpawnBlock));

    this.currentMap.getWorld().getEntitiesByClass(org.bukkit.entity.Sheep.class).forEach(org.bukkit.entity.Entity::remove);

    for (DyeColor color : this.colors) {
      Block block = blocks.getAny();
      this.spawnSheep(ExLocation.fromLocation(block.getLocation().add(0, 1, 0)), color);
    }

    for (User user : Server.getPreGameUsers()) {
      LinkedList<DyeColor> colorOrder = new LinkedList<>(this.colors);
      Collections.shuffle(colorOrder, this.random);
      this.colorsByUser.put(user, colorOrder);
    }
  }

  private boolean isValidSpawnBlock(Block block) {
    return block.getType().equals(Material.GRASS_BLOCK);
  }

  @Override
  public void start() {
    super.start();

    for (User user : Server.getInGameUsers()) {
      this.showNextColorToUser(((MicroGamesUser) user));
    }
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @Override
  public void reset() {
    super.reset();
    this.colors.clear();
    this.colorsByUser.clear();
  }

  private void spawnSheep(ExLocation location, DyeColor color) {
    Sheep sheep = new SheepBuilder()
        .addPathfinderGoal(1, e -> new FloatGoal(e))
        .addPathfinderGoal(2, e -> new WaterAvoidingRandomStrollGoal(e, 2.0D))
        .addPathfinderGoal(3, e -> new LookAtPlayerGoal(e, Player.class, 6.0F))
        .addPathfinderGoal(4, e -> new RandomLookAroundGoal(e))
        .applyOnEntity(e -> {
          e.setColor(net.minecraft.world.item.DyeColor.byId(color.getWoolData()));
          e.setPos(location.getX(), location.getY(), location.getZ());
          e.setInvulnerable(true);
          e.setSpeed(1.2F);
          e.setPersistenceRequired(true);
        })
        .build(location.getExWorld().getHandle());

    EntityManager.spawnEntity(location.getWorld(), sheep);
    this.sheep.add(sheep);
  }

  @EventHandler
  public void onShear(PlayerShearEntityEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (!(e.getEntity() instanceof org.bukkit.entity.Sheep sheep)) {
      return;
    }

    MicroGamesUser user = (MicroGamesUser) Server.getUser(e.getPlayer());

    if (user.isService()) {
      return;
    }

    this.onUserSheared(user, sheep.getColor());

    Server.runTaskLaterSynchrony(() -> ((CraftSheep) sheep).getHandle().ate(), 1, GameMicroGames.getPlugin());
  }

  @EventHandler
  public void onDrop(EntityDropItemEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    e.setCancelled(true);
  }

  private void onUserSheared(MicroGamesUser user, DyeColor color) {
    DyeColor targetColor = this.colorsByUser.get(user).getFirst();

    if (!color.equals(targetColor)) {
      return;
    }

    this.colorsByUser.get(user).removeFirst();
    this.updateUserScore(user, (u, v) -> v != null ? v + 1 : 1);

    if (this.colorsByUser.get(user).isEmpty()) {
      this.calcPlaces(true);
      this.stop();
      return;
    }

    this.showNextColorToUser(user);
  }

  private void showNextColorToUser(MicroGamesUser user) {
    DyeColor color = this.colorsByUser.get(user).getFirst();

    user.fillHotBar(new ExItemStack(getMaterialFromDyeColor(color))
        .setDropable(false).setMoveable(false).immutable());
    user.setItem(SHEARS);
  }

  private static Material getMaterialFromDyeColor(DyeColor color) {
    return switch (color) {
      case RED -> Material.RED_WOOL;
      case BLUE -> Material.BLUE_WOOL;
      case CYAN -> Material.CYAN_WOOL;
      case GRAY -> Material.GRAY_WOOL;
      case LIME -> Material.LIME_WOOL;
      case PINK -> Material.PINK_WOOL;
      case BLACK -> Material.BLACK_WOOL;
      case BROWN -> Material.BROWN_WOOL;
      case GREEN -> Material.GREEN_WOOL;
      case WHITE -> Material.WHITE_WOOL;
      case ORANGE -> Material.ORANGE_WOOL;
      case PURPLE -> Material.PURPLE_WOOL;
      case YELLOW -> Material.YELLOW_WOOL;
      case MAGENTA -> Material.MAGENTA_WOOL;
      case LIGHT_BLUE -> Material.LIGHT_BLUE_WOOL;
      case LIGHT_GRAY -> Material.LIGHT_GRAY_WOOL;
    };
  }
}
