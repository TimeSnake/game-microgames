/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.UserMoveEvent;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.game.microgames.game.basis.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.ExTextColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class ColorSwap extends FallOutGame implements Listener {

  protected static final Integer SPEC_LOCATION_INDEX = 0;
  protected static final Integer START_LOCATION_INDEX = 1;
  protected static final Integer FIRST_CORNER_LOCATION_INDEX = 2;
  protected static final Integer SECOND_CORNER_LOCATION_INDEX = 3;

  protected static final Integer MAX_LEVEL = 30;

  protected static final Material[] MATERIALS = {Material.BLACK_WOOL, Material.BLUE_WOOL,
      Material.GRAY_WOOL,
      Material.GREEN_WOOL, Material.LIGHT_BLUE_WOOL, Material.LIGHT_GRAY_WOOL,
      Material.LIME_WOOL,
      Material.MAGENTA_WOOL, Material.ORANGE_WOOL, Material.PURPLE_WOOL, Material.RED_WOOL,
      Material.WHITE_WOOL
      , Material.YELLOW_WOOL};

  protected static final Integer PATTERN_SIZE = 2; //beware of arena-size

  protected static final Integer LEVEL_TICKS = 4 * 20; //for first level
  protected static final Integer MIN_LEVEL_TICKS = 6;
  protected static final double LEVEL_TICKS_DECREASE = 7; //time decrease between first and second level

  protected static final Integer DIFFERENT_MATERIALS = 6; //for first level
  protected static final double DIFFERENT_MATERIALS_INCREASE = 0.25; // material increase per level-1

  private final Random random = new Random();

  private final HashMap<Integer, Level> levels = new HashMap<>();
  int beginX;
  int beginZ;
  int endX;
  int endZ;
  private Integer currentLevel = 1;
  private BukkitTask waitingTask;
  private BukkitTask countdownTask;
  private BukkitTask countdownTask1;
  private Integer ticks = 0;

  public ColorSwap() {
    super("colorswap", "ColorSwap", Material.WHITE_WOOL,
        "Try to stand on the color, which is shown in your " +
            "hotbar", 1, -1);

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public Integer getLocationAmount() {
    return 4;
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public void prepare() {
    super.prepare();

    super.currentMap.getWorld().setPVP(false);

    ExLocation firstCorner = this.getFirstCorner();
    ExLocation secondCorner = this.getSecondCorner();

    this.beginX = Math.min(firstCorner.getBlockX(), secondCorner.getBlockX());
    this.beginZ = Math.min(firstCorner.getBlockZ(), secondCorner.getBlockZ());

    this.endX = Math.max(firstCorner.getBlockX(), secondCorner.getBlockX());
    this.endZ = Math.max(firstCorner.getBlockZ(), secondCorner.getBlockZ());

    this.clearFloor();

    for (int i = 1; i <= ColorSwap.MAX_LEVEL; i++) {
      this.levels.put(i, new Level(i));
    }
  }

  @Override
  public void load() {
    super.load();

    super.sideboard.setScore(4, "§c§lLevel");
    super.sideboard.setScore(3, "§f" + this.currentLevel);
    super.sideboard.setScore(2, "§f-------------");
    super.sideboard.setScore(1, "§9§lPlayers");
    super.sideboard.setScore(0, Server.getPreGameUsers().size() + "");
  }

  @Override
  protected void loadDelayed() {
    for (User user : Server.getPreGameUsers()) {
      user.teleport(this.getStartLocation());
      user.lockInventory();
    }
  }

  @Override
  public void start() {
    super.start();
    this.startLevel();
  }

  public void startLevel() {
    super.sideboard.setScore(3, "§f" + this.currentLevel);
    Level level = this.levels.get(this.currentLevel);
    level.switchPattern();

    if (this.countdownTask != null) {
      this.countdownTask.cancel();
    }

    countdownTask1 = Server.runTaskLaterSynchrony(() -> {

      for (User user : Server.getInGameUsers()) {
        user.fillHotBar(new ItemStack(level.getPrimaryMaterial()));
      }
      Server.broadcastNote(Instrument.PLING, Note.natural(1, Note.Tone.A));

      this.ticks = level.getTicks();

      countdownTask = Server.runTaskTimerSynchrony(() -> {
        if (ticks == 0) {
          level.switchPatternToPrimaryMaterial();
          Server.broadcastNote(Instrument.STICKS, Note.natural(0, Note.Tone.A));

          waitingTask = Server.runTaskLaterSynchrony(() -> {
            for (User user : Server.getInGameUsers()) {
              user.fillHotBar(new ItemStack(Material.AIR));
            }
            if (this.currentLevel.equals(MAX_LEVEL)) {
              MicroGamesServer.broadcastMicroGamesMessage(
                  Component.text("You completed all levels", ExTextColor.GOLD));
              this.stop();
            } else {
              this.currentLevel++;
              this.startLevel();
            }
          }, 2 * 20, GameMicroGames.getPlugin());
        }

        ticks--;
      }, 0, 1, GameMicroGames.getPlugin());

    }, 3 * 20, GameMicroGames.getPlugin());
  }

  @Override
  public void stop() {
    for (User user : Server.getGameNotServiceUsers()) {
      user.unlockInventory();
    }

    if (this.waitingTask != null) {
      this.waitingTask.cancel();
    }

    if (this.countdownTask != null) {
      this.countdownTask.cancel();
    }

    if (this.countdownTask1 != null) {
      this.countdownTask1.cancel();
    }

    this.clearFloor();

    super.stop();
  }

  @Override
  public void reset() {
    super.reset();
    this.clearFloor();

    this.currentLevel = 1;
    super.sideboard.setScore(3, "§f" + this.currentLevel);

    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  private void clearFloor() {
    ExWorld world = this.currentMap.getWorld();
    int y = this.getFirstCorner().getBlockY() - 1;

    for (int x = this.beginX; x <= this.endX; x++) {
      for (int z = this.beginZ; z <= this.endZ; z++) {
        world.getBlockAt(x, y, z).setType(Material.WHITE_WOOL);
      }
    }
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

  @Override
  public ExLocation getSpecLocation() {
    return super.currentMap.getLocation(SPEC_LOCATION_INDEX);
  }

  @Override
  public ExLocation getStartLocation() {
    return super.currentMap.getLocation(START_LOCATION_INDEX);
  }

  public ExLocation getFirstCorner() {
    return this.currentMap.getLocation(FIRST_CORNER_LOCATION_INDEX);
  }

  public ExLocation getSecondCorner() {
    return this.currentMap.getLocation(SECOND_CORNER_LOCATION_INDEX);
  }

  @Override
  public Integer getDeathHeight() {
    return this.getFirstCorner().getBlockY();
  }

  @EventHandler
  public void onUserMove(UserMoveEvent e) {
    User user = e.getUser();

    if (!this.isGameRunning()) {
      return;
    }

    if (user.getStatus().equals(Status.User.IN_GAME)) {
      if (e.getTo().getBlockY() < this.getFirstCorner().getBlockY()) {
        super.onUserMove(e);
        super.sideboard.setScore(0, Server.getInGameUsers().size() + "");
      }
    }
  }

  private class Level {

    private final Integer level;
    private final Integer ticks;
    private final Material primaryMaterial;
    private final HashMap<Block, Material> blocks = new HashMap<>();

    protected Level(Integer level) {
      this.level = level;
      int tickDifference = ColorSwap.LEVEL_TICKS - ColorSwap.MIN_LEVEL_TICKS;
      this.ticks = (int) (
          tickDifference * Math.pow((double) (tickDifference - 1) / tickDifference,
              ColorSwap.LEVEL_TICKS_DECREASE * this.level)
              + ColorSwap.MIN_LEVEL_TICKS);

      int differentMaterials =
          (int) (ColorSwap.DIFFERENT_MATERIALS
              + (level - 1) * ColorSwap.DIFFERENT_MATERIALS_INCREASE);

      ArrayList<Material> materials = new ArrayList<>(Arrays.asList(ColorSwap.MATERIALS));

      ArrayList<Material> selectedMaterials = new ArrayList<>();

      //get Materials
      for (int i = 0; i < differentMaterials; i++) {
        int index = ColorSwap.this.random.nextInt(materials.size());
        selectedMaterials.add(materials.get(index));
        materials.remove(index);
      }

      //create pattern and save in HashMap
      ExWorld world = ColorSwap.this.currentMap.getWorld();
      int y = ColorSwap.this.getSpecLocation().getBlockY() - 1;

      int type = ColorSwap.this.random.nextInt(3);

      if (type == 0) {
        for (int x = ColorSwap.this.beginX; x <= ColorSwap.this.endX; x += 2) {
          for (int z = ColorSwap.this.beginZ; z <= ColorSwap.this.endZ; z += 2) {

            Material material =
                selectedMaterials.get(
                    ColorSwap.this.random.nextInt(selectedMaterials.size()));

            for (int patternX = 0; patternX < 2; patternX++) {
              for (int patternZ = 0; patternZ < 2; patternZ++) {
                if (x + patternX <= ColorSwap.this.endX
                    && z + patternZ <= ColorSwap.this.endZ) {
                  this.blocks.put(world.getBlockAt(x + patternX, y, z + patternZ),
                      material);
                }
              }
            }
          }
        }
      } else if (type == 1) {
        boolean xz = ColorSwap.this.random.nextBoolean();

        if (xz) {
          int halfZEnd =
              ColorSwap.this.endZ - (ColorSwap.this.endZ - ColorSwap.this.beginZ) / 2;

          for (int x = ColorSwap.this.beginX; x <= ColorSwap.this.endX; x += 1) {

            Material material =
                selectedMaterials.get(
                    ColorSwap.this.random.nextInt(selectedMaterials.size()));

            for (int z = ColorSwap.this.beginZ; z <= halfZEnd; z += 1) {
              this.blocks.put(world.getBlockAt(x, y, z), material);
            }

            material = selectedMaterials.get(
                ColorSwap.this.random.nextInt(selectedMaterials.size()));

            for (int z = halfZEnd + 1; z <= ColorSwap.this.endZ; z += 1) {
              this.blocks.put(world.getBlockAt(x, y, z), material);
            }
          }
        } else {
          int halfXEnd =
              ColorSwap.this.endX - (ColorSwap.this.endX - ColorSwap.this.beginX) / 2;

          for (int z = ColorSwap.this.beginZ; z <= ColorSwap.this.endZ; z += 1) {

            Material material =
                selectedMaterials.get(
                    ColorSwap.this.random.nextInt(selectedMaterials.size()));

            for (int x = ColorSwap.this.beginX; x <= halfXEnd; x += 1) {
              this.blocks.put(world.getBlockAt(x, y, z), material);
            }

            material = selectedMaterials.get(
                ColorSwap.this.random.nextInt(selectedMaterials.size()));

            for (int x = halfXEnd + 1; x <= ColorSwap.this.endX; x += 1) {
              this.blocks.put(world.getBlockAt(x, y, z), material);
            }
          }
        }

      } else if (type == 2) {
        for (int x = ColorSwap.this.beginX; x <= ColorSwap.this.endX; x += 1) {
          for (int z = ColorSwap.this.beginZ; z <= ColorSwap.this.endZ; z += 1) {
            Material material =
                selectedMaterials.get(
                    ColorSwap.this.random.nextInt(selectedMaterials.size()));
            this.blocks.put(world.getBlockAt(x, y, z), material);
          }
        }
      }

      //get primaryMaterial
      this.primaryMaterial = selectedMaterials.get(
          (int) (Math.random() * (selectedMaterials.size() - 1)));
    }

    public void switchPattern() {
      for (Map.Entry<Block, Material> entry : this.blocks.entrySet()) {
        entry.getKey().setType(entry.getValue());
      }
    }

    public void switchPatternToPrimaryMaterial() {
      for (Map.Entry<Block, Material> entry : this.blocks.entrySet()) {
        if (!entry.getValue().equals(this.primaryMaterial)) {
          entry.getKey().setType(Material.AIR);
        }
      }
    }

    public Integer getLevel() {
      return level;
    }

    public Material getPrimaryMaterial() {
      return primaryMaterial;
    }

    public Integer getTicks() {
      return ticks;
    }

  }

}
