/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.game.microgames.game;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.event.CancelPriority;
import de.timesnake.basic.bukkit.util.user.event.UserBlockBreakEvent;
import de.timesnake.basic.bukkit.util.user.inventory.ExItemStack;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.game.microgames.game.basis.BoxedScoreGame;
import de.timesnake.game.microgames.main.GameMicroGames;
import de.timesnake.game.microgames.user.MicroGamesUser;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.WeightedRandomCollection;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OreMiner extends BoxedScoreGame<Integer> implements Listener {

  private static final float ORE_DENSITY = 0.05f;

  private static final Map<Material, Integer> ORE_POINTS = java.util.Map.of(
      Material.COAL_ORE, 1,
      Material.COPPER_ORE, 2,
      Material.IRON_ORE, 4,
      Material.GOLD_ORE, 6,
      Material.DIAMOND_ORE, 9,
      Material.EMERALD_ORE, 12
  );

  private static final ExItemStack PICKAXE = new ExItemStack(Material.NETHERITE_PICKAXE)
      .unbreakable()
      .setDropable(false)
      .setMoveable(false)
      .addExEnchantment(Enchantment.EFFICIENCY, 10)
      .immutable();

  private final WeightedRandomCollection<Material> oreWeights = new WeightedRandomCollection<Material>(this.random)
      .addAll(new Tuple<>(0.325, Material.COAL_ORE),
          new Tuple<>(0.25, Material.COPPER_ORE),
          new Tuple<>(0.175, Material.IRON_ORE),
          new Tuple<>(0.125, Material.GOLD_ORE),
          new Tuple<>(0.075, Material.DIAMOND_ORE),
          new Tuple<>(0.05, Material.EMERALD_ORE)
      );

  public OreMiner() {
    super("oreminer",
        "Ore Miner",
        Material.DEEPSLATE_DIAMOND_ORE,
        "Mine most valuable ores",
        List.of("§hGoal: §pmost ore points", "Mine ores to get points.", "More noble ores gives more points."),
        1,
        Duration.ofSeconds(60));

    Server.registerListener(this, GameMicroGames.getPlugin());
  }

  @Override
  public void prepare() {
    super.prepare();

    Collection<Block> blocks = this.getBlocksWithinBox();

    for (Block block : blocks) {
      if (this.random.nextFloat() < ORE_DENSITY) {
        Material ore = oreWeights.next();
        this.setOreVine(block, ore, 1.0 / (2 * ORE_POINTS.get(ore)), blocks);

      }
    }
  }

  private void setOreVine(Block block, Material ore, double chance, Collection<Block> blocks) {
    if (block.getType().equals(ore)) {
      return;
    }

    block.setType(ore);

    if (chance < 0.05) {
      return;
    }

    for (Block besideBlock : ExBlock.fromBlock(block).getBesideBlocks()) {
      if (this.random.nextFloat() < chance && blocks.contains(besideBlock)) {
        this.setOreVine(besideBlock, ore, chance / 2, blocks);
      }
    }
  }

  @Override
  protected void applyBeforeStart() {
    Server.getPreGameUsers().forEach(u -> u.setItem(PICKAXE));
  }

  @Override
  public void start() {
    super.start();

    Server.getInGameUsers().forEach(u -> {
      u.setGameMode(GameMode.SURVIVAL);
      u.addPotionEffect(PotionEffectType.NIGHT_VISION, 60 * 20, 1, false);
    });
  }

  @Override
  public void stop() {
    Server.getInGameUsers().forEach(u -> this.updateUserScoreOnSideboard((MicroGamesUser) u));
    this.calcPlaces(true);
    super.stop();
  }

  @Override
  public void reset() {
    super.reset();
    if (this.previousMap != null) {
      Server.getWorldManager().reloadWorld(this.previousMap.getWorld());
    }
  }

  @Override
  public boolean hasSideboard() {
    return true;
  }

  @Override
  public Integer getDefaultScore() {
    return 0;
  }

  @EventHandler
  public void onUserBreakBlock(UserBlockBreakEvent e) {
    if (!this.isGameRunning()) {
      return;
    }

    if (e.getUser().isService()) {
      return;
    }

    e.setCancelled(CancelPriority.HIGH, false);
    e.setDropItems(false);
    e.setExpToDrop(0);

    this.updateUserScore(e.getUser(), (u, v) -> v + ORE_POINTS.getOrDefault(e.getBlock().getType(), 0));
  }
}
