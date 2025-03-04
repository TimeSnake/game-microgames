/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.core.server.MathHelper;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.BlockPolygon;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.game.microgames.game.basis.MicroGame;
import de.timesnake.game.microgames.game.extension.ArenaGame;
import de.timesnake.game.microgames.game.extension.FallOutGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.server.MicroGamesServer;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Tag;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class ColorSwap extends MicroGame implements FallOutGame, ArenaGame, Listener {

  protected static final Integer MAX_LEVEL = 30;

  protected static final List<Material> MATERIALS = List.of(Material.BLACK_WOOL, Material.BLUE_WOOL, Material.GRAY_WOOL,
      Material.GREEN_WOOL, Material.LIGHT_BLUE_WOOL, Material.LIGHT_GRAY_WOOL, Material.LIME_WOOL,
      Material.MAGENTA_WOOL,
      Material.ORANGE_WOOL, Material.PURPLE_WOOL, Material.RED_WOOL, Material.WHITE_WOOL, Material.YELLOW_WOOL);

  protected static final Integer LEVEL_TICKS = 2 * 20; //for first level
  protected static final Integer MIN_LEVEL_TICKS = 6;

  protected static final Integer DIFFERENT_MATERIALS = 6; //for first level
  protected static final double DIFFERENT_MATERIALS_INCREASE = 0.25; // material increase per level-1

  private final Random random = new Random();

  private final HashMap<Integer, Level> levels = new HashMap<>();
  protected BlockPolygon arena;
  protected List<List<ExBlock>> blocks;

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
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public void prepare() {
    super.prepare();

    super.currentMap.getWorld().setPVP(false);

    this.arena = this.getArena();
    this.blocks = this.arena.getBlocksInsideSortedByXThenZ();

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
  public void applyBeforeStart() {
    super.applyBeforeStart();

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
    this.currentLevel = 1;
    super.sideboard.setScore(3, "§f" + this.currentLevel);
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
    for (List<ExBlock> blocks : this.blocks) {
      for (ExBlock block : blocks) {
        if (this.isReplaceable(block)) {
          block.setType(Material.WHITE_WOOL);
        }
      }
    }
  }

  @Override
  public Integer getDeathHeight() {
    return this.arena.getMinHeight();
  }

  @Override
  public void addWinner(MicroGamesUser user, boolean firstWins) {
    super.addWinner(user, firstWins);
    super.sideboard.setScore(0, String.valueOf(Server.getInGameUsers().size()));
  }

  private boolean isReplaceable(ExBlock block) {
    return block.isEmpty() || Tag.WOOL.isTagged(block.getType());
  }

  private class Level {

    private final Integer level;
    private final Integer ticks;
    private final Material primaryMaterial;
    private final HashMap<ExBlock, Material> blocks = new HashMap<>();

    protected Level(Integer level, Integer ticks) {
      this.level = level;
      this.ticks = ticks;

      int materialsNumber =
          (int) (ColorSwap.DIFFERENT_MATERIALS + (level - 1) * ColorSwap.DIFFERENT_MATERIALS_INCREASE);

      ArrayList<Material> materials = new ArrayList<>(ColorSwap.MATERIALS);
      ArrayList<Material> selectedMaterials = new ArrayList<>();

      for (int i = 0; i < materialsNumber; i++) {
        int index = ColorSwap.this.random.nextInt(materials.size());
        selectedMaterials.add(materials.get(index));
        materials.remove(index);
      }

      HashSet<Material> availableMaterials = new HashSet<>();

      int type = ColorSwap.this.random.nextInt(4);

      switch (type) {
        case 0 -> {
          for (List<ExBlock> blocks : ColorSwap.this.blocks) {
            for (ExBlock block : blocks) {
              if (this.blocks.containsKey(block)) {
                continue;
              }

              Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);

              for (int patternX = 0; patternX < 2; patternX++) {
                for (int patternZ = 0; patternZ < 2; patternZ++) {
                  ExBlock block2 = block.getExRelative(patternX, 0, patternZ);
                  if (ColorSwap.this.arena.contains(block2)) {
                    this.blocks.putIfAbsent(block2, material);
                  }
                }
              }
            }
          }
        }
        case 1 -> {
          int zCenter = ColorSwap.this.arena.getCenterBlock().getZ();

          for (List<ExBlock> blocks : ColorSwap.this.blocks) {
            Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
            availableMaterials.add(material);
            for (ExBlock block : blocks) {
              if (block.getZ() == zCenter) {
                material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
                availableMaterials.add(material);
              }
              this.blocks.put(block, material);
            }
          }
        }
        case 2 -> {
          for (List<ExBlock> blocks : ColorSwap.this.blocks) {
            for (ExBlock block : blocks) {
              Material material = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
              availableMaterials.add(material);
              this.blocks.put(block, material);
            }
          }
        }
        case 3 -> {
          for (List<ExBlock> blocks : ColorSwap.this.blocks) {
            Material material1 = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
            Material material2 = selectedMaterials.get(ColorSwap.this.random.nextInt(selectedMaterials.size()));
            availableMaterials.add(material1);
            availableMaterials.add(material2);

            boolean alt = true;
            for (ExBlock block : blocks) {
              if (alt) {
                this.blocks.put(block, material1);
                alt = false;
              } else {
                this.blocks.put(block, material2);
                alt = true;
              }
            }
          }
        }
      }

      //get primaryMaterial
      this.primaryMaterial =
          new ArrayList<>(availableMaterials).get(ColorSwap.this.random.nextInt(availableMaterials.size()));
    }

    public void switchPattern() {
      for (Map.Entry<ExBlock, Material> entry : this.blocks.entrySet()) {
        ExBlock block = entry.getKey();
        if (ColorSwap.this.isReplaceable(block)) {
          block.setType(entry.getValue());
        }
      }
    }

    public void switchPatternToPrimaryMaterial() {
      for (Map.Entry<ExBlock, Material> entry : this.blocks.entrySet()) {
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
