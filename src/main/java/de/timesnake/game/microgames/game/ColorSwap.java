/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.core.server.MathHelper;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.game.microgames.game.basis.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class ColorSwap extends FallOutGame implements Listener {

  protected static final Integer FIRST_CORNER_LOCATION_INDEX = 3;
  protected static final Integer SECOND_CORNER_LOCATION_INDEX = 4;

  protected static final Integer MAX_LEVEL = 30;

  protected static final List<Material> MATERIALS = List.of(Material.BLACK_WOOL, Material.BLUE_WOOL, Material.GRAY_WOOL,
      Material.GREEN_WOOL, Material.LIGHT_BLUE_WOOL, Material.LIGHT_GRAY_WOOL, Material.LIME_WOOL, Material.MAGENTA_WOOL,
      Material.ORANGE_WOOL, Material.PURPLE_WOOL, Material.RED_WOOL, Material.WHITE_WOOL, Material.YELLOW_WOOL);

  protected static final Integer LEVEL_TICKS = 2 * 20; //for first level
  protected static final Integer MIN_LEVEL_TICKS = 6;

  protected static final Integer DIFFERENT_MATERIALS = 6; //for first level
  protected static final double DIFFERENT_MATERIALS_INCREASE = 0.25; // material increase per level-1

  private final Random random = new Random();

  private final HashMap<Integer, Level> levels = new HashMap<>();
  protected int beginX;
  protected int beginZ;
  protected int endX;
  protected int endZ;
  private Integer currentLevel = 1;
  private BukkitTask waitingTask;
  private BukkitTask countdownTask;
  private BukkitTask countdownTask1;

  public ColorSwap() {
    this("colorswap",
        "ColorSwap",
        Material.WHITE_WOOL,
        "Try to stand on the color, which is shown in your hotbar",
        List.of("§hGoal: §plast man standing", "Stand on the color, which is shown in your hotbar."),
        1,
        null);
  }

  public ColorSwap(String name, String displayName, Material material, String headLine, List<String> description,
                   Integer minPlayers, Duration maxTime) {
    super(name, displayName, material, headLine, description, minPlayers, maxTime);
    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public Integer getLocationAmount() {
    return 5;
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

    List<Integer> timings = MathHelper.getDecreasingValues(LEVEL_TICKS, 3, MAX_LEVEL, MIN_LEVEL_TICKS);
    for (int i = 0; i < ColorSwap.MAX_LEVEL; i++) {
      this.levels.put(i + 1, new Level(i + 1, timings.get(i)));
    }
  }

  @Override
  public void load() {
    super.load();

    super.sideboard.setScore(4, "§c§lLevel");
    super.sideboard.setScore(3, "§f" + this.currentLevel);
    super.sideboard.setScore(2, "§f-------------");
    super.sideboard.setScore(1, "§9§lPlayers");
    super.sideboard.setScore(0, String.valueOf(Server.getPreGameUsers().size()));
  }

  @Override
  protected void loadDelayed() {
    super.loadDelayed();

    for (User user : Server.getPreGameUsers()) {
      user.lockInventory();
    }
  }

  @Override
  public void start() {
    super.start();
    this.startLevel();
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

      countdownTask = Server.runTaskLaterSynchrony(() -> {
        level.switchPatternToPrimaryMaterial();
        Server.broadcastNote(Instrument.STICKS, Note.natural(0, Note.Tone.A));

        waitingTask = Server.runTaskLaterSynchrony(() -> {
          for (User user : Server.getInGameUsers()) {
            user.fillHotBar(new ItemStack(Material.AIR));
          }
          if (this.currentLevel.equals(MAX_LEVEL)) {
            MicroGamesServer.broadcastMicroGamesMessage(Component.text("You completed all levels", ExTextColor.GOLD));
            this.stop();
          } else {
            this.currentLevel++;
            this.startLevel();
          }
        }, 2 * 20, GameMicroGames.getPlugin());
      }, level.getTicks(), GameMicroGames.getPlugin());

    }, 3 * 20, GameMicroGames.getPlugin());
  }

  private void clearFloor() {
    ExWorld world = this.currentMap.getWorld();
    int y = this.getFirstCorner().getBlockY();

    for (int x = this.beginX; x <= this.endX; x++) {
      for (int z = this.beginZ; z <= this.endZ; z++) {
        Block block = world.getBlockAt(x, y, z);
        if (this.isReplaceable(block)) {
          block.setType(Material.WHITE_WOOL);
        }
      }
    }
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

  @Override
  protected void addWinner(MicroGamesUser user, boolean firstWins) {
    super.addWinner(user, firstWins);
    super.sideboard.setScore(0, String.valueOf(Server.getInGameUsers().size()));
  }

  private boolean isReplaceable(Block block) {
    return block.isEmpty() || Tag.WOOL.isTagged(block.getType());
  }

  private class Level {

    private final Integer level;
    private final Integer ticks;
    private final Material primaryMaterial;
    private final HashMap<Block, Material> blocks = new HashMap<>();

    protected Level(Integer level, Integer ticks) {
      this.level = level;
      this.ticks = ticks;

      int materialsNumber = (int) (ColorSwap.DIFFERENT_MATERIALS + (level - 1) * ColorSwap.DIFFERENT_MATERIALS_INCREASE);

      ArrayList<Material> materials = new ArrayList<>(ColorSwap.MATERIALS);
      ArrayList<Material> selectedMaterials = new ArrayList<>();

      //get Materials
      for (int i = 0; i < materialsNumber; i++) {
        int index = ColorSwap.this.random.nextInt(materials.size());
        selectedMaterials.add(materials.get(index));
        materials.remove(index);
      }

      HashSet<Material> availableMaterials = new HashSet<>();

      //create pattern and save in HashMap
      ExWorld world = ColorSwap.this.currentMap.getWorld();
      int y = ColorSwap.this.getFirstCorner().getBlockY();

      int type = ColorSwap.this.random.nextInt(3);

      switch (type) {
        case 0 -> {
          for (int x = ColorSwap.this.beginX; x <= ColorSwap.this.endX; x += 2) {
            for (int z = ColorSwap.this.beginZ; z <= ColorSwap.this.endZ; z += 2) {

              Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);

              for (int patternX = 0; patternX < 2; patternX++) {
                for (int patternZ = 0; patternZ < 2; patternZ++) {
                  if (x + patternX <= ColorSwap.this.endX && z + patternZ <= ColorSwap.this.endZ) {
                    this.blocks.put(world.getBlockAt(x + patternX, y, z + patternZ), material);
                  }
                }
              }
            }
          }
        }
        case 1 -> {
          boolean xz = ColorSwap.this.random.nextBoolean();
          if (xz) {
            int halfZEnd = ColorSwap.this.endZ - (ColorSwap.this.endZ - ColorSwap.this.beginZ) / 2;

            for (int x = ColorSwap.this.beginX; x <= ColorSwap.this.endX; x += 1) {

              Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);

              for (int z = ColorSwap.this.beginZ; z <= halfZEnd; z += 1) {
                this.blocks.put(world.getBlockAt(x, y, z), material);
              }

              material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);

              for (int z = halfZEnd + 1; z <= ColorSwap.this.endZ; z += 1) {
                this.blocks.put(world.getBlockAt(x, y, z), material);
              }
            }
          } else {
            int halfXEnd = ColorSwap.this.endX - (ColorSwap.this.endX - ColorSwap.this.beginX) / 2;

            for (int z = ColorSwap.this.beginZ; z <= ColorSwap.this.endZ; z += 1) {

              Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);

              for (int x = ColorSwap.this.beginX; x <= halfXEnd; x += 1) {
                this.blocks.put(world.getBlockAt(x, y, z), material);
              }

              material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);

              for (int x = halfXEnd + 1; x <= ColorSwap.this.endX; x += 1) {
                this.blocks.put(world.getBlockAt(x, y, z), material);
              }
            }
          }
        }
        case 2 -> {
          for (int x = ColorSwap.this.beginX; x <= ColorSwap.this.endX; x += 1) {
            for (int z = ColorSwap.this.beginZ; z <= ColorSwap.this.endZ; z += 1) {
              Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);
              this.blocks.put(world.getBlockAt(x, y, z), material);
            }
          }
        }
      }

      //get primaryMaterial
      this.primaryMaterial = new ArrayList<>(availableMaterials).get(ColorSwap.this.random.nextInt(availableMaterials.size()));
    }

    public void switchPattern() {
      for (Map.Entry<Block, Material> entry : this.blocks.entrySet()) {
        Block block = entry.getKey();
        if (ColorSwap.this.isReplaceable(block)) {
          block.setType(entry.getValue());
        }
      }
    }

    public void switchPatternToPrimaryMaterial() {
      for (Map.Entry<Block, Material> entry : this.blocks.entrySet()) {
        if (!entry.getValue().equals(this.primaryMaterial) && ColorSwap.this.isReplaceable(entry.getKey())) {
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
